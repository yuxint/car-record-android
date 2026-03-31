package com.tx.carrecord.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tx.carrecord.core.datastore.RootTabRoute
import com.tx.carrecord.feature.addcar.ui.AddCarEditorPage
import com.tx.carrecord.feature.addcar.ui.AddCarManagementSection
import com.tx.carrecord.feature.addcar.ui.AddCarViewModel
import com.tx.carrecord.feature.my.ui.MyScreen
import com.tx.carrecord.feature.records.ui.AddRecordPage
import com.tx.carrecord.feature.records.ui.RecordsScreen
import com.tx.carrecord.feature.records.ui.RecordsViewModel
import com.tx.carrecord.feature.datatransfer.ui.DataTransferSection
import com.tx.carrecord.feature.reminder.ui.ReminderViewModel
import com.tx.carrecord.feature.reminder.ui.ReminderScreen

private const val OVERLAY_PAGE_ANIMATION_DURATION_MS = 260

@Composable
fun CarRecordRoot(
    viewModel: AppShellViewModel = hiltViewModel(),
    addCarViewModel: AddCarViewModel = hiltViewModel(),
    recordsViewModel: RecordsViewModel = hiltViewModel(),
    reminderViewModel: ReminderViewModel = hiltViewModel(),
) {
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val recordsUiState by recordsViewModel.uiState.collectAsState()
    val (isCarEditorPageVisible, setCarEditorPageVisible) = remember { mutableStateOf(false) }
    val (isRecordsAddPageVisible, setRecordsAddPageVisible) = remember { mutableStateOf(false) }
    val (isReminderAddPageVisible, setReminderAddPageVisible) = remember { mutableStateOf(false) }
    val isRecordEditorPageVisible = isRecordsAddPageVisible || isReminderAddPageVisible

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    ROOT_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedRoute == tab.route,
                            onClick = { viewModel.selectRoute(tab.route) },
                            label = { Text(text = tab.label) },
                            icon = {},
                        )
                    }
                }
            },
        ) { contentPadding ->
            when (selectedRoute) {
                RootTabRoute.REMINDER -> ReminderScreen(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    isAddRecordPageVisible = isReminderAddPageVisible,
                    onOpenAddRecordPage = {
                        recordsViewModel.startNewRecordDraft()
                        recordsViewModel.clearMessage()
                        setReminderAddPageVisible(true)
                    },
                )
                RootTabRoute.RECORDS -> RecordsScreen(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    viewModel = recordsViewModel,
                    isAddRecordPageVisible = isRecordsAddPageVisible,
                    onAddRecordPageVisibleChange = setRecordsAddPageVisible,
                )
                RootTabRoute.MY -> MyScreen(
                    modifier = Modifier.padding(contentPadding),
                ) { state ->
                    AddCarManagementSection(
                        viewModel = addCarViewModel,
                        carAgeReferenceDate = state.currentDate,
                        onOpenAddCarEditorPage = { setCarEditorPageVisible(true) },
                        onOpenEditCarEditorPage = { setCarEditorPageVisible(true) },
                    )
                    DataTransferSection(
                    )
                }
            }
        }
        BackHandler(enabled = isCarEditorPageVisible) {
            addCarViewModel.closeCarEditor()
        }
        BackHandler(enabled = isRecordEditorPageVisible) {
            if (isRecordsAddPageVisible) {
                setRecordsAddPageVisible(false)
            } else {
                setReminderAddPageVisible(false)
            }
        }
        AnimatedVisibility(
            visible = isCarEditorPageVisible,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .statusBarsPadding(),
            enter = slideInHorizontally(
                animationSpec = tween(OVERLAY_PAGE_ANIMATION_DURATION_MS),
                initialOffsetX = { fullWidth -> fullWidth },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(OVERLAY_PAGE_ANIMATION_DURATION_MS),
                targetOffsetX = { fullWidth -> fullWidth },
            ),
        ) {
            AddCarEditorPage(
                modifier = Modifier.fillMaxSize(),
                viewModel = addCarViewModel,
                onBackRequested = {
                    addCarViewModel.closeCarEditor()
                },
                onEditorClosed = {
                    setCarEditorPageVisible(false)
                },
            )
        }
        AnimatedVisibility(
            visible = isRecordEditorPageVisible,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .statusBarsPadding(),
            enter = slideInHorizontally(
                animationSpec = tween(OVERLAY_PAGE_ANIMATION_DURATION_MS),
                initialOffsetX = { fullWidth -> fullWidth },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(OVERLAY_PAGE_ANIMATION_DURATION_MS),
                targetOffsetX = { fullWidth -> fullWidth },
            ),
        ) {
            AddRecordPage(
                uiState = recordsUiState,
                isCostReadOnly = recordsViewModel.isCostReadOnly,
                onBackClick = {
                    if (isRecordsAddPageVisible) {
                        setRecordsAddPageVisible(false)
                    } else {
                        setReminderAddPageVisible(false)
                    }
                },
                onSaveClick = recordsViewModel::saveEditorRecord,
                onConfirmIntervalSaveClick = {
                    recordsViewModel.confirmIntervalConfirmation {
                        if (isRecordsAddPageVisible) {
                            setRecordsAddPageVisible(false)
                        } else {
                            setReminderAddPageVisible(false)
                        }
                    }
                },
                onDismissValidationAlert = recordsViewModel::dismissValidationMessage,
                onDismissSaveErrorAlert = recordsViewModel::dismissSaveErrorMessage,
                onDismissIntervalConfirm = recordsViewModel::dismissIntervalConfirmation,
                onIntervalConfirmMileageChange = recordsViewModel::updateIntervalConfirmMileage,
                onIntervalConfirmYearChange = recordsViewModel::updateIntervalConfirmYear,
                onDateClick = recordsViewModel::updateMaintenanceDate,
                onMileageClick = recordsViewModel::updateMileage,
                onItemToggle = recordsViewModel::toggleItem,
                onCostChange = recordsViewModel::updateCostText,
                onNoteChange = recordsViewModel::updateNote,
            )
        }
    }
}

private data class RootTab(
    val route: RootTabRoute,
    val label: String,
)

private val ROOT_TABS: List<RootTab> = listOf(
    RootTab(route = RootTabRoute.REMINDER, label = "保养提醒"),
    RootTab(route = RootTabRoute.RECORDS, label = "保养记录"),
    RootTab(route = RootTabRoute.MY, label = "个人中心"),
)
