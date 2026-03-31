package com.tx.carrecord.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.tx.carrecord.feature.my.ui.MyScreen
import com.tx.carrecord.feature.records.ui.RecordsScreen
import com.tx.carrecord.feature.records.ui.RecordsViewModel
import com.tx.carrecord.feature.reminder.ui.ReminderScreen
import java.util.UUID

@Composable
fun CarRecordRoot(
    viewModel: AppShellViewModel = hiltViewModel(),
    recordsViewModel: RecordsViewModel = hiltViewModel(),
) {
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val (isCarEditorPageVisible, setCarEditorPageVisible) = remember { mutableStateOf(false) }
    val (isRecordsAddPageVisible, setRecordsAddPageVisible) = remember { mutableStateOf(false) }
    var recordsAddPageRequestNonce by remember { mutableStateOf<String?>(null) }
    var recordsAddPageSourceRoute by remember { mutableStateOf<RootTabRoute?>(null) }

    val hideBottomBar =
        (selectedRoute == RootTabRoute.MY && isCarEditorPageVisible) ||
            (selectedRoute == RootTabRoute.RECORDS && (isRecordsAddPageVisible || recordsAddPageRequestNonce != null))

    androidx.compose.runtime.LaunchedEffect(Unit) {
        recordsViewModel.refresh()
    }

    val openAddRecordPage = { sourceRoute: RootTabRoute ->
        recordsAddPageSourceRoute = sourceRoute
        recordsViewModel.clearMessage()
        recordsAddPageRequestNonce = UUID.randomUUID().toString()
        viewModel.selectRoute(RootTabRoute.RECORDS)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (!hideBottomBar) {
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
                }
            },
        ) { contentPadding ->
            when (selectedRoute) {
                RootTabRoute.REMINDER -> ReminderScreen(
                    modifier = Modifier.padding(contentPadding),
                    onOpenAddRecordPage = { openAddRecordPage(RootTabRoute.REMINDER) },
                )
                RootTabRoute.RECORDS -> RecordsScreen(
                    modifier = Modifier.padding(contentPadding),
                    viewModel = recordsViewModel,
                    openAddRecordPageRequestNonce = recordsAddPageRequestNonce,
                    onOpenAddRecordPageRequestConsumed = { recordsAddPageRequestNonce = null },
                    openedFromRoute = recordsAddPageSourceRoute,
                    onAddRecordPageClosed = { recordsAddPageSourceRoute = null },
                    onAddRecordPageVisibleChange = setRecordsAddPageVisible,
                    onOpenAddRecordPage = { openAddRecordPage(RootTabRoute.RECORDS) },
                    onReturnToReminderTab = {
                        recordsAddPageSourceRoute = null
                        viewModel.selectRoute(RootTabRoute.REMINDER)
                    },
                )
                RootTabRoute.MY -> MyScreen(
                    modifier = Modifier.padding(contentPadding),
                    onCarEditorPageVisibleChange = setCarEditorPageVisible,
                )
            }
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
