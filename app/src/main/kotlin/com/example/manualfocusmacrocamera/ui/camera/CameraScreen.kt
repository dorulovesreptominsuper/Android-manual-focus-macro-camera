package com.example.manualfocusmacrocamera.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraState
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.RoundedPolygon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manualfocusmacrocamera.ui.AnimatedAmplitudeWavyCircleButton
import com.example.manualfocusmacrocamera.ui.settings.UserSettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel = hiltViewModel(),
    settingsViewModel: UserSettingsViewModel = hiltViewModel(),
    showSnackbar: (String) -> Unit = {},
    showSnackbarWithCloseButton: (String) -> Unit = {},
    clickedVolumeKey: Pair<Int, Long>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val previewView = remember { PreviewView(context) }

    val settings by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val isSettingFetched by remember(settings) { mutableStateOf(settingsViewModel.hasReady) }

    val macroCameraInfo by cameraViewModel.macroCameraInfo.collectAsStateWithLifecycle()
    var diopterFocusDepth by remember { mutableFloatStateOf(0f) }
    val swipeSensitivityFactor = 0.03f // スワイプしたPX数にかける係数。実機で動かしてちょうどいい感じの数字がこの辺り。

    val cameraState by cameraViewModel.cameraState.collectAsStateWithLifecycle()
    val isCameraOpened by remember(cameraState) {
        mutableStateOf(cameraState == CameraState.Type.OPEN)
    }
    var permissionsTermFinished by remember { mutableStateOf(false) }
    var hasCameraPermission by remember(permissionsTermFinished) {
        mutableStateOf(context.checkPermission(Manifest.permission.CAMERA))
    }
    var hasLocationPermission by remember(permissionsTermFinished) {
        mutableStateOf(
            context.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    context.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }

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

    ControlLight(
        lifecycleOwner = lifecycleOwner,
        isCameraOpened = isCameraOpened,
        onAppLaunch = { cameraViewModel.switchTorch(settings.isInitialLightOn) },
        onAppResume = cameraViewModel::resumeTorch,
    )

    if (!isSettingFetched) {
        LoadingScreen()
    } else if (!permissionsTermFinished) {
        PermissionsTerm(
            isDialogShown = settings.isPermissionPurposeExplained,
            cameraPermissionsGranted = hasCameraPermission,
            locationPermissionsGranted = hasLocationPermission,
            onDialogOkClick = {
                settingsViewModel.updateIsPermissionPurposeExplained(true)
            },
            onPermissionRequestFinished = {
                permissionsTermFinished = true
            },
            onLocationPermissionsDenied = {
                showSnackbarWithCloseButton("位置情報の権限が拒否されたので写真にGPS情報を付与する機能はOFFになります。\nONにしたい場合は端末の設定アプリから位置情報権限を許可してね。")
            }
        )
    } else {

        LaunchedEffect(hasCameraPermission, settings, currentZoomRatio) {
            if (hasCameraPermission) {
                cameraViewModel.setZoomRatio(currentZoomRatio)
                cameraViewModel.setupCamera(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    settings = settings,
                )
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { diopterFocusDepth }.collect { depth ->
                cameraViewModel.setManualFocus(depth)
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = {
                    previewView.apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            when {
                hasCameraPermission.not() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "カメラの権限がないので撮影ができません。カメラ権限を付与するために設定アプリを開きますか？")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "（権限を付与したい場合は下のボタンから、画面上の「権限」欄をタップして「アプリの権限」画面を開き、「カメラ」「位置情報」をそれぞれタップして「許可」の選択肢を選んでください。）",
                            color = Color.LightGray,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                context.startActivity(intent)
                            },
                            shape = MaterialTheme.shapes.largeIncreased,
                        ) {
                            Text(text = "アプリ情報画面を開く")
                        }
                    }
                }

                !isCameraOpened -> {
                    LoadingScreen()
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(cutout),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val currentFocusDistanceCm = 100 / diopterFocusDepth
                        val text = String.format(Locale.US, "%.1f", 100 / diopterFocusDepth)

                        FlowRow(
                            modifier = Modifier
                                .background(color = Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                buildAnnotatedString {
                                    append("焦点距離 : ")
                                    pushStyle(
                                        SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    )
                                    append(text)
                                    if (currentFocusDistanceCm.isFinite()) append("cm")
                                },
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                buildAnnotatedString {
                                    append("ズーム倍率 : ")
                                    pushStyle(
                                        SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    )
                                    append("x")
                                    append(String.format(Locale.US, "%.2f", currentZoomRatio))
                                },
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragPxAmount ->
                                        change.consume() // イベントを消費して他のジェスチャーに影響を与えないようにする
                                        val newFocusDistance =
                                            (diopterFocusDepth - dragPxAmount * swipeSensitivityFactor)
                                                .coerceIn(0f, macroCameraInfo.minimumFocusDistance)
                                        diopterFocusDepth = newFocusDistance
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            cameraViewModel.takePhoto(
                                                context = context,
                                                isLocationEnabled =
                                                    hasLocationPermission && settings.isSaveGpsLocation,
                                                onSaved = { uri ->
                                                    showSnackbar("無事写真を保存できた！")
                                                },
                                                onError = { error ->
                                                    showSnackbar("失敗：${error.cause}")
                                                }
                                            )
                                        },
                                    )
                                },
                        )
                        LightOnOffButton(
                            isLightOn = cameraViewModel.isLightOn.value,
                            onClick = cameraViewModel::switchTorch,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlLight(
    isCameraOpened: Boolean,
    lifecycleOwner: LifecycleOwner,
    onAppLaunch: () -> Unit,
    onAppResume: () -> Unit,
) {
    var isLaunchedApp by remember { mutableStateOf(true) }

    LaunchedEffect(isCameraOpened) {
        if (isLaunchedApp && isCameraOpened) {
            isLaunchedApp = false
            onAppLaunch()
        }
    }
    // onStop時に勝手にライトが消えてしまうのでonResumeかつカメラOPEN時に再度付け直す
    DisposableEffect(lifecycleOwner, isCameraOpened) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onAppResume()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ContainedLoadingIndicator(
            modifier = Modifier.size(100.dp),
            indicatorColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.onPrimary,
            polygons = listOf(
                MaterialShapes.Clover8Leaf,
                MaterialShapes.Clover4Leaf,
            )
        )
    }
}

@Composable
private fun LightOnOffButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 80.dp,
    isLightOn: Boolean,
    onClick: (Boolean) -> Unit,
) {
    val sizeModifier = modifier.size(buttonSize)
    if (isLightOn) {
        AnimatedAmplitudeWavyCircleButton(
            modifier = sizeModifier,
            onClick = { onClick(!isLightOn) },
            imageVector = Icons.Default.FlashlightOn,
        )
    } else {
        Button(
            modifier = sizeModifier,
            onClick = { onClick(!isLightOn) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors().copy(containerColor = Color.Gray)
        ) {
            Icon(
                imageVector = Icons.Default.FlashlightOff,
                contentDescription = ""
            )
        }
    }
}

private fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission,
    ) == PackageManager.PERMISSION_GRANTED
}

@Preview
@Composable
fun LightOnOffButtonPreview() {
    LightOnOffButton(isLightOn = true, onClick = {})
}

@Preview
@Composable
fun LightOffButtonPreview() {
    LightOnOffButton(isLightOn = false, onClick = {})
}
