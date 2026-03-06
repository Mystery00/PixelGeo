package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.presentation.CompassIntent
import vip.mystery0.pixel.geo.presentation.CompassUiState
import vip.mystery0.pixel.geo.presentation.GpsSignalQuality
import vip.mystery0.pixel.geo.util.copyToClipboard
import vip.mystery0.pixel.geo.util.formatString

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 0. 抽屉指示器 (Drag Handle) ---
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .width(36.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                )
        )

        // ── 1. GPS 信号质量指示器行 ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 10dp 圆点（颜色随信号质量变化）
            val dotColor = when (uiState.gpsSignalQuality) {
                GpsSignalQuality.EXCELLENT -> MaterialTheme.colorScheme.tertiary
                GpsSignalQuality.GOOD -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                GpsSignalQuality.POOR -> MaterialTheme.colorScheme.error
                GpsSignalQuality.NONE -> Color.Gray
            }
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = dotColor)
            }

            // 精度文字
            val accuracyText = if (uiState.location != null) {
                formatString("精度: %.1f m", uiState.location.horizontalAccuracy)
            } else {
                "无 GPS 信号"
            }
            Text(
                text = accuracyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── 2. 坐标数据行 ──────────────────────────────────────────────
        if (uiState.location != null) {
            CoordinateRow(
                label = "纬度",
                value = formatUseCase.formatLatitude(
                    uiState.location.latitude,
                    uiState.coordinateFormat
                ),
                isAltitude = false
            )
            CoordinateRow(
                label = "经度",
                value = formatUseCase.formatLongitude(
                    uiState.location.longitude,
                    uiState.coordinateFormat
                ),
                isAltitude = false
            )
            CoordinateRow(
                label = "海拔",
                value = formatUseCase.formatAltitude(uiState.location.altitude),
                isAltitude = true
            )
        } else {
            CoordinateRow(label = "纬度", value = "--", isAltitude = false)
            CoordinateRow(label = "经度", value = "--", isAltitude = false)
            CoordinateRow(label = "海拔", value = "--", isAltitude = true)
        }

        // ── 3. 操作按钮区 ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一行：切换选项组（SegmentedButton）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 真北 / 磁北
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    SegmentedButton(
                        selected = uiState.northMode == NorthMode.TRUE_NORTH,
                        onClick = { onIntent(CompassIntent.ToggleNorthMode(NorthMode.TRUE_NORTH)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(text = "真北", style = MaterialTheme.typography.bodySmall)
                    }
                    SegmentedButton(
                        selected = uiState.northMode == NorthMode.MAGNETIC_NORTH,
                        onClick = { onIntent(CompassIntent.ToggleNorthMode(NorthMode.MAGNETIC_NORTH)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(text = "磁北", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // DD / DMS 坐标格式
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    SegmentedButton(
                        selected = uiState.coordinateFormat == CoordinateFormat.DD,
                        onClick = { onIntent(CompassIntent.ToggleCoordinateFormat(CoordinateFormat.DD)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(text = "DD", style = MaterialTheme.typography.bodySmall)
                    }
                    SegmentedButton(
                        selected = uiState.coordinateFormat == CoordinateFormat.DMS,
                        onClick = { onIntent(CompassIntent.ToggleCoordinateFormat(CoordinateFormat.DMS)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(text = "DMS", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 第二行：操作按钮（复制为次要操作，分享为主要操作）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 复制按钮（由 UI 层直接操作剪贴板，不发送 Intent）
                FilledTonalButton(
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
                            copyToClipboard(AnnotatedString(text))
                        }
                    },
                    enabled = uiState.location != null,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(text = "复制", style = MaterialTheme.typography.bodySmall)
                }

                // 分享按钮（主要操作）
                Button(
                    onClick = { onIntent(CompassIntent.ShareCoordinates) },
                    enabled = uiState.location != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "分享", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * 单行坐标数据展示
 *
 * @param label 左侧标签（灰色）
 * @param value 右侧数值（白色，等宽字体）
 * @param isAltitude 是否为海拔数据（如果是，则显示代表海拔的 icon）
 */
@Composable
private fun CoordinateRow(label: String, value: String, isAltitude: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isAltitude) TerrainIcon else LocationOnIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}

private val LocationOnIcon: ImageVector
    get() = ImageVector.Builder(
        name = "LocationOn",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12.0f, 2.0f)
            curveTo(8.13f, 2.0f, 5.0f, 5.13f, 5.0f, 9.0f)
            curveToRelative(0.0f, 5.25f, 7.0f, 13.0f, 7.0f, 13.0f)
            curveToRelative(0.0f, 0.0f, 7.0f, -7.75f, 7.0f, -13.0f)
            curveToRelative(0.0f, -3.87f, -3.13f, -7.0f, -7.0f, -7.0f)
            close()
            moveTo(12.0f, 11.5f)
            curveToRelative(-1.38f, 0.0f, -2.5f, -1.12f, -2.5f, -2.5f)
            reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
            reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
            reflectiveCurveToRelative(-1.12f, 2.5f, -2.5f, 2.5f)
            close()
        }
    }.build()

private val TerrainIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Terrain",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(14.0f, 6.0f)
            lineToRelative(-3.75f, 5.0f)
            lineToRelative(2.85f, 3.8f)
            lineToRelative(-1.6f, 1.2f)
            lineTo(14.0f, 13.0f)
            lineToRelative(5.0f, 7.0f)
            lineTo(2.0f, 20.0f)
            lineToRelative(8.0f, -11.0f)
            lineToRelative(4.0f, 5.0f)
            lineToRelative(0.0f, -8.0f)
            close()
        }
    }.build()
