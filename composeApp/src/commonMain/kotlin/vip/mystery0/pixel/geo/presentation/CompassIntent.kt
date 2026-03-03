package vip.mystery0.pixel.geo.presentation

import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode

// 用户操作意图（MVI Intent）
sealed interface CompassIntent {
    data object RequestLocationPermission : CompassIntent    // 请求定位权限
    data object OpenAppSettings : CompassIntent              // 打开系统设置
    data class ToggleNorthMode(val mode: NorthMode) : CompassIntent         // 切换北极模式
    data class ToggleCoordinateFormat(val format: CoordinateFormat) : CompassIntent  // 切换坐标格式
    data object CopyCoordinates : CompassIntent              // 复制坐标（由 UI 层处理剪贴板）
    data object ShareCoordinates : CompassIntent             // 分享坐标
}
