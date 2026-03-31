package com.tx.carrecord.feature.reminder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig.ProgressColorLevel
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.feature.reminder.data.ReminderRepository
import com.tx.carrecord.feature.reminder.domain.ReminderRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class ReminderUiState(
    val loading: Boolean = false,
    val rows: List<ReminderRow> = emptyList(),
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
    val canAddRecord: Boolean = false,
    val carDisplayName: String? = null,
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val appliedCarContext: AppliedCarContext,
    private val appDateContext: AppDateContext,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState(loading = true))
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        observeAppliedCarChanges()
        observeDateContext()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            when (val result = repository.loadAppliedCarRows()) {
                is RepositoryResult.Success -> {
                    val loadResult = result.value
                    _uiState.value = ReminderUiState(
                        loading = false,
                        rows = loadResult.rows,
                        emptyMessage = loadResult.emptyMessage,
                        canAddRecord = loadResult.canAddRecord,
                        carDisplayName = loadResult.carDisplayName,
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

    private fun observeAppliedCarChanges() {
        viewModelScope.launch {
            appliedCarContext.appliedCarIdFlow
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    private fun observeDateContext() {
        viewModelScope.launch {
            appDateContext.nowFlow()
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }
}

@Composable
fun ReminderScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ReminderViewModel = hiltViewModel(),
    isAddRecordPageVisible: Boolean = false,
    onOpenAddRecordPage: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val showFab by remember(uiState.canAddRecord, scrollState, isAddRecordPageVisible) {
        derivedStateOf {
            uiState.canAddRecord &&
                !scrollState.isScrollInProgress &&
                !isAddRecordPageVisible
        }
    }

    ReminderHomeContent(
        uiState = uiState,
        scrollState = scrollState,
        contentPadding = contentPadding,
        showFab = showFab,
        onOpenAddRecordPage = onOpenAddRecordPage,
    )
}

@Composable
private fun ReminderHomeContent(
    uiState: ReminderUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    contentPadding: PaddingValues,
    showFab: Boolean,
    onOpenAddRecordPage: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "保养提醒", style = MaterialTheme.typography.headlineSmall)

            uiState.carDisplayName?.let { carName ->
                Text(
                    text = carName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                uiState.loading -> StatusLoading(text = "加载提醒中...")
                uiState.errorMessage != null -> StatusMessage(
                    text = uiState.errorMessage.orEmpty(),
                    isError = true,
                )

                uiState.emptyMessage != null -> StatusMessage(text = uiState.emptyMessage.orEmpty())
                uiState.rows.isEmpty() -> StatusMessage(text = "暂无提醒数据")
                else -> {
                    uiState.rows.forEach { row ->
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

        AnimatedVisibility(
            visible = showFab,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            FloatingActionButton(onClick = onOpenAddRecordPage) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
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
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = progressColor,
                )
            }
            ReminderProgressBar(
                progress = progress,
                color = progressColor,
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
private fun ReminderProgressBar(
    progress: Float,
    color: Color,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp),
            ),
    ) {
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun progressColorFor(level: ProgressColorLevel): Color {
    return when (level) {
        ProgressColorLevel.NORMAL -> Color(0xFF2E7D32)
        ProgressColorLevel.WARNING -> Color(0xFFF9A825)
        ProgressColorLevel.DANGER -> Color(0xFFC62828)
    }
}
