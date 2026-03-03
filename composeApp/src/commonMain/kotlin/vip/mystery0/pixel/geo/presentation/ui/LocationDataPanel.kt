package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.geo.domain.model.NorthMode
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.presentation.CompassIntent
import vip.mystery0.pixel.geo.presentation.CompassUiState
import vip.mystery0.pixel.geo.presentation.GpsSignalQuality

/**
 * 坐标数据面板组件
 *
 * 包含：GPS 信号质量指示器、坐标数据行（纬度/经度/海拔）、操作按钮行
 *
 * @param uiState 当前 UI 状态
 * @param onIntent 用户操作意图回调
 */
@Composable
fun LocationDataPanel(
    uiState: CompassUiState,
    onIntent: (CompassIntent) -> Unit
) {
    // 直接实例化（spec 要求，不通过 Koin 注入）
    val formatUseCase = remember { FormatLocationUseCase() }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. GPS 信号质量指示器行 ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 10dp 圆点（颜色随信号质量变化）
            val dotColor = when (uiState.gpsSignalQuality) {
                GpsSignalQuality.EXCELLENT -> Color(0xFF4CAF50)  // 绿色
                GpsSignalQuality.GOOD      -> Color(0xFFFFC107)  // 黄色
                GpsSignalQuality.POOR      -> Color(0xFFF44336)  // 红色
                GpsSignalQuality.NONE      -> Color.Gray
            }
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = dotColor)
            }

            // 精度文字
            val accuracyText = if (uiState.location != null) {
                "精度: %.1f m".format(uiState.location.horizontalAccuracy)
            } else {
                "无 GPS 信号"
            }
            Text(
                text = accuracyText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace
            )
        }

        // ── 2. 坐标数据行 ──────────────────────────────────────────────
        if (uiState.location != null) {
            CoordinateRow(
                label = "纬度",
                value = formatUseCase.formatLatitude(uiState.location.latitude, uiState.coordinateFormat)
            )
            CoordinateRow(
                label = "经度",
                value = formatUseCase.formatLongitude(uiState.location.longitude, uiState.coordinateFormat)
            )
            CoordinateRow(
                label = "海拔",
                value = formatUseCase.formatAltitude(uiState.location.altitude)
            )
        } else {
            Text(
                text = "等待定位数据…",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // ── 3. 操作按钮行 ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2C),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF1E1E1E),
                disabledContentColor = Color.DarkGray
            )

            // 真北/磁北切换按钮
            Button(
                onClick = {
                    val nextMode = if (uiState.northMode == NorthMode.TRUE_NORTH) {
                        NorthMode.MAGNETIC_NORTH
                    } else {
                        NorthMode.TRUE_NORTH
                    }
                    onIntent(CompassIntent.ToggleNorthMode(nextMode))
                },
                colors = buttonColors,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (uiState.northMode == NorthMode.TRUE_NORTH) "真北" else "磁北",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // DD/DMS 坐标格式切换按钮
            Button(
                onClick = {
                    val nextFormat = when (uiState.coordinateFormat) {
                        vip.mystery0.pixel.geo.domain.model.CoordinateFormat.DD  ->
                            vip.mystery0.pixel.geo.domain.model.CoordinateFormat.DMS
                        vip.mystery0.pixel.geo.domain.model.CoordinateFormat.DMS ->
                            vip.mystery0.pixel.geo.domain.model.CoordinateFormat.DD
                    }
                    onIntent(CompassIntent.ToggleCoordinateFormat(nextFormat))
                },
                colors = buttonColors,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = uiState.coordinateFormat.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 复制按钮（由 UI 层直接操作剪贴板，不发送 Intent）
            Button(
                onClick = {
                    if (uiState.location != null) {
                        val lat = formatUseCase.formatLatitude(
                            uiState.location.latitude,
                            uiState.coordinateFormat
                        )
                        val lon = formatUseCase.formatLongitude(
                            uiState.location.longitude,
                            uiState.coordinateFormat
                        )
                        val alt = formatUseCase.formatAltitude(uiState.location.altitude)
                        val text = buildString {
                            appendLine(lat)
                            appendLine(lon)
                            append(alt)
                        }
                        clipboardManager.setText(AnnotatedString(text))
                    }
                },
                enabled = uiState.location != null,
                colors = buttonColors,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "复制",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 分享按钮
            Button(
                onClick = { onIntent(CompassIntent.ShareCoordinates) },
                enabled = uiState.location != null,
                colors = buttonColors,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "分享",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 单行坐标数据展示
 *
 * @param label 左侧标签（灰色）
 * @param value 右侧数值（白色，等宽字体）
 */
@Composable
private fun CoordinateRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
