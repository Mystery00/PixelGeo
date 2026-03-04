package vip.mystery0.pixel.geo.domain.usecase

import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.util.formatString
import kotlin.math.abs

// 坐标格式化用例：将 WGS-84 坐标转换为 DD 或 DMS 文本格式
class FormatLocationUseCase {

    /**
     * 格式化纬度
     * DD 示例：39.123456°N
     * DMS 示例：39°07'24.4"N
     */
    fun formatLatitude(value: Double, format: CoordinateFormat): String {
        val direction = if (value >= 0) "N" else "S"
        return formatCoordinate(abs(value), format, direction)
    }

    /**
     * 格式化经度
     * DD 示例：116.123456°E
     * DMS 示例：116°07'24.4"E
     */
    fun formatLongitude(value: Double, format: CoordinateFormat): String {
        val direction = if (value >= 0) "E" else "W"
        return formatCoordinate(abs(value), format, direction)
    }

    /**
     * 格式化海拔高度
     * 示例：123.4 m
     */
    fun formatAltitude(value: Double): String = formatString("%.1f m", value)

    // 通用坐标格式化逻辑（私有）
    private fun formatCoordinate(
        absValue: Double,
        format: CoordinateFormat,
        direction: String
    ): String {
        return when (format) {
            CoordinateFormat.DD -> formatString("%.6f°%s", absValue, direction)
            CoordinateFormat.DMS -> {
                val degrees = absValue.toInt()
                val minutesDecimal = (absValue - degrees) * 60
                val minutes = minutesDecimal.toInt()
                val seconds = (minutesDecimal - minutes) * 60
                // 格式：度°分'秒.一位"方向
                formatString("%d°%02d'%04.1f\"%s", degrees, minutes, seconds, direction)
            }
        }
    }
}
