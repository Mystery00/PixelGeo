package vip.mystery0.pixel.geo.di

import org.koin.core.module.Module

// 平台特定 Koin 模块（传感器实现），由各平台分别提供
expect val platformModule: Module
