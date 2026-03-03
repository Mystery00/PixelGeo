package vip.mystery0.pixel.geo.domain.model

data class LocationModel(
    val latitude: Double,           // 纬度（WGS-84）
    val longitude: Double,          // 经度（WGS-84）
    val altitude: Double,           // 海拔高度（米）
    val horizontalAccuracy: Double  // 水平精度（米），用于信号质量指示
)
