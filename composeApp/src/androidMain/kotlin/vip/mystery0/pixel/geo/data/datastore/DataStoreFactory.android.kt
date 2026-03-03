package vip.mystery0.pixel.geo.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Android 端使用 Context 扩展属性创建 DataStore
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

actual fun createDataStore(): DataStore<Preferences> {
    // 通过 Koin 获取 Android Context
    return object : KoinComponent {
        val context: Context by inject()
    }.context.dataStore
}
