package vip.mystery0.pixel.geo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import vip.mystery0.pixel.geo.di.appModule
import vip.mystery0.pixel.geo.di.platformModule
import vip.mystery0.pixel.geo.presentation.ui.CompassScreen

typealias KoinAppDeclaration = org.koin.core.KoinApplication.() -> Unit

@Composable
fun App(koinAppDeclaration: KoinAppDeclaration = {}) {
    KoinApplication(
        application = {
            koinAppDeclaration()
            modules(appModule, platformModule)
        }
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            CompassScreen()
        }
    }
}
