package vip.mystery0.pixel.geo.domain.model

data class CompassHeading(
    val magneticHeading: Float,     // 磁北方位角（0-359°）
    val trueHeading: Float?         // 真北方位角，GPS 未定位时为 null
)
