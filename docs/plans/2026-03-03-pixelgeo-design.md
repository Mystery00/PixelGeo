# Pixel Geo（原点罗盘）技术设计文档

**文档版本：** 1.0
**创建日期：** 2026-03-03
**项目阶段：** 设计阶段

---

## 1. 项目概述

Pixel Geo（原点罗盘）是一款基于 Kotlin Multiplatform (KMP) 的跨平台指南针与 WGS-84 坐标测绘工具，目标平台为 Android 和 iOS。

### 核心功能
- 实时方位角显示（磁北/真北双模式）
- WGS-84 原始坐标直出（经纬度、海拔）
- 坐标格式切换（DD/DMS）
- GPS 信号质量指示
- 坐标复制与分享

### 技术选型
- **架构模式：** Clean Architecture + MVI
- **UI 框架：** Compose Multiplatform
- **依赖注入：** Koin Core + Koin Compose
- **异步处理：** Kotlin Coroutines & Flow
- **权限管理：** MOKO Permissions
- **本地存储：** AndroidX DataStore Preferences

---

## 2. 架构设计

### 2.1 整体架构

采用标准 Clean Architecture 三层架构：

```
┌─────────────────────────────────────────┐
│      Presentation Layer (commonMain)    │
│  ┌─────────────────────────────────┐   │
│  │ UI (Composables)                │   │
│  │ ViewModel (MVI)                 │   │
│  │ State/Intent                    │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       Domain Layer (commonMain)         │
│  ┌─────────────────────────────────┐   │
│  │ UseCase                         │   │
│  │ - FormatLocationUseCase         │   │
│  │ - BuildShareTextUseCase         │   │
│  │ Models                          │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│    Data Layer (platform-specific)       │
│  ┌─────────────────────────────────┐   │
│  │ Sensors (Android/iOS)           │   │
│  │ Repository (UserPreferences)    │   │
│  │ DataStore (expect/actual)       │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### 2.2 目录结构

```
composeApp/src/
├── commonMain/kotlin/vip/mystery0/pixel/geo/
│   ├── di/
│   │   ├── AppModule.kt
│   │   └── PlatformModule.kt (expect)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── LocationModel.kt
│   │   │   ├── CompassHeading.kt
│   │   │   ├── NorthMode.kt
│   │   │   └── CoordinateFormat.kt
│   │   └── usecase/
│   │       ├── FormatLocationUseCase.kt
│   │       └── BuildShareTextUseCase.kt
│   ├── data/
│   │   ├── sensor/
│   │   │   ├── CompassSensor.kt
│   │   │   └── LocationSensor.kt
│   │   ├── repository/
│   │   │   └── UserPreferencesRepository.kt
│   │   └── datastore/
│   │       └── DataStoreFactory.kt (expect)
│   ├── presentation/
│   │   ├── CompassViewModel.kt
│   │   ├── CompassUiState.kt
│   │   ├── CompassIntent.kt
│   │   └── ui/
│   │       ├── CompassScreen.kt
│   │       ├── CompassCanvas.kt
│   │       └── LocationDataPanel.kt
│   ├── util/
│   │   └── ShareUtil.kt (expect)
│   └── App.kt
│
├── androidMain/kotlin/vip/mystery0/pixel/geo/
│   ├── di/
│   │   └── PlatformModule.android.kt (actual)
│   ├── data/
│   │   ├── sensor/
│   │   │   └── AndroidCompassSensor.kt
│   │   ├── repository/
│   │   │   └── UserPreferencesRepositoryImpl.kt
│   │   └── datastore/
│   │       └── DataStoreFactory.android.kt (actual)
│   ├── util/
│   │   └── ShareUtil.android.kt (actual)
│   └── MainActivity.kt
│
└── iosMain/kotlin/vip/mystery0/pixel/geo/
    ├── di/
    │   └── PlatformModule.ios.kt (actual)
    ├── data/
    │   ├── sensor/
    │   │   └── IOSLocationCompassManager.kt
    │   ├── repository/
    │   │   └── UserPreferencesRepositoryImpl.kt
    │   └── datastore/
    │       └── DataStoreFactory.ios.kt (actual)
    ├── util/
    │   └── ShareUtil.ios.kt (actual)
    └── MainViewController.kt
```

---

## 3. 数据层设计

### 3.1 核心数据模型

```kotlin
// LocationModel.kt
data class LocationModel(
    val latitude: Double,           // 纬度（WGS-84）
    val longitude: Double,          // 经度（WGS-84）
    val altitude: Double,           // 海拔高度（米）
    val horizontalAccuracy: Double  // 水平精度（米）
)

// CompassHeading.kt
data class CompassHeading(
    val magneticHeading: Float,     // 磁北方位角（0-359°）
    val trueHeading: Float?         // 真北方位角，GPS 未定位时为 null
)

// NorthMode.kt
enum class NorthMode {
    MAGNETIC_NORTH,  // 磁北模式
    TRUE_NORTH       // 真北模式
}

// CoordinateFormat.kt
enum class CoordinateFormat {
    DD,   // 十进制度数：39.123456°N
    DMS   // 度分秒：39°07'24.4"N
}
```

### 3.2 传感器接口

```kotlin
// CompassSensor.kt
interface CompassSensor {
    val headingData: Flow<CompassHeading>
    fun start()
    fun stop()
}

// LocationSensor.kt
interface LocationSensor {
    val locationData: Flow<LocationModel>
    fun start()
    fun stop()
}
```

### 3.3 用户偏好接口

```kotlin
// UserPreferencesRepository.kt
interface UserPreferencesRepository {
    val northMode: Flow<NorthMode>
    val coordinateFormat: Flow<CoordinateFormat>
    suspend fun setNorthMode(mode: NorthMode)
    suspend fun setCoordinateFormat(format: CoordinateFormat)
}
```

### 3.4 平台传感器实现

#### Android 实现

**AndroidCompassSensor** 同时实现 `CompassSensor` 和 `LocationSensor` 接口：

- 使用 `TYPE_ROTATION_VECTOR` 传感器获取平滑的方位角数据
- 使用 `FusedLocationProviderClient` 获取位置
- 使用 `GeomagneticField` 计算磁偏角，得出真北
- 单一类内聚所有逻辑，避免数据不同步

**关键实现点：**
```kotlin
class AndroidCompassSensor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : CompassSensor, LocationSensor {

    // 使用 TYPE_ROTATION_VECTOR 获取方位角
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // 计算真北
    val geoField = GeomagneticField(
        location.latitude.toFloat(),
        location.longitude.toFloat(),
        location.altitude.toFloat(),
        System.currentTimeMillis()
    )
    val trueHeading = (magneticHeading + geoField.declination + 360f) % 360f
}
```

#### iOS 实现

**IOSLocationCompassManager** 同时实现两个接口：

- 使用单一 `CLLocationManager` 实例（符合 iOS 最佳实践）
- 直接从 `CLHeading` 获取 `magneticHeading` 和 `trueHeading`
- 通过 Delegate 桥接到 Kotlin Flow

**关键实现点：**
```kotlin
class IOSLocationCompassManager : CompassSensor, LocationSensor {
    private val locationManager = CLLocationManager()

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
            _headingData.value = CompassHeading(
                magneticHeading = didUpdateHeading.magneticHeading.toFloat(),
                trueHeading = if (didUpdateHeading.trueHeading >= 0) {
                    didUpdateHeading.trueHeading.toFloat()
                } else null
            )
        }
    }
}
```

---

## 4. 领域层设计

### 4.1 FormatLocationUseCase

负责坐标格式转换：

**DD 格式：** `39.123456°N`（保留 6 位小数）
**DMS 格式：** `39°07'24.4"N`（度分秒）

```kotlin
class FormatLocationUseCase {
    fun formatLatitude(value: Double, format: CoordinateFormat): String
    fun formatLongitude(value: Double, format: CoordinateFormat): String
    fun formatAltitude(value: Double): String
}
```

### 4.2 BuildShareTextUseCase

生成预置分享文案：

```
📍 我的当前位置（WGS-84）

纬度：39.123456°N
经度：116.123456°E
海拔：123.4 m

由 Pixel Geo（原点罗盘）测量
```

```kotlin
class BuildShareTextUseCase(
    private val formatLocationUseCase: FormatLocationUseCase
) {
    fun execute(location: LocationModel, format: CoordinateFormat): String
}
```

---

## 5. 表现层设计

### 5.1 MVI 架构

#### UI 状态

```kotlin
data class CompassUiState(
    val heading: CompassHeading = CompassHeading(0f, null),
    val location: LocationModel? = null,
    val northMode: NorthMode = NorthMode.MAGNETIC_NORTH,
    val coordinateFormat: CoordinateFormat = CoordinateFormat.DD,
    val isLocationPermissionGranted: Boolean = false,
    val showPermissionRationale: Boolean = false,
    val gpsSignalQuality: GpsSignalQuality = GpsSignalQuality.NONE
)

enum class GpsSignalQuality {
    NONE,      // 无信号
    EXCELLENT, // <10m，绿色
    GOOD,      // 10-50m，黄色
    POOR       // >50m，红色
}
```

#### 用户意图

```kotlin
sealed interface CompassIntent {
    data object RequestLocationPermission : CompassIntent
    data object OpenAppSettings : CompassIntent
    data class ToggleNorthMode(val mode: NorthMode) : CompassIntent
    data class ToggleCoordinateFormat(val format: CoordinateFormat) : CompassIntent
    data object CopyCoordinates : CompassIntent
    data object ShareCoordinates : CompassIntent
}
```

### 5.2 ViewModel

**职责：**
- 订阅传感器数据流
- 订阅用户偏好数据流
- 处理用户意图
- 管理传感器生命周期（在 `onCleared()` 中停止传感器）

**生命周期管理：**
- 在 Composable 中使用 `DisposableEffect` + `LifecycleEventObserver`
- `ON_START` 事件时调用 `viewModel.startSensors()`
- `ON_STOP` 事件时调用 `viewModel.stopSensors()`

### 5.3 UI 组件

#### 布局结构（上下布局）

```
┌─────────────────────────────────┐
│                                 │
│                                 │
│        指南针表盘（Canvas）        │
│                                 │
│                                 │
├─────────────────────────────────┤
│     坐标数据面板                  │
│  - GPS 信号质量指示器             │
│  - 经纬度、海拔                   │
│  - 操作按钮（切换/复制/分享）       │
└─────────────────────────────────┘
```

#### CompassCanvas（指南针表盘）

**设计风格：** 简约工具风

- 圆形外框 + 刻度线（每 10° 短刻度，每 30° 长刻度）
- 四个主方向标记（N/E/S/W）
- 红色指针指向北方
- 当前角度数字显示（如 "045°"）
- 使用 `animateFloatAsState` + `spring()` 实现丝滑旋转

**真北等待态：**
- 当 `trueHeading == null` 且用户选择真北模式时
- 显示 "--" 占位符
- 显示提示文字"正在获取 GPS 信号…"

#### LocationDataPanel（坐标数据面板）

**组件：**
- GPS 信号质量指示器（圆点 + 精度文字）
- 坐标数据行（纬度/经度/海拔，等宽字体）
- 操作按钮行：
  - 真北/磁北切换
  - DD/DMS 格式切换
  - 复制按钮
  - 分享按钮

---

## 6. 依赖注入配置

### 6.1 Koin 模块

```kotlin
// AppModule.kt (commonMain)
val appModule = module {
    // Domain Layer
    single { FormatLocationUseCase() }
    single { BuildShareTextUseCase(get()) }

    // Data Layer
    single<DataStore<Preferences>> { createDataStore() }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }

    // Presentation Layer
    viewModel { CompassViewModel(get(), get(), get(), get(), get()) }
}

// PlatformModule.android.kt
actual val platformModule = module {
    single<Context> { androidContext() }
    single { LocationServices.getFusedLocationProviderClient(get<Context>()) }
    single<CompassSensor> { AndroidCompassSensor(get(), get()).also { it as LocationSensor } }
    single<LocationSensor> { get<CompassSensor>() as LocationSensor }
}

// PlatformModule.ios.kt
actual val platformModule = module {
    single<IOSLocationCompassManager> { IOSLocationCompassManager() }
    single<CompassSensor> { get<IOSLocationCompassManager>() }
    single<LocationSensor> { get<IOSLocationCompassManager>() }
}
```

### 6.2 应用初始化

```kotlin
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

---

## 7. 跨平台适配

### 7.1 DataStore 工厂

```kotlin
// commonMain
expect fun createDataStore(): DataStore<Preferences>

// androidMain
actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(context.filesDir, "user_preferences.preferences_pb").absolutePath }
    )
}

// iosMain
actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { NSHomeDirectory() + "/Library/Preferences/user_preferences.preferences_pb" }
    )
}
```

### 7.2 分享功能

```kotlin
// commonMain
expect fun shareText(text: String)

// androidMain
actual fun shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

// iosMain
actual fun shareText(text: String) {
    val activityViewController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    // 展示分享面板
}
```

---

## 8. 权限管理

### 8.1 权限请求策略

**启动时主动请求（推荐方案）：**

1. App 启动后立即检查定位权限状态
2. 如果未授权，显示权限说明后请求
3. 用户拒绝后：
   - 降级为纯磁力计模式（只显示磁北）
   - 显示"前往设置"按钮
   - 坐标数据区显示"需要定位权限"提示

### 8.2 MOKO Permissions 集成

```kotlin
val permissionState = rememberPermissionState(Permission.LOCATION)

LaunchedEffect(Unit) {
    if (!permissionState.isGranted) {
        permissionState.request()
    }
}
```

---

## 9. 依赖清单

需要在 `gradle/libs.versions.toml` 中添加以下依赖：

```toml
[versions]
koin = "4.0.0"
moko-permissions = "0.18.0"
datastore = "1.1.1"
play-services-location = "21.3.0"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
moko-permissions = { module = "dev.icerock.moko:permissions", version.ref = "moko-permissions" }
moko-permissions-compose = { module = "dev.icerock.moko:permissions-compose", version.ref = "moko-permissions" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
play-services-location = { module = "com.google.android.gms:play-services-location", version.ref = "play-services-location" }
```

---

## 10. 开发顺序（自底向上）

### 阶段 1：数据层
1. 定义核心数据模型（LocationModel、CompassHeading、枚举）
2. 定义传感器接口（CompassSensor、LocationSensor）
3. 实现 Android 传感器（AndroidCompassSensor）
4. 实现 iOS 传感器（IOSLocationCompassManager）
5. 实现 DataStore 工厂（expect/actual）
6. 实现 UserPreferencesRepository

### 阶段 2：领域层
1. 实现 FormatLocationUseCase
2. 实现 BuildShareTextUseCase
3. 编写单元测试

### 阶段 3：表现层
1. 定义 UiState 和 Intent
2. 实现 ViewModel
3. 实现 CompassCanvas（指南针表盘）
4. 实现 LocationDataPanel（坐标数据面板）
5. 实现 CompassScreen（主屏幕）
6. 集成权限管理
7. 实现分享功能（expect/actual）

### 阶段 4：依赖注入
1. 配置 Koin 模块
2. 集成到 App 入口

### 阶段 5：测试与优化
1. 真机测试传感器精度
2. 性能优化（Canvas 绘制、动画流畅度）
3. 边界情况处理（无 GPS 信号、权限拒绝）

---

## 11. 关键技术决策

| 决策点 | 选择方案 | 理由 |
|--------|---------|------|
| 开发顺序 | 自底向上 | 每层都有扎实基础，便于测试 |
| 依赖注入 | Koin Core + Koin Compose | 轻量级，Compose 集成友好 |
| 传感器生命周期 | ViewModel 层管理 | 业务逻辑集中，UI 层无需关心 |
| Android 传感器 | 单一传感器类 | 逻辑内聚，避免数据不同步 |
| iOS 传感器 | 单一管理器类 | 符合 iOS API 设计，避免多实例冲突 |
| DataStore 适配 | expect/actual 工厂 | DataStore 实例共享，只有创建方式平台特定 |
| UI 布局 | 上下布局 | 符合常见指南针 App 习惯，信息层次清晰 |
| 权限请求 | 启动时主动请求 | 用户体验流畅，功能可用性明确 |
| 表盘风格 | 简约工具风 | 清晰易读，性能好，符合专业工具定位 |
| 坐标格式 | 纯函数转换 | 简单直接，易于测试 |
| 分享功能 | expect/actual 函数 | 简单直接，符合平台习惯 |

---

## 12. 风险与挑战

### 12.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Android 传感器精度不足 | 指针抖动严重 | 使用 TYPE_ROTATION_VECTOR + Compose 动画平滑 |
| iOS 真北计算延迟 | 切换真北模式时等待时间长 | 显示"正在获取 GPS 信号"提示 |
| DataStore 跨平台兼容性 | iOS 文件路径问题 | 使用官方推荐的 NSHomeDirectory 路径 |
| 权限拒绝后用户体验 | 功能不可用 | 优雅降级为纯磁力计模式 |

### 12.2 性能风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Canvas 绘制性能 | 低端设备卡顿 | 简化绘制逻辑，避免复杂路径 |
| 传感器高频回调 | CPU 占用高 | 使用 SENSOR_DELAY_UI，Compose 动画自动节流 |
| Flow 订阅泄漏 | 内存泄漏 | 在 ViewModel.onCleared() 中停止传感器 |

---

## 13. 后续扩展方向

- 坐标历史记录
- 轨迹记录与导出
- 多点测距
- 离线地图集成
- Apple Watch 支持

---

**文档结束**
