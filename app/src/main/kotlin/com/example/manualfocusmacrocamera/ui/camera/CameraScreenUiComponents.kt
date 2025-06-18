package com.example.manualfocusmacrocamera.ui.camera

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manualfocusmacrocamera.ui.AnimatedAmplitudeWavyCircleButton
import java.util.Locale


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoadingScreen() {
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
internal fun LightOnOffButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 80.dp,
    isLightOn: Boolean,
    onInitialize: () -> Unit = {},
    onClick: (Boolean) -> Unit,
) {
    LaunchedEffect(Unit) {
        onInitialize()
    }

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

@Composable
internal fun FocusDepthAndZoomRatio(
    currentFocusDistanceCm: Float,
    currentZoomRatio: Float
) {
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
                append(String.format(Locale.US, "%.1f", currentFocusDistanceCm))
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
}

@Composable
internal fun GestureArea(
    modifier: Modifier = Modifier,
    onVerticalDrag: (Float) -> Unit,
    onDoubleTap: () -> Unit,
) {
    val swipeSensitivityFactor = 0.03f // スワイプしたPX数にかける係数。実機で動かしてちょうどいい感じの数字がこの辺り。

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragPxAmount ->
                    change.consume() // イベントを消費して他のジェスチャーに影響を与えないようにする

                    onVerticalDrag(-dragPxAmount * swipeSensitivityFactor)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() }
                )
            },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AskOpenAppSettingsForPermissionLayout(
    onOpenSettingClicked: () -> Unit = {}
) {
    val context = LocalContext.current

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
                onOpenSettingClicked()

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

@ComposePreview
@Composable
fun LightOnOffButtonPreview() {
    LightOnOffButton(isLightOn = true, onClick = {})
}

@ComposePreview
@Composable
fun LightOffButtonPreview() {
    LightOnOffButton(isLightOn = false, onClick = {})
}
