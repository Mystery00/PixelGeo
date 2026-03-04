package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * PixelGeo 自定义深色颜色方案
 *
 * 语义映射说明：
 * - background / onBackground  → 主屏幕背景（纯黑）与主前景（纯白）
 * - surface / onSurface        → 坐标面板背景与坐标数值文字
 * - surfaceVariant / onSurfaceVariant → 按钮容器与标签/辅助文字
 * - outline / outlineVariant   → 表盘外圆框+主刻度 / 副刻度线
 * - primary                    → 北方（0°）红色刻度指示
 * - secondary                  → GPS 信号良好（琥珀黄）
 * - tertiary                   → GPS 信号优秀（绿色）
 * - error                      → GPS 信号较弱（红色）
 */
private val PixelGeoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF4444),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = Color(0xFFFFC107),
    onSecondary = Color(0xFF3E2800),
    secondaryContainer = Color(0xFF593E00),
    onSecondaryContainer = Color(0xFFFFDFA6),

    tertiary = Color(0xFF4CAF50),
    onTertiary = Color(0xFF003910),
    tertiaryContainer = Color(0xFF00531A),
    onTertiaryContainer = Color(0xFF5DFC6C),

    error = Color(0xFFF44336),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFAAAAAA),

    outline = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFF808080),
)

@Composable
fun PixelGeoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelGeoDarkColorScheme,
        content = content
    )
}
