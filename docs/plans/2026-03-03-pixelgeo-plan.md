# Pixel Geo 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 自底向上实现 Pixel Geo（原点罗盘）—— 一款 KMP 跨平台指南针与 WGS-84 坐标测绘工具。

**Architecture:** Clean Architecture + MVI 单向数据流。数据层（传感器接口 + DataStore）→ 领域层（UseCase）→ 表现层（ViewModel + Compose UI）。平台差异通过 `expect`/`actual` 和 Koin 依赖注入隔离。

**Tech Stack:** Kotlin Multiplatform、Compose Multiplatform、Koin Core + Koin Compose、MOKO Permissions、AndroidX DataStore、Kotlin Coroutines & Flow、Jetpack Lifecycle

---

## Task 1：添加项目依赖

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

**Step 1: 在版本目录中添加新依赖版本**

打开 `gradle/libs.versions.toml`，在 `[versions]` 块中追加：

```toml
koin = "4.0.2"
moko-permissions = "0.18.1"
datastore = "1.1.4"
play-services-location = "21.3.0"
```

在 `[libraries]` 块中追加：

```toml
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
moko-permissions = { module = "dev.icerock.moko:permissions", version.ref = "moko-permissions" }
moko-permissions-compose = { module = "dev.icerock.moko:permissions-compose", version.ref = "moko-permissions" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
play-services-location = { module = "com.google.android.gms:play-services-location", version.ref = "play-services-location" }
```

**Step 2: 在 build.gradle.kts 中引用新依赖**

打开 `composeApp/build.gradle.kts`，在 `sourceSets` 块中添加：

```kotlin
commonMain.dependencies {
    // 已有依赖保持不变，追加以下内容
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.moko.permissions)
    implementation(libs.moko.permissions.compose)
    implementation(libs.androidx.datastore.preferences)
}

androidMain.dependencies {
    // 已有依赖保持不变，追加以下内容
    implementation(libs.koin.android)
    implementation(libs.play.services.location)
}
```

**Step 3: 同步 Gradle 并验证编译通过**

```bash
./gradlew :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "feat: 添加 Koin、DataStore、MOKO Permissions、Location 依赖"
```

---

## Task 2：定义核心数据模型

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/domain/model/LocationModel.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/domain/model/CompassHeading.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/domain/model/NorthMode.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/domain/model/CoordinateFormat.kt`

**Step 1: 创建 LocationModel**

```kotlin
// LocationModel.kt
package vip.mystery0.pixel.geo.domain.model

data class LocationModel(
    val latitude: Double,           // 纬度（WGS-84）
    val longitude: Double,          // 经度（WGS-84）
    val altitude: Double,           // 海拔高度（米）
    val horizontalAccuracy: Double  // 水平精度（米），用于信号质量指示
)
```

**Step 2: 创建 CompassHeading**

```kotlin
// CompassHeading.kt
package vip.mystery0.pixel.geo.domain.model

data class CompassHeading(
    val magneticHeading: Float,     // 磁北方位角（0-359°）
    val trueHeading: Float?         // 真北方位角，GPS 未定位时为 null
)
```

**Step 3: 创建 NorthMode**

```kotlin
// NorthMode.kt
package vip.mystery0.pixel.geo.domain.model

enum class NorthMode {
    MAGNETIC_NORTH,  // 磁北模式
    TRUE_NORTH       // 真北模式
}
```

**Step 4: 创建 CoordinateFormat**

```kotlin
// CoordinateFormat.kt
package vip.mystery0.pixel.geo.domain.model

enum class CoordinateFormat {
    DD,   // 十进制度数：39.123456°N
    DMS   // 度分秒：39°07'24.4"N
}
```

**Step 5: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 添加核心数据模型（LocationModel、CompassHeading、枚举）"
```

---

## Task 3：定义数据层接口

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/data/sensor/CompassSensor.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/data/sensor/LocationSensor.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/data/repository/UserPreferencesRepository.kt`

**Step 1: 创建 CompassSensor 接口**

```kotlin
// CompassSensor.kt
package vip.mystery0.pixel.geo.data.sensor

import kotlinx.coroutines.flow.Flow
import vip.mystery0.pixel.geo.domain.model.CompassHeading

interface CompassSensor {
    val headingData: Flow<CompassHeading>
    fun start()
    fun stop()
}
```

**Step 2: 创建 LocationSensor 接口**

```kotlin
// LocationSensor.kt
package vip.mystery0.pixel.geo.data.sensor

import kotlinx.coroutines.flow.Flow
import vip.mystery0.pixel.geo.domain.model.LocationModel

interface LocationSensor {
    val locationData: Flow<LocationModel>
    fun start()
    fun stop()
}
```

**Step 3: 创建 UserPreferencesRepository 接口**

```kotlin
// UserPreferencesRepository.kt
package vip.mystery0.pixel.geo.data.repository

import kotlinx.coroutines.flow.Flow
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode

interface UserPreferencesRepository {
    val northMode: Flow<NorthMode>               // 当前北极模式
    val coordinateFormat: Flow<CoordinateFormat> // 当前坐标格式
    suspend fun setNorthMode(mode: NorthMode)
    suspend fun setCoordinateFormat(format: CoordinateFormat)
}
```

**Step 4: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 定义 CompassSensor、LocationSensor、UserPreferencesRepository 接口"
```

---

## Task 4：实现 DataStore 工厂（expect/actual）

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/data/datastore/DataStoreFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/vip/mystery0/pixel/geo/data/datastore/DataStoreFactory.android.kt`
- Create: `composeApp/src/iosMain/kotlin/vip/mystery0/pixel/geo/data/datastore/DataStoreFactory.ios.kt`

**Step 1: 定义 commonMain expect 函数**

```kotlin
// DataStoreFactory.kt (commonMain)
package vip.mystery0.pixel.geo.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(): DataStore<Preferences>
```

**Step 2: 实现 Android actual**

```kotlin
// DataStoreFactory.android.kt (androidMain)
package vip.mystery0.pixel.geo.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

actual fun createDataStore(): DataStore<Preferences> {
    return object : KoinComponent {
        val context: Context by inject()
    }.context.dataStore
}
```

**Step 3: 实现 iOS actual**

```kotlin
// DataStoreFactory.ios.kt (iosMain)
package vip.mystery0.pixel.geo.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import platform.Foundation.NSHomeDirectory

actual fun createDataStore(): DataStore<Preferences> {
    return androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            "${NSHomeDirectory()}/Library/Preferences/user_preferences.preferences_pb"
        }
    )
}
```

**Step 4: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/
git commit -m "feat: 实现 DataStore expect/actual 工厂"
```

---

## Task 5：实现 UserPreferencesRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/data/repository/UserPreferencesRepositoryImpl.kt`

**Step 1: 创建实现类**

```kotlin
// UserPreferencesRepositoryImpl.kt (commonMain)
package vip.mystery0.pixel.geo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    companion object {
        private val KEY_NORTH_MODE = stringPreferencesKey("north_mode")
        private val KEY_COORDINATE_FORMAT = stringPreferencesKey("coordinate_format")
    }

    override val northMode: Flow<NorthMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_NORTH_MODE]) {
            NorthMode.TRUE_NORTH.name -> NorthMode.TRUE_NORTH
            else -> NorthMode.MAGNETIC_NORTH  // 默认磁北
        }
    }

    override val coordinateFormat: Flow<CoordinateFormat> = dataStore.data.map { prefs ->
        when (prefs[KEY_COORDINATE_FORMAT]) {
            CoordinateFormat.DMS.name -> CoordinateFormat.DMS
            else -> CoordinateFormat.DD  // 默认 DD 格式
        }
    }

    override suspend fun setNorthMode(mode: NorthMode) {
        dataStore.edit { prefs ->
            prefs[KEY_NORTH_MODE] = mode.name
        }
    }

    override suspend fun setCoordinateFormat(format: CoordinateFormat) {
        dataStore.edit { prefs ->
            prefs[KEY_COORDINATE_FORMAT] = format.name
        }
    }
}
```

**Step 2: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 实现 UserPreferencesRepositoryImpl（DataStore 持久化）"
```

---

## Task 6：实现 Android 传感器

**Files:**
- Create: `composeApp/src/androidMain/kotlin/vip/mystery0/pixel/geo/data/sensor/AndroidCompassSensor.kt`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

**Step 1: 添加权限到 AndroidManifest.xml**

在 `<manifest>` 标签内，`<application>` 标签之前添加：

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

**Step 2: 创建 AndroidCompassSensor**

```kotlin
// AndroidCompassSensor.kt (androidMain)
package vip.mystery0.pixel.geo.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.hardware.GeomagneticField
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

    private var currentLocation: Location? = null
    private var currentMagneticHeading: Float = 0f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // 使用旋转矩阵计算方位角
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            // 弧度转角度，并归一化到 0-360
            currentMagneticHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (currentMagneticHeading < 0) currentMagneticHeading += 360f

            // 利用当前位置计算真北（如果已有位置数据）
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
        // 启动旋转矢量传感器（SENSOR_DELAY_UI 约 60ms 更新一次，适合 UI 展示）
        sensorManager?.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        // 启动位置更新（每秒更新一次，精度优先）
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // 权限未授予时忽略，UI 层会处理降级逻辑
        }
    }

    override fun stop() {
        sensorManager?.unregisterListener(sensorListener)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
```

**Step 3: 验证 Android 编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/androidMain/
git commit -m "feat: 实现 AndroidCompassSensor（TYPE_ROTATION_VECTOR + FusedLocation + GeomagneticField）"
```

---

## Task 7：实现 iOS 传感器

**Files:**
- Create: `composeApp/src/iosMain/kotlin/vip/mystery0/pixel/geo/data/sensor/IOSLocationCompassManager.kt`

**Step 1: 创建 IOSLocationCompassManager**

```kotlin
// IOSLocationCompassManager.kt (iosMain)
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

class IOSLocationCompassManager : CompassSensor, LocationSensor {

    private val locationManager = CLLocationManager()

    private val _headingData = MutableStateFlow(CompassHeading(0f, null))
    override val headingData: Flow<CompassHeading> = _headingData.asStateFlow()

    private val _locationData = MutableStateFlow<LocationModel?>(null)
    override val locationData: Flow<LocationModel> = _locationData.filterNotNull()

    // CLLocationManager Delegate：桥接 Obj-C 回调到 Kotlin Flow
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
            // trueHeading < 0 表示无效（GPS 未就绪），此时返回 null
            val trueHeading = if (didUpdateHeading.trueHeading >= 0) {
                didUpdateHeading.trueHeading.toFloat()
            } else null

            _headingData.value = CompassHeading(
                magneticHeading = didUpdateHeading.magneticHeading.toFloat(),
                trueHeading = trueHeading
            )
        }

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
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        locationManager.startUpdatingHeading()
    }

    override fun stop() {
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
    }
}
```

**Step 2: 验证 iOS 编译通过**

```bash
./gradlew :composeApp:compileKotlinIosArm64
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/iosMain/
git commit -m "feat: 实现 IOSLocationCompassManager（CLLocationManager Delegate 桥接）"
```

---

## Task 8：实现领域层 UseCase

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/domain/usecase/FormatLocationUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/domain/usecase/BuildShareTextUseCase.kt`

**Step 1: 创建 FormatLocationUseCase**

```kotlin
// FormatLocationUseCase.kt
package vip.mystery0.pixel.geo.domain.usecase

import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import kotlin.math.abs

class FormatLocationUseCase {

    /**
     * 格式化纬度
     * DD 示例：39.123456°N
     * DMS 示例：39°07'24.4"N
     */
    fun formatLatitude(value: Double, format: CoordinateFormat): String {
        val direction = if (value >= 0) "N" else "S"
        return formatCoordinate(abs(value), format, direction)
    }

    /**
     * 格式化经度
     * DD 示例：116.123456°E
     * DMS 示例：116°07'24.4"E
     */
    fun formatLongitude(value: Double, format: CoordinateFormat): String {
        val direction = if (value >= 0) "E" else "W"
        return formatCoordinate(abs(value), format, direction)
    }

    /**
     * 格式化海拔高度
     * 示例：123.4 m
     */
    fun formatAltitude(value: Double): String = "%.1f m".format(value)

    private fun formatCoordinate(absValue: Double, format: CoordinateFormat, direction: String): String {
        return when (format) {
            CoordinateFormat.DD -> "%.6f°%s".format(absValue, direction)
            CoordinateFormat.DMS -> {
                val degrees = absValue.toInt()
                val minutesDecimal = (absValue - degrees) * 60
                val minutes = minutesDecimal.toInt()
                val seconds = (minutesDecimal - minutes) * 60
                "%d°%02d'%04.1f\"%s".format(degrees, minutes, seconds, direction)
            }
        }
    }
}
```

**Step 2: 创建 BuildShareTextUseCase**

```kotlin
// BuildShareTextUseCase.kt
package vip.mystery0.pixel.geo.domain.usecase

import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.LocationModel

class BuildShareTextUseCase(
    private val formatLocationUseCase: FormatLocationUseCase
) {

    /**
     * 生成预置分享文案，坐标格式与用户当前设置一致
     */
    fun execute(location: LocationModel, format: CoordinateFormat): String {
        val latitude = formatLocationUseCase.formatLatitude(location.latitude, format)
        val longitude = formatLocationUseCase.formatLongitude(location.longitude, format)
        val altitude = formatLocationUseCase.formatAltitude(location.altitude)

        return """
            📍 我的当前位置（WGS-84）

            纬度：$latitude
            经度：$longitude
            海拔：$altitude

            由 Pixel Geo（原点罗盘）测量
        """.trimIndent()
    }
}
```

**Step 3: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 实现 FormatLocationUseCase 和 BuildShareTextUseCase"
```

---

## Task 9：实现分享功能（expect/actual）

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/util/ShareUtil.kt`
- Create: `composeApp/src/androidMain/kotlin/vip/mystery0/pixel/geo/util/ShareUtil.android.kt`
- Create: `composeApp/src/iosMain/kotlin/vip/mystery0/pixel/geo/util/ShareUtil.ios.kt`

**Step 1: commonMain expect 声明**

```kotlin
// ShareUtil.kt (commonMain)
package vip.mystery0.pixel.geo.util

expect fun shareText(text: String)
```

**Step 2: Android actual 实现**

```kotlin
// ShareUtil.android.kt (androidMain)
package vip.mystery0.pixel.geo.util

import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun shareText(text: String) {
    val context: Context by object : KoinComponent { val ctx: Context by inject() }.let {
        object { val context = it.ctx }
    }.let { return@let { it.context } }.let { return@let it }
    // 更简洁的写法：
    val koin = object : KoinComponent { val ctx: Context by inject() }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    koin.ctx.startActivity(Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
```

> 注意：Android 的 `shareText` 需要访问 `Context`，通过 Koin 注入获取。

**Step 3: iOS actual 实现**

```kotlin
// ShareUtil.ios.kt (iosMain)
package vip.mystery0.pixel.geo.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareText(text: String) {
    val activityViewController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    // 获取当前最顶层的 ViewController 并展示分享面板
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(activityViewController, animated = true, completion = null)
}
```

**Step 4: 验证 Android 编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/
git commit -m "feat: 实现 shareText expect/actual（Android Intent + iOS UIActivityViewController）"
```

---

## Task 10：配置 Koin 依赖注入

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/di/AppModule.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/di/PlatformModule.kt`
- Create: `composeApp/src/androidMain/kotlin/vip/mystery0/pixel/geo/di/PlatformModule.android.kt`
- Create: `composeApp/src/iosMain/kotlin/vip/mystery0/pixel/geo/di/PlatformModule.ios.kt`

**Step 1: 创建 AppModule（共享模块）**

```kotlin
// AppModule.kt (commonMain)
package vip.mystery0.pixel.geo.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import vip.mystery0.pixel.geo.data.datastore.createDataStore
import vip.mystery0.pixel.geo.data.repository.UserPreferencesRepository
import vip.mystery0.pixel.geo.data.repository.UserPreferencesRepositoryImpl
import vip.mystery0.pixel.geo.domain.usecase.BuildShareTextUseCase
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.presentation.CompassViewModel

val appModule = module {
    // DataStore
    single { createDataStore() }

    // Repository
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }

    // Domain
    single { FormatLocationUseCase() }
    single { BuildShareTextUseCase(get()) }

    // ViewModel
    viewModel { CompassViewModel(get(), get(), get(), get(), get()) }
}
```

**Step 2: 定义 PlatformModule expect（commonMain）**

```kotlin
// PlatformModule.kt (commonMain)
package vip.mystery0.pixel.geo.di

import org.koin.core.module.Module

expect val platformModule: Module
```

**Step 3: 实现 Android PlatformModule**

```kotlin
// PlatformModule.android.kt (androidMain)
package vip.mystery0.pixel.geo.di

import com.google.android.gms.location.LocationServices
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import vip.mystery0.pixel.geo.data.sensor.AndroidCompassSensor
import vip.mystery0.pixel.geo.data.sensor.CompassSensor
import vip.mystery0.pixel.geo.data.sensor.LocationSensor

actual val platformModule: Module = module {
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }

    // AndroidCompassSensor 同时实现两个接口，共享同一实例
    single { AndroidCompassSensor(androidContext(), get()) }
    single<CompassSensor> { get<AndroidCompassSensor>() }
    single<LocationSensor> { get<AndroidCompassSensor>() }
}
```

**Step 4: 实现 iOS PlatformModule**

```kotlin
// PlatformModule.ios.kt (iosMain)
package vip.mystery0.pixel.geo.di

import org.koin.core.module.Module
import org.koin.dsl.module
import vip.mystery0.pixel.geo.data.sensor.CompassSensor
import vip.mystery0.pixel.geo.data.sensor.IOSLocationCompassManager
import vip.mystery0.pixel.geo.data.sensor.LocationSensor

actual val platformModule: Module = module {
    // IOSLocationCompassManager 同时实现两个接口，共享同一实例
    single { IOSLocationCompassManager() }
    single<CompassSensor> { get<IOSLocationCompassManager>() }
    single<LocationSensor> { get<IOSLocationCompassManager>() }
}
```

**Step 5: 验证 Android 编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add composeApp/src/
git commit -m "feat: 配置 Koin 依赖注入模块（AppModule + PlatformModule）"
```

---

## Task 11：实现 MVI 状态层（UiState / Intent / ViewModel）

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/presentation/CompassUiState.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/presentation/CompassIntent.kt`
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/presentation/CompassViewModel.kt`

**Step 1: 创建 CompassUiState**

```kotlin
// CompassUiState.kt
package vip.mystery0.pixel.geo.presentation

import vip.mystery0.pixel.geo.domain.model.CompassHeading
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.LocationModel
import vip.mystery0.pixel.geo.domain.model.NorthMode

data class CompassUiState(
    val heading: CompassHeading = CompassHeading(0f, null),
    val location: LocationModel? = null,
    val northMode: NorthMode = NorthMode.MAGNETIC_NORTH,
    val coordinateFormat: CoordinateFormat = CoordinateFormat.DD,
    val isLocationPermissionGranted: Boolean = false,
    val gpsSignalQuality: GpsSignalQuality = GpsSignalQuality.NONE
)

enum class GpsSignalQuality {
    NONE,       // 无信号（未获取到位置）
    EXCELLENT,  // 精度 < 10m，显示绿色
    GOOD,       // 精度 10~50m，显示黄色
    POOR        // 精度 > 50m，显示红色
}
```

**Step 2: 创建 CompassIntent**

```kotlin
// CompassIntent.kt
package vip.mystery0.pixel.geo.presentation

import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode

sealed interface CompassIntent {
    data object RequestLocationPermission : CompassIntent
    data object OpenAppSettings : CompassIntent
    data class ToggleNorthMode(val mode: NorthMode) : CompassIntent
    data class ToggleCoordinateFormat(val format: CoordinateFormat) : CompassIntent
    data object CopyCoordinates : CompassIntent
    data object ShareCoordinates : CompassIntent
}
```

**Step 3: 创建 CompassViewModel**

```kotlin
// CompassViewModel.kt
package vip.mystery0.pixel.geo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vip.mystery0.pixel.geo.data.repository.UserPreferencesRepository
import vip.mystery0.pixel.geo.data.sensor.CompassSensor
import vip.mystery0.pixel.geo.data.sensor.LocationSensor
import vip.mystery0.pixel.geo.domain.usecase.BuildShareTextUseCase
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.util.shareText

class CompassViewModel(
    private val compassSensor: CompassSensor,
    private val locationSensor: LocationSensor,
    private val preferencesRepository: UserPreferencesRepository,
    private val formatLocationUseCase: FormatLocationUseCase,
    private val buildShareTextUseCase: BuildShareTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompassUiState())
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    init {
        // 订阅指南针数据
        viewModelScope.launch {
            compassSensor.headingData.collect { heading ->
                _uiState.update { it.copy(heading = heading) }
            }
        }

        // 订阅位置数据，同步计算 GPS 信号质量
        viewModelScope.launch {
            locationSensor.locationData.collect { location ->
                val quality = when {
                    location.horizontalAccuracy < 10 -> GpsSignalQuality.EXCELLENT
                    location.horizontalAccuracy < 50 -> GpsSignalQuality.GOOD
                    else -> GpsSignalQuality.POOR
                }
                _uiState.update { it.copy(location = location, gpsSignalQuality = quality) }
            }
        }

        // 订阅北极模式偏好
        viewModelScope.launch {
            preferencesRepository.northMode.collect { mode ->
                _uiState.update { it.copy(northMode = mode) }
            }
        }

        // 订阅坐标格式偏好
        viewModelScope.launch {
            preferencesRepository.coordinateFormat.collect { format ->
                _uiState.update { it.copy(coordinateFormat = format) }
            }
        }
    }

    fun handleIntent(intent: CompassIntent) {
        when (intent) {
            is CompassIntent.ToggleNorthMode -> {
                viewModelScope.launch {
                    preferencesRepository.setNorthMode(intent.mode)
                }
            }
            is CompassIntent.ToggleCoordinateFormat -> {
                viewModelScope.launch {
                    preferencesRepository.setCoordinateFormat(intent.format)
                }
            }
            is CompassIntent.ShareCoordinates -> {
                val state = _uiState.value
                state.location?.let { location ->
                    val text = buildShareTextUseCase.execute(location, state.coordinateFormat)
                    shareText(text)
                }
            }
            // 以下意图由 UI 层处理（权限、剪贴板）
            is CompassIntent.CopyCoordinates,
            is CompassIntent.RequestLocationPermission,
            is CompassIntent.OpenAppSettings -> {}
        }
    }

    fun startSensors() {
        compassSensor.start()
        locationSensor.start()
    }

    fun stopSensors() {
        compassSensor.stop()
        locationSensor.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}
```

**Step 4: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 实现 MVI 状态层（CompassUiState、CompassIntent、CompassViewModel）"
```

---

## Task 12：实现指南针表盘 UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/presentation/ui/CompassCanvas.kt`

**Step 1: 创建 CompassCanvas**

```kotlin
// CompassCanvas.kt
package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassCanvas(
    heading: Float?,              // null 表示真北等待态
    isWaitingForGps: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用 spring 动画平滑旋转，避免手写低通滤波
    val animatedHeading by animateFloatAsState(
        targetValue = heading ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "compassHeading"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 表盘（刻度 + 方向标记），整体旋转实现指针效果
        Canvas(
            modifier = modifier
                .size(280.dp)
                .rotate(-animatedHeading)  // 表盘随方位角旋转，指针（N）始终指向正上方
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // 外圆框
            drawCircle(
                color = Color.White,
                radius = radius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 刻度线：每 10° 一个短线，每 30° 一个长线
            for (angle in 0 until 360 step 10) {
                val isMain = angle % 30 == 0
                val lineLength = if (isMain) 20.dp.toPx() else 10.dp.toPx()
                val strokeWidth = if (isMain) 2.dp.toPx() else 1.dp.toPx()
                val lineColor = if (isMain) Color.White else Color.Gray

                val angleRad = Math.toRadians(angle.toDouble())
                val startRadius = radius - lineLength - 4.dp.toPx()

                val startX = center.x + startRadius * sin(angleRad).toFloat()
                val startY = center.y - startRadius * cos(angleRad).toFloat()
                val endX = center.x + (radius - 4.dp.toPx()) * sin(angleRad).toFloat()
                val endY = center.y - (radius - 4.dp.toPx()) * cos(angleRad).toFloat()

                drawLine(
                    color = lineColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 北方红色长刻度线（覆盖普通刻度，突出显示）
            val northLength = 30.dp.toPx()
            drawLine(
                color = Color.Red,
                start = Offset(center.x, center.y - (radius - northLength - 4.dp.toPx())),
                end = Offset(center.x, center.y - (radius - 4.dp.toPx())),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 当前方位角或等待态显示
        if (isWaitingForGps) {
            Text(
                text = "--",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "正在获取 GPS 信号…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            Text(
                text = "%03.0f°".format(heading ?: animatedHeading),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
```

**Step 2: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 实现 CompassCanvas 指南针表盘（刻度 + spring 动画旋转 + 真北等待态）"
```

---

## Task 13：实现坐标数据面板 UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/presentation/ui/LocationDataPanel.kt`

**Step 1: 创建 LocationDataPanel**

```kotlin
// LocationDataPanel.kt
package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.presentation.CompassIntent
import vip.mystery0.pixel.geo.presentation.CompassUiState
import vip.mystery0.pixel.geo.presentation.GpsSignalQuality

@Composable
fun LocationDataPanel(
    uiState: CompassUiState,
    onIntent: (CompassIntent) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val formatUseCase = FormatLocationUseCase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // GPS 信号质量指示器
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val signalColor = when (uiState.gpsSignalQuality) {
                GpsSignalQuality.EXCELLENT -> Color(0xFF4CAF50)  // 绿色
                GpsSignalQuality.GOOD -> Color(0xFFFFC107)       // 黄色
                GpsSignalQuality.POOR -> Color(0xFFF44336)       // 红色
                GpsSignalQuality.NONE -> Color.Gray
            }
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = signalColor)
            }
            Text(
                text = uiState.location?.let { "精度: %.1f m".format(it.horizontalAccuracy) }
                    ?: "无 GPS 信号",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 坐标数据行
        uiState.location?.let { location ->
            CoordinateRow("纬度", formatUseCase.formatLatitude(location.latitude, uiState.coordinateFormat))
            CoordinateRow("经度", formatUseCase.formatLongitude(location.longitude, uiState.coordinateFormat))
            CoordinateRow("海拔", formatUseCase.formatAltitude(location.altitude))
        } ?: Text(
            text = "等待定位数据…",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮行
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 真北/磁北切换
            Button(
                onClick = {
                    val newMode = if (uiState.northMode == NorthMode.MAGNETIC_NORTH)
                        NorthMode.TRUE_NORTH else NorthMode.MAGNETIC_NORTH
                    onIntent(CompassIntent.ToggleNorthMode(newMode))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text(
                    text = if (uiState.northMode == NorthMode.TRUE_NORTH) "真北" else "磁北",
                    color = Color.White
                )
            }

            // DD/DMS 格式切换
            Button(
                onClick = {
                    val newFormat = if (uiState.coordinateFormat == CoordinateFormat.DD)
                        CoordinateFormat.DMS else CoordinateFormat.DD
                    onIntent(CompassIntent.ToggleCoordinateFormat(newFormat))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text(text = uiState.coordinateFormat.name, color = Color.White)
            }

            // 复制坐标（由 UI 层直接操作剪贴板）
            Button(
                onClick = {
                    uiState.location?.let { location ->
                        val text = buildString {
                            appendLine(formatUseCase.formatLatitude(location.latitude, uiState.coordinateFormat))
                            appendLine(formatUseCase.formatLongitude(location.longitude, uiState.coordinateFormat))
                            append(formatUseCase.formatAltitude(location.altitude))
                        }
                        clipboard.setText(AnnotatedString(text))
                    }
                },
                enabled = uiState.location != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text(text = "复制", color = Color.White)
            }

            // 分享坐标（由 ViewModel 调用 shareText）
            Button(
                onClick = { onIntent(CompassIntent.ShareCoordinates) },
                enabled = uiState.location != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text(text = "分享", color = Color.White)
            }
        }
    }
}

@Composable
private fun CoordinateRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
```

**Step 2: 验证编译通过**

```bash
./gradlew :composeApp:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/commonMain/
git commit -m "feat: 实现 LocationDataPanel（GPS 信号指示器 + 坐标展示 + 操作按钮）"
```

---

## Task 14：实现主屏幕并集成权限管理

**Files:**
- Create: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/presentation/ui/CompassScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/vip/mystery0/pixel/geo/App.kt`
- Modify: `composeApp/src/androidMain/kotlin/vip/mystery0/pixel/geo/MainActivity.kt`
- Modify: `composeApp/src/iosMain/kotlin/vip/mystery0/pixel/geo/MainViewController.kt`

**Step 1: 创建 CompassScreen**

```kotlin
// CompassScreen.kt
package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.koin.compose.viewmodel.koinViewModel
import vip.mystery0.pixel.geo.domain.model.NorthMode
import vip.mystery0.pixel.geo.presentation.CompassViewModel

@Composable
fun CompassScreen(viewModel: CompassViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 生命周期管理：前台启动传感器，后台停止传感器
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startSensors()
                Lifecycle.Event.ON_STOP -> viewModel.stopSensors()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // 权限管理：启动时主动请求定位权限
    val factory = rememberPermissionsControllerFactory()
    val controller = factory.createPermissionsController()
    BindEffect(controller)

    LaunchedEffect(Unit) {
        try {
            controller.providePermission(Permission.LOCATION)
        } catch (_: Exception) {
            // 用户拒绝权限，UI 自动降级（传感器无位置数据时 trueHeading 为 null）
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 上半部分：指南针表盘
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val displayHeading = when (uiState.northMode) {
                NorthMode.MAGNETIC_NORTH -> uiState.heading.magneticHeading
                NorthMode.TRUE_NORTH -> uiState.heading.trueHeading
            }
            val isWaitingForGps = uiState.northMode == NorthMode.TRUE_NORTH
                && uiState.heading.trueHeading == null

            CompassCanvas(
                heading = displayHeading,
                isWaitingForGps = isWaitingForGps
            )
        }

        // 下半部分：坐标数据面板
        LocationDataPanel(
            uiState = uiState,
            onIntent = viewModel::handleIntent
        )
    }
}
```

**Step 2: 更新 App.kt，初始化 Koin 并使用深色主题**

```kotlin
// App.kt
package vip.mystery0.pixel.geo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import vip.mystery0.pixel.geo.di.appModule
import vip.mystery0.pixel.geo.di.platformModule
import vip.mystery0.pixel.geo.presentation.ui.CompassScreen

@Composable
fun App() {
    KoinApplication(
        application = {
            modules(appModule, platformModule)
        }
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            CompassScreen()
        }
    }
}
```

**Step 3: 验证 Android 编译通过**

```bash
./gradlew :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/
git commit -m "feat: 实现 CompassScreen 主屏幕，集成 Koin、MOKO Permissions 和生命周期管理"
```

---

## Task 15：真机测试与边界情况验证

**本任务无代码提交，为手工验证步骤。**

**Step 1: 安装到 Android 真机**

```bash
./gradlew :composeApp:installDebug
```

**Step 2: 验证核心功能**

- [ ] 启动时弹出定位权限请求
- [ ] 指南针表盘旋转流畅（60fps+），无明显抖动
- [ ] 磁北模式下方位角正确显示
- [ ] 切换到真北模式：未定位时显示"--"和提示文字，定位后自动切换到真北
- [ ] 坐标数据正确显示（经纬度、海拔）
- [ ] GPS 信号质量指示器颜色随精度变化（绿/黄/红）
- [ ] DD/DMS 格式切换正确
- [ ] 复制功能：坐标正确写入剪贴板
- [ ] 分享功能：弹出系统分享面板，文案格式正确
- [ ] 拒绝权限：不崩溃，仅磁北指南针可用
- [ ] App 退到后台再回到前台：传感器正常暂停/恢复

**Step 3: 如发现 Bug，使用 superpowers:systematic-debugging 排查**

---

**实施计划完成。**
