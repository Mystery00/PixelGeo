package vip.mystery0.pixel.geo.data.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.core.content.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import vip.mystery0.pixel.geo.domain.model.CompassHeading
import vip.mystery0.pixel.geo.domain.model.LocationModel

// Android 传感器实现：同时实现 CompassSensor 和 LocationSensor 接口
// 使用 TYPE_ROTATION_VECTOR 获取平滑方位角，使用 GeomagneticField 计算真北
class AndroidCompassSensor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : CompassSensor, LocationSensor {

    private val sensorManager = context.getSystemService<SensorManager>()
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _headingData = MutableStateFlow(CompassHeading(0f, null))
    override val headingData: Flow<CompassHeading> = _headingData.asStateFlow()

    private val _locationData = MutableStateFlow<LocationModel?>(null)
    override val locationData: Flow<LocationModel> = _locationData.filterNotNull()

    // 缓存最新位置，用于计算真北磁偏角
    private var currentLocation: Location? = null
    private var currentMagneticHeading: Float = 0f

    // 旋转矢量传感器监听器
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // 使用旋转矩阵计算方位角
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            // 弧度转角度，归一化到 0-360°
            currentMagneticHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (currentMagneticHeading < 0) currentMagneticHeading += 360f

            // 利用当前位置和 GeomagneticField 计算真北
            val trueHeading = currentLocation?.let { loc ->
                val geoField = GeomagneticField(
                    loc.latitude.toFloat(),
                    loc.longitude.toFloat(),
                    loc.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                (currentMagneticHeading + geoField.declination + 360f) % 360f
            }

            _headingData.value = CompassHeading(currentMagneticHeading, trueHeading)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // 位置更新回调
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                currentLocation = location
                _locationData.value = LocationModel(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    horizontalAccuracy = location.accuracy.toDouble()
                )
            }
        }
    }

    override fun start() {
        // 启动旋转矢量传感器（SENSOR_DELAY_UI 约 60ms 更新一次）
        sensorManager?.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        // 启动高精度位置更新（每秒一次）
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // 定位权限未授予时静默忽略，UI 层负责处理降级逻辑
        }
    }

    override fun stop() {
        sensorManager?.unregisterListener(sensorListener)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
