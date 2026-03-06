package vip.mystery0.pixel.geo.data.sensor

import kotlinx.coroutines.flow.Flow
import vip.mystery0.pixel.geo.domain.model.Attitude
import vip.mystery0.pixel.geo.domain.model.CompassHeading

// 指南针传感器接口，由各平台分别实现
interface CompassSensor {
    val headingData: Flow<CompassHeading>  // 方位角数据流
    val attitudeData: Flow<Attitude>       // 姿态（俯仰、翻滚）数据流
    fun start()  // 启动传感器监听
    fun stop()   // 停止传感器监听
}
