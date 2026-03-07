package vip.mystery0.pixel.geo.domain.usecase

import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.LocationModel
import vip.mystery0.pixel.geo.presentation.ShareTextStrings

// 分享文案生成用例：根据当前位置和坐标格式生成预置分享文字
class BuildShareTextUseCase(
    private val formatLocationUseCase: FormatLocationUseCase
) {

    /**
     * 生成预置分享文案
     * 坐标格式与用户当前设置（DD/DMS）保持一致
     */
    fun execute(
        location: LocationModel,
        format: CoordinateFormat,
        strings: ShareTextStrings
    ): String {
        val latitude = formatLocationUseCase.formatLatitude(location.latitude, format)
        val longitude = formatLocationUseCase.formatLongitude(location.longitude, format)
        val altitude = formatLocationUseCase.formatAltitude(location.altitude)

        val title = strings.title
        val latLabel = strings.latLabel
        val lonLabel = strings.lonLabel
        val altLabel = strings.altLabel
        val footer = strings.footer

        return """
            $title

            $latLabel$latitude
            $lonLabel$longitude
            $altLabel$altitude

            $footer
        """.trimIndent()
    }
}
