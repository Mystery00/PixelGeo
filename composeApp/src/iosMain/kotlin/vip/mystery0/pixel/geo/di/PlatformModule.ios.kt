package vip.mystery0.pixel.geo.di

import org.koin.core.module.Module
import org.koin.dsl.module
import vip.mystery0.pixel.geo.data.sensor.CompassSensor
import vip.mystery0.pixel.geo.data.sensor.IOSLocationCompassManager
import vip.mystery0.pixel.geo.data.sensor.LocationSensor

// iOS 平台模块：注册 iOS 传感器实现
actual val platformModule: Module = module {
    // IOSLocationCompassManager 同时实现两个接口，共享同一实例
    single { IOSLocationCompassManager() }
    single<CompassSensor> { get<IOSLocationCompassManager>() }
    single<LocationSensor> { get<IOSLocationCompassManager>() }
}
