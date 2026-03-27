package com.tx.carrecord.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tx.carrecord.core.datastore.RootTabRoute
import com.tx.carrecord.feature.my.ui.MyScreen
import com.tx.carrecord.feature.records.ui.RecordsScreen
import com.tx.carrecord.feature.reminder.ui.ReminderScreen

@Composable
fun CarRecordRoot(
    viewModel: AppShellViewModel = hiltViewModel(),
) {
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val (isCarEditorPageVisible, setCarEditorPageVisible) = remember { mutableStateOf(false) }
    val hideBottomBar = selectedRoute == RootTabRoute.MY && isCarEditorPageVisible

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
            RootTabRoute.REMINDER -> ReminderScreen(modifier = Modifier.padding(contentPadding))
            RootTabRoute.RECORDS -> RecordsScreen(modifier = Modifier.padding(contentPadding))
            RootTabRoute.MY -> MyScreen(
                modifier = Modifier.padding(contentPadding),
                onCarEditorPageVisibleChange = setCarEditorPageVisible,
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
