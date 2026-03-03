package vip.mystery0.pixel.geo.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

// iOS 端使用 NSHomeDirectory 路径创建 DataStore
actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            // 存储到 iOS 沙盒的 Library/Preferences 目录
            "${NSHomeDirectory()}/Library/Preferences/user_preferences.preferences_pb".toPath()
        }
    )
}
