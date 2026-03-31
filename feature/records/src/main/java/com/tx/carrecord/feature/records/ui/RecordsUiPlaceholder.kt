package com.tx.carrecord.feature.records.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import android.widget.NumberPicker
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.error.RoomRepositoryErrorMapper
import com.tx.carrecord.core.database.model.CarEntity
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.feature.addcar.ui.AppDatePickerDialog
import com.tx.carrecord.feature.addcar.ui.AppMileagePickerDialog
import com.tx.carrecord.feature.records.data.RecordRepository
import com.tx.carrecord.feature.records.data.RecordSnapshot
import com.tx.carrecord.feature.records.data.SaveRecordIntervalDraft
import com.tx.carrecord.feature.records.data.SaveRecordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RecordsUiState(
    val loading: Boolean = false,
    val records: List<RecordSnapshot> = emptyList(),
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
    val message: String? = null,
    val canAddRecord: Boolean = false,
    val displayMode: RecordDisplayMode = RecordDisplayMode.BY_CYCLE,
    val availableCars: List<RecordCarChoice> = emptyList(),
    val selectedCarId: String? = null,
    val availableItemOptions: List<RecordItemOptionChoice> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val editingRecordId: String? = null,
    val maintenanceDate: LocalDate = LocalDate.now(),
    val mileageWan: Int = 0,
    val mileageQian: Int = 0,
    val mileageBai: Int = 0,
    val costText: String = "0",
    val note: String = "",
    val editorLoading: Boolean = false,
    val isSaving: Boolean = false,
    val validationMessage: String? = null,
    val isValidationAlertPresented: Boolean = false,
    val saveErrorMessage: String? = null,
    val isSaveErrorAlertPresented: Boolean = false,
    val intervalConfirmDrafts: List<RecordIntervalConfirmDraft> = emptyList(),
    val isIntervalConfirmPresented: Boolean = false,
    val pendingDeleteRecord: RecordSnapshot? = null,
    val isDeleteConfirmPresented: Boolean = false,
)

data class RecordIntervalConfirmDraft(
    val id: String,
    val name: String,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val yearInterval: Double,
)

data class RecordCarChoice(
    val id: String,
    val brand: String,
    val modelName: String,
    val mileage: Int,
    val purchaseDate: LocalDate,
)

data class RecordItemOptionChoice(
    val id: String,
    val name: String,
    val catalogKey: String? = null,
    val remindByMileage: Boolean = false,
    val mileageInterval: Int = 0,
    val remindByTime: Boolean = false,
    val monthInterval: Int = 0,
)

data class RecordMileageSegments(
    val wan: Int,
    val qian: Int,
    val bai: Int,
)

data class RecordsLoadResult(
    val records: List<RecordSnapshot>,
    val emptyMessage: String? = null,
    val canAddRecord: Boolean,
    val availableCars: List<RecordCarChoice> = emptyList(),
    val selectedCarId: String? = null,
    val availableItemOptions: List<RecordItemOptionChoice> = emptyList(),
    val maintenanceDate: LocalDate = LocalDate.now(),
    val mileageWan: Int = 0,
    val mileageQian: Int = 0,
    val mileageBai: Int = 0,
)

enum class RecordDisplayMode(val title: String) {
    BY_CYCLE("按周期"),
    BY_ITEM("按项目"),
}

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: RecordRepository,
    private val dao: CarRecordDao,
    private val appliedCarContext: AppliedCarContext,
    private val appDateContext: AppDateContext,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordsUiState(loading = true))
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            when (val result = loadPageData()) {
                is RepositoryResult.Success -> {
                    val loadResult = result.value
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        records = loadResult.records,
                        emptyMessage = loadResult.emptyMessage,
                        canAddRecord = loadResult.canAddRecord,
                        availableCars = loadResult.availableCars,
                        selectedCarId = loadResult.selectedCarId,
                        availableItemOptions = loadResult.availableItemOptions,
                        maintenanceDate = loadResult.maintenanceDate,
                        mileageWan = loadResult.mileageWan,
                        mileageQian = loadResult.mileageQian,
                        mileageBai = loadResult.mileageBai,
                        selectedItemIds = emptySet(),
                        costText = "0",
                        note = "",
                        editorLoading = false,
                        isSaving = false,
                        message = null,
                        validationMessage = null,
                        isValidationAlertPresented = false,
                        saveErrorMessage = null,
                        isSaveErrorAlertPresented = false,
                        intervalConfirmDrafts = emptyList(),
                        isIntervalConfirmPresented = false,
                        pendingDeleteRecord = null,
                        isDeleteConfirmPresented = false,
                    )
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        errorMessage = result.error.message,
                        editorLoading = false,
                        isSaving = false,
                        validationMessage = null,
                        isValidationAlertPresented = false,
                        saveErrorMessage = null,
                        isSaveErrorAlertPresented = false,
                        intervalConfirmDrafts = emptyList(),
                        isIntervalConfirmPresented = false,
                        pendingDeleteRecord = null,
                        isDeleteConfirmPresented = false,
                    )
                }
            }
        }
    }

    fun startNewRecordDraft() {
        val state = _uiState.value
        val selectedCarId = state.selectedCarId ?: state.availableCars.firstOrNull()?.id
        val selectedCar = state.availableCars.firstOrNull { it.id == selectedCarId }
        val mileage = mileageSegmentsFromValue(selectedCar?.mileage ?: 0)
        _uiState.value = state.copy(
            selectedCarId = selectedCarId,
            selectedItemIds = emptySet(),
            editingRecordId = null,
            maintenanceDate = state.maintenanceDate,
            mileageWan = mileage.wan,
            mileageQian = mileage.qian,
            mileageBai = mileage.bai,
            costText = "0",
            note = "",
            validationMessage = null,
            isValidationAlertPresented = false,
            saveErrorMessage = null,
            isSaveErrorAlertPresented = false,
            intervalConfirmDrafts = emptyList(),
            isIntervalConfirmPresented = false,
            pendingDeleteRecord = null,
            isDeleteConfirmPresented = false,
            message = null,
            isSaving = false,
        )
    }

    fun openEditRecord(record: RecordSnapshot) {
        viewModelScope.launch {
            val car = dao.findCarById(record.carId) ?: return@launch
            val availableItemOptions = sortItemOptionsByDefaultOrder(
                carBrand = car.brand,
                carModelName = car.modelName,
                itemOptions = dao.listItemOptionsByCarId(car.id).map { option ->
                    RecordItemOptionChoice(
                        id = option.id,
                        name = option.name,
                        catalogKey = option.catalogKey,
                        remindByMileage = option.remindByMileage,
                        mileageInterval = option.mileageInterval,
                        remindByTime = option.remindByTime,
                        monthInterval = option.monthInterval,
                    )
                },
            )
            val selectedItemIds = MaintenanceItemConfig.parseItemIDs(record.itemIDsRaw)
                .filter { itemId -> availableItemOptions.any { it.id == itemId } }
                .toSet()
            val mileage = mileageSegmentsFromValue(record.mileage)
            _uiState.value = _uiState.value.copy(
                selectedCarId = car.id,
                availableItemOptions = availableItemOptions,
                selectedItemIds = selectedItemIds,
                editingRecordId = record.id,
                maintenanceDate = record.date,
                mileageWan = mileage.wan,
                mileageQian = mileage.qian,
                mileageBai = mileage.bai,
                costText = formatCostText(record.cost),
                note = record.note,
                validationMessage = null,
                isValidationAlertPresented = false,
                saveErrorMessage = null,
                isSaveErrorAlertPresented = false,
                intervalConfirmDrafts = emptyList(),
                isIntervalConfirmPresented = false,
                pendingDeleteRecord = null,
                isDeleteConfirmPresented = false,
                message = null,
                isSaving = false,
            )
        }
    }

    fun selectEditorCar(carId: String) {
        viewModelScope.launch {
            val cars = dao.listCars()
            val car = cars.firstOrNull { it.id == carId } ?: return@launch
            updateEditorForCar(car)
        }
    }

    fun updateMaintenanceDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(maintenanceDate = date)
    }

    fun updateMileage(wan: Int, qian: Int, bai: Int) {
        _uiState.value = _uiState.value.copy(
            mileageWan = wan.coerceAtLeast(0),
            mileageQian = qian.coerceIn(0, 9),
            mileageBai = bai.coerceIn(0, 9),
        )
    }

    fun updateDisplayMode(displayMode: RecordDisplayMode) {
        _uiState.value = _uiState.value.copy(displayMode = displayMode)
    }

    fun toggleItem(optionId: String) {
        if (_uiState.value.availableItemOptions.none { it.id == optionId }) return
        val nextSelected = _uiState.value.selectedItemIds.toMutableSet()
        if (!nextSelected.add(optionId)) {
            nextSelected.remove(optionId)
        }
        _uiState.value = _uiState.value.copy(selectedItemIds = nextSelected)
    }

    fun updateCostText(raw: String) {
        _uiState.value = _uiState.value.copy(costText = sanitizeCostInput(raw))
    }

    fun updateNote(raw: String) {
        _uiState.value = _uiState.value.copy(note = raw)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            message = null,
            validationMessage = null,
            isValidationAlertPresented = false,
            isIntervalConfirmPresented = false,
            intervalConfirmDrafts = emptyList(),
            pendingDeleteRecord = null,
            isDeleteConfirmPresented = false,
        )
    }

    fun saveEditorRecord() {
        val state = _uiState.value
        val selectedCarId = state.selectedCarId
        if (selectedCarId.isNullOrBlank()) {
            showValidationMessage("请先选择车辆。")
            return
        }
        if (state.selectedItemIds.isEmpty()) {
            showValidationMessage("请至少选择一个保养项目。")
            return
        }

        val cost = state.costText.toDoubleOrNull()
        if (cost == null || cost < 0.0) {
            showValidationMessage("费用必须是非负数字。")
            return
        }

        val mileage = mileageValueFromSegments(state.mileageWan, state.mileageQian, state.mileageBai)
        if (mileage < 0) {
            showValidationMessage("里程必须是非负整数。")
            return
        }

        prepareIntervalConfirmationDrafts()
        _uiState.value = _uiState.value.copy(isIntervalConfirmPresented = true)
    }

    fun dismissValidationMessage() {
        _uiState.value = _uiState.value.copy(
            validationMessage = null,
            isValidationAlertPresented = false,
        )
    }

    fun dismissSaveErrorMessage() {
        _uiState.value = _uiState.value.copy(
            saveErrorMessage = null,
            isSaveErrorAlertPresented = false,
        )
    }

    fun dismissIntervalConfirmation() {
        _uiState.value = _uiState.value.copy(
            isIntervalConfirmPresented = false,
        )
    }

    fun updateIntervalConfirmMileage(itemId: String, mileageInterval: Int) {
        _uiState.value = _uiState.value.copy(
            intervalConfirmDrafts = _uiState.value.intervalConfirmDrafts.map { draft ->
                if (draft.id == itemId) {
                    draft.copy(mileageInterval = mileageInterval.coerceIn(1_000, 100_000))
                } else {
                    draft
                }
            },
        )
    }

    fun updateIntervalConfirmYear(itemId: String, yearInterval: Double) {
        _uiState.value = _uiState.value.copy(
            intervalConfirmDrafts = _uiState.value.intervalConfirmDrafts.map { draft ->
                if (draft.id == itemId) {
                    draft.copy(yearInterval = yearInterval.coerceIn(0.5, 10.0))
                } else {
                    draft
                }
            },
        )
    }

    fun requestDeleteRecord(record: RecordSnapshot) {
        _uiState.value = _uiState.value.copy(
            pendingDeleteRecord = record,
            isDeleteConfirmPresented = true,
        )
    }

    fun dismissDeleteRecordConfirmation() {
        _uiState.value = _uiState.value.copy(
            pendingDeleteRecord = null,
            isDeleteConfirmPresented = false,
        )
    }

    fun confirmDeleteRecord() {
        val record = _uiState.value.pendingDeleteRecord ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)
            when (val result = repository.deleteRecord(record.id)) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        pendingDeleteRecord = null,
                        isDeleteConfirmPresented = false,
                        message = "删除记录成功",
                    )
                    refresh()
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveErrorMessage = result.error.message,
                        isSaveErrorAlertPresented = true,
                    )
                }
            }
        }
    }

    fun confirmIntervalConfirmation(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        val selectedCarId = state.selectedCarId
        if (selectedCarId.isNullOrBlank()) {
            return
        }

        val cost = state.costText.toDoubleOrNull() ?: return
        val mileage = mileageValueFromSegments(state.mileageWan, state.mileageQian, state.mileageBai)
        if (state.availableCars.none { it.id == selectedCarId }) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)
            when (
                val result = repository.saveRecord(
                    SaveRecordRequest(
                        recordId = state.editingRecordId,
                        carId = selectedCarId,
                        date = state.maintenanceDate,
                        itemIDsRaw = joinItemIDs(state.selectedItemIds, state.availableItemOptions),
                        cost = cost,
                        mileage = mileage,
                        note = state.note.trim(),
                        intervalDrafts = state.intervalConfirmDrafts.map { draft ->
                            SaveRecordIntervalDraft(
                                itemId = draft.id,
                                remindByMileage = draft.remindByMileage,
                                mileageInterval = draft.mileageInterval,
                                remindByTime = draft.remindByTime,
                                monthInterval = (draft.yearInterval * 12).roundToInt().coerceAtLeast(1),
                            )
                        },
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isIntervalConfirmPresented = false,
                        validationMessage = null,
                        isValidationAlertPresented = false,
                        saveErrorMessage = null,
                        isSaveErrorAlertPresented = false,
                        intervalConfirmDrafts = emptyList(),
                        message = if (state.editingRecordId == null) "新增记录成功" else "更新记录成功",
                        editingRecordId = null,
                    )
                    refresh()
                    onSuccess?.invoke()
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isIntervalConfirmPresented = false,
                        saveErrorMessage = result.error.message,
                        isSaveErrorAlertPresented = true,
                    )
                }
            }
        }
    }

    private fun showValidationMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            validationMessage = message,
            isValidationAlertPresented = true,
            isIntervalConfirmPresented = false,
            intervalConfirmDrafts = emptyList(),
            message = null,
            saveErrorMessage = null,
            isSaveErrorAlertPresented = false,
        )
    }

    private fun prepareIntervalConfirmationDrafts() {
        val state = _uiState.value
        val drafts = state.availableItemOptions
            .filter { it.id in state.selectedItemIds }
            .map { option ->
                RecordIntervalConfirmDraft(
                    id = option.id,
                    name = option.name,
                    remindByMileage = option.remindByMileage,
                    mileageInterval = option.mileageInterval.coerceAtLeast(1_000),
                    remindByTime = option.remindByTime,
                    yearInterval = (option.monthInterval.coerceAtLeast(1) / 12.0).coerceAtLeast(0.5),
                )
            }
        _uiState.value = _uiState.value.copy(
            intervalConfirmDrafts = drafts,
            isIntervalConfirmPresented = drafts.isNotEmpty(),
        )
    }

    private suspend fun loadPageData(): RepositoryResult<RecordsLoadResult> = runCatching {
        val cars = dao.listCars()
        if (cars.isEmpty()) {
            return@runCatching RecordsLoadResult(
                records = emptyList(),
                emptyMessage = "请先在个人中心添加并应用车辆。",
                canAddRecord = false,
            )
        }

        val availableCarIds = cars.mapNotNull { entity ->
            runCatching { UUID.fromString(entity.id) }.getOrNull()
        }
        val rawAppliedCarId = appliedCarContext.rawAppliedCarIdFlow.first()
        val resolvedAppliedCarId = appliedCarContext.resolveAppliedCarId(
            rawId = rawAppliedCarId,
            availableCarIds = availableCarIds,
        )
        val normalizedRawId = resolvedAppliedCarId?.toString().orEmpty()
        if (rawAppliedCarId != normalizedRawId && availableCarIds.isNotEmpty()) {
            appliedCarContext.setRawAppliedCarId(normalizedRawId)
        }

        val selectedCar = cars.firstOrNull { it.id == normalizedRawId } ?: cars.first()
        val targetCarRecords = dao.listRecordsByCarId(selectedCar.id)
        val availableItemOptions = sortItemOptionsByDefaultOrder(
            carBrand = selectedCar.brand,
            carModelName = selectedCar.modelName,
            itemOptions = dao.listItemOptionsByCarId(selectedCar.id).map { option ->
                RecordItemOptionChoice(
                    id = option.id,
                    name = option.name,
                    catalogKey = option.catalogKey,
                    remindByMileage = option.remindByMileage,
                    mileageInterval = option.mileageInterval,
                    remindByTime = option.remindByTime,
                    monthInterval = option.monthInterval,
                )
            },
        )
        val mileage = mileageSegmentsFromValue(selectedCar.mileage)

        RecordsLoadResult(
            records = targetCarRecords.map { entity ->
                RecordSnapshot(
                    id = entity.id,
                    carId = entity.carId,
                    date = fromEpochSeconds(entity.date),
                    itemIDsRaw = entity.itemIDsRaw,
                    cost = entity.cost,
                    mileage = entity.mileage,
                    note = entity.note,
                    cycleKey = entity.cycleKey,
                )
            },
            emptyMessage = if (targetCarRecords.isEmpty()) {
                "当前车辆还没有保养记录，点击右下角“+”开始新增。"
            } else {
                null
            },
            canAddRecord = true,
            availableCars = cars.map {
                RecordCarChoice(
                    id = it.id,
                    brand = it.brand,
                    modelName = it.modelName,
                    mileage = it.mileage,
                    purchaseDate = fromEpochSeconds(it.purchaseDate),
                )
            },
            selectedCarId = selectedCar.id,
            availableItemOptions = availableItemOptions,
            maintenanceDate = appDateContext.now(),
            mileageWan = mileage.wan,
            mileageQian = mileage.qian,
            mileageBai = mileage.bai,
        )
    }.fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
    )

    private suspend fun updateEditorForCar(car: CarEntity) {
        val availableItemOptions = sortItemOptionsByDefaultOrder(
            carBrand = car.brand,
            carModelName = car.modelName,
            itemOptions = dao.listItemOptionsByCarId(car.id).map { option ->
                RecordItemOptionChoice(
                    id = option.id,
                    name = option.name,
                    catalogKey = option.catalogKey,
                    remindByMileage = option.remindByMileage,
                    mileageInterval = option.mileageInterval,
                    remindByTime = option.remindByTime,
                    monthInterval = option.monthInterval,
                )
            },
        )
        val mileage = mileageSegmentsFromValue(car.mileage)
        _uiState.value = _uiState.value.copy(
            selectedCarId = car.id,
            availableItemOptions = availableItemOptions,
            selectedItemIds = emptySet(),
            mileageWan = mileage.wan,
            mileageQian = mileage.qian,
            mileageBai = mileage.bai,
            message = null,
        )
    }

    private fun fromEpochSeconds(epochSeconds: Long): LocalDate =
        Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()

    private fun sanitizeCostInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' }
        if (filtered.isEmpty()) return ""

        var result = ""
        var hasDot = false
        var fractionCount = 0

        for (char in filtered) {
            if (char == '.') {
                if (hasDot) continue
                hasDot = true
                if (result.isEmpty()) {
                    result = "0"
                }
                result += char
                continue
            }

            if (hasDot) {
                if (fractionCount >= 2) continue
                fractionCount += 1
            }
            result += char
        }

        return result
    }

    private fun joinItemIDs(selectedItemIds: Set<String>, availableOptions: List<RecordItemOptionChoice>): String {
        return availableOptions.map { it.id }.filter { it in selectedItemIds }.joinToString("|")
    }

    private fun mileageSegmentsFromValue(mileage: Int): RecordMileageSegments {
        val normalized = mileage.coerceAtLeast(0)
        return RecordMileageSegments(
            wan = normalized / 10_000,
            qian = (normalized % 10_000) / 1_000,
            bai = (normalized % 1_000) / 100,
        )
    }

    private fun mileageValueFromSegments(wan: Int, qian: Int, bai: Int): Int {
        return wan.coerceAtLeast(0) * 10_000 + qian.coerceIn(0, 9) * 1_000 + bai.coerceIn(0, 9) * 100
    }

    private fun sortItemOptionsByDefaultOrder(
        carBrand: String,
        carModelName: String,
        itemOptions: List<RecordItemOptionChoice>,
    ): List<RecordItemOptionChoice> {
        val defaultOrderByKey = MaintenanceItemConfig.modelConfig(
            brand = carBrand,
            modelName = carModelName,
        ).defaultOrderByKey
        return MaintenanceItemConfig.sortItemOptionsByDefaultOrder(
            options = itemOptions,
            defaultOrderByKey = defaultOrderByKey,
            catalogKeySelector = { it.catalogKey },
        )
    }
}

@Composable
fun RecordsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: RecordsViewModel = hiltViewModel(),
    isAddRecordPageVisible: Boolean = false,
    onAddRecordPageVisibleChange: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background),
    ) {
        RecordsListPage(
            uiState = uiState,
            isAddRecordPageVisible = isAddRecordPageVisible,
            contentPadding = contentPadding,
            onAddRecordClick = {
                viewModel.startNewRecordDraft()
                viewModel.clearMessage()
                onAddRecordPageVisibleChange(true)
            },
            onEditRecordClick = { record ->
                viewModel.clearMessage()
                viewModel.openEditRecord(record)
                onAddRecordPageVisibleChange(true)
            },
            onDeleteRecordClick = viewModel::requestDeleteRecord,
            onDisplayModeChange = viewModel::updateDisplayMode,
        )
    }

    if (uiState.isDeleteConfirmPresented && uiState.pendingDeleteRecord != null) {
        val pendingDeleteRecord = uiState.pendingDeleteRecord
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteRecordConfirmation,
            title = { Text(text = "确认删除记录？") },
            text = {
                Text(
                    text = "删除后无法恢复，确定要删除 ${formatDate(pendingDeleteRecord?.date ?: LocalDate.now())} 的这条保养记录吗？",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeleteRecord,
                    enabled = uiState.isSaving == false,
                ) {
                    Text(text = if (uiState.isSaving) "删除中" else "确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteRecordConfirmation) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
private fun RecordsListPage(
    uiState: RecordsUiState,
    isAddRecordPageVisible: Boolean,
    contentPadding: PaddingValues,
    onAddRecordClick: () -> Unit,
    onEditRecordClick: (RecordSnapshot) -> Unit,
    onDeleteRecordClick: (RecordSnapshot) -> Unit,
    onDisplayModeChange: (RecordDisplayMode) -> Unit,
) {
    val scrollState = rememberScrollState()
    val showFab by remember(uiState.canAddRecord, scrollState, isAddRecordPageVisible) {
        derivedStateOf {
            uiState.canAddRecord && !scrollState.isScrollInProgress && !isAddRecordPageVisible
        }
    }
    val selectedCar = uiState.availableCars.firstOrNull { it.id == uiState.selectedCarId }
    val cycleGroups = remember(uiState.records, uiState.availableItemOptions) {
        buildRecordCycleGroups(
            records = uiState.records,
            itemOptions = uiState.availableItemOptions,
        )
    }
    val itemRows = remember(uiState.records, uiState.availableItemOptions) {
        buildRecordItemRows(
            records = uiState.records,
            itemOptions = uiState.availableItemOptions,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "保养记录", style = MaterialTheme.typography.headlineSmall)
            if (selectedCar != null) {
                val selectedCarText = listOf(selectedCar.brand, selectedCar.modelName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                Text(
                    text = selectedCarText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RecordDisplayMode.values().forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.displayMode == mode,
                        onClick = { onDisplayModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = RecordDisplayMode.values().size,
                        ),
                        label = { Text(text = mode.title) },
                        icon = {},
                    )
                }
            }

            when {
                uiState.loading -> StatusLoading(text = "加载保养记录中...")
                uiState.errorMessage != null -> StatusMessage(
                    text = uiState.errorMessage.orEmpty(),
                    isError = true,
                )

                uiState.emptyMessage != null -> StatusMessage(text = uiState.emptyMessage.orEmpty())

                uiState.records.isEmpty() -> StatusMessage(text = "暂无保养记录。")

                uiState.displayMode == RecordDisplayMode.BY_CYCLE -> {
                    cycleGroups.forEach { group ->
                        RecordCycleGroupCard(
                            group = group,
                            onEditRecordClick = onEditRecordClick,
                            onDeleteRecordClick = onDeleteRecordClick,
                        )
                    }
                }

                else -> {
                    itemRows.forEach { row ->
                        RecordItemRowCard(
                            row = row,
                            onEditRecordClick = onEditRecordClick,
                            onDeleteRecordClick = onDeleteRecordClick,
                        )
                    }
                }
            }

            uiState.message?.let {
                StatusMessage(text = it)
            }
        }

        AnimatedVisibility(
            visible = uiState.canAddRecord && showFab,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            FloatingActionButton(onClick = onAddRecordClick) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun AddRecordPage(
    uiState: RecordsUiState,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onConfirmIntervalSaveClick: () -> Unit,
    onDismissValidationAlert: () -> Unit,
    onDismissSaveErrorAlert: () -> Unit,
    onDismissIntervalConfirm: () -> Unit,
    onIntervalConfirmMileageChange: (String, Int) -> Unit,
    onIntervalConfirmYearChange: (String, Double) -> Unit,
    onCarClick: (String) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onMileageClick: (Int, Int, Int) -> Unit,
    onItemToggle: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    var showCarPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMileagePicker by remember { mutableStateOf(false) }
    var showItemPicker by remember { mutableStateOf(false) }

    val selectedCar = uiState.availableCars.firstOrNull { it.id == uiState.selectedCarId }
    val selectedCarText = selectedCar?.let {
        "${it.brand} ${it.modelName}（${formatDate(it.purchaseDate)}）"
    } ?: "未选择"
    val selectedItemsText = when (uiState.selectedItemIds.size) {
        0 -> "请选择（可多选）"
        else -> "已选${uiState.selectedItemIds.size}项"
    }
    val canSubmit = uiState.canAddRecord &&
        uiState.selectedCarId != null &&
        uiState.selectedItemIds.isNotEmpty() &&
        uiState.isSaving == false &&
        uiState.costText.toDoubleOrNull() != null &&
        uiState.isIntervalConfirmPresented == false &&
        uiState.isValidationAlertPresented == false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBackClick) {
                    Text(text = "返回")
                }
                Text(
                    text = if (uiState.editingRecordId == null) "新增保养记录" else "编辑保养记录",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                TextButton(
                    onClick = onSaveClick,
                    enabled = canSubmit,
                ) {
                    Text(text = if (uiState.isSaving) "保存中" else "保存")
                }
            }
            HorizontalDivider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "车辆信息",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        if (uiState.canAddRecord == false) {
                            Text(
                                text = "请先在个人中心添加车辆。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            PickerFieldRow(
                                label = "车辆",
                                value = selectedCarText,
                                onClick = if (uiState.availableCars.size > 1) {
                                    { showCarPicker = true }
                                } else {
                                    null
                                },
                            )
                            PickerFieldRow(
                                label = "保养时间",
                                value = formatDate(uiState.maintenanceDate),
                                onClick = { showDatePicker = true },
                            )
                            PickerFieldRow(
                                label = "当前里程",
                                value = mileageDisplayText(uiState.mileageWan, uiState.mileageQian, uiState.mileageBai),
                                onClick = { showMileagePicker = true },
                            )
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "保养项目",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        if (uiState.availableItemOptions.isEmpty()) {
                            Text(
                                text = "当前车辆还没有可选保养项目。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            PickerFieldRow(
                                label = "选择项目",
                                value = selectedItemsText,
                                onClick = { showItemPicker = true },
                            )
                            if (uiState.selectedItemIds.isNotEmpty()) {
                                Text(
                                    text = uiState.availableItemOptions
                                        .filter { it.id in uiState.selectedItemIds }
                                        .joinToString("，") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "保养费用",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.costText,
                            onValueChange = onCostChange,
                            label = { Text("总费用") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.note,
                            onValueChange = onNoteChange,
                            label = { Text("备注（选填）") },
                            minLines = 2,
                        )
                    }
                }
            }
        }
    }

    if (showCarPicker) {
        AlertDialog(
            onDismissRequest = { showCarPicker = false },
            title = { Text(text = "选择车辆") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    uiState.availableCars.forEach { car ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = car.id == uiState.selectedCarId,
                                onClick = {
                                    onCarClick(car.id)
                                    showCarPicker = false
                                },
                            )
                            Column(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(text = "${car.brand} ${car.modelName}")
                                Text(
                                    text = formatDate(car.purchaseDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCarPicker = false }) {
                    Text(text = "取消")
                }
            },
        )
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            title = "选择保养时间",
            currentDate = uiState.maintenanceDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateClick(it)
                showDatePicker = false
            },
        )
    }

    if (showMileagePicker) {
        AppMileagePickerDialog(
            title = "选择当前里程",
            wan = uiState.mileageWan,
            qian = uiState.mileageQian,
            bai = uiState.mileageBai,
            onDismiss = { showMileagePicker = false },
            onConfirm = { wan, qian, bai ->
                onMileageClick(wan, qian, bai)
                showMileagePicker = false
            },
        )
    }

    if (showItemPicker) {
        AlertDialog(
            onDismissRequest = { showItemPicker = false },
            title = { Text(text = "选择保养项目") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    uiState.availableItemOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = option.id in uiState.selectedItemIds,
                                onCheckedChange = {
                                    onItemToggle(option.id)
                                },
                            )
                            Text(
                                text = option.name,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showItemPicker = false }) {
                    Text(text = "完成")
                }
            },
        )
    }

    if (uiState.isValidationAlertPresented && uiState.validationMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissValidationAlert,
            title = { Text(text = "提示") },
            text = { Text(text = uiState.validationMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = onDismissValidationAlert) {
                    Text(text = "我知道了")
                }
            },
        )
    }

    if (uiState.isSaveErrorAlertPresented && uiState.saveErrorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissSaveErrorAlert,
            title = { Text(text = "保存失败") },
            text = { Text(text = uiState.saveErrorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = onDismissSaveErrorAlert) {
                    Text(text = "我知道了")
                }
            },
        )
    }

    if (uiState.isIntervalConfirmPresented) {
        AlertDialog(
            onDismissRequest = onDismissIntervalConfirm,
            title = { Text(text = "确认下次保养间隔") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "请确认本次保养项目的下次提醒间隔，确认后再保存记录。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    uiState.intervalConfirmDrafts.forEach { draft ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = draft.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            )
                            if (draft.remindByMileage) {
                                NumberAdjustRow(
                                    label = "里程间隔",
                                    value = draft.mileageInterval,
                                    step = 500,
                                    minValue = 1_000,
                                    maxValue = 100_000,
                                    valueText = ::formatReminderMileageText,
                                    onValueChange = { onIntervalConfirmMileageChange(draft.id, it) },
                                )
                            }
                            if (draft.remindByTime) {
                                YearAdjustRow(
                                    label = "时间间隔",
                                    value = draft.yearInterval,
                                    step = 0.5,
                                    minValue = 0.5,
                                    maxValue = 10.0,
                                    onValueChange = { onIntervalConfirmYearChange(draft.id, it) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmIntervalSaveClick,
                    enabled = uiState.isSaving == false,
                ) {
                    Text(text = if (uiState.isSaving) "保存中" else "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissIntervalConfirm) {
                    Text(text = "取消")
                }
            },
        )
    }
}

@Composable
private fun PickerFieldRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val valueColor = if (onClick != null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { base ->
                    if (onClick != null) {
                        base
                            .padding(vertical = 8.dp)
                            .clickable { onClick() }
                    } else {
                        base
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onClick != null) {
                    Text(
                        text = "⌄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun NumberAdjustRow(
    label: String,
    value: Int,
    step: Int,
    minValue: Int,
    maxValue: Int,
    valueText: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$label：${valueText(value)}")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { onValueChange((value - step).coerceAtLeast(minValue)) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(text = "-")
            }
            OutlinedButton(
                onClick = { onValueChange((value + step).coerceAtMost(maxValue)) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(text = "+")
            }
        }
    }
}

@Composable
private fun YearAdjustRow(
    label: String,
    value: Double,
    step: Double,
    minValue: Double,
    maxValue: Double,
    onValueChange: (Double) -> Unit,
) {
    val displayText = yearIntervalText(value)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$label：$displayText")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { onValueChange((value - step).coerceAtLeast(minValue)) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(text = "-")
            }
            OutlinedButton(
                onClick = { onValueChange((value + step).coerceAtMost(maxValue)) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(text = "+")
            }
        }
    }
}

private fun formatReminderMileageText(value: Int): String {
    val safeValue = value.coerceAtLeast(0)
    if (safeValue < 10_000) {
        return "${safeValue}公里"
    }
    val wan = safeValue / 10_000
    val remainder = safeValue % 10_000
    val qian = remainder / 1_000
    val bai = (remainder % 1_000) / 100
    if (qian > 0 || bai > 0) {
        val decimalValue = (qian * 1_000 + bai * 100) / 10_000.0
        val fullString = String.format(Locale.CHINA, "%.1f", decimalValue)
        val decimalPart = fullString
            .substringAfter('.', missingDelimiterValue = "")
            .replace(Regex("^0+|0+$"), "")
        return if (decimalPart.isEmpty()) {
            "${wan}万公里"
        } else {
            "${wan}.${decimalPart}万公里"
        }
    }
    return "${wan}万公里"
}

private fun yearIntervalText(value: Double): String {
    val normalized = value.coerceAtLeast(0.5)
    return if (normalized % 1.0 == 0.0) {
        "${normalized.toInt()}年"
    } else {
        String.format(Locale.CHINA, "%.1f年", normalized)
    }
}

private fun formatCostText(value: Double): String {
    if (value == 0.0) return "0"
    return String.format(Locale.CHINA, "%.2f", value)
        .replace(Regex("\\.?0+$"), "")
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

private data class RecordCycleGroup(
    val date: LocalDate,
    val itemSummary: String,
    val records: List<RecordCycleRecord>,
    val totalCost: Double,
)

private data class RecordCycleRecord(
    val record: RecordSnapshot,
    val itemSummary: String,
)

private data class RecordItemRow(
    val id: String,
    val itemName: String,
    val record: RecordSnapshot,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordCycleGroupCard(
    group: RecordCycleGroup,
    onEditRecordClick: (RecordSnapshot) -> Unit,
    onDeleteRecordClick: (RecordSnapshot) -> Unit,
) {
    val record = group.records.firstOrNull() ?: return
    var menuExpanded by remember(group.date, record.record.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true },
                ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = formatDate(group.date),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = "里程：${formatMileageKm(record.record.mileage)}km",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "项目：${group.itemSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "总费用：${formatMoney(group.totalCost)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.wrapContentSize(Alignment.BottomEnd),
            ) {
                DropdownMenuItem(
                    text = { Text(text = "编辑") },
                    onClick = {
                        menuExpanded = false
                        onEditRecordClick(record.record)
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = "删除") },
                    onClick = {
                        menuExpanded = false
                        onDeleteRecordClick(record.record)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordCycleRecordCard(
    record: RecordCycleRecord,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var menuExpanded by remember(record.record.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true },
                ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "里程：${formatMileageKm(record.record.mileage)}km",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "项目：${record.itemSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "费用：${formatMoney(record.record.cost)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (record.record.note.isNotBlank()) {
                    Text(
                        text = "备注：${record.record.note.trim()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.wrapContentSize(Alignment.BottomEnd),
            ) {
                DropdownMenuItem(
                    text = { Text(text = "编辑") },
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = "删除") },
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordItemRowCard(
    row: RecordItemRow,
    onEditRecordClick: (RecordSnapshot) -> Unit,
    onDeleteRecordClick: (RecordSnapshot) -> Unit,
) {
    var menuExpanded by remember(row.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true },
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = row.itemName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = "日期：${formatDate(row.record.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "里程：${formatMileageKm(row.record.mileage)}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (row.record.note.isNotBlank()) {
                    Text(
                        text = "备注：${row.record.note.trim()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.wrapContentSize(Alignment.BottomEnd),
            ) {
                DropdownMenuItem(
                    text = { Text(text = "编辑") },
                    onClick = {
                        menuExpanded = false
                        onEditRecordClick(row.record)
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = "删除") },
                    onClick = {
                        menuExpanded = false
                        onDeleteRecordClick(row.record)
                    },
                )
            }
        }
    }
}

private fun formatMileageKm(mileage: Int): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    formatter.isGroupingUsed = true
    return formatter.format(mileage.coerceAtLeast(0))
}

private fun formatMoney(cost: Double): String {
    return NumberFormat.getNumberInstance(Locale.CHINA).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
        isGroupingUsed = false
    }.format(cost)
}

private fun buildRecordCycleGroups(
    records: List<RecordSnapshot>,
    itemOptions: List<RecordItemOptionChoice>,
) : List<RecordCycleGroup> {
    val nameById = itemOptions.associateBy({ it.id }, { it.name })
    val orderById = itemOptions.mapIndexed { index, option -> option.id to index }.toMap()

    return records
        .groupBy { it.date }
        .map { (date, groupRecords) ->
            val groupRecordsSorted = groupRecords.sortedWith(
                compareByDescending<RecordSnapshot> { it.date }
                    .thenByDescending { it.mileage }
                    .thenBy { it.id },
            )
            val seenItemIds = linkedSetOf<String>()
            val uniqueItemIds = buildList {
                groupRecordsSorted.forEach { record ->
                    MaintenanceItemConfig.parseItemIDs(record.itemIDsRaw).forEach { itemId ->
                        if (seenItemIds.add(itemId)) {
                            add(itemId)
                        }
                    }
                }
            }
            val groupItemSummary = uniqueItemIds
                .sortedBy { orderById[it] ?: Int.MAX_VALUE }
                .mapNotNull { nameById[it] }
                .joinToString("、")
                .ifEmpty { "未标注项目" }
            RecordCycleGroup(
                date = date,
                itemSummary = groupItemSummary,
                records = groupRecordsSorted.map { record ->
                    val itemSummary = MaintenanceItemConfig.parseItemIDs(record.itemIDsRaw)
                        .mapNotNull { nameById[it] }
                        .ifEmpty { listOf("未标注项目") }
                        .joinToString("、")
                    RecordCycleRecord(
                        record = record,
                        itemSummary = itemSummary,
                    )
                },
                totalCost = groupRecords.sumOf { it.cost },
            )
        }
        .sortedByDescending { it.date }
}

private fun buildRecordItemRows(
    records: List<RecordSnapshot>,
    itemOptions: List<RecordItemOptionChoice>,
): List<RecordItemRow> {
    val nameById = itemOptions.associateBy({ it.id }, { it.name })

    return records.flatMap { record ->
        MaintenanceItemConfig.parseItemIDs(record.itemIDsRaw).mapNotNull { itemId ->
            val itemName = nameById[itemId] ?: return@mapNotNull null
            RecordItemRow(
                id = "${record.id}-$itemId",
                itemName = itemName,
                record = record,
            )
        }
    }.sortedWith(
        compareByDescending<RecordItemRow> { it.record.date }
            .thenByDescending { it.record.mileage }
            .thenBy { it.itemName }
            .thenBy { it.id },
    )
}

private fun mileageDisplayText(wan: Int, qian: Int, bai: Int): String {
    val normalizedWan = wan.coerceAtLeast(0)
    val normalizedQian = qian.coerceIn(0, 9)
    val normalizedBai = bai.coerceIn(0, 9)
    return "${normalizedWan}万${normalizedQian}千${normalizedBai}百"
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDatePickerDialog(
    purchaseDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = localDateToEpochMillis(purchaseDate),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        onConfirm(epochMillisToLocalDate(selectedMillis))
                    }
                    onDismiss()
                },
            ) {
                Text(text = "应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
        )
    }
}

@Composable
private fun RecordMileagePickerDialog(
    wan: Int,
    qian: Int,
    bai: Int,
    onDismiss: () -> Unit,
    onConfirm: (wan: Int, qian: Int, bai: Int) -> Unit,
) {
    var editingWan by remember(wan) { mutableStateOf(wan.coerceAtLeast(0)) }
    var editingQian by remember(qian) { mutableStateOf(qian.coerceIn(0, 9)) }
    var editingBai by remember(bai) { mutableStateOf(bai.coerceIn(0, 9)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "选择里程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RecordNumberWheelPicker(
                        label = "万",
                        value = editingWan,
                        range = 0..99,
                        onValueChange = { editingWan = it },
                    )
                    RecordNumberWheelPicker(
                        label = "千",
                        value = editingQian,
                        range = 0..9,
                        onValueChange = { editingQian = it },
                    )
                    RecordNumberWheelPicker(
                        label = "百",
                        value = editingBai,
                        range = 0..9,
                        onValueChange = { editingBai = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(editingWan, editingQian, editingBai)
                },
            ) {
                Text(text = "完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun RecordNumberWheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    val itemHeightPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = false
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setOnValueChangedListener { _, _, newVal ->
                        onValueChange(newVal)
                    }
                }
            },
            update = { picker ->
                picker.minValue = range.first
                picker.maxValue = range.last
                picker.value = value.coerceIn(range.first, range.last)
                picker.setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
                picker.layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    itemHeightPx * 3,
                )
            },
        )
    }
}

private fun localDateToEpochMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun epochMillisToLocalDate(epochMillis: Long): LocalDate {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
