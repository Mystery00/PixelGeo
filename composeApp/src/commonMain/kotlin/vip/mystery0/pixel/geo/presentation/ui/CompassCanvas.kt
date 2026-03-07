package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pixelgeo.composeapp.generated.resources.Res
import pixelgeo.composeapp.generated.resources.attitude_format
import pixelgeo.composeapp.generated.resources.dir_e
import pixelgeo.composeapp.generated.resources.dir_n
import pixelgeo.composeapp.generated.resources.dir_ne
import pixelgeo.composeapp.generated.resources.dir_nw
import pixelgeo.composeapp.generated.resources.dir_s
import pixelgeo.composeapp.generated.resources.dir_se
import pixelgeo.composeapp.generated.resources.dir_sw
import pixelgeo.composeapp.generated.resources.dir_w
import pixelgeo.composeapp.generated.resources.getting_gps_signal
import vip.mystery0.pixel.geo.domain.model.Attitude
import vip.mystery0.pixel.geo.util.formatString
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 指南针表盘组件
 *
 * @param heading 当前方位角（0-359°）。真北模式下 GPS 未就绪时为 null。
 * @param isWaitingForGps 是否处于真北等待 GPS 信号状态。
 *   参数约束：[heading] == null 与 [isWaitingForGps] == true 应同步出现；
 *   若 [heading] != null 且 [isWaitingForGps] == true，表盘仍会旋转但文字显示 "--"（不推荐此组合）。
 * @param attitude 当前设备姿态（俯仰、翻滚）。可以为空（例如权限未授予或仍在加载时）。
 * @param modifier Compose 修饰符，作用于整个组件的根容器
 */
@Composable
fun CompassCanvas(
    heading: Float?,
    isWaitingForGps: Boolean,
    attitude: Attitude?,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    // C1 修复：使用累积角度追踪绝对旋转量，避免 360°/0° 边界跨越时动画反转
    val accumulatedHeadingState = remember { mutableStateOf(heading ?: 0f) }

    // 每次 recompose 时计算最短路径并更新累积值
    val newRaw = heading ?: accumulatedHeadingState.value
    val current = accumulatedHeadingState.value % 360f
    val delta = ((newRaw - current + 540f) % 360f) - 180f
    val newAccumulated = accumulatedHeadingState.value + delta
    if (newAccumulated != accumulatedHeadingState.value) {
        accumulatedHeadingState.value = newAccumulated
    }

    // 判断是否绝对水平（俯仰角和翻滚角绝对值都小于 1.0°）
    val isLevel = attitude != null && abs(attitude.pitch) < 1.0f && abs(attitude.roll) < 1.0f

    // 触觉反馈 (Haptic)
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isLevel) {
        if (isLevel) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // S3 修复：改为无回弹（DampingRatioNoBouncy），工具类指南针不应有回弹效果
    val animatedHeading by animateFloatAsState(
        targetValue = accumulatedHeadingState.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "compassHeading"
    )

    // Canvas DrawScope 无法访问 MaterialTheme，提前在 Composable 层取色
    val colorScheme = MaterialTheme.colorScheme

    val textMeasurer = rememberTextMeasurer()
    // 刻度数字样式：普通刻度用辅助色，0° 用主色（与北方刻度一致）
    val labelTextStyle = TextStyle(
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        color = colorScheme.onSurfaceVariant
    )
    val northLabelTextStyle = labelTextStyle.copy(color = colorScheme.primary)

    // 提前在 @Composable 作用域内解析所需的字符串资源
    val stringDirN = stringResource(Res.string.dir_n)
    val stringDirE = stringResource(Res.string.dir_e)
    val stringDirS = stringResource(Res.string.dir_s)
    val stringDirW = stringResource(Res.string.dir_w)

    val cardinals = remember(stringDirN, stringDirE, stringDirS, stringDirW) {
        mapOf(
            0 to stringDirN,
            90 to stringDirE,
            180 to stringDirS,
            270 to stringDirW
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 表盘：外层固定指示器 + 旋转内部刻度和文字
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(if (isLandscape) 240.dp else 340.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // Constants for the ticks
                val tickMarginPx = 30.dp.toPx() // Keep ticks inside to leave room for outer text
                val edgeRadiusPx = radius - tickMarginPx

                val longTickLengthPx = 18.dp.toPx()
                val mediumTickLengthPx = 12.dp.toPx()
                val shortTickLengthPx = 8.dp.toPx()

                // --- 1. Draw Fixed Indicator (Top White Line) ---
                val indicatorLengthPx = 35.dp.toPx()
                drawLine(
                    color = colorScheme.onBackground,
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y - radius + indicatorLengthPx),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // --- 2. Central Crosshair & Circle (牛眼水平仪外圈) ---
                val innerCircleRadius = 40.dp.toPx()
                val crosshairLength = 55.dp.toPx()

                // 根据是否水平状态切换颜色
                val levelColor =
                    if (isLevel) colorScheme.primary else Color.White.copy(alpha = 0.3f)
                val targetColor = if (isLevel) colorScheme.primary else colorScheme.onSurfaceVariant

                // Dark translucent circle in the middle
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = innerCircleRadius,
                    center = center
                )

                // Crosshair lines
                drawLine(
                    color = targetColor,
                    start = Offset(center.x - crosshairLength, center.y),
                    end = Offset(center.x + crosshairLength, center.y),
                    strokeWidth = (if (isLevel) 2.dp else 1.dp).toPx()
                )
                drawLine(
                    color = targetColor,
                    start = Offset(center.x, center.y - crosshairLength),
                    end = Offset(center.x, center.y + crosshairLength),
                    strokeWidth = (if (isLevel) 2.dp else 1.dp).toPx()
                )

                // --- 2.1 绘制动态气泡 (Dynamic Bubble) ---
                if (attitude != null) {
                    val maxRadius = 30.dp.toPx() // 气泡最大偏移半径（保持在内圈中）

                    // 将 roll ([-180, 180]) 映射为 X 偏移，pitch 映射为 Y 偏移
                    // 灵敏度因子，可以微调。这里使用简单的线性映射：45度即可达到边界
                    val xOffsetRaw = (attitude.roll / 45f) * maxRadius
                    val yOffsetRaw = (attitude.pitch / 45f) * maxRadius

                    // 限幅 (Clamp)
                    val xOffsetClamped = xOffsetRaw.coerceIn(-maxRadius, maxRadius)
                    val yOffsetClamped = yOffsetRaw.coerceIn(-maxRadius, maxRadius)

                    val bubbleCenter = Offset(
                        x = center.x + xOffsetClamped,
                        y = center.y + yOffsetClamped
                    )

                    val bubbleRadius = 16.dp.toPx() // 白点的大小调大2倍 (8dp -> 16dp)
                    drawCircle(
                        color = levelColor,
                        radius = bubbleRadius,
                        center = bubbleCenter
                    )
                }

                // --- 3. Rotating Dial (Ticks and Texts) ---
                withTransform({
                    rotate(-animatedHeading, center)
                }) {
                    // Draw Ticks (every 2 degrees)
                    for (angle in 0 until 360 step 2) {
                        val isLong = angle % 30 == 0
                        val isMedium = angle % 10 == 0 && !isLong

                        val lineLength = when {
                            isLong -> longTickLengthPx
                            isMedium -> mediumTickLengthPx
                            else -> shortTickLengthPx
                        }

                        val strokeWidth = if (isLong) 2.dp.toPx() else 1.dp.toPx()

                        // Default color is white for ticks, except the North marker which we'll handle specially
                        val lineColor = colorScheme.onBackground

                        val angleRad = angle * PI / 180.0

                        // We do NOT draw the 0 degree line here, we draw a triangle instead
                        if (angle == 0) continue

                        val startRadius = edgeRadiusPx
                        val endRadius = edgeRadiusPx - lineLength

                        drawLine(
                            color = lineColor,
                            start = Offset(
                                center.x + startRadius * sin(angleRad).toFloat(),
                                center.y - startRadius * cos(angleRad).toFloat()
                            ),
                            end = Offset(
                                center.x + endRadius * sin(angleRad).toFloat(),
                                center.y - endRadius * cos(angleRad).toFloat()
                            ),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // North Triangle (0 degrees)
                    val triangleWidth = 12.dp.toPx()
                    val triangleHeight = 14.dp.toPx()
                    val path = androidx.compose.ui.graphics.Path().apply {
                        val topY = center.y - edgeRadiusPx
                        moveTo(center.x, topY)
                        lineTo(center.x - triangleWidth / 2f, topY + triangleHeight)
                        lineTo(center.x + triangleWidth / 2f, topY + triangleHeight)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = colorScheme.primary
                    )

                    // Draw Outer Degree Numbers (0, 30, 60...)
                    val numberRadiusPx = edgeRadiusPx + 16.dp.toPx()
                    for (angle in 0 until 360 step 30) {
                        // Skip 0 for numbers, or maybe leave it? The iOS design has 0 near the triangle
                        // Actually the image shows '0' next to the triangle
                        val text = angle.toString()
                        val measured = textMeasurer.measure(text = text, style = labelTextStyle)
                        val angleRad = angle * PI / 180.0

                        val textCenterX = center.x + numberRadiusPx * sin(angleRad).toFloat()
                        val textCenterY = center.y - numberRadiusPx * cos(angleRad).toFloat()

                        // To keep text upright, we counter-rotate it by the same angle it was rotated globally.
                        // Wait, the numbers in the dial actually stay upright relative to the phone? No, the image shows
                        // they form a circle but they are standing upright. 
                        // Actually, if we reverse the `-animatedHeading` and apply the `angle`, let's just draw them rotated?
                        // If we look at the image, '30' is upright, '60' is upright. 
                        withTransform({
                            translate(left = textCenterX, top = textCenterY)
                            // Counter-rotate the Canvas so the text is always upright relative to screen
                            rotate(animatedHeading, Offset.Zero)
                        }) {
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    x = -measured.size.width / 2f,
                                    y = -measured.size.height / 2f
                                )
                            )
                        }
                    }

                    // Draw Inner Cardinal Directions (北, 东, 南, 西)
                    val cardinalRadiusPx = edgeRadiusPx - longTickLengthPx - 30.dp.toPx()

                    val cardinalTextStyle = TextStyle(
                        fontSize = 20.sp,
                        color = colorScheme.onBackground,
                        fontFamily = FontFamily.Default
                    )

                    for ((angle, text) in cardinals) {
                        val measured = textMeasurer.measure(text = text, style = cardinalTextStyle)
                        val angleRad = angle * PI / 180.0

                        val textCenterX = center.x + cardinalRadiusPx * sin(angleRad).toFloat()
                        val textCenterY = center.y - cardinalRadiusPx * cos(angleRad).toFloat()

                        withTransform({
                            translate(left = textCenterX, top = textCenterY)
                            // Counter-rotate so text is always upright
                            rotate(animatedHeading, Offset.Zero)
                        }) {
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    x = -measured.size.width / 2f,
                                    y = -measured.size.height / 2f
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

        // 当前方位角显示，或真北等待态提示
        Box(
            modifier = Modifier.heightIn(min = 110.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isWaitingForGps) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "--",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.getting_gps_signal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val currentHeading = heading ?: animatedHeading
                val directionText = when (currentHeading) {
                    in 337.5..360.0, in 0.0..22.5 -> stringResource(Res.string.dir_n)
                    in 22.5..67.5 -> stringResource(Res.string.dir_ne)
                    in 67.5..112.5 -> stringResource(Res.string.dir_e)
                    in 112.5..157.5 -> stringResource(Res.string.dir_se)
                    in 157.5..202.5 -> stringResource(Res.string.dir_s)
                    in 202.5..247.5 -> stringResource(Res.string.dir_sw)
                    in 247.5..292.5 -> stringResource(Res.string.dir_w)
                    in 292.5..337.5 -> stringResource(Res.string.dir_nw)
                    else -> ""
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatString("%.0f°", currentHeading),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 64.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = " $directionText",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 12.dp) // Align slightly above the baseline of the large digits
                        )
                    }

                    // 底部增加水平角度的数字显示
                    if (attitude != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                Res.string.attitude_format,
                                formatString("%.1f", abs(attitude.pitch)),
                                formatString("%.1f", abs(attitude.roll))
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
