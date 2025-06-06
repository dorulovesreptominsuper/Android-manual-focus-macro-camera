package com.example.manualfocusmacrocamera.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.location.Location
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.manualfocusmacrocamera.data.UserPreferencesProtoRepository
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesProtoRepository
) : ViewModel() {
    private var camera: Camera? = null
    private var _cameraState: MutableStateFlow<CameraState.Type> =
        MutableStateFlow(CameraState.Type.PENDING_OPEN)
    val cameraState: StateFlow<CameraState.Type> = _cameraState
    val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private lateinit var imageCapture: ImageCapture
    private var _diopters = mutableFloatStateOf(0f)
    val diopters: State<Float> = _diopters
    private var hasFlashLight = false
    private var _isPermissionPurposeExplained = mutableStateOf(true)
    val isPermissionPurposeExplained: State<Boolean> = _isPermissionPurposeExplained
    private var _isInitialLightOn = mutableStateOf(false)
    val isInitialLightOn: State<Boolean> = _isInitialLightOn
    private var _isLightOn = mutableStateOf(false)
    val isLightOn: State<Boolean> = _isLightOn
    private var _isSaveGpsLocation = mutableStateOf(false)
    val isSaveGpsLocation: State<Boolean> = _isSaveGpsLocation

    init {
        viewModelScope.launch {
            userPreferencesRepository.userSettingsFlow.collect {
                _isPermissionPurposeExplained.value = it.isPermissionPurposeExplained
                _isInitialLightOn.value = it.isInitialLightOn
                _isSaveGpsLocation.value = it.isSaveGpsLocation
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalCamera2Interop::class)
    suspend fun setupCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
    ): Float {
        val cameraProvider = context.getCameraProvider()
        val logicalBackCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        // 論理カメラのID。論理カメラは複数の物理カメラにより構成される場合があり、マクロカメラはこのパターンに該当するはず
        // （つまり論理カメラIDをさらに分解してマクロカメラに該当する物理カメラIDを取得する必要がある）
        val backCameraId = camMgr.cameraIdList.firstOrNull {
            val characteristics = camMgr.getCameraCharacteristics(it)
            _diopters.floatValue =
                characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            val isBackCamera =
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            if (isBackCamera) {
                hasFlashLight =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            isBackCamera
        }.orEmpty()

        val capabilities = camMgr.getCameraCharacteristics(backCameraId)
            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val isLogicalMultiCamera =
            capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
        val targetCameraId = if (isLogicalMultiCamera) {
            val physicalCameraIds =
                camMgr.getCameraCharacteristics(backCameraId).physicalCameraIds
            // 明示的にマクロカメラを取得する方法はなさそうなので、複数あるカメラのうち最短焦点距離が短いものをマクロカメラと見なすことにしている。
            // LENS_INFO_MINIMUM_FOCUS_DISTANCE で取れる数値はディオプターという単位であり、数値が大きいほど焦点距離が短いそう。
            val consideredTheMostMacroLensIdAndMaximumFocus = physicalCameraIds.map { cameraId ->
                val characteristics = camMgr.getCameraCharacteristics(cameraId)
                val diopters =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                cameraId to diopters
            }.maxByOrNull { it.second ?: 0f }
            _diopters.floatValue = consideredTheMostMacroLensIdAndMaximumFocus?.second ?: 0f
            consideredTheMostMacroLensIdAndMaximumFocus?.first.orEmpty()
        } else {
            // 背面カメラが複数ではない場合はマクロカメラはないものと考えてもよければここはマクロ非対応のアラートを表示するロジックでいいが、デバイスの統一仕様がわからないので保留。
            backCameraId
        }

        val resolutionSelector = createResolutionSelector(targetCameraId)
        val previewBuilder = Preview.Builder()
        Camera2Interop.Extender(previewBuilder).setPhysicalCameraId(targetCameraId)
        val imageCaptureBuilder = ImageCapture.Builder()
        Camera2Interop.Extender(imageCaptureBuilder).setPhysicalCameraId(targetCameraId)
        imageCapture = imageCaptureBuilder
            .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .build()

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            logicalBackCameraSelector,
            previewBuilder.build().apply {
                surfaceProvider = previewView.surfaceProvider
            },
            imageCapture,
        ).apply {
            _isLightOn.value = isInitialLightOn.value
            cameraControl.enableTorch(isInitialLightOn.value)
            viewModelScope.launch {
                cameraInfo.cameraState.asFlow().collect {
                    _cameraState.value = it.type
                }
            }
        }

        return diopters.value
    }

    fun switchTorch(isOn: Boolean) {
        _isLightOn.value = isOn
        camera?.cameraControl?.enableTorch(isLightOn.value)
    }

    fun readPermissionPurposeExplanation() {
        viewModelScope.launch {
            userPreferencesRepository.updateIsPermissionPurposeExplained(true)
        }
    }

    fun setInitialLightOn(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIsInitialLightOn(value)
        }
    }

    fun setIfSaveGpsLocation(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIsSaveGpsLocation(value)
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setManualFocus(distance: Float) {
        val camera2Control = camera?.let { Camera2CameraControl.from(it.cameraControl) } ?: return
        val options = CaptureRequestOptions.Builder().setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_MACRO
        ).setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF
        ).setCaptureRequestOption(
            CaptureRequest.LENS_FOCUS_DISTANCE, distance
        ).build()

        camera2Control.captureRequestOptions = options
    }

    private fun getSupportedJpegSizes(cameraId: String): List<Size> {
        val characteristics = camMgr.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.toList() ?: emptyList()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun createResolutionSelector(cameraId: String): ResolutionSelector {
        // TODO: desiredSizeとAspectRatioは設定で選択できるようにする
        val aspectRatioStrategy = AspectRatioStrategy(
            AspectRatio.RATIO_16_9,
            AspectRatioStrategy.FALLBACK_RULE_AUTO
        )
        val resolutions = getSupportedJpegSizes(cameraId)
        val desiredSize = resolutions.maxByOrNull { it.width * it.height } ?: resolutions[0]
        val resolutionStrategy = ResolutionStrategy(
            desiredSize,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy)
            .setResolutionStrategy(resolutionStrategy)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun takePhoto(context: Context, onSaved: (Uri) -> Unit, onError: (Exception) -> Unit) {
        if (!::imageCapture.isInitialized) {
            onError(IllegalStateException("imageCapture not initialized"))
            return
        }

        val resolver = context.contentResolver
        val contentValues = createContentValues()
        val outputUri: Uri =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            resolver, outputUri, contentValues,
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    viewModelScope.launch {
                        val location = context.getCurrentLocation()

                        savedUri?.let {
                            if (isSaveGpsLocation.value) {
                                val pfd = resolver.openFileDescriptor(savedUri, "rw")
                                pfd?.use { descriptor ->
                                    val exif = ExifInterface(descriptor.fileDescriptor)
                                    if (location != null) {
                                        exif.setGpsInfo(location)
                                        exif.setAltitude(location.altitude)
                                        exif.saveAttributes()
                                    }
                                }
                            }

                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            resolver.update(it, values, null, null)
                            onSaved(it)
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    onError(exc)
                }
            }
        )
    }

    private fun createContentValues(): ContentValues {
        val photoDisplayName = formatCurrentTimeToJapaneseDateTimeString()
        val mimeType = "image/jpeg"

        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photoDisplayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    withContext(Dispatchers.IO) {
        ProcessCameraProvider.getInstance(this@getCameraProvider).get()
    }

suspend fun Context.getCurrentLocation(): Location? =
    suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            cont.resume(null)
        }
        client.lastLocation
            .addOnSuccessListener { location: Location? ->
                cont.resume(location)
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
    }

fun formatCurrentTimeToJapaneseDateTimeString(): String {
    val instant = Instant.ofEpochMilli(System.currentTimeMillis())
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH時mm分", Locale.JAPAN)
    return formatter.format(zonedDateTime)
}