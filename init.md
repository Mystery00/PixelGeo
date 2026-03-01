# 跨端指南针 App (CMP) 需求与架构开发指导文档

## 1. 项目概述

本项目旨在利用 Compose Multiplatform (CMP) 技术，开发一款支持 iOS 与 Android 双端的纯粹、专业级指南针与经纬度测绘工具
App。核心目标是在合规的前提下（尤其是针对国区 iOS 设备的 UI 屏蔽），通过底层 API 直接为用户提供实时、精准的
WGS-84 经纬度、海拔及高精度方位角信息。

## 2. 功能需求边界 (Functional Requirements)

### 2.1 核心定向功能

* **实时方位角 (Heading)：** 精确显示设备当前的朝向（0° - 359°）。
* **双北极模式：** 支持用户在 UI 上无缝切换**真北 (True North)** 与 **磁北 (Magnetic North)**。真北数据需结合
  GPS 提供的地理位置进行磁偏角计算。

### 2.2 精确定位数据 (WGS-84)

* **经纬度直出：** 直接获取并显示硬件底层的 WGS-84 原始坐标，以纯文本/数字形式展示，规避国内地图 SDK 的
  GCJ-02 加密偏移问题。
* **格式切换：** 支持“度分秒 (DMS)”与“十进制度数 (DD)”两种常见格式的切换。
* **高程数据：** 实时显示当前海拔高度 (Altitude)。

### 2.3 状态与精度反馈

* **信号指示：** 根据底层返回的 `horizontalAccuracy` (水平精度)，以直观的 UI 元素（如红黄绿状态点及具体误差米数）向用户反馈当前
  GPS 信号质量。

## 3. 非功能性需求 (Non-Functional Requirements)

* **UI/UX 表现：** 采用全局深色模式 (Dark Mode)，高对比度纯粹工具风。指南针表盘需支持 60fps+
  的丝滑旋转，文字展示区采用等宽字体 (Monospaced) 防止高频刷新带来的 UI 闪烁。
* **生命周期与功耗：** App 退入后台 (`onPause`/`sceneDidEnterBackground`) 时，必须立即注销所有传感器和
  GPS 监听以节省电量；切回前台时无缝恢复。
* **优雅降级：** 在用户拒绝定位权限或仅授予“模糊定位”时，App
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
    * `CalculateTrueNorthUseCase`: 根据经纬度和磁北计算真北。
    * `FormatLocationUseCase`: 处理 WGS-84 坐标向不同文本格式的转换。
    * `ApplyLowPassFilterUseCase`: 核心**防抖算法**，对传感器传来的原始方位角高频数据进行低通滤波处理，确保表盘指针不剧烈抖动。

### 4.3 数据与平台硬件层 (Data & Platform Layer)

通过依赖反转（Dependency Inversion），在 Shared 模块定义接口，在 iOS/Android 平台模块分别实现。

* **Shared Interfaces:**
    ```kotlin
    interface CompassSensor {
        val heading: Flow<Float>
        fun start()
        fun stop()
    }

    interface LocationSensor {
        val locationData: Flow<LocationModel> // 包含经纬度、海拔、精度等
        fun start()
        fun stop()
    }
    ```
* **iOS Implementation (`iosMain`):** 封装 `CLLocationManager`，实现上述接口，处理 Objective-C
  Delegate 到 Kotlin Flow 的转换。
* **Android Implementation (`androidMain`):** 封装 `SensorManager` (
  提取地磁和加速度传感器数据合成方向) 以及 `FusedLocationProviderClient`
  。这种分层隔离使得在实体设备上调试特定的硬件传感器行为变得极其干净，不会污染核心业务逻辑。

## 5. 技术栈清单

| 模块         | 技术选型                     | 说明                     |
|:-----------|:-------------------------|:-----------------------|
| **UI 框架**  | Compose Multiplatform    | 统一双端 UI，绘制自定义表盘 Canvas |
| **架构模式**   | MVI (Model-View-Intent)  | 规范高频传感器数据的单向流动         |
| **异步/响应式** | Kotlin Coroutines & Flow | 处理底层传感器回调与数据流转换        |
| **依赖注入**   | Koin                     | 轻量级，支持 KMP 的双端单例/工厂注入  |
| **权限管理**   | MOKO Permissions         | 统一跨端的权限请求 API          |