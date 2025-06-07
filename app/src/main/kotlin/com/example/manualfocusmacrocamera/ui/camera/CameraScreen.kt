package com.example.manualfocusmacrocamera.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraState
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manualfocusmacrocamera.ui.AnimatedAmplitudeWavyCircleButton
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
    showSnackbar: (String) -> Unit = {},
    showSnackbarWithCloseButton: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val previewView = remember { PreviewView(context) }

    var maxFocusDistance by remember { mutableFloatStateOf(0f) }
    var diopterFocusDepth by remember { mutableFloatStateOf(0f) }
    val swipeSensitivityFactor = 0.03f // スワイプしたPX数にかける係数。実機で動かしてちょうどいい感じの数字がこの辺り。

    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
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
    val isInitialized by viewModel.isUserSettingsReady.collectAsStateWithLifecycle()

    if (!isInitialized) {
        LoadingScreen()
    } else if (!permissionsTermFinished) {
        PermissionsTerm(
            isDialogShown = viewModel.isPermissionPurposeExplained ?: false,
            cameraPermissionsGranted = hasCameraPermission,
            locationPermissionsGranted = hasLocationPermission,
            onDialogOkClick = {
                viewModel.readPermissionPurposeExplanation()
            },
            onPermissionRequestFinished = {
                permissionsTermFinished = true
            },
            onLocationPermissionsDenied = {
                showSnackbarWithCloseButton("位置情報の権限が拒否されたので写真にGPS情報を付与する機能はOFFになります。\nONにしたい場合は端末の設定アプリから位置情報権限を許可してね。")
            }
        )
    } else {

        LaunchedEffect(hasCameraPermission) {
            if (hasCameraPermission) {
                maxFocusDistance = viewModel.setupCamera(previewView, lifecycleOwner)
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { diopterFocusDepth }.collect { depth ->
                viewModel.setManualFocus(depth)
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { previewView },
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
                    viewModel.switchTorch(viewModel.isLightOn.value)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(cutout),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val currentFocusDistanceCm = 100 / diopterFocusDepth
                        val text = String.format(Locale.US, "%.1f", 100 / diopterFocusDepth)

                        Text(
                            buildAnnotatedString {
                                append("フォーカス距離 : ")
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                                append(text)
                                if (currentFocusDistanceCm.isFinite()) append("cm")
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(320.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragPxAmount ->
                                        change.consume() // イベントを消費して他のジェスチャーに影響を与えないようにする
                                        val newFocusDistance =
                                            (diopterFocusDepth - dragPxAmount * swipeSensitivityFactor)
                                                .coerceIn(
                                                    0f,
                                                    maxFocusDistance
                                                ) // 0f から maxFocusDistance の範囲に制限
                                        diopterFocusDepth = newFocusDistance
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            viewModel.takePhoto(
                                                context = context,
                                                isLocationEnabled = hasLocationPermission,
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
                            isLightOn = viewModel.isLightOn.value,
                            onClick = viewModel::switchTorch,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(modifier = Modifier.size(100.dp))
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
