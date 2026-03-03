package vip.mystery0.pixel.geo.data.repository

import kotlinx.coroutines.flow.Flow
import vip.mystery0.pixel.geo.domain.model.CoordinateFormat
import vip.mystery0.pixel.geo.domain.model.NorthMode

// 用户偏好仓库接口，负责读写持久化的用户设置
interface UserPreferencesRepository {
    val northMode: Flow<NorthMode>               // 当前北极模式（真北/磁北）
    val coordinateFormat: Flow<CoordinateFormat> // 当前坐标格式（DD/DMS）
    suspend fun setNorthMode(mode: NorthMode)
    suspend fun setCoordinateFormat(format: CoordinateFormat)
}
