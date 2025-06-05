package com.example.manualfocusmacrocamera.ui.camera

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraState
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var maxFocusDistance by remember { mutableFloatStateOf(0f) }
    var focusDistance by remember { mutableFloatStateOf(0f) }
    val swipeSensitivityFactor = 0.03f // スワイプしたPX数にかける係数。実機で動かしてちょうどいい感じの数字がこの辺り。

    val cutout = WindowInsets.displayCutout.asPaddingValues()

    val tets by viewModel.cameraState.collectAsStateWithLifecycle()
    val isCompleteCameraInitialize by remember(tets) { mutableStateOf(tets == CameraState.Type.OPEN) }

    LaunchedEffect(Unit) {
        maxFocusDistance = viewModel.setupCamera(previewView, lifecycleOwner)
    }

    LaunchedEffect(focusDistance) {
        viewModel.setManualFocus(focusDistance)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
        )

        if (!isCompleteCameraInitialize) {
            LoadingIndicator(modifier = Modifier.size(100.dp))
        } else {
            viewModel.switchTorch(viewModel.isLightOn.value)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cutout),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentFocusDistance = 100 / focusDistance
                val text = String.format(Locale.US, "%.1f", 100 / focusDistance)

                Text(
                    buildAnnotatedString {
                        append("フォーカス距離 : ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                        append(text)
                        if (currentFocusDistance.isFinite()) append("cm")
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
                                    (focusDistance - dragPxAmount * swipeSensitivityFactor)
                                        .coerceIn(
                                            0f,
                                            maxFocusDistance
                                        ) // 0f から maxFocusDistance の範囲に制限
                                focusDistance = newFocusDistance
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    try {
                                        viewModel.takePhoto(
                                            context,
                                            onSaved = { uri ->
                                                showSnackbar("無事写真を保存できた！")
                                            },
                                            onError = { error ->
                                                showSnackbar("失敗：${error.cause}")
                                            }
                                        )
                                    } catch (e: Exception) {
                                        showSnackbar("位置情報取得エラー: ${e.localizedMessage}")
                                    }
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
