package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 指南针表盘组件
 *
 * @param heading 当前方位角（0-359°）。真北模式下 GPS 未就绪时为 null。
 * @param isWaitingForGps 是否处于真北等待 GPS 信号状态。
 *   参数约束：[heading] == null 与 [isWaitingForGps] == true 应同步出现；
 *   若 [heading] != null 且 [isWaitingForGps] == true，表盘仍会旋转但文字显示 "--"（不推荐此组合）。
 * @param modifier Compose 修饰符，作用于整个组件的根容器
 */
@Composable
fun CompassCanvas(
    heading: Float?,
    isWaitingForGps: Boolean,
    modifier: Modifier = Modifier
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

    // S3 修复：改为无回弹（DampingRatioNoBouncy），工具类指南针不应有回弹效果
    val animatedHeading by animateFloatAsState(
        targetValue = accumulatedHeadingState.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
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

            // I1 修复：预计算所有像素值，避免在循环和绘制调用中重复转换
            val borderWidthPx = 2.dp.toPx()
            val tickMarginPx = 4.dp.toPx()
            val mainTickLengthPx = 20.dp.toPx()
            val subTickLengthPx = 10.dp.toPx()
            val mainTickWidthPx = 2.dp.toPx()
            val subTickWidthPx = 1.dp.toPx()
            val northTickLengthPx = 30.dp.toPx()
            val northTickWidthPx = 3.dp.toPx()
            val edgeRadiusPx = radius - tickMarginPx

            // 外圆框
            drawCircle(
                color = Color.White,
                radius = radius - borderWidthPx,
                center = center,
                style = Stroke(width = borderWidthPx)
            )

            // 刻度线：每 10° 一短线，每 30° 一长线
            for (angle in 0 until 360 step 10) {
                // 跳过 0°，北方由专用红色刻度处理，避免刻度线重叠
                if (angle == 0) continue

                val isMain = angle % 30 == 0
                val lineLength = if (isMain) mainTickLengthPx else subTickLengthPx
                val strokeWidth = if (isMain) mainTickWidthPx else subTickWidthPx
                val lineColor = if (isMain) Color.White else Color.Gray

                val angleRad = angle * PI / 180.0
                val startRadius = edgeRadiusPx - lineLength

                drawLine(
                    color = lineColor,
                    start = Offset(
                        center.x + startRadius * sin(angleRad).toFloat(),
                        center.y - startRadius * cos(angleRad).toFloat()
                    ),
                    end = Offset(
                        center.x + edgeRadiusPx * sin(angleRad).toFloat(),
                        center.y - edgeRadiusPx * cos(angleRad).toFloat()
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 北方（0°）红色长刻度，突出标识
            drawLine(
                color = Color.Red,
                start = Offset(center.x, center.y - (edgeRadiusPx - northTickLengthPx)),
                end = Offset(center.x, center.y - edgeRadiusPx),
                strokeWidth = northTickWidthPx,
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // I4 修复：固定最小高度，避免 isWaitingForGps 切换时文字区域高度跳变
        Box(
            modifier = Modifier.heightIn(min = 72.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // 当前方位角显示，或真北等待态提示
            if (isWaitingForGps) {
                // 真北等待态：表盘冻结，显示占位符和提示
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                }
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
}
