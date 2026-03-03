package vip.mystery0.pixel.geo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode

// 基于 DataStore 的用户偏好持久化实现
class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    companion object {
        private val KEY_NORTH_MODE = stringPreferencesKey("north_mode")
        private val KEY_COORDINATE_FORMAT = stringPreferencesKey("coordinate_format")
    }

    // 读取北极模式，默认值为磁北
    override val northMode: Flow<NorthMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_NORTH_MODE]) {
            NorthMode.TRUE_NORTH.name -> NorthMode.TRUE_NORTH
            else -> NorthMode.MAGNETIC_NORTH
        }
    }

    // 读取坐标格式，默认值为 DD
    override val coordinateFormat: Flow<CoordinateFormat> = dataStore.data.map { prefs ->
        when (prefs[KEY_COORDINATE_FORMAT]) {
            CoordinateFormat.DMS.name -> CoordinateFormat.DMS
            else -> CoordinateFormat.DD
        }
    }

    // 写入北极模式
    override suspend fun setNorthMode(mode: NorthMode) {
        dataStore.edit { prefs ->
            prefs[KEY_NORTH_MODE] = mode.name
        }
    }

    // 写入坐标格式
    override suspend fun setCoordinateFormat(format: CoordinateFormat) {
        dataStore.edit { prefs ->
            prefs[KEY_COORDINATE_FORMAT] = format.name
        }
    }
}
