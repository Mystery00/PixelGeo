# 原点罗盘 (Pixel Geo)

<p align="center">
  <img src="composeApp/src/androidMain/ic_launcher-playstore.png" width="200" alt="Pixel Meter Logo"/>
</p>

<p align="center">
  <strong>一款纯粹、专业的跨平台指南针与经纬度测绘工具。</strong>
</p>

<p align="center">
    <a href="https://github.com/Mystery00/PixelGeo/releases/latest"><img src="https://img.shields.io/github/v/release/Mystery00/PixelGeo" alt="GitHub Release"></a>
    <a href="https://play.google.com/store/apps/details?id=vip.mystery0.pixel.geo"><img src="https://img.shields.io/badge/Google_Play-PixelGeo-green?logo=google-play&logoColor=white" alt="Google Play"></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/Mystery00/PixelGeo" alt="License"></a>
</p>

## 项目简介

**原点罗盘 (Pixel Geo)** 是基于 Kotlin Multiplatform (KMP) 与 Compose Multiplatform 开发的开源工具应用。它专注于提供最原始、最精准的地理位置数据，直接调用硬件 API 获取 **WGS-84** 原始坐标，规避国内地图常见的偏移问题。

### 核心功能

*   **实时方位角**：提供 0° - 359° 高精度朝向显示，支持**真北 (True North)** 与 **磁北 (Magnetic North)** 切换。
*   **WGS-84 原始坐标**：直接输出未经加密偏移的经纬度数据，适合专业测绘与户外探险。
*   **坐标格式切换**：支持“度分秒 (DMS)”与“十进制度数 (DD)”双格式切换。
*   **实时海拔高度**：显示当前位置的海拔数据。
*   **GPS 信号质量监测**：直观反馈水平定位精度（<10m 优秀，10-50m 良好，>50m 较差）。
*   **坐标分享与复制**：一键生成标准坐标文本，支持系统级分享。

## 应用截图

![截图](docs/screenshots/screenshot.png)

## 技术栈

*   **跨平台框架**：Compose Multiplatform (Android / iOS)
*   **架构模式**：Clean Architecture + MVI (Model-View-Intent)
*   **异步处理**：Kotlin Coroutines & Flow
*   **依赖注入**：Koin
*   **权限管理**：MOKO Permissions
*   **本地存储**：AndroidX DataStore Preferences

## 项目结构

*   `composeApp/commonMain`: 共享的 UI (Compose) 与业务逻辑 (ViewModel/UseCase)。
*   `composeApp/androidMain`: Android 平台特定实现（SensorManager, FusedLocation）。
*   `composeApp/iosMain`: iOS 平台特定实现（CLLocationManager）。

## 构建与运行

### Android
```bash
./gradlew :composeApp:assembleDebug
```

### iOS
1. 在 macOS 上使用 Xcode 打开 `iosApp` 目录。
2. 运行 `iosApp` 方案。

## 相关文档

*   [隐私政策](PRIVACY.md)

## 开源协议

本项目采用 [知识共享 署名-非商业性使用-相同方式共享 4.0 国际许可协议 (CC BY-NC-SA 4.0)](LICENSE) 进行许可。

这意味着您可以自由地阅读、修改和分发本项目代码，但必须遵循以下条件：
- **署名**：必须给出适当的署名。
- **非商业性使用**：不得将本项目及其衍生版本用于任何商业目的。
- **相同方式共享**：如果您再混合、转换或基于本项目进行创作，您必须基于与原先许可协议相同的许可协议分发您的贡献。

---
由 Pixel Geo 开发团队维护。
