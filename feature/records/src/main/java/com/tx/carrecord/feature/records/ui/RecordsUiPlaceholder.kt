package com.tx.carrecord.feature.records.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.feature.records.data.RecordRepository
import com.tx.carrecord.feature.records.data.RecordSnapshot
import com.tx.carrecord.feature.records.data.SaveRecordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordsUiState(
    val loading: Boolean = false,
    val records: List<RecordSnapshot> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: RecordRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordsUiState(loading = true))
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            when (val result = repository.listRecords()) {
                is RepositoryResult.Success -> {
                    _uiState.value = RecordsUiState(loading = false, records = result.value)
                }
                is RepositoryResult.Failure -> {
                    _uiState.value = RecordsUiState(loading = false, message = result.error.message)
                }
            }
        }
    }

    fun saveRecord(
        itemIDsRaw: String,
        mileageText: String,
        costText: String,
        note: String,
    ) {
        val mileage = mileageText.toIntOrNull()
        if (mileage == null || mileage < 0) {
            _uiState.value = _uiState.value.copy(message = "里程必须是非负整数")
            return
        }
        val cost = costText.toDoubleOrNull()
        if (cost == null || cost < 0.0) {
            _uiState.value = _uiState.value.copy(message = "费用必须是非负数字")
            return
        }

        viewModelScope.launch {
            when (
                val result = repository.saveRecord(
                    SaveRecordRequest(
                        itemIDsRaw = itemIDsRaw,
                        mileage = mileage,
                        cost = cost,
                        note = note,
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(message = "新增记录成功：${result.value.recordId}")
                    refresh()
                }
                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }
}

@Composable
fun RecordsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var itemIDsRaw by remember { mutableStateOf("") }
    var mileageText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "保养记录", style = MaterialTheme.typography.headlineSmall)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "记录概览",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                when {
                    uiState.loading -> StatusLoading(text = "加载记录中...")
                    uiState.records.isEmpty() -> StatusMessage(text = "暂无记录")
                    else -> {
                        Text(
                            text = "记录数量：${uiState.records.size}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        HorizontalDivider()
                        uiState.records.takeLast(5).reversed().forEach { record ->
                            RecordRowCard(
                                date = record.date.toString(),
                                mileage = record.mileage,
                                itemIDsRaw = record.itemIDsRaw,
                                cost = record.cost,
                            )
                        }
                    }
                }

                uiState.message?.let {
                    HorizontalDivider()
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
                    text = "新增记录入口",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = itemIDsRaw,
                    onValueChange = { itemIDsRaw = it },
                    label = { Text("项目ID原始串（|分隔）") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = mileageText,
                    onValueChange = { mileageText = it },
                    label = { Text("里程") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("费用") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                )

                Button(
                    onClick = {
                        viewModel.saveRecord(
                            itemIDsRaw = itemIDsRaw,
                            mileageText = mileageText,
                            costText = costText,
                            note = note,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "保存新增记录")
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
                    Text(text = "刷新列表")
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
private fun StatusMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecordRowCard(
    date: String,
    mileage: Int,
    itemIDsRaw: String,
    cost: Double,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = "里程：${mileage} km",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "项目：$itemIDsRaw",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "费用：$cost",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
