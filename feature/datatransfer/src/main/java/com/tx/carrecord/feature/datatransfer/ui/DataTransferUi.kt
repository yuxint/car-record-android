package com.tx.carrecord.feature.datatransfer.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import com.tx.carrecord.core.datastore.logging.AppLogger
import com.tx.carrecord.feature.datatransfer.data.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DATA_MANAGEMENT_HINT_TEXT =
    "备份按车型保存保养项目配置与车辆记录。恢复会使用备份内容覆盖当前数据，且在有数据时先二次确认再清空。"
private const val TRANSFER_RESULT_TITLE = "备份恢复结果"
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
    private val appLogger: AppLogger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DataTransferUiState())
    val uiState: StateFlow<DataTransferUiState> = _uiState.asStateFlow()

    fun exportBackup() {
        viewModelScope.launch {
            appLogger.info("开始备份数据")
            when (val result = backupRepository.exportBackupJson()) {
                is RepositoryResult.Success -> {
                    appLogger.info(
                        message = "备份数据已生成",
                        payload = "jsonLength=${result.value.length}",
                    )
                    _uiState.value = _uiState.value.copy(
                        exportJsonPendingWrite = result.value,
                    )
                }

                is RepositoryResult.Failure -> {
                    appLogger.error(
                        message = "备份数据失败",
                        payload = result.error.message,
                    )
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun onExportWriteConsumed() {
        _uiState.value = _uiState.value.copy(exportJsonPendingWrite = null)
    }

    fun onTransferResultConsumed() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun showTransferResult(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun importBackup(
        jsonText: String,
    ) {
        viewModelScope.launch {
            appLogger.info("开始恢复数据")
            when (val result = backupRepository.importBackupJson(jsonText)) {
                is RepositoryResult.Success -> {
                    appLogger.info(
                        message = "恢复数据完成",
                        payload = "cars=${result.value.importedCarCount}, items=${result.value.importedItemOptionCount}, records=${result.value.importedRecordCount}",
                    )
                    _uiState.value = _uiState.value.copy(
                        message = "恢复完成：恢复项目${result.value.importedItemOptionCount}项，恢复车辆${result.value.importedCarCount}辆，恢复保养记录${result.value.importedRecordCount}条。",
                    )
                }

                is RepositoryResult.Failure -> {
                    appLogger.error(
                        message = "恢复数据失败",
                        payload = result.error.message,
                    )
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            appLogger.info("开始重置全部数据")
            when (val result = backupRepository.resetBusinessData()) {
                is RepositoryResult.Success -> {
                    appLogger.info("重置全部数据完成")
                    _uiState.value = _uiState.value.copy(message = null)
                }

                is RepositoryResult.Failure -> {
                    appLogger.error(
                        message = "重置全部数据失败",
                        payload = result.error.message,
                    )
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
            appLogger.info("检查恢复前是否需要二次确认")
            when (val result = backupRepository.hasAnyBusinessData()) {
                is RepositoryResult.Success -> {
                    appLogger.info(
                        message = "是否存在业务数据",
                        payload = result.value,
                    )
                    if (result.value) {
                        onNeedConfirm()
                    } else {
                        onProceedImport()
                    }
                }

                is RepositoryResult.Failure -> {
                    appLogger.error(
                        message = "检查业务数据失败",
                        payload = result.error.message,
                    )
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
            viewModel.onExportWriteConsumed()
            return@rememberLauncherForActivityResult
        }

        val writeResult = writeTextToUri(
            context = context,
            uri = uri,
            text = json,
        )
        if (writeResult) {
            viewModel.showTransferResult("备份成功：${resolveDisplayName(context, uri)}")
        } else {
            viewModel.showTransferResult("备份失败：写入备份文件失败")
        }
        viewModel.onExportWriteConsumed()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        val importText = readTextFromUri(context, uri)
        if (importText == null) {
            viewModel.showTransferResult("恢复失败：读取恢复文件失败")
            return@rememberLauncherForActivityResult
        }
        viewModel.importBackup(importText)
    }

    LaunchedEffect(uiState.exportJsonPendingWrite) {
        if (uiState.exportJsonPendingWrite != null) {
            exportLauncher.launch(buildBackupFilename())
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

    uiState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onTransferResultConsumed,
            title = { Text(text = TRANSFER_RESULT_TITLE) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = viewModel::onTransferResultConsumed) {
                    Text(text = "知道了")
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

private fun resolveDisplayName(
    context: Context,
    uri: Uri,
): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment.orEmpty()
}

private fun buildBackupFilename(): String {
    val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).apply {
        isLenient = false
    }
    return "car-record-backup-${formatter.format(Date())}.json"
}
