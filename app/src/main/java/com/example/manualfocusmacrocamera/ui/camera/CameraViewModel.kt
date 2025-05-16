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
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    var diopters: Float = 0f
        private set
    private var camera: Camera? = null
    private lateinit var imageCapture: ImageCapture
    private var hasFlashLight = false
    private var isLightOn = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalCamera2Interop::class)
    suspend fun setupCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        enableTorch: Boolean = true,
    ): Float {
        val cameraProvider = context.getCameraProvider()
        val logicalBackCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 論理カメラのID。論理カメラは複数の物理カメラにより構成される場合があり、マクロカメラはこのパターンに該当するはず
        // （つまり論理カメラIDをさらに分解してマクロカメラに該当する物理カメラIDを取得する必要がある）
        val backCameraId = camMgr.cameraIdList.firstOrNull {
            val characteristics = camMgr.getCameraCharacteristics(it)
            diopters =
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
            diopters = consideredTheMostMacroLensIdAndMaximumFocus?.second ?: 0f
            consideredTheMostMacroLensIdAndMaximumFocus?.first.orEmpty()
        } else {
            // 背面カメラが複数ではない場合はマクロカメラはないものと考えてもよければここはマクロ非対応のアラートを表示するロジックでいいが、デバイスの統一仕様がわからないので保留。
            backCameraId
        }

        val previewBuilder = Preview.Builder()
        Camera2Interop.Extender(previewBuilder).setPhysicalCameraId(targetCameraId)
        val imageCaptureBuilder = ImageCapture.Builder()
        Camera2Interop.Extender(imageCaptureBuilder).setPhysicalCameraId(targetCameraId)
        imageCapture = imageCaptureBuilder.build()

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            logicalBackCameraSelector,
            previewBuilder.build().apply {
                surfaceProvider = previewView.surfaceProvider
            },
            imageCapture,
        ).apply {
            cameraControl.enableTorch(enableTorch)
            isLightOn = enableTorch
        }
        return diopters
    }

    fun switchTorch() {
        isLightOn = !isLightOn
        camera?.cameraControl?.enableTorch(isLightOn)
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
                            val pfd = resolver.openFileDescriptor(savedUri, "rw")
                            pfd?.use { descriptor ->
                                val exif = ExifInterface(descriptor.fileDescriptor)
                                if (location != null) {
                                    exif.setGpsInfo(location)
                                    exif.setAltitude(location.altitude)
                                    exif.saveAttributes()
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
        val photoTitle = "タイトル_${System.currentTimeMillis()}"
        val photoDisplayName = "${System.currentTimeMillis()}撮影.jpg"
        val mimeType = "image/jpeg"

        return ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, photoTitle)
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
