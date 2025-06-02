package com.example.manualfocusmacrocamera.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledSplitButton() {
    var checked by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { /* アクション */ },
            ) {
                Icon(
                    Icons.Filled.Edit,
                    modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    contentDescription = null,
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("編集")
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = checked,
                onCheckedChange = { checked = it },
                modifier = Modifier.semantics {
                    stateDescription = if (checked) "展開中" else "折りたたみ中"
                    contentDescription = "トグルボタン"
                },
            ) {
                val rotation: Float by animateFloatAsState(
                    targetValue = if (checked) 180f else 0f,
                    label = "Trailing Icon Rotation"
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    modifier = Modifier
                        .size(SplitButtonDefaults.TrailingIconSize)
                        .graphicsLayer { rotationZ = rotation },
                    contentDescription = null
                )
            }

            // ▼ ドロップダウンメニュー
            DropdownMenu(
                expanded = checked,
                onDismissRequest = { checked = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Option 1") },
                    onClick = {
                        checked = false
                        // アクション 1
                    }
                )
                DropdownMenuItem(
                    text = { Text("Option 2") },
                    onClick = {
                        checked = false
                        // アクション 2
                    }
                )
            }
        }
    )
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectableShapeButton() {
    var selected by remember { mutableStateOf(false) }

    // 状態によってshapeを切り替え
    val unselectedShape: CornerBasedShape = RoundedCornerShape(16.dp)
    val selectedShape: CornerBasedShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 32.dp,
        bottomEnd = 0.dp,
        bottomStart = 32.dp
    )

    Button(
        onClick = { selected = !selected },
        shape = if (selected) selectedShape else unselectedShape,
        modifier = Modifier
            .padding(16.dp)
            .height(56.dp)
    ) {
        Text(if (selected) "選択中（角の形状が変化）" else "未選択")
    }
}


class AnimatedWavyCircleShape(
    private val waveCount: Int = 12,
    private val waveAmplitude: Float = 10f, // px
    private val phase: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path()
        val radius = size.minDimension / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val points = 180
        for (i in 0..points) {
            val theta = 2 * PI * i / points
            // phaseを追加して波が動くように
            val r = radius + waveAmplitude * sin(waveCount * theta + phase)
            val x = centerX + r * cos(theta)
            val y = centerY + r * sin(theta)
            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun AnimatedWavyCircleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    imageVector: ImageVector = Icons.Filled.Favorite,
) {
    // 位相（波の進み具合）を無限アニメーション
    var phase by remember { mutableStateOf(0f) }

    // アニメーション用の永続コルーチン
    LaunchedEffect(Unit) {
        while (true) {
            // 波が1秒で一周するスピード
            kotlinx.coroutines.delay(16L) // 約60FPS
            phase += 0.12f
        }
    }

    Button(
        onClick = onClick,
        shape = AnimatedWavyCircleShape(
            waveCount = 10,
            waveAmplitude = 8f,
            phase = phase
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = ""
        )
    }
}

class AnimatedAmplitudeWavyCircleShape(
    private val waveCount: Int = 12,
    private val amplitude: Float = 0f, // 0=まん丸, >0=波
    private val phase: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path()
        val radius = size.minDimension / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val points = 180
        for (i in 0..points) {
            val theta = 2 * PI * i / points
            val r = radius + amplitude * sin(waveCount * theta + phase)
            val x = centerX + r * cos(theta)
            val y = centerY + r * sin(theta)
            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun AnimatedAmplitudeWavyCircleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    imageVector: ImageVector,
) {
    // 0でまん丸、最大で波打つ高さ
    var t by remember { mutableFloatStateOf(0f) }
    val maxAmplitude = 10f  // 波の最大の高さ
    val waveCount = 10

    // アニメーションコルーチン
    LaunchedEffect(Unit) {
        while (true) {
            // tは0～2πを周期的に増やす
            kotlinx.coroutines.delay(16L) // 約60fps
            t += 0.06f
            if (t > 2 * PI) t -= (2 * PI).toFloat()
        }
    }

    // 高さを周期的に 0→最大→0 になるようsinで変化
    val amplitude = (sin(t) * 0.5f + 0.5f) * maxAmplitude
    // 波の位相も進めてぐるぐる波を動かす
    val phase = t * 2f

    Button(
        onClick = onClick,
        shape = AnimatedAmplitudeWavyCircleShape(
            waveCount = waveCount,
            amplitude = amplitude,
            phase = phase
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = ""
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun FilledSplitButtonPreview() {
    FilledSplitButton()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun SelectableShapeButtonDemoPreview() {
    SelectableShapeButton()
}

@Preview
@Composable
fun AnimatedWavyCircleButtonDemoPreview() {
    AnimatedWavyCircleButton()
}

@Preview
@Composable
fun AnimatedAmplitudeWavyCircleButtonDemoPreview() {
    AnimatedAmplitudeWavyCircleButton(
        imageVector = Icons.Filled.FlashlightOn
    )
}


