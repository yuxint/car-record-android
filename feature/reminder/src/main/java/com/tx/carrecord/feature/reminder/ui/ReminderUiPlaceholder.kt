package com.tx.carrecord.feature.reminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.datastore.AppNavigationContext
import com.tx.carrecord.core.datastore.RootTabRoute
import com.tx.carrecord.feature.reminder.data.ReminderRepository
import com.tx.carrecord.feature.reminder.domain.ReminderProgressColorLevel
import com.tx.carrecord.feature.reminder.domain.ReminderRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReminderUiState(
    val loading: Boolean = false,
    val rows: List<ReminderRow> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val appNavigationContext: AppNavigationContext,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState(loading = true))
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            when (val result = repository.loadAppliedCarRows()) {
                is RepositoryResult.Success -> {
                    _uiState.value = ReminderUiState(
                        loading = false,
                        rows = result.value,
                    )
                }
                is RepositoryResult.Failure -> {
                    _uiState.value = ReminderUiState(
                        loading = false,
                        errorMessage = result.error.message,
                    )
                }
            }
        }
    }

    fun goToRecordsTab() {
        viewModelScope.launch {
            appNavigationContext.requestNavigation(RootTabRoute.RECORDS)
        }
    }

    fun goToMyTab() {
        viewModelScope.launch {
            appNavigationContext.requestNavigation(RootTabRoute.MY)
        }
    }
}

@Composable
fun ReminderScreen(
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "保养提醒", style = MaterialTheme.typography.headlineSmall)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "提醒概览",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                when {
                    uiState.loading -> StatusLoading(text = "加载提醒中...")
                    uiState.errorMessage != null -> StatusMessage(
                        text = uiState.errorMessage.orEmpty(),
                        isError = true,
                    )
                    uiState.rows.isEmpty() -> StatusMessage(text = "暂无提醒数据")
                    else -> {
                        Text(
                            text = "提醒项数量：${uiState.rows.size}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        HorizontalDivider()
                        uiState.rows.take(5).forEach { row ->
                            ReminderRowCard(
                                itemName = row.itemName,
                                progressText = row.progressText,
                                progress = row.displayProgress.toFloat(),
                                details = row.detailTexts,
                                progressColor = progressColorFor(row.progressColorLevel),
                            )
                        }
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
                    text = "快捷操作",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                FilledTonalButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "刷新")
                }
                Button(
                    onClick = viewModel::goToRecordsTab,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "去保养记录")
                }
                FilledTonalButton(
                    onClick = viewModel::goToMyTab,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "去个人中心")
                }
            }
        }
    }
}

@Composable
private fun StatusLoading(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusMessage(
    text: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReminderRowCard(
    itemName: String,
    progressText: String,
    progress: Float,
    details: List<String>,
    progressColor: Color,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyMedium,
                color = progressColor,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            details.forEach { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun progressColorFor(level: ReminderProgressColorLevel): Color {
    return when (level) {
        ReminderProgressColorLevel.NORMAL -> MaterialTheme.colorScheme.primary
        ReminderProgressColorLevel.WARNING -> MaterialTheme.colorScheme.tertiary
        ReminderProgressColorLevel.DANGER -> MaterialTheme.colorScheme.error
    }
}
