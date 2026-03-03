package vip.mystery0.pixel.geo.di

import com.google.android.gms.location.LocationServices
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import vip.mystery0.pixel.geo.data.sensor.AndroidCompassSensor
import vip.mystery0.pixel.geo.data.sensor.CompassSensor
import vip.mystery0.pixel.geo.data.sensor.LocationSensor

// Android 平台模块：注册 Android 传感器实现
actual val platformModule: Module = module {
    // FusedLocationProviderClient 单例
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }

    // AndroidCompassSensor 同时实现两个接口，共享同一实例
    single { AndroidCompassSensor(androidContext(), get()) }
    single<CompassSensor> { get<AndroidCompassSensor>() }
    single<LocationSensor> { get<AndroidCompassSensor>() }
}
