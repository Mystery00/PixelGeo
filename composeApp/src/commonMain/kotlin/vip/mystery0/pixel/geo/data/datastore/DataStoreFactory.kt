package vip.mystery0.pixel.geo.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

// DataStore 工厂函数，由各平台分别实现
expect fun createDataStore(): DataStore<Preferences>
