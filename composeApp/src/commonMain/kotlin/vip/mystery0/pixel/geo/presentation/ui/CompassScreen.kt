package vip.mystery0.pixel.geo.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.koin.compose.viewmodel.koinViewModel
import vip.mystery0.pixel.geo.domain.model.NorthMode
import vip.mystery0.pixel.geo.presentation.CompassViewModel

@Composable
fun CompassScreen(viewModel: CompassViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 生命周期管理：前台启动传感器，后台停止传感器
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startSensors()
                Lifecycle.Event.ON_STOP -> viewModel.stopSensors()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // 权限管理：启动时主动请求定位权限
    val factory = rememberPermissionsControllerFactory()
    // controller 必须用 remember 缓存，避免 recompose 时重新创建导致权限回调丢失
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    LaunchedEffect(Unit) {
        try {
            controller.providePermission(Permission.LOCATION)
        } catch (_: Exception) {
            // 用户拒绝权限，UI 自动降级（传感器无位置数据时 trueHeading 为 null）
        }
    }

    // 根据当前北极模式计算显示的方位角和等待状态
    val displayHeading = when (uiState.northMode) {
        NorthMode.MAGNETIC_NORTH -> uiState.heading.magneticHeading
        NorthMode.TRUE_NORTH -> uiState.heading.trueHeading
    }
    val isWaitingForGps = uiState.northMode == NorthMode.TRUE_NORTH
        && uiState.heading.trueHeading == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 上半部分：指南针表盘
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CompassCanvas(
                heading = displayHeading,
                isWaitingForGps = isWaitingForGps
            )
        }

        // 下半部分：坐标数据面板
        LocationDataPanel(
            uiState = uiState,
            onIntent = { viewModel.handleIntent(it) }
        )
    }
}
