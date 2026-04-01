package com.tx.carrecord.feature.my.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogConsolePage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: AppLogConsoleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "控制台日志") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::refreshLogFile) {
                        Text(text = "刷新")
                    }
                    TextButton(
                        onClick = viewModel::clearLogFile,
                        enabled = uiState.logContent.isNotEmpty(),
                    ) {
                        Text(text = "清空")
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "日志文件",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        SelectionContainer {
                            Text(
                                text = uiState.logFilePath.ifEmpty { "未找到日志文件" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "文件大小：${formatByteSize(uiState.logFileSizeInBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "日志会每秒自动刷新一次。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (uiState.parsedLines.isEmpty()) {
                item {
                    Text(
                        text = "暂无日志输出。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.parsedLines) { line ->
                    SelectionContainer {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = logColor(line),
                        )
                    }
                }
            }
        }
    }
}

private fun formatByteSize(byteSize: Int): String {
    if (byteSize <= 0) {
        return "0 B"
    }

    val sizeUnits = listOf("B", "KB", "MB", "GB")
    var remainingSize = byteSize.toDouble()
    var unitIndex = 0
    while (remainingSize >= 1024 && unitIndex < sizeUnits.lastIndex) {
        remainingSize /= 1024
        unitIndex += 1
    }
    return when (unitIndex) {
        0 -> "${remainingSize.toInt()} ${sizeUnits[unitIndex]}"
        else -> String.format("%.1f %s", remainingSize, sizeUnits[unitIndex])
    }
}

@Composable
private fun logColor(line: String) = when {
    "[ERROR]" in line -> MaterialTheme.colorScheme.error
    "[WARN]" in line -> MaterialTheme.colorScheme.tertiary
    "[INFO]" in line -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
