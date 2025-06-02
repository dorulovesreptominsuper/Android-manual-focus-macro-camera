package com.example.manualfocusmacrocamera.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp

@Composable
fun AnimatedColorSwitch(
    switchHeightDp: Dp = 32.dp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val switchWidthDp = switchHeightDp * 1.6f
    val horizontalPaddingDp = switchHeightDp / 8
    val circleDiameter = switchHeightDp * 0.75f
    val transitionDegree = switchWidthDp - horizontalPaddingDp * 2 - circleDiameter
    val transition = updateTransition(checked, label = "switchTransition")
    val backgroundColor by transition.animateColor(label = "bgColor") { isChecked ->
        if (isChecked) Color.Green else Color.Gray
    }
    val toggleOffset by animateDpAsState(
        targetValue = if (checked) transitionDegree else 0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "switchSpring"
    )

    Box(
        Modifier
            .width(switchWidthDp)
            .height(switchHeightDp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = horizontalPaddingDp),
        contentAlignment = Alignment.CenterStart
    ) {
        RollingIcon(
            Modifier
                .offset(x = toggleOffset)
                .size(circleDiameter),
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun RollingIcon(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val transition = updateTransition(checked, label = "iconSwitch")
    val rotation by transition.animateFloat(label = "iconRotation") { if (it) 360f else 0f }
    val scale by transition.animateFloat(label = "iconScale") { if (it) 1f else 0.8f }
    val color by transition.animateColor(label = "iconColor") {
        if (it) Color.White else Color.Transparent
    }

    Box(
        modifier
            .clip(CircleShape)
            .background(color, shape = CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.Done else Icons.Filled.Cancel,
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
                .size(28.dp),
            tint = if (checked) Color.Green else Color.LightGray
        )
    }
}

@Composable
fun BearSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val transition = updateTransition(checked, label = "bearSwitch")
    val offset by transition.animateDp(label = "bearOffset") { if (it) 32.dp else 0.dp }
    val faceColor by transition.animateColor(label = "faceColor") {
        if (it) Color(0xFFFFD54F) else Color(
            0xFFBCAAA4
        )
    }
    Box(
        Modifier
            .width(64.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFE0E0E0))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(28.dp)
                .background(faceColor, shape = CircleShape)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val eyeY = size.height * 0.4f
                drawCircle(Color.Black, 2.dp.toPx(), center.copy(x = size.width * 0.32f, y = eyeY))
                drawCircle(Color.Black, 2.dp.toPx(), center.copy(x = size.width * 0.68f, y = eyeY))
                drawArc(
                    color = Color.Black,
                    startAngle = if (checked) 10f else 170f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        size.width * 0.35f,
                        size.height * 0.7f
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.3f, size.height * 0.2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SpecialFlowerSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val transition = updateTransition(targetState = checked, label = "flowerSwitch")
    val offset by transition.animateDp(label = "offset") { if (it) 32.dp else 0.dp }

    Box(
        Modifier
            .width(64.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE0F7FA))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(32.dp)
        ) {
            if (checked) {
                // 咲いているスペシャルフラワーのイメージ
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = this.center
                    // 花びら（白）
                    repeat(8) { i ->
                        val angle = Math.toRadians(i * 45.0)
                        val r = size.width * 0.42f
                        val px = center.x + r * kotlin.math.cos(angle).toFloat()
                        val py = center.y + r * kotlin.math.sin(angle).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = size.width * 0.22f,
                            center = androidx.compose.ui.geometry.Offset(px, py)
                        )
                    }
                    // 中心（黄）
                    drawCircle(
                        color = Color(0xFFFFF176),
                        radius = size.width * 0.21f,
                        center = center
                    )
                    // ニコニコ顔
                    // 目
                    drawCircle(
                        Color.Black, size.width * 0.045f,
                        center = center.copy(
                            x = center.x - size.width * 0.06f,
                            y = center.y - size.height * 0.03f
                        )
                    )
                    drawCircle(
                        Color.Black, size.width * 0.045f,
                        center = center.copy(
                            x = center.x + size.width * 0.06f,
                            y = center.y - size.height * 0.03f
                        )
                    )
                    // 口（スマイル）
                    drawArc(
                        color = Color.Black,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            center.x - size.width * 0.09f,
                            center.y + size.height * 0.01f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            size.width * 0.18f,
                            size.height * 0.10f
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            } else {
                // 閉じている花のイメージ
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = this.center
                    // 花びらを閉じてるイメージ（灰色でコンパクトに）
                    drawOval(
                        color = Color.White,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            center.x - size.width * 0.16f,
                            center.y - size.height * 0.18f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            size.width * 0.32f,
                            size.height * 0.36f
                        )
                    )
                    // 花の中心
                    drawCircle(
                        color = Color(0xFFFBC02D),
                        radius = size.width * 0.14f,
                        center = center
                    )
                    // 目（眠そう/閉じ目）
                    drawLine(
                        color = Color.Black,
                        start = center.copy(
                            x = center.x - size.width * 0.07f,
                            y = center.y - size.height * 0.03f
                        ),
                        end = center.copy(
                            x = center.x - size.width * 0.02f,
                            y = center.y - size.height * 0.03f
                        ),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Black,
                        start = center.copy(
                            x = center.x + size.width * 0.02f,
                            y = center.y - size.height * 0.03f
                        ),
                        end = center.copy(
                            x = center.x + size.width * 0.07f,
                            y = center.y - size.height * 0.03f
                        ),
                        strokeWidth = 2f
                    )
                    // 口（への字）
                    drawArc(
                        color = Color.Black,
                        startAngle = 210f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            center.x - size.width * 0.08f,
                            center.y + size.height * 0.04f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            size.width * 0.16f,
                            size.height * 0.07f
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun SpringSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Springの物理パラメータ（柔らかさや反発を調整）
    val toggleOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,      // 柔らかめ
            dampingRatio = Spring.DampingRatioMediumBouncy // バウンド強め
        ),
        label = "switchSpring"
    )
    val backgroundColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else Color(0xFFB0BEC5),
        label = "switchBg"
    )
    Box(
        Modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = toggleOffset)
                .size(24.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Preview
@Composable
fun AnimatedColorSwitchPreview() {
    var checked by remember { mutableStateOf(false) }
    AnimatedColorSwitch(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

@Preview
@Composable
fun SpringSwitchPreview() {
    var checked by remember { mutableStateOf(false) }
    SpringSwitch(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}


@Preview
@Composable
fun IconSwitchPreview() {
    var checked by remember { mutableStateOf(false) }
    RollingIcon(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

@Preview
@Composable
fun BearSwitchPreview() {
    var checked by remember { mutableStateOf(false) }
    BearSwitch(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

@Preview
@Composable
fun SpecialFlowerSwitchPreview() {
    var checked by remember { mutableStateOf(false) }
    SpecialFlowerSwitch(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

