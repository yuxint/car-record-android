package com.tx.carrecord.feature.datatransfer.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.feature.datatransfer.data.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DATA_MANAGEMENT_HINT_TEXT =
    "备份按车型保存保养项目配置与车辆记录。恢复会使用备份内容覆盖当前数据，且在有数据时先二次确认再清空。"
private const val RESTORE_CONFIRM_TITLE = "确认恢复数据？"
private const val RESTORE_CONFIRM_TEXT = "恢复会先清空当前全部数据，再导入备份文件。"
private const val RESTORE_CONFIRM_ACTION = "确认恢复"
private const val RESET_CONFIRM_TITLE = "确认重置数据？"
private const val RESET_CONFIRM_TEXT = "将清空车辆、保养记录和全部保养项目，且无法恢复。"
private const val RESET_CONFIRM_ACTION = "确认重置"

data class DataTransferUiState(
    val exportJsonPendingWrite: String? = null,
    val message: String? = null,
)

@HiltViewModel
class DataTransferViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataTransferUiState())
    val uiState: StateFlow<DataTransferUiState> = _uiState.asStateFlow()

    fun exportBackup() {
        viewModelScope.launch {
            when (val result = backupRepository.exportBackupJson()) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        exportJsonPendingWrite = result.value,
                        message = "已生成备份内容，请选择文件保存",
                    )
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun onExportWriteConsumed() {
        _uiState.value = _uiState.value.copy(exportJsonPendingWrite = null)
    }

    fun onExportWriteResult(success: Boolean, detail: String? = null) {
        _uiState.value = _uiState.value.copy(
            message = if (success) {
                "备份导出成功"
            } else {
                detail ?: "备份导出失败"
            },
        )
    }

    fun importBackup(
        jsonText: String,
    ) {
        viewModelScope.launch {
            when (val result = backupRepository.importBackupJson(jsonText)) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        message = "导入完成：车辆${result.value.importedCarCount}，项目${result.value.importedItemOptionCount}，记录${result.value.importedRecordCount}",
                    )
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            when (val result = backupRepository.resetBusinessData()) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(message = "已重置全部数据")
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun requestRestoreData(
        onNeedConfirm: () -> Unit,
        onProceedImport: () -> Unit,
    ) {
        viewModelScope.launch {
            when (val result = backupRepository.hasAnyBusinessData()) {
                is RepositoryResult.Success -> {
                    if (result.value) {
                        onNeedConfirm()
                    } else {
                        onProceedImport()
                    }
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }
}

@Composable
fun DataTransferSection(
    modifier: Modifier = Modifier,
    viewModel: DataTransferViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var shouldConfirmRestore by remember { mutableStateOf(false) }
    var shouldConfirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = uiState.exportJsonPendingWrite
        if (uri == null || json == null) {
            viewModel.onExportWriteResult(success = false, detail = "未选择导出文件")
            viewModel.onExportWriteConsumed()
            return@rememberLauncherForActivityResult
        }

        val writeResult = writeTextToUri(
            context = context,
            uri = uri,
            text = json,
        )
        viewModel.onExportWriteResult(
            success = writeResult,
            detail = if (writeResult) null else "写入备份文件失败",
        )
        viewModel.onExportWriteConsumed()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            viewModel.onExportWriteResult(success = false, detail = "未选择恢复文件")
            return@rememberLauncherForActivityResult
        }

        val importText = readTextFromUri(context, uri)
        if (importText == null) {
            viewModel.onExportWriteResult(success = false, detail = "读取恢复文件失败")
            return@rememberLauncherForActivityResult
        }
        viewModel.importBackup(importText)
    }

    LaunchedEffect(uiState.exportJsonPendingWrite) {
        if (uiState.exportJsonPendingWrite != null) {
            exportLauncher.launch("car-record-backup.json")
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            FilledTonalButton(
                onClick = viewModel::exportBackup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "备份数据")
            }

            Button(
                onClick = {
                    viewModel.requestRestoreData(
                        onNeedConfirm = { shouldConfirmRestore = true },
                        onProceedImport = {
                            importLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "恢复数据")
            }

            Button(
                onClick = { shouldConfirmReset = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(text = "重置全部数据")
            }

            StatusMessage(text = DATA_MANAGEMENT_HINT_TEXT)
            uiState.message?.let {
                HorizontalDivider()
                StatusMessage(text = it)
            }
        }
    }

    if (shouldConfirmRestore) {
        AlertDialog(
            onDismissRequest = { shouldConfirmRestore = false },
            title = { Text(text = RESTORE_CONFIRM_TITLE) },
            text = { Text(text = RESTORE_CONFIRM_TEXT) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldConfirmRestore = false
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                ) {
                    Text(text = RESTORE_CONFIRM_ACTION)
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldConfirmRestore = false }) {
                    Text(text = "取消")
                }
            },
        )
    }

    if (shouldConfirmReset) {
        AlertDialog(
            onDismissRequest = { shouldConfirmReset = false },
            title = { Text(text = RESET_CONFIRM_TITLE) },
            text = { Text(text = RESET_CONFIRM_TEXT) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldConfirmReset = false
                        viewModel.resetAllData()
                    },
                ) {
                    Text(text = RESET_CONFIRM_ACTION)
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldConfirmReset = false }) {
                    Text(text = "取消")
                }
            },
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

private fun writeTextToUri(
    context: Context,
    uri: Uri,
    text: String,
): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
            if (writer == null) return false
            writer.write(text)
            writer.flush()
        }
        true
    }.getOrDefault(false)
}

private fun readTextFromUri(
    context: Context,
    uri: Uri,
): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
            reader?.readText()
        }
    }.getOrNull()
}
