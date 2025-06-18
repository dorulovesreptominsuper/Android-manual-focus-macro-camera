package com.example.manualfocusmacrocamera.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
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
import android.util.Log
import android.util.Size
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manualfocusmacrocamera.data.UserPreferences
import com.example.manualfocusmacrocamera.ui.CameraUiState
import com.example.manualfocusmacrocamera.ui.SettingsBottomSheet
import com.example.manualfocusmacrocamera.ui.settings.UserSettingsViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MacroCameraInfo(
    val minimumFocusDistance: Float,
    val minimumZoomRatio: Float,
    val maximumZoomRatio: Float,
)


@OptIn(ExperimentalCamera2Interop::class)
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel = hiltViewModel(),
    settingsViewModel: UserSettingsViewModel = hiltViewModel(),
    showSnackbar: (String) -> Unit = {},
    clickedVolumeKey: Pair<Int, Long>,
    showSheet: Boolean,
    setShowSheet: (Boolean) -> Unit,
) {
    val settings by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(settings) {
        if (settings == null) {
            cameraViewModel.updateUiState(CameraUiState.CheckPermission)
        }
    }
    settings?.let {
        CameraScreenContent(
            uiState = uiState,
            userPreferences = it,
            modifier = modifier,
            cameraViewModel = cameraViewModel,
            showSnackbar = showSnackbar,
            clickedVolumeKey = clickedVolumeKey,
            showSheet = showSheet,
            setShowSheet = setShowSheet,
        )
    } ?: LoadingScreen()
}


@OptIn(ExperimentalCamera2Interop::class)
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun CameraScreenContent(
    uiState: CameraUiState,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel = hiltViewModel(),
    settingsViewModel: UserSettingsViewModel = hiltViewModel(),
    showSnackbar: (String) -> Unit = {},
    clickedVolumeKey: Pair<Int, Long>,
    showSheet: Boolean,
    setShowSheet: (Boolean) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLightOn by remember { mutableStateOf(userPreferences.isInitialLightOn) }

    LaunchedEffect(uiState) {
        Log.d("デバッグ：UIState", "$uiState")
    }

    when (uiState) {
        CameraUiState.DataStoreFetching -> {
            LoadingScreen()
        }

        CameraUiState.CheckPermission -> {
            val isFirstAppLaunch by remember { mutableStateOf(!userPreferences.isPermissionPurposeExplained) }

            CheckPermissions(
                shouldShowPermissionExplanationDialog = isFirstAppLaunch,
                onDialogOkClick = { settingsViewModel.updateIsPermissionPurposeExplained(true) }
            ) { hasCameraPermission ->
                if (hasCameraPermission) {
                    cameraViewModel.updateUiState(CameraUiState.SetupCamera)
                } else {
                    cameraViewModel.updateUiState(CameraUiState.NoCameraPermission)
                }
            }
        }

        is CameraUiState.PermissionRequesting -> {
            RequestPermissions(
                targetPermissions = uiState.targetPermissions,
                onPermissionRequestFinished = {
                    if (it) {
                        cameraViewModel.updateUiState(CameraUiState.SetupCamera)
                    } else {
                        cameraViewModel.updateUiState(CameraUiState.NoCameraPermission)
                    }
                }
            )
        }

        CameraUiState.NoCameraPermission -> {
            var isLifecycleStopped by remember { mutableStateOf(false) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && isLifecycleStopped) {
                        cameraViewModel.updateUiState(CameraUiState.CheckPermission)
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            AskOpenAppSettingsForPermissionLayout(
                onOpenSettingClicked = {
                    isLifecycleStopped = true
                }
            )
        }

        CameraUiState.SetupCamera -> {
            Setup(
                settings = userPreferences,
            ) { previewView, camera, capture, macroCameraInfo ->
                cameraViewModel.updateUiState(
                    CameraUiState.CameraOpened(previewView, camera, capture, macroCameraInfo)
                )
            }
        }

        is CameraUiState.CameraOpened -> {
            val context = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current.lifecycle

            val previewView = uiState.previewView
            val camera = uiState.camera
            val imageCapture = uiState.capture
            val macroCameraInfo = uiState.cameraInfo

            var hasLocationPermission by remember {
                mutableStateOf(
                    context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                            context.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }

            var hasFlashLight by remember { mutableStateOf(camera.cameraInfo.hasFlashUnit()) }

            var diopterFocusDepth by remember { mutableFloatStateOf(0f) }
            var currentZoomRatio by remember { mutableFloatStateOf(1f) }

            val switchTorch: (Boolean) -> Unit = { isOn ->
                isLightOn = isOn
                camera.cameraControl.enableTorch(isOn)
            }

            DisposableEffect(lifecycleOwner) {
                var isStopped = false

                val observer = LifecycleEventObserver { _, event ->
                    Log.d("デバッグ：Composeライフサイクル", "Event: $event $isStopped")
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            if (isStopped) {
                                camera.cameraControl.enableTorch(isLightOn)
                                isStopped = false
                            }
                        }

                        Lifecycle.Event.ON_STOP -> {
                            isStopped = true
                        }

                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)

                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(currentZoomRatio) {
                camera.cameraControl.setZoomRatio(currentZoomRatio)
            }

            LaunchedEffect(Unit) {
                snapshotFlow { diopterFocusDepth }.collect { depth ->
                    val camera2Control = Camera2CameraControl.from(camera.cameraControl)
                    val options = CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE,
                            CameraMetadata.CONTROL_AF_MODE_MACRO
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE,
                            CameraMetadata.CONTROL_AF_MODE_OFF
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.LENS_FOCUS_DISTANCE,
                            depth
                        )
                        .build()
                    camera2Control.captureRequestOptions = options
                }
            }

            LaunchedEffect(clickedVolumeKey) {
                val keyCode = clickedVolumeKey.first

                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        currentZoomRatio = (currentZoomRatio + 0.25f).coerceAtMost(
                            minOf(macroCameraInfo.maximumZoomRatio, 2f)
                        )
                    }

                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        currentZoomRatio = (currentZoomRatio - 0.25f).coerceAtLeast(
                            maxOf(macroCameraInfo.minimumZoomRatio, 0.5f)
                        )
                    }
                }
            }

            SettingsBottomSheet(
                settings = userPreferences,
                macroCameraInfo = macroCameraInfo,
                showSheet = showSheet,
                setShowSheet = setShowSheet,
                onInitialLightStateChanged = {
                    settingsViewModel.updateIsInitialLightOn(it)
                },
                onGpsLocationStateChanged = {
                    settingsViewModel.updateIsSaveGpsLocation(it)
                },
                onPreviewFullScreenStateChanged = {
                    settingsViewModel.updateIsPreviewFullScreen(it)
                },
                onAspectChanged = {
                    settingsViewModel.updateAspect(it) {
                        cameraViewModel.updateUiState(CameraUiState.SetupCamera) // ImageCaptureへの操作なのでセットアップの工程をやり直す必要がある
                    }
                },
                onQualityChanged = {
                    settingsViewModel.updateQuality(it) {
                        cameraViewModel.updateUiState(CameraUiState.SetupCamera) // ImageCaptureへの操作なのでセットアップの工程をやり直す必要がある
                    }
                },
            )

            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.scaleType =
                            if (userPreferences.isPreviewFullScreen) PreviewView.ScaleType.FILL_CENTER else PreviewView.ScaleType.FIT_CENTER
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.displayCutout.asPaddingValues()), // 画面上にレンズなどがある場合そこを避ける
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    val currentFocusDistanceCm = 100 / diopterFocusDepth

                    FocusDepthAndZoomRatio(
                        currentFocusDistanceCm = currentFocusDistanceCm,
                        currentZoomRatio = currentZoomRatio
                    )
                    GestureArea(
                        modifier = Modifier.weight(1f),
                        onVerticalDrag = { correctedDragAmount ->
                            val newValue = (diopterFocusDepth + correctedDragAmount)
                                .coerceIn(0f, macroCameraInfo.minimumFocusDistance)
                            diopterFocusDepth = newValue
                        },
                        onDoubleTap = {
                            takePhoto(
                                context = context,
                                scope = coroutineScope,
                                imageCapture = imageCapture,
                                isLocationEnabled =
                                    hasLocationPermission && userPreferences.isSaveGpsLocation,
                                onSaved = { uri ->
                                    showSnackbar("写真を保存しました！")
                                },
                                onError = { error ->
                                    showSnackbar("失敗：${error.message}")
                                }
                            )
                        },
                    )
                    if (hasFlashLight) {
                        LightOnOffButton(
                            isLightOn = isLightOn,
                            onInitialize = { camera.cameraControl.enableTorch(isLightOn) },
                            onClick = switchTorch,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Setup(
    settings: UserPreferences,
    onSetupCompleted: (PreviewView, Camera, ImageCapture, MacroCameraInfo) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var macroCameraInfo by remember { mutableStateOf(MacroCameraInfo(0f, 0f, 0f)) }
    var type by remember { mutableStateOf(CameraState.Type.CLOSED) }

    LaunchedEffect(type) {
        if (type == CameraState.Type.OPEN && camera != null && imageCapture != null) {
            onSetupCompleted(
                requireNotNull(previewView),
                requireNotNull(camera),
                requireNotNull(imageCapture),
                macroCameraInfo
            )
        }
    }

    LoadingScreen()

    context.SetupCamera(
        isPreviewFullScreen = settings.isPreviewFullScreen,
        aspectRatio = settings.aspect,
        quality = settings.quality,
        onSetupCompleted = { pre, cam, cap, info ->
            previewView = pre
            camera = cam
            imageCapture = cap
            macroCameraInfo = info
            type = cam.cameraInfo.cameraState.value?.type ?: CameraState.Type.CLOSED
            cam.cameraInfo.cameraState.observe(
                lifecycleOwner,
                { state ->
                    type = state.type
                }
            )
        }
    )
}

@OptIn(ExperimentalCamera2Interop::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Context.SetupCamera(
    isPreviewFullScreen: Boolean,
    aspectRatio: UserPreferences.AspectRatio,
    quality: UserPreferences.Quality,
    onSetupCompleted: (PreviewView, Camera, ImageCapture, MacroCameraInfo) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(this).apply {
            scaleType =
                if (isPreviewFullScreen) PreviewView.ScaleType.FILL_CENTER else PreviewView.ScaleType.FIT_CENTER
        }
    }

    LaunchedEffect(Unit) {
        try {
            val result = SetupCamera(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                aspectRatio = aspectRatio,
                quality = quality,
            )
            if (result.first == null) {
                throw Exception("Camera or ImageCapture is null")
            } else {
                onSetupCompleted(previewView, result.first!!, result.second, result.third)
            }
        } catch (e: Exception) {
            throw e
        }
    }
}

fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission,
    ) == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalCamera2Interop::class)
suspend fun Context.SetupCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    aspectRatio: UserPreferences.AspectRatio,
    quality: UserPreferences.Quality,
): Triple<Camera?, ImageCapture, MacroCameraInfo> {
    val camMgr = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraProvider = getCameraProvider()
    val logicalBackCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // 論理カメラのID。論理カメラは複数の物理カメラにより構成される場合があり、マクロカメラはこのパターンに該当するはず
    // （つまり論理カメラIDをさらに分解してマクロカメラに該当する物理カメラIDを取得する必要がある）
    val backCameraId = camMgr.cameraIdList.firstOrNull {
        val characteristics = camMgr.getCameraCharacteristics(it)
        val isBackCamera =
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
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
        consideredTheMostMacroLensIdAndMaximumFocus?.first.orEmpty()
    } else {
        // 背面カメラが複数ではない場合はマクロカメラはないものと考えてもよければここはマクロ非対応のアラートを表示するロジックでいいが、デバイスの統一仕様がわからないので保留。
        backCameraId
    }

    val resolutionSelector =
        createResolutionSelector(
            camMgr,
            cameraId = targetCameraId,
            aspectRatio = aspectRatio,
            quality = quality
        )
    val previewBuilder = CameraPreview.Builder().setResolutionSelector(resolutionSelector)
    Camera2Interop.Extender(previewBuilder).setPhysicalCameraId(targetCameraId)
    val imageCaptureBuilder = ImageCapture.Builder()
    Camera2Interop.Extender(imageCaptureBuilder).setPhysicalCameraId(targetCameraId)
    val imageCapture = imageCaptureBuilder
        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setResolutionSelector(resolutionSelector)
        .build()

    cameraProvider.unbindAll()
    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        logicalBackCameraSelector,
        previewBuilder.build().apply {
            surfaceProvider = previewView.surfaceProvider
        },
        imageCapture
    )

    val macroCameraInfo = MacroCameraInfo(
        minimumFocusDistance = camMgr.getCameraCharacteristics(targetCameraId)
            .get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f,
        minimumZoomRatio = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 0f,
        maximumZoomRatio = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 0f,
    )

    return Triple(camera, imageCapture, macroCameraInfo)
}

@OptIn(ExperimentalCamera2Interop::class)
private fun createResolutionSelector(
    camMgr: CameraManager,
    cameraId: String,
    aspectRatio: UserPreferences.AspectRatio,
    quality: UserPreferences.Quality,
): ResolutionSelector {
    val aspectRatioStrategy = AspectRatioStrategy(
        aspectRatio.ordinal,
        AspectRatioStrategy.FALLBACK_RULE_AUTO
    )
    val resolutions =
        getSupportedJpegSizes(camMgr, cameraId).ifEmpty { throw IllegalStateException() }
    val desiredSize = when (quality) {
        UserPreferences.Quality.HIGH -> {
            resolutions.maxByOrNull { it.width * it.height } ?: resolutions[0]
        }

        UserPreferences.Quality.MIDDLE -> {
            resolutions.sortedByDescending { it.width * it.height }[resolutions.size / 4]
        }

        UserPreferences.Quality.LOW -> {
            resolutions.sortedByDescending { it.width * it.height }[resolutions.size / 2]
        }
    }
    val resolutionStrategy = ResolutionStrategy(
        desiredSize,
        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
    )
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(aspectRatioStrategy)
        .setResolutionStrategy(resolutionStrategy)
        .build()
}

private fun getSupportedJpegSizes(camMgr: CameraManager, cameraId: String): List<Size> {
    val characteristics = camMgr.getCameraCharacteristics(cameraId)
    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    return map?.getOutputSizes(ImageFormat.JPEG)?.toList() ?: emptyList()
}

@RequiresApi(Build.VERSION_CODES.Q)
fun takePhoto(
    context: Context,
    scope: CoroutineScope,
    imageCapture: ImageCapture,
    isLocationEnabled: Boolean,
    onSaved: (Uri) -> Unit,
    onError: (Exception) -> Unit
) {
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
                savedUri?.let {
                    if (isLocationEnabled) {
                        // Launch a coroutine to handle location
                        scope.launch {
                            try {
                                val location = context.getCurrentLocation()
                                withContext(Dispatchers.IO) {
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

                                // Update the media store
                                withContext(Dispatchers.IO) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Images.Media.IS_PENDING, 0)
                                    }
                                    resolver.update(it, values, null, null)
                                }

                                // Notify success
                                withContext(Dispatchers.Main) {
                                    onSaved(it)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    onError(e)
                                }
                            }
                        }
                    } else {
                        // No location needed, just update and notify
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
                // ロケーションのパーミッションが2種類あるためそれぞれで重複してresumeされてしまいクラッシュするのを防止
                if (cont.isActive) cont.resumeWithException(e)
            }
    }

fun formatCurrentTimeToJapaneseDateTimeString(): String {
    val instant = Instant.ofEpochMilli(System.currentTimeMillis())
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH時mm分", Locale.JAPAN)
    return formatter.format(zonedDateTime)
}
