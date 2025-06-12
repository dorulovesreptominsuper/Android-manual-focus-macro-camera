package com.example.manualfocusmacrocamera.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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

@Composable
internal fun ControlLight(
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