package vip.mystery0.pixel.geo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vip.mystery0.pixel.geo.data.repository.UserPreferencesRepository
import vip.mystery0.pixel.geo.data.sensor.CompassSensor
import vip.mystery0.pixel.geo.data.sensor.LocationSensor
import vip.mystery0.pixel.geo.domain.usecase.BuildShareTextUseCase
import vip.mystery0.pixel.geo.domain.usecase.FormatLocationUseCase
import vip.mystery0.pixel.geo.util.shareText

// 主界面 ViewModel：订阅传感器和偏好数据，处理用户意图，管理传感器生命周期
class CompassViewModel(
    private val compassSensor: CompassSensor,
    private val locationSensor: LocationSensor,
    private val preferencesRepository: UserPreferencesRepository,
    private val formatLocationUseCase: FormatLocationUseCase,
    private val buildShareTextUseCase: BuildShareTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompassUiState())
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    init {
        // 订阅指南针方位角数据
        viewModelScope.launch {
            compassSensor.headingData.collect { heading ->
                _uiState.update { it.copy(heading = heading) }
            }
        }

        // 订阅位置数据，同步计算 GPS 信号质量
        viewModelScope.launch {
            locationSensor.locationData.collect { location ->
                val quality = when {
                    location.horizontalAccuracy < 10 -> GpsSignalQuality.EXCELLENT
                    location.horizontalAccuracy < 50 -> GpsSignalQuality.GOOD
                    else -> GpsSignalQuality.POOR
                }
                _uiState.update { it.copy(location = location, gpsSignalQuality = quality) }
            }
        }

        // 订阅北极模式偏好
        viewModelScope.launch {
            preferencesRepository.northMode.collect { mode ->
                _uiState.update { it.copy(northMode = mode) }
            }
        }

        // 订阅坐标格式偏好
        viewModelScope.launch {
            preferencesRepository.coordinateFormat.collect { format ->
                _uiState.update { it.copy(coordinateFormat = format) }
            }
        }
    }

    // 处理用户意图
    fun handleIntent(intent: CompassIntent) {
        when (intent) {
            is CompassIntent.ToggleNorthMode -> {
                viewModelScope.launch {
                    preferencesRepository.setNorthMode(intent.mode)
                }
            }
            is CompassIntent.ToggleCoordinateFormat -> {
                viewModelScope.launch {
                    preferencesRepository.setCoordinateFormat(intent.format)
                }
            }
            is CompassIntent.ShareCoordinates -> {
                // 获取当前状态，生成分享文案并调用系统分享
                val state = _uiState.value
                state.location?.let { location ->
                    val text = buildShareTextUseCase.execute(location, state.coordinateFormat)
                    shareText(text)
                }
            }
            // 以下意图由 UI 层直接处理（权限、剪贴板等平台操作）
            is CompassIntent.CopyCoordinates,
            is CompassIntent.RequestLocationPermission,
            is CompassIntent.OpenAppSettings -> { /* UI 层处理 */ }
        }
    }

    // 启动传感器（由 Composable 生命周期触发）
    fun startSensors() {
        compassSensor.start()
        locationSensor.start()
    }

    // 停止传感器（由 Composable 生命周期触发）
    fun stopSensors() {
        compassSensor.stop()
        locationSensor.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}
