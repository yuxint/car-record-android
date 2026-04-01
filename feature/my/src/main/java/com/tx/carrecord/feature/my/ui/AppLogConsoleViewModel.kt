package com.tx.carrecord.feature.my.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.datastore.logging.AppLogFileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AppLogConsoleUiState(
    val logFilePath: String = "",
    val logFileSizeInBytes: Int = 0,
    val logContent: String = "",
    val parsedLines: List<String> = emptyList(),
)

@HiltViewModel
class AppLogConsoleViewModel @Inject constructor(
    private val appLogFileStore: AppLogFileStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppLogConsoleUiState())
    val uiState: StateFlow<AppLogConsoleUiState> = _uiState.asStateFlow()

    init {
        refreshLogFile()
        observeAutoRefresh()
    }

    fun refreshLogFile() {
        viewModelScope.launch {
            loadLogFile()
        }
    }

    fun clearLogFile() {
        viewModelScope.launch {
            appLogFileStore.clear()
            loadLogFile()
        }
    }

    private fun observeAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                loadLogFile()
                delay(1000)
            }
        }
    }

    private suspend fun loadLogFile() {
        val content = appLogFileStore.readAll()
        _uiState.value = _uiState.value.copy(
            logFilePath = appLogFileStore.filePath(),
            logFileSizeInBytes = appLogFileStore.currentFileSizeInBytes(),
            logContent = content,
            parsedLines = content
                .lineSequence()
                .filter { it.isNotEmpty() }
                .toList()
                .asReversed(),
        )
    }
}
