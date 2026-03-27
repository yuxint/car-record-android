package com.tx.carrecord.feature.addcar.ui

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.feature.addcar.data.CarItemOptionSnapshot
import com.tx.carrecord.feature.addcar.data.CarItemOptionUpsertDraft
import com.tx.carrecord.feature.addcar.data.CarRepository
import com.tx.carrecord.feature.addcar.data.CarUpsertRequest
import com.tx.carrecord.feature.addcar.data.SaveCarItemOptionsRequest
import com.tx.carrecord.feature.addcar.domain.CarManagementRules
import com.tx.carrecord.feature.addcar.domain.CarProfileSnapshot
import com.tx.carrecord.feature.datatransfer.domain.MyDataTransferTimeCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val ADD_CAR_BRAND_OPTIONS: List<String> = listOf("本田", "日产")

private fun addCarModelOptions(brand: String): List<String> = when (brand) {
    "本田" -> listOf("22款思域", "23款CR-V", "24款雅阁")
    "日产" -> listOf("22款轩逸", "23款奇骏", "24款天籁")
    else -> listOf("22款思域")
}

private data class MileageSegments(
    val wan: Int,
    val qian: Int,
    val bai: Int,
)

private fun mileageSegmentsFromValue(mileage: Int): MileageSegments {
    val normalized = mileage.coerceAtLeast(0)
    return MileageSegments(
        wan = normalized / 10_000,
        qian = (normalized % 10_000) / 1_000,
        bai = (normalized % 1_000) / 100,
    )
}

private fun mileageValueFromSegments(
    wan: Int,
    qian: Int,
    bai: Int,
): Int = wan.coerceAtLeast(0) * 10_000 + qian.coerceIn(0, 9) * 1_000 + bai.coerceIn(0, 9) * 100

data class CarEditorState(
    val editingCarId: String? = null,
    val brand: String = "",
    val modelName: String = "",
    val mileageWan: Int = 0,
    val mileageQian: Int = 0,
    val mileageBai: Int = 0,
    val purchaseDate: LocalDate = LocalDate.now(),
    val disabledItemIDsRaw: String = "",
    val itemDrafts: List<ItemRuleDraft> = emptyList(),
)

data class ItemConfigEditorState(
    val carId: String,
    val carDisplayName: String,
    val drafts: List<ItemRuleDraft>,
)

data class ItemRuleDraft(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val catalogKey: String?,
    val isEnabled: Boolean,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
    val warningStartPercent: Int,
    val dangerStartPercent: Int,
)

data class AddCarUiState(
    val loading: Boolean = false,
    val cars: List<CarProfileSnapshot> = emptyList(),
    val appliedCarId: String = "",
    val activeCarEditor: CarEditorState? = null,
    val activeItemConfigEditor: ItemConfigEditorState? = null,
    val pendingDeleteCar: CarProfileSnapshot? = null,
    val message: String? = null,
)

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val appDateContext: AppDateContext,
) : ViewModel() {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val _uiState = MutableStateFlow(AddCarUiState(loading = true))
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    init {
        refreshCars()
    }

    fun refreshCars() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = null)
            when (val result = carRepository.listCars()) {
                is RepositoryResult.Success -> {
                    when (val appliedResult = carRepository.resolveAppliedCarId(result.value)) {
                        is RepositoryResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                loading = false,
                                cars = result.value,
                                appliedCarId = appliedResult.value,
                            )
                        }

                        is RepositoryResult.Failure -> {
                            _uiState.value = _uiState.value.copy(
                                loading = false,
                                cars = result.value,
                                message = appliedResult.error.message,
                            )
                        }
                    }
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(loading = false, message = result.error.message)
                }
            }
        }
    }

    fun openAddCarEditor() {
        viewModelScope.launch {
            val defaultBrand = ADD_CAR_BRAND_OPTIONS.firstOrNull().orEmpty()
            val defaultModel = addCarModelOptions(defaultBrand).firstOrNull().orEmpty()
            val now = appDateContext.now()
            _uiState.value = _uiState.value.copy(
                activeCarEditor = CarEditorState(
                    brand = defaultBrand,
                    modelName = defaultModel,
                    mileageWan = 0,
                    mileageQian = 0,
                    mileageBai = 0,
                    purchaseDate = now,
                    itemDrafts = buildEditorDrafts(
                        carId = null,
                        brand = defaultBrand,
                        modelName = defaultModel,
                        disabledItemIDsRaw = "",
                        existingOptions = emptyList(),
                    ),
                ),
                message = null,
            )
        }
    }

    fun openEditCarEditor(car: CarProfileSnapshot) {
        viewModelScope.launch {
            val existingOptions = when (val optionsResult = carRepository.listItemOptionsByCarId(car.id)) {
                is RepositoryResult.Success -> optionsResult.value
                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = optionsResult.error.message)
                    return@launch
                }
            }
            val segments = mileageSegmentsFromValue(car.mileage)
            _uiState.value = _uiState.value.copy(
                activeCarEditor = CarEditorState(
                    editingCarId = car.id,
                    brand = car.brand,
                    modelName = car.modelName,
                    mileageWan = segments.wan,
                    mileageQian = segments.qian,
                    mileageBai = segments.bai,
                    purchaseDate = car.purchaseDate,
                    disabledItemIDsRaw = car.disabledItemIDsRaw,
                    itemDrafts = buildEditorDrafts(
                        carId = car.id,
                        brand = car.brand,
                        modelName = car.modelName,
                        disabledItemIDsRaw = car.disabledItemIDsRaw,
                        existingOptions = existingOptions,
                    ),
                ),
                message = null,
            )
        }
    }

    fun closeCarEditor() {
        _uiState.value = _uiState.value.copy(activeCarEditor = null)
    }

    fun updateCarEditorBrand(brand: String) {
        val editor = _uiState.value.activeCarEditor ?: return
        if (editor.editingCarId != null) return
        val normalizedBrand = brand.trim()
        if (normalizedBrand.isEmpty()) return
        val model = addCarModelOptions(normalizedBrand).firstOrNull().orEmpty()
        _uiState.value = _uiState.value.copy(
            activeCarEditor = editor.copy(
                brand = normalizedBrand,
                modelName = model,
                disabledItemIDsRaw = "",
                itemDrafts = buildEditorDrafts(
                    carId = null,
                    brand = normalizedBrand,
                    modelName = model,
                    disabledItemIDsRaw = "",
                    existingOptions = emptyList(),
                ),
            ),
        )
    }

    fun updateCarEditorModel(modelName: String) {
        val editor = _uiState.value.activeCarEditor ?: return
        if (editor.editingCarId != null) return
        val normalizedModel = modelName.trim()
        if (normalizedModel.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            activeCarEditor = editor.copy(
                modelName = normalizedModel,
                disabledItemIDsRaw = "",
                itemDrafts = buildEditorDrafts(
                    carId = null,
                    brand = editor.brand,
                    modelName = normalizedModel,
                    disabledItemIDsRaw = "",
                    existingOptions = emptyList(),
                ),
            ),
        )
    }

    fun saveCar(
        editor: CarEditorState,
        drafts: List<ItemRuleDraft>,
    ) {
        val brand = editor.brand.trim()
        if (brand.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "品牌不能为空")
            return
        }

        val modelName = editor.modelName.trim()
        if (modelName.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "车型不能为空")
            return
        }

        val mileage = mileageValueFromSegments(
            wan = editor.mileageWan,
            qian = editor.mileageQian,
            bai = editor.mileageBai,
        )
        val targetDrafts = if (drafts.isEmpty()) {
            buildEditorDrafts(
                carId = editor.editingCarId,
                brand = brand,
                modelName = modelName,
                disabledItemIDsRaw = editor.disabledItemIDsRaw,
                existingOptions = emptyList(),
            )
        } else {
            drafts
        }

        if (targetDrafts.none { it.isEnabled }) {
            _uiState.value = _uiState.value.copy(message = "请至少保留一个保养项目。")
            return
        }
        val duplicateName = targetDrafts
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .groupBy { it }
            .entries
            .firstOrNull { it.value.size > 1 }
        if (duplicateName != null) {
            _uiState.value = _uiState.value.copy(message = "存在重名项目，请先调整后再保存。")
            return
        }
        if (targetDrafts.any { it.name.trim().isEmpty() }) {
            _uiState.value = _uiState.value.copy(message = "项目名称不能为空。")
            return
        }
        if (targetDrafts.any { !it.remindByMileage && !it.remindByTime }) {
            _uiState.value = _uiState.value.copy(message = "请至少开启一种提醒方式。")
            return
        }

        viewModelScope.launch {
            when (
                val result = carRepository.upsertCar(
                    request = CarUpsertRequest(
                        editingCarId = editor.editingCarId,
                        brand = brand,
                        modelName = modelName,
                        mileage = mileage,
                        purchaseDateEpochSeconds = MyDataTransferTimeCodec.toEpochSecondsAtStartOfDay(
                            date = editor.purchaseDate,
                            zoneId = zoneId,
                        ),
                        disabledItemIDsRaw = editor.disabledItemIDsRaw,
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    val disabledItemIDsRaw = targetDrafts
                        .filter { !it.isEnabled }
                        .joinToString("|") { it.id }
                    val saveOptionsResult = carRepository.saveItemOptions(
                        request = SaveCarItemOptionsRequest(
                            carId = result.value.carId,
                            drafts = targetDrafts.map { draft ->
                                CarItemOptionUpsertDraft(
                                    id = draft.id,
                                    name = draft.name.trim(),
                                    isDefault = draft.isDefault,
                                    catalogKey = draft.catalogKey,
                                    remindByMileage = draft.remindByMileage,
                                    mileageInterval = draft.mileageInterval,
                                    remindByTime = draft.remindByTime,
                                    monthInterval = draft.monthInterval,
                                    warningStartPercent = draft.warningStartPercent,
                                    dangerStartPercent = draft.dangerStartPercent,
                                )
                            },
                            disabledItemIDsRaw = disabledItemIDsRaw,
                        ),
                    )
                    if (saveOptionsResult is RepositoryResult.Failure) {
                        _uiState.value = _uiState.value.copy(
                            appliedCarId = result.value.normalizedRawAppliedCarId,
                            activeCarEditor = editor.copy(
                                editingCarId = result.value.carId,
                                disabledItemIDsRaw = disabledItemIDsRaw,
                                itemDrafts = targetDrafts,
                            ),
                            message = saveOptionsResult.error.message,
                        )
                        refreshCars()
                        return@launch
                    }
                    _uiState.value = _uiState.value.copy(
                        appliedCarId = result.value.normalizedRawAppliedCarId,
                        activeCarEditor = null,
                        message = if (editor.editingCarId == null) {
                            "添加车辆成功"
                        } else {
                            "更新车辆成功"
                        },
                    )
                    refreshCars()
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    private fun buildEditorDrafts(
        carId: String?,
        brand: String,
        modelName: String,
        disabledItemIDsRaw: String,
        existingOptions: List<CarItemOptionSnapshot>,
    ): List<ItemRuleDraft> {
        val options = if (existingOptions.isNotEmpty()) {
            existingOptions
        } else {
            val defaults = carRepository.modelItemDefaults(brand, modelName)
            defaults.definitions.map { definition ->
                CarItemOptionSnapshot(
                    id = UUID.randomUUID().toString(),
                    name = definition.defaultName,
                    ownerCarId = carId,
                    isDefault = true,
                    catalogKey = definition.key,
                    remindByMileage = definition.remindByMileage,
                    mileageInterval = definition.mileageInterval ?: 5000,
                    remindByTime = definition.remindByTime,
                    monthInterval = definition.monthInterval ?: 12,
                    warningStartPercent = definition.warningStartPercent,
                    dangerStartPercent = definition.dangerStartPercent,
                    createdAtEpochSeconds = 0L,
                )
            }
        }
        val disabledIds = CarManagementRules.parseDisabledItemIDs(disabledItemIDsRaw).toSet()
        return options.map { option ->
            ItemRuleDraft(
                id = option.id,
                name = option.name,
                isDefault = option.isDefault,
                catalogKey = option.catalogKey,
                isEnabled = option.id !in disabledIds,
                remindByMileage = option.remindByMileage,
                mileageInterval = option.mileageInterval,
                remindByTime = option.remindByTime,
                monthInterval = option.monthInterval,
                warningStartPercent = option.warningStartPercent,
                dangerStartPercent = option.dangerStartPercent,
            )
        }
    }

    fun openItemConfigEditor(car: CarProfileSnapshot) {
        viewModelScope.launch {
            when (val result = carRepository.listItemOptionsByCarId(car.id)) {
                is RepositoryResult.Success -> {
                    val snapshots = if (result.value.isEmpty()) {
                        val defaults = carRepository.modelItemDefaults(car.brand, car.modelName)
                        defaults.definitions.map { definition ->
                            CarItemOptionSnapshot(
                                id = UUID.randomUUID().toString(),
                                name = definition.defaultName,
                                ownerCarId = car.id,
                                isDefault = true,
                                catalogKey = definition.key,
                                remindByMileage = definition.remindByMileage,
                                mileageInterval = definition.mileageInterval ?: 5000,
                                remindByTime = definition.remindByTime,
                                monthInterval = definition.monthInterval ?: 12,
                                warningStartPercent = definition.warningStartPercent,
                                dangerStartPercent = definition.dangerStartPercent,
                                createdAtEpochSeconds = 0L,
                            )
                        }
                    } else {
                        result.value
                    }

                    val disabledIds = CarManagementRules.parseDisabledItemIDs(car.disabledItemIDsRaw).toSet()
                    _uiState.value = _uiState.value.copy(
                        activeItemConfigEditor = ItemConfigEditorState(
                            carId = car.id,
                            carDisplayName = "${car.brand} ${car.modelName}",
                            drafts = snapshots.map { option ->
                                ItemRuleDraft(
                                    id = option.id,
                                    name = option.name,
                                    isDefault = option.isDefault,
                                    catalogKey = option.catalogKey,
                                    isEnabled = option.id !in disabledIds,
                                    remindByMileage = option.remindByMileage,
                                    mileageInterval = option.mileageInterval,
                                    remindByTime = option.remindByTime,
                                    monthInterval = option.monthInterval,
                                    warningStartPercent = option.warningStartPercent,
                                    dangerStartPercent = option.dangerStartPercent,
                                )
                            },
                        ),
                    )
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun closeItemConfigEditor() {
        _uiState.value = _uiState.value.copy(activeItemConfigEditor = null)
    }

    fun saveItemConfig(
        editor: ItemConfigEditorState,
        drafts: List<ItemRuleDraft>,
    ) {
        if (drafts.none { it.isEnabled }) {
            _uiState.value = _uiState.value.copy(message = "请至少保留一个保养项目。")
            return
        }
        viewModelScope.launch {
            val duplicateName = drafts
                .map { it.name.trim() }
                .filter { it.isNotEmpty() }
                .groupBy { it }
                .entries
                .firstOrNull { it.value.size > 1 }
            if (duplicateName != null) {
                _uiState.value = _uiState.value.copy(message = "存在重名项目，请先调整后再保存。")
                return@launch
            }

            if (drafts.any { it.name.trim().isEmpty() }) {
                _uiState.value = _uiState.value.copy(message = "项目名称不能为空。")
                return@launch
            }
            if (drafts.any { !it.remindByMileage && !it.remindByTime }) {
                _uiState.value = _uiState.value.copy(message = "请至少开启一种提醒方式。")
                return@launch
            }

            when (
                val result = carRepository.saveItemOptions(
                    request = SaveCarItemOptionsRequest(
                        carId = editor.carId,
                        drafts = drafts.map { draft ->
                            CarItemOptionUpsertDraft(
                                id = draft.id,
                                name = draft.name.trim(),
                                isDefault = draft.isDefault,
                                catalogKey = draft.catalogKey,
                                remindByMileage = draft.remindByMileage,
                                mileageInterval = draft.mileageInterval,
                                remindByTime = draft.remindByTime,
                                monthInterval = draft.monthInterval,
                                warningStartPercent = draft.warningStartPercent,
                                dangerStartPercent = draft.dangerStartPercent,
                            )
                        },
                        disabledItemIDsRaw = drafts
                            .filter { !it.isEnabled }
                            .joinToString("|") { it.id },
                    ),
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeItemConfigEditor = null,
                        message = "保养项目规则已更新",
                    )
                    refreshCars()
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun applyCar(carId: String) {
        viewModelScope.launch {
            when (val result = carRepository.applyCar(carId)) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        appliedCarId = result.value,
                        message = "已应用车型",
                    )
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = result.error.message)
                }
            }
        }
    }

    fun requestDeleteCar(car: CarProfileSnapshot) {
        _uiState.value = _uiState.value.copy(pendingDeleteCar = car)
    }

    fun dismissDeleteCar() {
        _uiState.value = _uiState.value.copy(pendingDeleteCar = null)
    }

    fun confirmDeleteCar() {
        val deletingCar = _uiState.value.pendingDeleteCar ?: return
        viewModelScope.launch {
            when (val result = carRepository.deleteCar(deletingCar.id)) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        pendingDeleteCar = null,
                        appliedCarId = result.value.normalizedRawAppliedCarId,
                        message = "删除车辆成功",
                    )
                    refreshCars()
                }

                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        pendingDeleteCar = null,
                        message = result.error.message,
                    )
                }
            }
        }
    }
}

@Composable
fun AddCarManagementSection(
    modifier: Modifier = Modifier,
    viewModel: AddCarViewModel = hiltViewModel(),
    onCarEditorPageVisibleChange: (Boolean) -> Unit = {},
    onCarsAvailabilityChange: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.activeCarEditor != null) {
        onCarEditorPageVisibleChange(uiState.activeCarEditor != null)
    }
    LaunchedEffect(uiState.cars.isNotEmpty()) {
        onCarsAvailabilityChange(uiState.cars.isNotEmpty())
    }

    uiState.activeCarEditor?.let { editor ->
        CarEditorPage(
            editor = editor,
            carCount = uiState.cars.size,
            onBack = viewModel::closeCarEditor,
            onSave = viewModel::saveCar,
            onBrandChange = viewModel::updateCarEditorBrand,
            onModelChange = viewModel::updateCarEditorModel,
        )
        return
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "车辆管理",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            when {
                uiState.loading -> StatusLoading(text = "加载车辆中...")
                uiState.cars.isEmpty() -> StatusMessage(text = "还没有车辆，点击下方“添加车辆”开始记录")
                else -> {
                    Text(
                        text = "车辆数量：${uiState.cars.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HorizontalDivider()
                    uiState.cars.forEach { car ->
                        CarRowCard(
                            car = car,
                            isApplied = car.id == uiState.appliedCarId,
                            onApply = { viewModel.applyCar(car.id) },
                            onEdit = { viewModel.openEditCarEditor(car) },
                            onEditItemConfig = { viewModel.openItemConfigEditor(car) },
                            onDelete = { viewModel.requestDeleteCar(car) },
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = viewModel::openAddCarEditor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "添加车辆")
            }

            uiState.message?.let {
                HorizontalDivider()
                StatusMessage(text = it)
            }
        }
    }

    uiState.pendingDeleteCar?.let { car ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteCar,
            title = { Text(text = "确认删除车辆？") },
            text = {
                Text(
                    text = "将删除该车辆及其关联的保养记录、保养项目设置等全部数据，且无法恢复。\n\n${car.brand} ${car.modelName}",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteCar) {
                    Text(text = "确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteCar) {
                    Text(text = "取消")
                }
            },
        )
    }

    uiState.activeItemConfigEditor?.let { editor ->
        var drafts by remember(editor) { mutableStateOf(editor.drafts) }
        var editingDraftId by remember { mutableStateOf<String?>(null) }
        var addingCustomDraft by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::closeItemConfigEditor,
            title = { Text(text = "车型级项目配置") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = editor.carDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    drafts.forEach { draft ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = draft.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Switch(
                                        checked = draft.isEnabled,
                                        onCheckedChange = { enabled ->
                                            drafts = drafts.map {
                                                if (it.id == draft.id) it.copy(isEnabled = enabled) else it
                                            }
                                        },
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = { editingDraftId = draft.id },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(text = "编辑规则")
                                    }
                                    if (!draft.isDefault) {
                                        OutlinedButton(
                                            onClick = {
                                                drafts = drafts.filterNot { it.id == draft.id }
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(text = "删除")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = { addingCustomDraft = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "新增自定义项目")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveItemConfig(
                            editor = editor,
                            drafts = drafts,
                        )
                    },
                ) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeItemConfigEditor) {
                    Text(text = "取消")
                }
            },
        )

        val editingDraft = drafts.firstOrNull { it.id == editingDraftId }
        if (editingDraft != null) {
            ItemRuleEditDialog(
                title = "保养项目设置",
                draft = editingDraft,
                onDismiss = { editingDraftId = null },
                onSave = { updated ->
                    drafts = drafts.map { if (it.id == updated.id) updated else it }
                    editingDraftId = null
                },
            )
        }

        if (addingCustomDraft) {
            ItemRuleEditDialog(
                title = "新增自定义项目",
                draft = ItemRuleDraft(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    isDefault = false,
                    catalogKey = null,
                    isEnabled = true,
                    remindByMileage = true,
                    mileageInterval = 5000,
                    remindByTime = false,
                    monthInterval = 12,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                ),
                onDismiss = { addingCustomDraft = false },
                onSave = { created ->
                    drafts = drafts + created
                    addingCustomDraft = false
                },
            )
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
private fun CarRowCard(
    car: CarProfileSnapshot,
    isApplied: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onEditItemConfig: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${car.brand} ${car.modelName}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = "上路日期：${MyDataTransferTimeCodec.formatDate(car.purchaseDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "里程：${car.mileage} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isApplied) {
                Text(
                    text = "当前应用车型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!isApplied) {
                    OutlinedButton(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "应用")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "编辑")
                }
                OutlinedButton(
                    onClick = onEditItemConfig,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "项目配置")
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "删除")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarEditorPage(
    editor: CarEditorState,
    carCount: Int,
    onBack: () -> Unit,
    onSave: (CarEditorState, List<ItemRuleDraft>) -> Unit,
    onBrandChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
) {
    var brand by remember(editor) { mutableStateOf(editor.brand) }
    var modelName by remember(editor) { mutableStateOf(editor.modelName) }
    var mileageWan by remember(editor) { mutableStateOf(editor.mileageWan) }
    var mileageQian by remember(editor) { mutableStateOf(editor.mileageQian) }
    var mileageBai by remember(editor) { mutableStateOf(editor.mileageBai) }
    var purchaseDate by remember(editor) { mutableStateOf(editor.purchaseDate) }
    var showMileagePicker by remember { mutableStateOf(false) }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var drafts by remember(editor) { mutableStateOf(editor.itemDrafts) }
    var editingDraftId by remember { mutableStateOf<String?>(null) }
    var addingCustomDraft by remember { mutableStateOf(false) }
    val modelOptions = remember(brand) { addCarModelOptions(brand) }

    LaunchedEffect(brand, modelOptions) {
        if (modelName !in modelOptions) {
            modelName = modelOptions.firstOrNull().orEmpty()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = purchaseDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) {
                Text(text = "返回")
            }
            Text(
                text = if (editor.editingCarId == null) "添加车辆" else "编辑车辆",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            TextButton(
                onClick = {
                    onSave(
                        editor.copy(
                            brand = brand,
                            modelName = modelName,
                            mileageWan = mileageWan,
                            mileageQian = mileageQian,
                            mileageBai = mileageBai,
                            purchaseDate = purchaseDate,
                            itemDrafts = drafts,
                        ),
                        drafts,
                    )
                },
            ) {
                Text(text = if (editor.editingCarId == null) "保存" else "更新")
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (editor.editingCarId == null) {
                    SingleChoiceDropdownField(
                        label = "品牌",
                        selected = brand,
                        options = ADD_CAR_BRAND_OPTIONS,
                        onSelect = { selected ->
                            brand = selected
                            val nextModels = addCarModelOptions(selected)
                            if (modelName !in nextModels) {
                                modelName = nextModels.firstOrNull().orEmpty()
                            }
                            onBrandChange(selected)
                        },
                    )
                    SingleChoiceDropdownField(
                        label = "车型",
                        selected = modelName,
                        options = modelOptions,
                        onSelect = {
                            modelName = it
                            onModelChange(it)
                        },
                    )
                } else {
                    Text(text = "品牌：$brand", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "车型：$modelName", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedButton(
                    onClick = { showPurchaseDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "上路日期：${MyDataTransferTimeCodec.formatDate(purchaseDate)}")
                }

                OutlinedButton(
                    onClick = { showMileagePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "里程：${mileageValueFromSegments(wan = mileageWan, qian = mileageQian, bai = mileageBai)} km",
                    )
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "保养项目设置",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                )
                drafts.forEach { draft ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = draft.name, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = draft.isEnabled,
                                    onCheckedChange = { enabled ->
                                        drafts = drafts.map {
                                            if (it.id == draft.id) it.copy(isEnabled = enabled) else it
                                        }
                                    },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { editingDraftId = draft.id },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = "编辑规则")
                                }
                                if (!draft.isDefault) {
                                    OutlinedButton(
                                        onClick = {
                                            drafts = drafts.filterNot { it.id == draft.id }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(text = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
                FilledTonalButton(
                    onClick = { addingCustomDraft = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "新增自定义项目")
                }
                Text(
                    text = "当前车辆数量：$carCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showMileagePicker) {
        MileageWheelPickerDialog(
            wan = mileageWan,
            qian = mileageQian,
            bai = mileageBai,
            onDismiss = { showMileagePicker = false },
            onConfirm = { wan, qian, bai ->
                mileageWan = wan
                mileageQian = qian
                mileageBai = bai
                showMileagePicker = false
            },
        )
    }

    if (showPurchaseDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showPurchaseDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            purchaseDate = epochMillisToLocalDate(selectedMillis)
                        }
                        showPurchaseDatePicker = false
                    },
                ) {
                    Text(text = "应用")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDatePicker = false }) {
                    Text(text = "取消")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val editingDraft = drafts.firstOrNull { it.id == editingDraftId }
    if (editingDraft != null) {
        ItemRuleEditDialog(
            title = "保养项目设置",
            draft = editingDraft,
            onDismiss = { editingDraftId = null },
            onSave = { updated ->
                drafts = drafts.map { if (it.id == updated.id) updated else it }
                editingDraftId = null
            },
        )
    }

    if (addingCustomDraft) {
        ItemRuleEditDialog(
            title = "新增自定义项目",
            draft = ItemRuleDraft(
                id = UUID.randomUUID().toString(),
                name = "",
                isDefault = false,
                catalogKey = null,
                isEnabled = true,
                remindByMileage = true,
                mileageInterval = 5000,
                remindByTime = false,
                monthInterval = 12,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
            onDismiss = { addingCustomDraft = false },
            onSave = { created ->
                drafts = drafts + created
                addingCustomDraft = false
            },
        )
    }
}

@Composable
private fun SingleChoiceDropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "$label：$selected")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MileageWheelPickerDialog(
    wan: Int,
    qian: Int,
    bai: Int,
    onDismiss: () -> Unit,
    onConfirm: (wan: Int, qian: Int, bai: Int) -> Unit,
) {
    var editingWan by remember(wan) { mutableStateOf(wan) }
    var editingQian by remember(qian) { mutableStateOf(qian) }
    var editingBai by remember(bai) { mutableStateOf(bai) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "设置里程（滚动选择）") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    NumberWheelPicker(
                        label = "万",
                        value = editingWan,
                        range = 0..99,
                        onValueChange = { editingWan = it },
                    )
                    NumberWheelPicker(
                        label = "千",
                        value = editingQian,
                        range = 0..9,
                        onValueChange = { editingQian = it },
                    )
                    NumberWheelPicker(
                        label = "百",
                        value = editingBai,
                        range = 0..9,
                        onValueChange = { editingBai = it },
                    )
                }
                Text(
                    text = "当前里程：${mileageValueFromSegments(editingWan, editingQian, editingBai)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun NumberWheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    val itemHeightPx = with(LocalDensity.current) { 36.dp.roundToPx() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            modifier = Modifier,
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = true
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

@Composable
private fun ItemRuleEditDialog(
    title: String,
    draft: ItemRuleDraft,
    onDismiss: () -> Unit,
    onSave: (ItemRuleDraft) -> Unit,
) {
    var name by remember(draft.id) { mutableStateOf(draft.name) }
    var remindByMileage by remember(draft.id) { mutableStateOf(draft.remindByMileage) }
    var mileageInterval by remember(draft.id) { mutableStateOf(draft.mileageInterval.coerceAtLeast(1000)) }
    var remindByTime by remember(draft.id) { mutableStateOf(draft.remindByTime) }
    var monthInterval by remember(draft.id) { mutableStateOf(draft.monthInterval.coerceAtLeast(1)) }
    var warningStartPercent by remember(draft.id) { mutableStateOf(draft.warningStartPercent.coerceAtLeast(0)) }
    var dangerStartPercent by remember(draft.id) { mutableStateOf(draft.dangerStartPercent.coerceAtLeast(warningStartPercent)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "按里程提醒")
                    Switch(
                        checked = remindByMileage,
                        onCheckedChange = { remindByMileage = it },
                    )
                }
                if (remindByMileage) {
                    NumberAdjustRow(
                        label = "里程间隔（km）",
                        value = mileageInterval,
                        step = 1000,
                        minValue = 1000,
                        maxValue = 100_000,
                        onValueChange = { mileageInterval = it },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "按时间提醒")
                    Switch(
                        checked = remindByTime,
                        onCheckedChange = { remindByTime = it },
                    )
                }
                if (remindByTime) {
                    NumberAdjustRow(
                        label = "时间间隔（月）",
                        value = monthInterval,
                        step = 1,
                        minValue = 1,
                        maxValue = 120,
                        onValueChange = { monthInterval = it },
                    )
                }

                NumberAdjustRow(
                    label = "黄色阈值（%）",
                    value = warningStartPercent,
                    step = 5,
                    minValue = 50,
                    maxValue = 200,
                    onValueChange = {
                        warningStartPercent = it
                        if (dangerStartPercent < it) {
                            dangerStartPercent = it
                        }
                    },
                )
                NumberAdjustRow(
                    label = "红色阈值（%）",
                    value = dangerStartPercent,
                    step = 5,
                    minValue = warningStartPercent,
                    maxValue = 250,
                    onValueChange = { dangerStartPercent = it.coerceAtLeast(warningStartPercent) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isEmpty()) return@TextButton
                    if (!remindByMileage && !remindByTime) return@TextButton
                    onSave(
                        draft.copy(
                            name = name.trim(),
                            remindByMileage = remindByMileage,
                            mileageInterval = if (remindByMileage) mileageInterval.coerceAtLeast(1) else 0,
                            remindByTime = remindByTime,
                            monthInterval = if (remindByTime) monthInterval.coerceAtLeast(1) else 0,
                            warningStartPercent = warningStartPercent.coerceAtLeast(0),
                            dangerStartPercent = dangerStartPercent.coerceAtLeast(warningStartPercent),
                        ),
                    )
                },
            ) {
                Text(text = "保存")
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
private fun NumberAdjustRow(
    label: String,
    value: Int,
    step: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "$label：$value")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onValueChange((value - step).coerceAtLeast(minValue)) }) {
                Text(text = "-")
            }
            OutlinedButton(onClick = { onValueChange((value + step).coerceAtMost(maxValue)) }) {
                Text(text = "+")
            }
        }
    }
}

private fun epochMillisToLocalDate(epochMillis: Long): LocalDate {
    return java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
