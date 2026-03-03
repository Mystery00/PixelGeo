package vip.mystery0.pixel.geo.presentation

import vip.mystery0.pixel.geo.domain.model.CompassHeading
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.LocationModel
import vip.mystery0.pixel.geo.domain.model.NorthMode

// GPS 信号质量等级
enum class GpsSignalQuality {
    NONE,       // 无信号（未获取到位置）
    EXCELLENT,  // 精度 < 10m，显示绿色
    GOOD,       // 精度 10~50m，显示黄色
    POOR        // 精度 > 50m，显示红色
}

// 主界面 UI 状态（不可变数据类）
data class CompassUiState(
    val heading: CompassHeading = CompassHeading(0f, null),
    val location: LocationModel? = null,
    val northMode: NorthMode = NorthMode.MAGNETIC_NORTH,
    val coordinateFormat: CoordinateFormat = CoordinateFormat.DD,
    val isLocationPermissionGranted: Boolean = false,
    val gpsSignalQuality: GpsSignalQuality = GpsSignalQuality.NONE
)
