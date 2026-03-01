# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

> **语言约定：** 与用户交流、代码注释、文档内容均使用中文。

## 项目概述

**Pixel Geo（原点罗盘）** 是一款基于 Kotlin Multiplatform (KMP) 的跨端 App，目标平台为 Android 和 iOS。
核心价值：直接从硬件底层获取 WGS-84 原始坐标，绕过国内地图 SDK 的 GCJ-02 加密偏移，同时提供高精度指南针功能。

包名：`vip.mystery0.pixel.geo`

## 构建命令

**Android：**

```bash
# 调试包
./gradlew :composeApp:assembleDebug

# 正式包（需在 local.properties 中配置签名）
./gradlew :composeApp:assembleRelease
```

**iOS：** 在 Xcode 中打开 `iosApp/` 目录后运行，或使用 IDE 运行配置。

**签名配置：** 从 `local.properties` 读取（键名：`storeFile`、`storePassword`、`keyAlias`、`keyPassword`
），支持环境变量兜底。

## 架构设计

整体遵循 **Clean Architecture + MVI（Model-View-Intent）** 单向数据流模式。

### 分层结构（见 `init.md`）

**表现层**（`commonMain`）— Compose Multiplatform 共享 UI。ViewModel 接收用户 `Intent`，通过 `StateFlow`
向 View 发射不可变的 `UiState`。

**领域层**（`commonMain`）— 平台无关的纯业务逻辑：
- `FormatLocationUseCase` — WGS-84 坐标格式转换（十进制度数 DD / 度分秒 DMS）
- `BuildShareTextUseCase` — 根据当前位置和坐标格式偏好生成预置分享文案
- *(注：真北及磁偏角直接从平台底层 API 获取，方位角防抖由 Compose 动画如 `animateFloatAsState`
  处理，不再需要手写低通滤波和真北算法)*

**数据/平台层** — 接口在 `commonMain` 定义，各平台分别实现：

```kotlin
data class LocationModel(
    val latitude: Double,           // 纬度（WGS-84）
    val longitude: Double,          // 经度（WGS-84）
    val altitude: Double,           // 海拔高度（米）
    val horizontalAccuracy: Double  // 水平精度（米）
)

data class CompassHeading(val magneticHeading: Float, val trueHeading: Float?)

interface CompassSensor { val headingData: Flow<CompassHeading>; fun start(); fun stop() }
interface LocationSensor {
    val locationData: Flow<LocationModel>;fun start();fun stop()
}
interface UserPreferencesRepository {
    val northMode: Flow<NorthMode>               // TRUE_NORTH / MAGNETIC_NORTH
    val coordinateFormat: Flow<CoordinateFormat> // DD / DMS
    suspend fun setNorthMode(mode: NorthMode)
    suspend fun setCoordinateFormat(format: CoordinateFormat)
}
```

- `androidMain`：`SensorManager`（使用旋转矢量传感器 `TYPE_ROTATION_VECTOR`）+ `GeomagneticField` (
  计算真北) + `FusedLocationProviderClient`
- `iosMain`：封装 `CLLocationManager`，将 Obj-C Delegate 桥接为 Kotlin Flow，直接提供 `trueHeading`与
  `magneticHeading`

### 多平台开发约定

- 平台差异通过 `expect`/`actual` 机制隔离（见 `Platform.kt`）
- 共享 UI 与业务逻辑放在 `commonMain`
- 硬件传感器实现仅放在 `androidMain` / `iosMain`
- iOS 入口：`iosMain/MainViewController.kt` → `iosApp/ContentView.swift`
- Android 入口：`androidMain/MainActivity.kt`

### 待引入依赖（尚未添加至 Gradle）

| 用途      | 库                                        |
|---------|------------------------------------------|
| 跨端生命周期  | Jetpack Lifecycle (`androidx.lifecycle`) |
| 依赖注入    | Koin（支持 KMP）                             |
| 权限管理    | MOKO Permissions                         |
| 异步/响应式  | Kotlin Coroutines & Flow                 |
| 用户偏好持久化 | AndroidX DataStore Preferences           |

## UI/UX 要求

- 全局深色模式，高对比度工具风格
- 指南针表盘：Canvas 绘制，60fps+ 丝滑旋转
- 经纬度文本：等宽字体，防止高频刷新引起布局抖动
- 拒绝定位权限时：优雅降级为纯磁力计指南针，提供跳转系统设置的入口
- 后台生命周期：进入后台（`onPause` / `sceneDidEnterBackground`）时立即注销所有传感器和 GPS
  监听，回到前台时无缝恢复

## 依赖版本管理

所有版本号统一在 `gradle/libs.versions.toml` 中维护。新增依赖先在此文件声明版本，再在
`build.gradle.kts` 中通过 `libs.*` 别名引用。

## 版本号规则

`versionCode` 由 git 提交数自动计算，`versionName` 取短 commit hash，逻辑在
`composeApp/build.gradle.kts` 中。
