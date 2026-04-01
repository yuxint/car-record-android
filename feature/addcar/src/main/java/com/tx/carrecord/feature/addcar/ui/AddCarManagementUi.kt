package com.tx.carrecord.feature.addcar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.MaintenanceDataChangeContext
import com.tx.carrecord.core.common.time.AppTimeCodec
import com.tx.carrecord.core.datastore.logging.AppLogger
import com.tx.carrecord.feature.addcar.data.CarItemOptionSnapshot
import com.tx.carrecord.feature.addcar.data.CarItemOptionUpsertDraft
import com.tx.carrecord.feature.addcar.data.CarRepository
import com.tx.carrecord.feature.addcar.data.CarUpsertRequest
import com.tx.carrecord.feature.addcar.data.SaveCarItemOptionsRequest
import com.tx.carrecord.feature.addcar.domain.CarManagementRules
import com.tx.carrecord.feature.addcar.domain.CarProfileSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.text.NumberFormat
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

private fun carModelKey(brand: String, modelName: String): String {
    val normalizedBrand = brand.trim()
    val normalizedModel = modelName.trim()
    return "$normalizedBrand|$normalizedModel"
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
    val pendingDeleteCar: CarProfileSnapshot? = null,
    val editorValidationMessage: String? = null,
    val isEditorValidationAlertPresented: Boolean = false,
    val editorSaveErrorMessage: String? = null,
    val isEditorSaveErrorAlertPresented: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val appDateContext: AppDateContext,
    private val maintenanceDataChangeContext: MaintenanceDataChangeContext,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val _uiState = MutableStateFlow(AddCarUiState(loading = true))
    val uiState: StateFlow<AddCarUiState> = _uiState.asStateFlow()

    init {
        refreshCars()
        observeMaintenanceDataChanges()
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

    private fun observeMaintenanceDataChanges() {
        viewModelScope.launch {
            maintenanceDataChangeContext.changesFlow.collect { refreshCars() }
        }
    }

    fun openAddCarEditor() {
        viewModelScope.launch {
            val defaultBrand = ADD_CAR_BRAND_OPTIONS.firstOrNull().orEmpty()
            val defaultModel = addCarModelOptions(defaultBrand).firstOrNull().orEmpty()
            val now = appDateContext.now()
            _uiState.value = _uiState.value.copy(
                activeCarEditor = buildCarEditorState(
                    editingCar = null,
                    existingOptions = emptyList(),
                    purchaseDate = now,
                    brand = defaultBrand,
                    modelName = defaultModel,
                    mileageWan = 0,
                    mileageQian = 0,
                    mileageBai = 0,
                    disabledItemIDsRaw = "",
                ),
                editorValidationMessage = null,
                isEditorValidationAlertPresented = false,
                editorSaveErrorMessage = null,
                isEditorSaveErrorAlertPresented = false,
                message = null,
            )
        }
    }

    fun openEditCarEditor(car: CarProfileSnapshot) {
        val segments = mileageSegmentsFromValue(car.mileage)
        _uiState.value = _uiState.value.copy(
            activeCarEditor = buildCarEditorState(
                editingCar = car,
                existingOptions = emptyList(),
                purchaseDate = car.purchaseDate,
                brand = car.brand,
                modelName = car.modelName,
                mileageWan = segments.wan,
                mileageQian = segments.qian,
                mileageBai = segments.bai,
                disabledItemIDsRaw = car.disabledItemIDsRaw,
            ),
            editorValidationMessage = null,
            isEditorValidationAlertPresented = false,
            editorSaveErrorMessage = null,
            isEditorSaveErrorAlertPresented = false,
            message = null,
        )
        viewModelScope.launch {
            val existingOptions = when (val optionsResult = carRepository.listItemOptionsByCarId(car.id)) {
                is RepositoryResult.Success -> optionsResult.value
                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(message = optionsResult.error.message)
                    return@launch
                }
            }
            val currentEditor = _uiState.value.activeCarEditor
            if (currentEditor?.editingCarId != car.id) return@launch
            _uiState.value = _uiState.value.copy(
                activeCarEditor = currentEditor.copy(
                    itemDrafts = buildEditorDrafts(
                        carId = car.id,
                        brand = car.brand,
                        modelName = car.modelName,
                        disabledItemIDsRaw = car.disabledItemIDsRaw,
                        existingOptions = existingOptions,
                    ),
                ),
            )
        }
    }

    fun closeCarEditor() {
        _uiState.value = _uiState.value.copy(
            activeCarEditor = null,
            editorValidationMessage = null,
            isEditorValidationAlertPresented = false,
            editorSaveErrorMessage = null,
            isEditorSaveErrorAlertPresented = false,
        )
    }

    fun dismissEditorValidationAlert() {
        _uiState.value = _uiState.value.copy(
            editorValidationMessage = null,
            isEditorValidationAlertPresented = false,
        )
    }

    fun dismissEditorSaveErrorAlert() {
        _uiState.value = _uiState.value.copy(
            editorSaveErrorMessage = null,
            isEditorSaveErrorAlertPresented = false,
        )
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
        cars: List<CarProfileSnapshot>,
        editor: CarEditorState,
        drafts: List<ItemRuleDraft>,
    ) {
        val brand = editor.brand.trim()
        if (brand.isEmpty()) {
            showEditorValidationMessage("品牌不能为空")
            return
        }

        val modelName = editor.modelName.trim()
        if (modelName.isEmpty()) {
            showEditorValidationMessage("车型不能为空")
            return
        }

        if (editor.editingCarId == null) {
            val targetModelKey = carModelKey(brand = brand, modelName = modelName)
            val conflictCar = cars.firstOrNull { car ->
                carModelKey(brand = car.brand, modelName = car.modelName) == targetModelKey
            }
            if (conflictCar != null) {
                showEditorSaveError(
                    "车型“${conflictCar.brand} ${conflictCar.modelName}”已存在，不能重复添加。"
                )
                return
            }
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
            showEditorValidationMessage(
                if (editor.editingCarId == null) {
                    "请至少保留一个默认项目或新增一个自定义项目。"
                } else {
                    "请至少保留一个保养项目。"
                }
            )
            return
        }
        if (targetDrafts.any { it.name.trim().isEmpty() }) {
            showEditorValidationMessage("项目名称不能为空。")
            return
        }
        val duplicateName = targetDrafts
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .groupBy { it }
            .entries
            .firstOrNull { it.value.size > 1 }
        if (duplicateName != null) {
            showEditorValidationMessage("存在重名项目，请先调整后再保存。")
            return
        }
        if (targetDrafts.any { !it.remindByMileage && !it.remindByTime }) {
            showEditorValidationMessage("请至少开启一种提醒方式。")
            return
        }

        viewModelScope.launch {
            try {
                val disabledItemIDsRaw = targetDrafts
                    .filter { !it.isEnabled }
                    .joinToString("|") { it.id }
                when (
                    val result = carRepository.upsertCar(
                        request = CarUpsertRequest(
                            editingCarId = editor.editingCarId,
                            brand = brand,
                            modelName = modelName,
                            mileage = mileage,
                            purchaseDateEpochSeconds = AppTimeCodec.toEpochSecondsAtStartOfDay(
                                date = editor.purchaseDate,
                                zoneId = zoneId,
                            ),
                            disabledItemIDsRaw = disabledItemIDsRaw,
                            itemOptionSaveRequest = SaveCarItemOptionsRequest(
                                carId = editor.editingCarId.orEmpty(),
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
                        ),
                    )
                ) {
                    is RepositoryResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            appliedCarId = result.value.normalizedRawAppliedCarId,
                            activeCarEditor = null,
                            editorValidationMessage = null,
                            isEditorValidationAlertPresented = false,
                            editorSaveErrorMessage = null,
                            isEditorSaveErrorAlertPresented = false,
                            message = null,
                        )
                    }

                    is RepositoryResult.Failure -> {
                        showEditorSaveError(saveVehicleSaveFailureMessage(result.error.message))
                    }
                }
            } catch (_: Throwable) {
                showEditorSaveError("保存车辆与保养项目失败，请稍后重试。")
            }
        }
    }

    private fun showEditorValidationMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            editorValidationMessage = message,
            isEditorValidationAlertPresented = true,
            editorSaveErrorMessage = null,
            isEditorSaveErrorAlertPresented = false,
        )
    }

    private fun showEditorSaveError(message: String) {
        _uiState.value = _uiState.value.copy(
            editorSaveErrorMessage = message,
            isEditorSaveErrorAlertPresented = true,
            editorValidationMessage = null,
            isEditorValidationAlertPresented = false,
        )
    }

    private fun saveVehicleSaveFailureMessage(detail: String?): String {
        val normalizedDetail = detail.orEmpty().trim()
        return if (
            normalizedDetail.contains("重复", ignoreCase = true) ||
            normalizedDetail.contains("constraint", ignoreCase = true) ||
            normalizedDetail.contains("unique", ignoreCase = true) ||
            normalizedDetail.contains("violat", ignoreCase = true)
        ) {
            "保存车辆与保养项目失败：存在重复数据，请检查同车同日记录、项目名称或车辆信息。"
        } else {
            "保存车辆与保养项目失败，请稍后重试。"
        }
    }

    private fun buildCarEditorState(
        editingCar: CarProfileSnapshot?,
        existingOptions: List<CarItemOptionSnapshot>,
        purchaseDate: LocalDate,
        brand: String,
        modelName: String,
        mileageWan: Int,
        mileageQian: Int,
        mileageBai: Int,
        disabledItemIDsRaw: String,
    ): CarEditorState {
        val normalizedBrand = brand.trim()
        val normalizedModel = modelName.trim()
        return CarEditorState(
            editingCarId = editingCar?.id,
            brand = normalizedBrand,
            modelName = normalizedModel,
            mileageWan = mileageWan,
            mileageQian = mileageQian,
            mileageBai = mileageBai,
            purchaseDate = purchaseDate,
            disabledItemIDsRaw = disabledItemIDsRaw,
            itemDrafts = buildEditorDrafts(
                carId = editingCar?.id,
                brand = normalizedBrand,
                modelName = normalizedModel,
                disabledItemIDsRaw = disabledItemIDsRaw,
                existingOptions = existingOptions,
            ),
        )
    }

    private fun buildEditorDrafts(
        carId: String?,
        brand: String,
        modelName: String,
        disabledItemIDsRaw: String,
        existingOptions: List<CarItemOptionSnapshot>,
    ): List<ItemRuleDraft> {
        val options = if (existingOptions.isNotEmpty()) {
            val defaultOrderByKey = carRepository.modelItemDefaults(brand, modelName).defaultOrderByKey
            MaintenanceItemConfig.sortItemOptionsByDefaultOrder(
                options = existingOptions,
                defaultOrderByKey = defaultOrderByKey,
                catalogKeySelector = { it.catalogKey },
            )
        } else {
            val defaults = carRepository.modelItemDefaults(brand, modelName)
            defaults.defaultItemDefinitions.map { definition ->
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
        val disabledIds = MaintenanceItemConfig.parseItemIDs(disabledItemIDsRaw).toSet()
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
                warningStartPercent = MaintenanceItemConfig.warningRangeStartPercent,
                dangerStartPercent = MaintenanceItemConfig.dangerStartPercent,
            )
        }
    }

    fun applyCar(carId: String) {
        viewModelScope.launch {
            when (val result = carRepository.applyCar(carId)) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        appliedCarId = result.value,
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
            appLogger.info(
                "开始删除车辆",
                payload = "carId=${deletingCar.id}, brand=${deletingCar.brand}, modelName=${deletingCar.modelName}",
            )
            when (val result = carRepository.deleteCar(deletingCar.id)) {
                is RepositoryResult.Success -> {
                    appLogger.info(
                        "删除车辆完成",
                        payload = "carId=${result.value.carId}, normalizedRawAppliedCarId=${result.value.normalizedRawAppliedCarId}",
                    )
                    _uiState.value = _uiState.value.copy(
                        pendingDeleteCar = null,
                        appliedCarId = result.value.normalizedRawAppliedCarId,
                        message = "删除车辆成功",
                    )
                }

                is RepositoryResult.Failure -> {
                    appLogger.error(
                        message = "删除车辆失败",
                        payload = result.error.message,
                    )
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
fun AddCarEditorPage(
    modifier: Modifier = Modifier,
    viewModel: AddCarViewModel = hiltViewModel(),
    onBackRequested: () -> Unit = viewModel::closeCarEditor,
    onEditorClosed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val editor = uiState.activeCarEditor
    var hasShownEditor by remember { mutableStateOf(false) }
    var lastVisibleEditor by remember { mutableStateOf<CarEditorState?>(null) }

    if (editor != null) {
        hasShownEditor = true
        lastVisibleEditor = editor
    }

    LaunchedEffect(editor, hasShownEditor) {
        if (editor == null && hasShownEditor) {
            onEditorClosed()
        }
    }

    val visibleEditor = editor ?: lastVisibleEditor
    if (visibleEditor == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CarEditorPage(
            modifier = Modifier.fillMaxSize(),
            editor = visibleEditor,
            onBack = onBackRequested,
            onSave = { currentEditor, drafts ->
                viewModel.saveCar(
                    cars = uiState.cars,
                    editor = currentEditor,
                    drafts = drafts,
                )
            },
            onBrandChange = viewModel::updateCarEditorBrand,
            onModelChange = viewModel::updateCarEditorModel,
        )
    }

    if (uiState.isEditorValidationAlertPresented) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditorValidationAlert,
            title = { Text(text = "提示") },
            text = { Text(text = uiState.editorValidationMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissEditorValidationAlert) {
                    Text(text = "我知道了")
                }
            },
        )
    }

    if (uiState.isEditorSaveErrorAlertPresented) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditorSaveErrorAlert,
            title = { Text(text = "保存失败") },
            text = { Text(text = uiState.editorSaveErrorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissEditorSaveErrorAlert) {
                    Text(text = "我知道了")
                }
            },
        )
    }
}

@Composable
fun AddCarManagementSection(
    modifier: Modifier = Modifier,
    viewModel: AddCarViewModel = hiltViewModel(),
    carAgeReferenceDate: LocalDate,
    onOpenAddCarEditorPage: () -> Unit = {},
    onOpenEditCarEditorPage: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var openCarMenuKey by remember { mutableStateOf<String?>(null) }

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
                    uiState.cars.forEach { car ->
                        CarRowCard(
                            car = car,
                            isApplied = car.id == uiState.appliedCarId,
                            carAgeReferenceDate = carAgeReferenceDate,
                            isActionsMenuOpen = openCarMenuKey == car.id,
                            onActionsMenuOpen = { openCarMenuKey = car.id },
                            onActionsMenuDismiss = {
                                if (openCarMenuKey == car.id) {
                                    openCarMenuKey = null
                                }
                            },
                            onApply = { viewModel.applyCar(car.id) },
                            onEdit = {
                                viewModel.openEditCarEditor(car)
                                onOpenEditCarEditorPage()
                            },
                            onDelete = { viewModel.requestDeleteCar(car) },
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = {
                    viewModel.openAddCarEditor()
                    onOpenAddCarEditorPage()
                },
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CarRowCard(
    car: CarProfileSnapshot,
    isApplied: Boolean,
    carAgeReferenceDate: LocalDate,
    isActionsMenuOpen: Boolean,
    onActionsMenuOpen: () -> Unit,
    onActionsMenuDismiss: () -> Unit,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val containerColor = if (isApplied) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (isApplied) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = onActionsMenuOpen,
                ),
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${car.brand} ${car.modelName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = "上路日期：${AppTimeCodec.formatDate(car.purchaseDate)}  车龄：${carAgeText(car.purchaseDate, carAgeReferenceDate)}  里程：${formatMileageKm(car.mileage)}km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = isActionsMenuOpen,
                onDismissRequest = onActionsMenuDismiss,
                modifier = Modifier.wrapContentSize(Alignment.BottomEnd),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "编辑",
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    onClick = {
                        onActionsMenuDismiss()
                        onEdit()
                    },
                )
                if (!isApplied) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "应用",
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                        onClick = {
                            onActionsMenuDismiss()
                            onApply()
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "删除",
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        onActionsMenuDismiss()
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun CarEditorPage(
    modifier: Modifier = Modifier,
    editor: CarEditorState,
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
    var openDraftMenuKey by remember { mutableStateOf<String?>(null) }
    val modelOptions = remember(brand) { addCarModelOptions(brand) }
    val canSave = brand.trim().isNotEmpty() && modelName.trim().isNotEmpty()

    LaunchedEffect(brand, modelOptions) {
        if (modelName !in modelOptions) {
            modelName = modelOptions.firstOrNull().orEmpty()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
                enabled = canSave,
            ) {
                Text(text = "保存")
            }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "车辆信息",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
                            LabelValueRow(label = "品牌", value = brand, enabled = false)
                            LabelValueRow(label = "车型", value = modelName, enabled = false)
                        }
                        LabelValueRow(
                            label = "上路日期",
                            value = AppTimeCodec.formatDate(purchaseDate),
                            onClick = { showPurchaseDatePicker = true },
                        )
                        LabelValueRow(
                            label = "里程",
                            value = mileageDisplayText(mileageWan, mileageQian, mileageBai),
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
                        text = "保养项目设置",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "可按需关闭项目，也可点击项目进入设置里程/时间提醒规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        drafts.forEach { draft ->
                            MaintenanceDraftSummaryCard(
                                draft = draft,
                                onClick = { editingDraftId = draft.id },
                                isMenuOpen = openDraftMenuKey == draft.id,
                                onMenuOpen = { openDraftMenuKey = draft.id },
                                onMenuDismiss = {
                                    if (openDraftMenuKey == draft.id) {
                                        openDraftMenuKey = null
                                    }
                                },
                                onEnabledChange = { enabled ->
                                    drafts = drafts.map {
                                        if (it.id == draft.id) it.copy(isEnabled = enabled) else it
                                    }
                                },
                                onDelete = if (!draft.isDefault) {
                                    {
                                        if (openDraftMenuKey == draft.id) {
                                            openDraftMenuKey = null
                                        }
                                        drafts = drafts.filterNot { it.id == draft.id }
                                        if (editingDraftId == draft.id) {
                                            editingDraftId = null
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        FilledTonalButton(
                            onClick = { addingCustomDraft = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "新增自定义项目")
                        }
                    }
                }
            }
        }
    }

    if (showMileagePicker) {
        CarMileagePickerDialog(
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
        CarPurchaseDatePickerDialog(
            purchaseDate = purchaseDate,
            onDismiss = { showPurchaseDatePicker = false },
            onConfirm = {
                purchaseDate = it
                showPurchaseDatePicker = false
            },
        )
    }

        val editingDraft = drafts.firstOrNull { it.id == editingDraftId }
        if (editingDraft != null) {
            ItemRuleEditDialog(
                title = "保养项目设置",
                draft = editingDraft,
                canEditName = editingDraft.isDefault == false,
                onRestoreDefaults = if (editingDraft.isDefault) {
                    restoreItemDraftDefaults(
                        brand = editor.brand,
                        modelName = editor.modelName,
                        draft = editingDraft,
                    )?.let { restored ->
                        { restored }
                    }
                } else {
                    null
                },
                validateResult = { candidate ->
                    val normalizedName = candidate.name.trim()
                    if (drafts.any { it.id != candidate.id && it.name.trim() == normalizedName }) {
                        "项目名称已存在，请更换后再保存。"
                    } else {
                        null
                    }
                },
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
                canEditName = true,
                onRestoreDefaults = null,
                validateResult = { candidate ->
                    val normalizedName = candidate.name.trim()
                    if (drafts.any { it.id != candidate.id && it.name.trim() == normalizedName }) {
                        "项目名称已存在，请更换后再保存。"
                    } else {
                        null
                    }
                },
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
    var anchorWidthPx by remember { mutableStateOf(0) }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    anchorWidthPx = coordinates.size.width
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = selected.ifBlank { "请选择" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = "⌄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(
                        min = 220.dp,
                        max = if (anchorWidthPx > 0) {
                            with(LocalDensity.current) { anchorWidthPx.toDp() }.coerceAtLeast(220.dp)
                        } else {
                            320.dp
                        },
                    ),
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
    HorizontalDivider()
}

@Composable
private fun LabelValueRow(
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val valueColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (onClick != null) base.clickable(onClick = onClick) else base
            },
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            if (onClick != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = valueColor,
                    )
                    Text(
                        text = "⌄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = valueColor,
                )
            }
        },
    )
    HorizontalDivider()
}

@Composable
private fun MaintenanceDraftSummaryCard(
    draft: ItemRuleDraft,
    onClick: () -> Unit,
    isMenuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    if (onDelete == null) {
        MaintenanceDraftSummaryCardContent(
            draft = draft,
            onClick = onClick,
            onEnabledChange = onEnabledChange,
        )
        return
    }

    Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.BottomEnd)) {
        MaintenanceDraftSummaryCardContent(
            draft = draft,
            onClick = onClick,
            onLongClick = onMenuOpen,
            onEnabledChange = onEnabledChange,
        )

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = onMenuDismiss,
                modifier = Modifier.wrapContentSize(Alignment.BottomEnd),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "删除",
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        onMenuDismiss()
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MaintenanceDraftSummaryCardContent(
    draft: ItemRuleDraft,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onEnabledChange: (Boolean) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = draft.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (draft.isDefault) {
                            DefaultBadge()
                        }
                    }
                    Text(
                        text = "提醒：${
                            MaintenanceItemConfig.reminderSummaryText(
                                option = draft,
                                remindByMileageSelector = { it.remindByMileage },
                                mileageIntervalSelector = { it.mileageInterval },
                                remindByTimeSelector = { it.remindByTime },
                                monthIntervalSelector = { it.monthInterval },
                                mileageTextFormatter = ::reminderDistanceText,
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draft.isEnabled,
                    onCheckedChange = onEnabledChange,
                )
            }
        }
    }
}

@Composable
private fun DefaultBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "默认",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun reminderDistanceText(value: Int): String {
    return MaintenanceItemConfig.formatReminderMileageText(value)
}

private fun mileageDisplayText(wan: Int, qian: Int, bai: Int): String {
    val normalizedWan = wan.coerceAtLeast(0)
    val normalizedQian = qian.coerceIn(0, 9)
    val normalizedBai = bai.coerceIn(0, 9)
    return "${normalizedWan}万${normalizedQian}千${normalizedBai}百"
}

private fun formatMileageKm(mileage: Int): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    formatter.isGroupingUsed = true
    return formatter.format(mileage.coerceAtLeast(0))
}

private fun carAgeText(purchaseDate: LocalDate, now: LocalDate): String {
    val ageDays = ChronoUnit.DAYS.between(purchaseDate, now).coerceAtLeast(0)
    val years = ageDays / 365.25
    return String.format(Locale.ROOT, "%.1f年", years)
}

@Composable
private fun ItemRuleEditDialog(
    title: String,
    draft: ItemRuleDraft,
    canEditName: Boolean,
    onRestoreDefaults: (() -> ItemRuleDraft)? = null,
    validateResult: ((ItemRuleDraft) -> String?)? = null,
    onDismiss: () -> Unit,
    onSave: (ItemRuleDraft) -> Unit,
) {
    var name by remember(draft.id) { mutableStateOf(draft.name) }
    var remindByMileage by remember(draft.id) { mutableStateOf(draft.remindByMileage) }
    var mileageInterval by remember(draft.id) { mutableStateOf(draft.mileageInterval) }
    var remindByTime by remember(draft.id) { mutableStateOf(draft.remindByTime) }
    var yearInterval by remember(draft.id) {
        mutableStateOf(draft.monthInterval / 12.0)
    }
    var validationMessage by remember(draft.id) { mutableStateOf<String?>(null) }
    val restoredDefaults = onRestoreDefaults?.invoke()
    val currentDraft = draft.copy(
        remindByMileage = remindByMileage,
        mileageInterval = if (remindByMileage) mileageInterval else 0,
        remindByTime = remindByTime,
        monthInterval = if (remindByTime) (yearInterval * 12).roundToInt() else 0,
    )
    val canRestoreDefaultsNow = restoredDefaults != null &&
        canRestoreItemDraftDefaults(
            draft = currentDraft,
            restored = restoredDefaults,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "项目名称",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            )
                            if (canEditName) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("请输入项目名称") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "提醒方式",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "按里程提醒",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = remindByMileage,
                                    onCheckedChange = { remindByMileage = it },
                                )
                            }
                            if (remindByMileage) {
                                NumberAdjustRow(
                                    label = "里程间隔",
                                    value = mileageInterval,
                                    minValue = 1_000,
                                    maxValue = 500_000,
                                    valueText = ::reminderDistanceText,
                                    onValueChange = { mileageInterval = it },
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "按时间提醒",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = remindByTime,
                                    onCheckedChange = { remindByTime = it },
                                )
                            }
                            if (remindByTime) {
                                YearAdjustRow(
                                    label = "时间间隔",
                                    value = yearInterval,
                                    minValue = 0.5,
                                    maxValue = 20.0,
                                    onValueChange = { yearInterval = it },
                                )
                            }
                        }

                        if (onRestoreDefaults != null) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        restoredDefaults?.let { restored ->
                                            name = restored.name
                                            remindByMileage = restored.remindByMileage
                                            mileageInterval = restored.mileageInterval
                                            remindByTime = restored.remindByTime
                                            yearInterval = restored.monthInterval / 12.0
                                        }
                                    },
                                    enabled = canRestoreDefaultsNow,
                                ) {
                                    Text(text = "恢复默认值")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedName = name.trim()
                    if (normalizedName.isEmpty()) {
                        validationMessage = "项目名称不能为空。"
                        return@TextButton
                    }
                    if (!remindByMileage && !remindByTime) {
                        validationMessage = "请至少开启一种提醒方式。"
                        return@TextButton
                    }
                    val candidate = draft.copy(
                        name = normalizedName,
                        remindByMileage = remindByMileage,
                        mileageInterval = if (remindByMileage) mileageInterval else 0,
                        remindByTime = remindByTime,
                        monthInterval = if (remindByTime) (yearInterval * 12).roundToInt() else 0,
                        warningStartPercent = MaintenanceItemConfig.warningRangeStartPercent,
                        dangerStartPercent = MaintenanceItemConfig.dangerStartPercent,
                    )
                    val duplicateMessage = validateResult?.invoke(candidate)
                    if (duplicateMessage != null) {
                        validationMessage = duplicateMessage
                        return@TextButton
                    }
                    onSave(
                        candidate,
                    )
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

    if (validationMessage != null) {
        AlertDialog(
            onDismissRequest = { validationMessage = null },
            title = { Text(text = "提示") },
            text = { Text(text = validationMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { validationMessage = null }) {
                    Text(text = "我知道了")
                }
            },
        )
    }
}

@Composable
private fun NumberAdjustRow(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    valueText: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
) {
    val step = mileageAdjustStep(value)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label：${valueText(value)}",
            modifier = Modifier.widthIn(max = 180.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AdjustButtonGroup(
            onDecrease = { onValueChange((value - step).coerceAtLeast(minValue)) },
            onIncrease = { onValueChange((value + step).coerceAtMost(maxValue)) },
        )
    }
}

@Composable
private fun YearAdjustRow(
    label: String,
    value: Double,
    minValue: Double,
    maxValue: Double,
    onValueChange: (Double) -> Unit,
) {
    val step = yearAdjustStep(value)
    val displayText = yearIntervalText(value)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label：$displayText",
            modifier = Modifier.widthIn(max = 180.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AdjustButtonGroup(
            onDecrease = { onValueChange((value - step).coerceAtLeast(minValue)) },
            onIncrease = { onValueChange((value + step).coerceAtMost(maxValue)) },
        )
    }
}

private fun mileageAdjustStep(value: Int): Int {
    return if (value <= 10_000) 1_000 else 5_000
}

private fun yearAdjustStep(value: Double): Double {
    return if (value <= 5.0) 0.5 else 1.0
}

@Composable
private fun AdjustButtonGroup(
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onDecrease,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(38.dp)
                    .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                    .height(28.dp),
            ) {
                Text(text = "-", style = MaterialTheme.typography.titleMedium)
            }
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .widthIn(min = 1.dp, max = 1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            TextButton(
                onClick = onIncrease,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(38.dp)
                    .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                    .height(28.dp),
            ) {
                Text(text = "+", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun yearIntervalText(value: Double): String {
    val normalized = value.coerceAtLeast(0.5)
    return if (normalized % 1.0 == 0.0) {
        "${normalized.toInt()}年"
    } else {
        String.format("%.1f年", normalized)
    }
}

private fun restoreItemDraftDefaults(
    brand: String,
    modelName: String,
    draft: ItemRuleDraft,
): ItemRuleDraft? {
    val definition = MaintenanceItemConfig
        .defaultItemDefinitions(brand, modelName)
        .firstOrNull { it.key == draft.catalogKey }
        ?: return null

    return draft.copy(
        name = definition.defaultName,
        remindByMileage = definition.remindByMileage,
        mileageInterval = definition.mileageInterval ?: 1_000,
        remindByTime = definition.remindByTime,
        monthInterval = definition.monthInterval ?: 12,
        warningStartPercent = definition.warningStartPercent,
        dangerStartPercent = definition.dangerStartPercent,
    )
}

private fun canRestoreItemDraftDefaults(
    draft: ItemRuleDraft,
    restored: ItemRuleDraft,
): Boolean {
    return normalizedItemDraftRestoreComparable(draft) != normalizedItemDraftRestoreComparable(restored)
}

private fun normalizedItemDraftRestoreComparable(draft: ItemRuleDraft): List<Any> {
    return listOf(
        draft.remindByMileage,
        if (draft.remindByMileage) draft.mileageInterval else 0,
        draft.remindByTime,
        if (draft.remindByTime) draft.monthInterval else 0,
    )
}

internal fun epochMillisToLocalDate(epochMillis: Long): LocalDate {
    return java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
}
