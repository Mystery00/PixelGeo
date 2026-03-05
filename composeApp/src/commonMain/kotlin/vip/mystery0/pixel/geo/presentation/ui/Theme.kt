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
    primary = Color(0xFFA8C7FA), // Quiet Tech Blue for primary actions/pointer
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF284777),
    onPrimaryContainer = Color(0xFFD3E3FD),

    secondary = Color(0xFFC2C7CF),
    onSecondary = Color(0xFF2C3137),
    secondaryContainer = Color(0xFF42474E),
    onSecondaryContainer = Color(0xFFDEE3EB),

    // Used for Warning/Accuracy prompts (Soft yellow/amber)
    tertiary = Color(0xFFF2C044),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFDF9B),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF000000), // Pure black for deep immersive background
    onBackground = Color(0xFFE3E2E6),

    surface = Color(0xFF111318), // Slightly elevated from pure black
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),

    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
)

@Composable
fun PixelGeoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelGeoDarkColorScheme,
        content = content
    )
}
