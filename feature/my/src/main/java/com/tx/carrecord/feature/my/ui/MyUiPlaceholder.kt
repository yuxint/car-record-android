package com.tx.carrecord.feature.my.ui

import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.feature.addcar.ui.CarPurchaseDatePickerDialog
import com.tx.carrecord.feature.addcar.ui.AddCarViewModel
import com.tx.carrecord.feature.datatransfer.domain.MyDataTransferTimeCodec
import com.tx.carrecord.feature.datatransfer.ui.DataTransferSection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    val message: String? = null,
)

@HiltViewModel
class MyViewModel @Inject constructor(
    private val appDateContext: AppDateContext,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentDate = appDateContext.now())
        }
        viewModelScope.launch {
            val manualDate = appDateContext.manualNowDateFlow.first()
            _uiState.value = _uiState.value.copy(
                manualNowDate = manualDate,
                manualNowDateText = appDateContext.formatShortDate(manualDate),
            )
        }
        observeDateContext()
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

    fun setManualNowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appDateContext.setManualNowEnabled(enabled)
        }
    }

    fun setManualNowDate(date: LocalDate) {
        viewModelScope.launch {
            appDateContext.setManualNowDate(date)
            _uiState.value = _uiState.value.copy(
                message = "手动日期已更新为 ${appDateContext.formatShortDate(date)}",
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
    onCarEditorPageVisibleChange: (Boolean) -> Unit = {},
) {
    val addCarViewModel: AddCarViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isCarEditorRoute = navBackStackEntry?.destination?.route == MY_CAR_EDITOR_ROUTE
    var showManualDateEditor by remember { mutableStateOf(false) }
    var hasCars by remember { mutableStateOf(false) }

    LaunchedEffect(isCarEditorRoute) {
        onCarEditorPageVisibleChange(isCarEditorRoute)
    }

    NavHost(
        navController = navController,
        startDestination = MY_HOME_ROUTE,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(
            route = MY_HOME_ROUTE,
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
        ) {
            MyHomeContent(
                uiState = uiState,
                hasCars = hasCars,
                versionName = appVersionName(context),
                addCarViewModel = addCarViewModel,
                onCarsAvailabilityChange = { hasCars = it },
                onOpenCarEditor = {
                    navController.navigate(MY_CAR_EDITOR_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onSetManualNowEnabled = viewModel::setManualNowEnabled,
                onShowManualDateEditor = { showManualDateEditor = true },
            )
        }
        composable(
            route = MY_CAR_EDITOR_ROUTE,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(260),
                    initialOffsetX = { fullWidth -> fullWidth },
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(260),
                    targetOffsetX = { fullWidth -> fullWidth },
                )
            },
        ) {
            BackHandler {
                navController.popBackStack()
            }
            com.tx.carrecord.feature.addcar.ui.AddCarEditorPage(
                modifier = Modifier.fillMaxSize(),
                viewModel = addCarViewModel,
                onBackRequested = {
                    navController.popBackStack()
                },
                onEditorClosed = {
                    if (navController.currentBackStackEntry?.destination?.route == MY_CAR_EDITOR_ROUTE) {
                        navController.popBackStack()
                    }
                },
            )
        }
    }

    if (showManualDateEditor) {
        var manualDate by remember(uiState.manualNowDate) { mutableStateOf(uiState.manualNowDate) }
        CarPurchaseDatePickerDialog(
            purchaseDate = manualDate,
            onDismiss = { showManualDateEditor = false },
            onConfirm = {
                manualDate = it
                viewModel.setManualNowDate(it)
                showManualDateEditor = false
            },
        )
    }
}

private const val MY_HOME_ROUTE = "my/home"
private const val MY_CAR_EDITOR_ROUTE = "my/car_editor"

@Composable
private fun MyHomeContent(
    uiState: MyUiState,
    hasCars: Boolean,
    versionName: String,
    addCarViewModel: AddCarViewModel,
    onCarsAvailabilityChange: (Boolean) -> Unit,
    onOpenCarEditor: () -> Unit,
    onSetManualNowEnabled: (Boolean) -> Unit,
    onShowManualDateEditor: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "个人中心", style = MaterialTheme.typography.headlineSmall)

        com.tx.carrecord.feature.addcar.ui.AddCarManagementSection(
            viewModel = addCarViewModel,
            carAgeReferenceDate = uiState.currentDate,
            onOpenAddCarEditorPage = onOpenCarEditor,
            onOpenEditCarEditorPage = onOpenCarEditor,
            onCarsAvailabilityChange = onCarsAvailabilityChange,
        )

        DataTransferSection(
            hasCars = hasCars,
            onImportSuccess = addCarViewModel::refreshCars,
        )

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
                StatusMessage(text = "仅影响本地“当前日期”计算，不会修改历史记录日期。")
                uiState.message?.let {
                    StatusMessage(text = it)
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
                Text(
                    text = "版本号：$versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
