package vip.mystery0.pixel.geo.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import vip.mystery0.pixel.geo.data.datastore.createDataStore
import vip.mystery0.pixel.geo.data.repository.UserPreferencesRepository
import vip.mystery0.pixel.geo.data.repository.UserPreferencesRepositoryImpl
import vip.mystery0.pixel.geo.domain.usecase.BuildShareTextUseCase
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.presentation.CompassViewModel

// 应用通用 Koin 模块：注册数据层和领域层依赖
val appModule = module {
    // DataStore 单例
    single { createDataStore() }

    // 用户偏好仓库
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }

    // 领域层 UseCase
    single { FormatLocationUseCase() }
    single { BuildShareTextUseCase(get()) }

    // 表现层 ViewModel
    viewModel { CompassViewModel(get(), get(), get(), get(), get()) }
}
