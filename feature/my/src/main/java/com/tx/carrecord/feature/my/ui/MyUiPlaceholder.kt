package com.tx.carrecord.feature.my.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.DebugModeContext
import com.tx.carrecord.core.common.ui.AppDatePickerDialog
import com.tx.carrecord.core.datastore.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MyUiState(
    val isManualNowEnabled: Boolean = false,
    val manualNowDateText: String = "",
    val manualNowDate: LocalDate = LocalDate.of(1970, 1, 1),
    val currentDate: LocalDate = LocalDate.of(1970, 1, 1),
    val isDebugModeEnabled: Boolean = false,
    val debugModeStatusMessage: String? = null,
    val message: String? = null,
)

@HiltViewModel
class MyViewModel @Inject constructor(
    private val appDateContext: AppDateContext,
    private val debugModeContext: DebugModeContext,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
    private var debugTapState = DebugTapState()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentDate = appDateContext.now())
        }
        viewModelScope.launch {
            val manualEnabled = appDateContext.isManualNowEnabledFlow.first()
            val manualDate = appDateContext.manualNowDateFlow.first()
            _uiState.value = _uiState.value.copy(
                isManualNowEnabled = manualEnabled,
                manualNowDate = manualDate,
                manualNowDateText = appDateContext.formatShortDate(manualDate),
            )
        }
        viewModelScope.launch {
            val debugModeEnabled = debugModeContext.isDebugModeEnabledFlow.first()
            _uiState.value = _uiState.value.copy(isDebugModeEnabled = debugModeEnabled)
        }
        observeDateContext()
        observeDebugModeContext()
    }

    private fun observeDateContext() {
        viewModelScope.launch {
            appDateContext.isManualNowEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isManualNowEnabled = enabled)
            }
        }
        viewModelScope.launch {
            appDateContext.manualNowDateFlow.collect { date ->
                _uiState.value = _uiState.value.copy(
                    manualNowDate = date,
                    manualNowDateText = appDateContext.formatShortDate(date),
                )
            }
        }
        viewModelScope.launch {
            appDateContext.nowFlow().collect { date ->
                _uiState.value = _uiState.value.copy(currentDate = date)
            }
        }
    }

    private fun observeDebugModeContext() {
        viewModelScope.launch {
            debugModeContext.isDebugModeEnabledFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isDebugModeEnabled = enabled)
            }
        }
    }

    fun setManualNowEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isManualNowEnabled = enabled)
        viewModelScope.launch {
            appDateContext.setManualNowEnabled(enabled)
            appLogger.info(
                if (enabled) "自定义当前日期已开启" else "自定义当前日期已关闭",
                payload = "enabled=$enabled",
            )
        }
    }

    fun setManualNowDate(date: LocalDate) {
        viewModelScope.launch {
            appDateContext.setManualNowDate(date)
            val dateText = appDateContext.formatShortDate(date)
            appLogger.info("手动日期已更新", payload = dateText)
            _uiState.value = _uiState.value.copy(
                message = "手动日期已更新为 $dateText",
            )
        }
    }

    fun handleVersionTap(nowMillis: Long = System.currentTimeMillis()) {
        val lastTapAtMillis = debugTapState.lastTapAtMillis
        if (lastTapAtMillis != null && nowMillis - lastTapAtMillis > DEBUG_TAP_WINDOW_MILLIS) {
            debugTapState = DebugTapState()
        }
        debugTapState = debugTapState.copy(
            tapCount = debugTapState.tapCount + 1,
            lastTapAtMillis = nowMillis,
        )

        if (debugTapState.tapCount < DEBUG_TAP_THRESHOLD) {
            return
        }

        debugTapState = DebugTapState()
        val nextEnabled = !_uiState.value.isDebugModeEnabled
        _uiState.value = _uiState.value.copy(
            isDebugModeEnabled = nextEnabled,
            debugModeStatusMessage = if (nextEnabled) {
                "调试模式已开启，现在可以使用“调试工具”中的时间临时设置和控制台日志。"
            } else {
                "调试模式已关闭。"
            },
        )
        viewModelScope.launch {
            debugModeContext.setDebugModeEnabled(nextEnabled)
            appLogger.info(
                if (nextEnabled) "调试模式已开启" else "调试模式已关闭",
                payload = "enabled=$nextEnabled",
            )
        }
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
}

@Composable
fun MyScreen(
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = hiltViewModel(),
    extraContent: @Composable ColumnScope.(MyUiState) -> Unit = {},
    onOpenLogConsole: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showManualDateEditor by remember { mutableStateOf(false) }

    MyHomeContent(
        modifier = modifier.fillMaxSize(),
        uiState = uiState,
        versionName = appVersionName(context),
        extraContent = extraContent,
        onSetManualNowEnabled = viewModel::setManualNowEnabled,
        onShowManualDateEditor = { showManualDateEditor = true },
        onShowLogConsolePage = onOpenLogConsole,
        onVersionTap = viewModel::handleVersionTap,
    )

    if (showManualDateEditor) {
        var manualDate by remember(uiState.manualNowDate) { mutableStateOf(uiState.manualNowDate) }
        AppDatePickerDialog(
            currentDate = manualDate,
            onDismiss = { showManualDateEditor = false },
            onConfirm = {
                manualDate = it
                viewModel.setManualNowDate(it)
                showManualDateEditor = false
            },
        )
    }
}

@Composable
private fun MyHomeContent(
    modifier: Modifier = Modifier,
    uiState: MyUiState,
    versionName: String,
    extraContent: @Composable ColumnScope.(MyUiState) -> Unit,
    onSetManualNowEnabled: (Boolean) -> Unit,
    onShowManualDateEditor: () -> Unit,
    onShowLogConsolePage: () -> Unit,
    onVersionTap: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "个人中心", style = MaterialTheme.typography.headlineSmall)

        extraContent(uiState)

        if (uiState.isDebugModeEnabled) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "调试工具",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "自定义当前日期",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = uiState.isManualNowEnabled,
                            onCheckedChange = onSetManualNowEnabled,
                        )
                    }
                    if (uiState.isManualNowEnabled) {
                        OutlinedButton(
                            onClick = onShowManualDateEditor,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "手动日期：${uiState.manualNowDateText}")
                        }
                    }
                    OutlinedButton(
                        onClick = onShowLogConsolePage,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "控制台日志")
                    }
                    StatusMessage(text = "仅影响本地“当前日期”计算，不会修改历史记录日期。")
                    uiState.message?.let {
                        StatusMessage(text = it)
                    }
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onVersionTap),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "版本号",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = versionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun appVersionName(context: Context): String {
    return runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName?.trim().orEmpty().ifEmpty { "未知版本" }
    }.getOrDefault("未知版本")
}

private data class DebugTapState(
    val tapCount: Int = 0,
    val lastTapAtMillis: Long? = null,
)

private const val DEBUG_TAP_THRESHOLD: Int = 5
private const val DEBUG_TAP_WINDOW_MILLIS: Long = 1_200L
