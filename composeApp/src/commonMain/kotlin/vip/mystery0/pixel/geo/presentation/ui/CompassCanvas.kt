package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 指南针表盘组件
 * @param heading 当前方位角（null 表示真北等待态）
 * @param isWaitingForGps 是否处于真北等待 GPS 信号状态
 * @param modifier Compose 修饰符
 */
@Composable
fun CompassCanvas(
    heading: Float?,
    isWaitingForGps: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用 spring 动画平滑旋转，替代手写低通滤波
    val animatedHeading by animateFloatAsState(
        targetValue = heading ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "compassHeading"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 表盘：整体旋转实现指针效果（表盘转，正上方始终是北方）
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .rotate(-animatedHeading)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // 外圆框
            drawCircle(
                color = Color.White,
                radius = radius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 刻度线：每 10° 一短线，每 30° 一长线
            for (angle in 0 until 360 step 10) {
                val isMain = angle % 30 == 0
                val lineLength = if (isMain) 20.dp.toPx() else 10.dp.toPx()
                val strokeWidth = if (isMain) 2.dp.toPx() else 1.dp.toPx()
                val lineColor = if (isMain) Color.White else Color.Gray

                val angleRad = Math.toRadians(angle.toDouble())
                val startRadius = radius - lineLength - 4.dp.toPx()
                val endRadius = radius - 4.dp.toPx()

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

            // 北方（0°）红色长刻度，突出标识
            val northLength = 30.dp.toPx()
            drawLine(
                color = Color.Red,
                start = Offset(center.x, center.y - (radius - northLength - 4.dp.toPx())),
                end = Offset(center.x, center.y - (radius - 4.dp.toPx())),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 当前方位角显示，或真北等待态提示
        if (isWaitingForGps) {
            // 真北等待态：表盘冻结，显示占位符和提示
            Text(
                text = "--",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "正在获取 GPS 信号…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            // 正常显示：三位数角度 + 度符号
            Text(
                text = "%03.0f°".format(heading ?: animatedHeading),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
