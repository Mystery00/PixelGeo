package vip.mystery0.pixel.geo.data.sensor

import kotlinx.coroutines.flow.Flow
import vip.mystery0.pixel.geo.domain.model.LocationModel

// 定位传感器接口，由各平台分别实现
interface LocationSensor {
    val locationData: Flow<LocationModel>  // 位置数据流
    fun start()  // 启动定位监听
    fun stop()   // 停止定位监听
}
