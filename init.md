# 《原点罗盘 (Pixel Geo)》跨端开发需求与架构指导文档

## 1. 项目概述

本项目《原点罗盘 (Pixel Geo)》旨在利用 Compose Multiplatform (CMP) 技术，开发一款支持 iOS 与 Android
双端的纯粹、专业级指南针与经纬度测绘工具 App。核心目标是通过底层 API 直接为用户提供实时、精准的 WGS-84
经纬度、海拔及高精度方位角信息。

> **背景说明：** 国区 iOS 系统自带指南针 App 对精确坐标展示存在 UI 屏蔽，本项目绕过系统内置应用直接调用
> 底层硬件 API。根据已知信息，App Store 审核不会拒绝显示 WGS-84 经纬度数据。

## 2. 功能需求边界 (Functional Requirements)

### 2.1 核心定向功能
* **实时方位角 (Heading)：** 精确显示设备当前的朝向（0° - 359°）。
* **双北极模式：** 支持用户在 UI 上无缝切换**真北 (True North)** 与 **磁北 (Magnetic North)**。真北数据需结合
  GPS 提供的地理位置进行磁偏角计算。

### 2.2 精确定位数据 (WGS-84)

* **经纬度直出：** 直接获取并显示硬件底层的 WGS-84 原始坐标，以纯文本/数字形式展示，规避国内地图 SDK 的
  GCJ-02 加密偏移问题。
* **格式切换：** 支持"度分秒 (DMS)"与"十进制度数 (DD)"两种常见格式的切换。
* **高程数据：** 实时显示当前海拔高度 (Altitude)。

> **当前版本范围：** `LocationModel` 仅包含经纬度、海拔、水平精度四个字段（见第 4.3 节），其他字段（速度、
> 行进方向等）暂不需要考虑。

### 2.3 状态与精度反馈

* **信号指示：** 根据底层返回的 `horizontalAccuracy` (水平精度)，以直观的 UI 元素（如红黄绿状态点及具体误差米数）向用户反馈当前
  GPS 信号质量。精度定义：**<10m 为绿色（优秀），10~50m 为黄色（良好），>50m 为红色（较差）**。

### 2.4 坐标交互

* **一键复制：** 用户可点击复制当前坐标（经纬度 + 海拔）到系统剪贴板。
* **分享坐标：** 通过系统分享面板分享坐标，预置分享文案如下：

```
📍 我的当前位置（WGS-84）

纬度：{latitude}
经度：{longitude}
海拔：{altitude} m

由 Pixel Geo（原点罗盘）测量
```

其中坐标格式与当前用户选择的 DD/DMS 设置保持一致。

### 2.5 用户偏好持久化

以下用户设置需在 App 重启后保留，使用 **DataStore** 存储（数据量极小，无需数据库）：

* 真北 / 磁北模式选择
* 坐标格式选择（DD / DMS）

## 3. 非功能性需求 (Non-Functional Requirements)

* **页面结构：** 单页面设计，打开 App 即直接展示完整界面——指南针表盘与坐标文字数据同屏显示，无 Tab
  或二级页面。
* **UI/UX 表现：** 采用全局深色模式 (Dark Mode)，高对比度纯粹工具风。指南针表盘需支持 60fps+
  的丝滑旋转，文字展示区采用等宽字体 (Monospaced) 防止高频刷新带来的 UI 闪烁。
* **生命周期与功耗：** App 退入后台 (`onPause`/`sceneDidEnterBackground`) 时，必须立即注销所有传感器和
  GPS 监听以节省电量；切回前台时无缝恢复。统一引入 **Jetpack Lifecycle (`androidx.lifecycle`)**
  组件以便在跨端共享代码中统一感知并管控前后台状态。
* **优雅降级：** 在用户拒绝定位权限或仅授予"模糊定位"时，App
  自动降级为纯磁力计指南针，并友好提示精确定位功能不可用，提供前往系统设置的快捷入口。

## 4. 系统架构与分层设计 (Architecture & Layers)

项目将严格遵循 **Clean Architecture** 思想，结合 **MVI (Model-View-Intent)** 模式进行单向数据流管理。

### 4.1 表现层 (Presentation Layer - Shared)
* **UI 构建：** 100% 使用 Compose Multiplatform 开发，跨端共享 UI 代码。
* **状态管理：** 使用 Kotlin Coroutines 与 `StateFlow`。ViewModel 接收用户的 `Intent`
  （如切换格式、请求权限），经过业务逻辑处理后，向 View 层发射唯一且不可变的 `UiState`。

### 4.2 领域层 (Domain Layer - Shared)
* **职责：** 封装平台无关的核心纯业务逻辑。
* **核心 UseCase：**
    * `FormatLocationUseCase`: 处理 WGS-84 坐标向不同文本格式的转换。
  * `BuildShareTextUseCase`: 根据当前 `LocationModel` 和坐标格式偏好，生成预置分享文案。
  * _(注：方位角防抖直接由 Compose 动画如 `animateFloatAsState(spring(...))`
    处理，移除手动低通滤波算法。真北与磁北数值直接从平台底层获取，移除手动计算偏角的逻辑)_

### 4.3 数据与平台硬件层 (Data & Platform Layer)
通过依赖反转（Dependency Inversion），在 Shared 模块定义接口，在 iOS/Android 平台模块分别实现。

* **核心数据模型：**
    ```kotlin
    data class LocationModel(
        val latitude: Double,           // 纬度（WGS-84）
        val longitude: Double,          // 经度（WGS-84）
        val altitude: Double,           // 海拔高度（米）
        val horizontalAccuracy: Double  // 水平精度（米），用于信号质量指示
    )
    ```

* **Shared Interfaces:**
    ```kotlin
    data class CompassHeading(
        val magneticHeading: Float,
        val trueHeading: Float? // 如果未获取到 GPS 坐标，则不提供真北
    )

    interface CompassSensor {
        val headingData: Flow<CompassHeading>
        fun start()
        fun stop()
    }

    interface LocationSensor {
        val locationData: Flow<LocationModel>
        fun start()
        fun stop()
    }

    interface UserPreferencesRepository {
        val northMode: Flow<NorthMode>           // TRUE_NORTH / MAGNETIC_NORTH
        val coordinateFormat: Flow<CoordinateFormat> // DD / DMS
        suspend fun setNorthMode(mode: NorthMode)
        suspend fun setCoordinateFormat(format: CoordinateFormat)
    }
    ```

* **iOS Implementation (`iosMain`):** 封装 `CLLocationManager`，实现上述接口，处理 Objective-C
  Delegate 到 Kotlin Flow 的转换。直接提供底层计算好的 `trueHeading` 和 `magneticHeading`。
* **Android Implementation (`androidMain`):** 封装 `SensorManager` (
  使用基于硬件融合的 **旋转矢量传感器 `TYPE_ROTATION_VECTOR`** 获取平滑方位角数据) 以及
  `FusedLocationProviderClient`。同时利用原生的 `GeomagneticField` 计算真北。
  这种分层隔离使得在实体设备上调试特定的硬件传感器行为变得极其干净，不会污染核心业务逻辑。
* **DataStore Implementation：** `UserPreferencesRepository` 的实现基于 AndroidX DataStore
  Preferences，
  通过 `expect`/`actual` 或 Koin 注入适配双端。

## 5. 技术栈清单

| 模块         | 技术选型                     | 说明                            |
|:-----------|:-------------------------|:------------------------------|
| **UI 框架**  | Compose Multiplatform    | 统一双端 UI，绘制自定义表盘 Canvas        |
| **架构模式**   | MVI (Model-View-Intent)  | 规范高频传感器数据的单向流动                |
| **异步/响应式** | Kotlin Coroutines & Flow | 处理底层传感器回调与数据流转换               |
| **依赖注入**   | Koin                     | 轻量级，支持 KMP 的双端单例/工厂注入         |
| **权限管理**   | MOKO Permissions         | 统一跨端的权限请求 API                 |
| **本地存储**   | AndroidX DataStore       | 持久化用户偏好（北极模式、坐标格式），数据量小，无需数据库 |
