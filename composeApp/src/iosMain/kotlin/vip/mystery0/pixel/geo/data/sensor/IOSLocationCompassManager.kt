package vip.mystery0.pixel.geo.data.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import vip.mystery0.pixel.geo.domain.model.CompassHeading
import vip.mystery0.pixel.geo.domain.model.LocationModel

// iOS 传感器管理器：同时实现 CompassSensor 和 LocationSensor 接口
// 使用单一 CLLocationManager 实例，通过 Delegate 桥接到 Kotlin Flow
class IOSLocationCompassManager : CompassSensor, LocationSensor {

    private val locationManager = CLLocationManager()

    private val _headingData = MutableStateFlow(CompassHeading(0f, null))
    override val headingData: Flow<CompassHeading> = _headingData.asStateFlow()

    private val _locationData = MutableStateFlow<LocationModel?>(null)
    override val locationData: Flow<LocationModel> = _locationData.filterNotNull()

    // CLLocationManager Delegate：将 Obj-C 回调桥接为 Kotlin Flow 更新
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

        // 方位角更新回调
        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
            // trueHeading < 0 表示 GPS 未就绪，此时真北不可用
            val trueHeading = if (didUpdateHeading.trueHeading >= 0) {
                didUpdateHeading.trueHeading.toFloat()
            } else null

            _headingData.value = CompassHeading(
                magneticHeading = didUpdateHeading.magneticHeading.toFloat(),
                trueHeading = trueHeading
            )
        }

        // 位置更新回调
        override fun locationManager(
            manager: CLLocationManager,
            didUpdateLocations: List<*>
        ) {
            (didUpdateLocations.lastOrNull() as? CLLocation)?.let { location ->
                _locationData.value = LocationModel(
                    latitude = location.coordinate.latitude,
                    longitude = location.coordinate.longitude,
                    altitude = location.altitude,
                    horizontalAccuracy = location.horizontalAccuracy
                )
            }
        }
    }

    init {
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    override fun start() {
        // 请求使用期间定位权限（首次使用时弹窗）
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        locationManager.startUpdatingHeading()
    }

    override fun stop() {
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
    }
}
