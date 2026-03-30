package com.tx.carrecord.feature.addcar.data

import androidx.room.withTransaction
import com.tx.carrecord.core.common.RepositoryError
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.error.RoomRepositoryErrorMapper
import com.tx.carrecord.core.database.model.CarEntity
import com.tx.carrecord.core.database.model.MaintenanceItemOptionEntity
import com.tx.carrecord.core.database.room.CarRecordDatabase
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.feature.addcar.domain.CarManagementRules
import com.tx.carrecord.feature.addcar.domain.CarProfileSnapshot
import com.tx.carrecord.feature.addcar.domain.CarUpsertDecision
import com.tx.carrecord.feature.addcar.domain.CarUpsertInput
import com.tx.carrecord.feature.datatransfer.domain.MyDataTransferTimeCodec
import java.time.ZoneId
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class CarUpsertRequest(
    val editingCarId: String? = null,
    val brand: String,
    val modelName: String,
    val mileage: Int,
    val purchaseDateEpochSeconds: Long,
    val disabledItemIDsRaw: String,
)

data class CarMutationResult(
    val carId: String,
    val normalizedRawAppliedCarId: String,
)

data class CarItemOptionSnapshot(
    val id: String,
    val name: String,
    val ownerCarId: String?,
    val isDefault: Boolean,
    val catalogKey: String?,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
    val warningStartPercent: Int,
    val dangerStartPercent: Int,
    val createdAtEpochSeconds: Long,
)

data class CarItemOptionUpsertDraft(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val catalogKey: String?,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
    val warningStartPercent: Int,
    val dangerStartPercent: Int,
)

data class SaveCarItemOptionsRequest(
    val carId: String,
    val drafts: List<CarItemOptionUpsertDraft>,
    val disabledItemIDsRaw: String,
)

interface CarRepository {
    suspend fun listCars(): RepositoryResult<List<CarProfileSnapshot>>

    suspend fun resolveAppliedCarId(cars: List<CarProfileSnapshot>): RepositoryResult<String>

    suspend fun applyCar(carId: String): RepositoryResult<String>

    suspend fun listItemOptionsByCarId(carId: String): RepositoryResult<List<CarItemOptionSnapshot>>

    suspend fun saveItemOptions(request: SaveCarItemOptionsRequest): RepositoryResult<Unit>

    suspend fun setupDefaultItemOptionsForCarIfAbsent(
        carId: String,
        brand: String,
        modelName: String,
    ): RepositoryResult<Unit>

    fun modelItemDefaults(
        brand: String,
        modelName: String,
    ): MaintenanceItemConfig.ModelConfig

    suspend fun upsertCar(request: CarUpsertRequest): RepositoryResult<CarMutationResult>

    suspend fun deleteCar(carId: String): RepositoryResult<CarMutationResult>

    suspend fun updateDisabledItemIDsRaw(
        carId: String,
        disabledItemIDsRaw: String,
    ): RepositoryResult<Unit>
}

class RoomCarRepository @Inject constructor(
    private val database: CarRecordDatabase,
    private val dao: CarRecordDao,
    private val appliedCarContext: AppliedCarContext,
) : CarRepository {
    private companion object {
        const val ITEM_ID_SEPARATOR: Char = '|'
    }

    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun listCars(): RepositoryResult<List<CarProfileSnapshot>> = runCatching {
        dao.listCars().map { it.toSnapshot() }
    }.fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
    )

    override suspend fun resolveAppliedCarId(cars: List<CarProfileSnapshot>): RepositoryResult<String> {
        return runCatching {
            val rawAppliedCarId = appliedCarContext.rawAppliedCarIdFlow.first()
            val resolution = CarManagementRules.resolveAppliedCar(
                rawAppliedCarId = rawAppliedCarId,
                cars = cars,
            )
            if (rawAppliedCarId != resolution.normalizedRawAppliedCarId) {
                appliedCarContext.setRawAppliedCarId(resolution.normalizedRawAppliedCarId)
            }
            RepositoryResult.Success(resolution.resolvedCarId.orEmpty())
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override suspend fun applyCar(carId: String): RepositoryResult<String> {
        return runCatching {
            val normalizedCarId = carId.trim()
            if (normalizedCarId.isEmpty()) {
                return RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "CAR_INVALID_ID",
                        message = "车型标识无效。",
                    ),
                )
            }
            val exists = dao.findCarById(normalizedCarId) != null
            if (!exists) {
                return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "CAR_NOT_FOUND",
                        message = "未找到要应用的车辆。",
                    ),
                )
            }
            appliedCarContext.setRawAppliedCarId(normalizedCarId)
            RepositoryResult.Success(normalizedCarId)
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override suspend fun listItemOptionsByCarId(carId: String): RepositoryResult<List<CarItemOptionSnapshot>> {
        return runCatching {
            dao.listItemOptionsByCarId(carId).map { option ->
                option.toSnapshot()
            }
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
        )
    }

    override suspend fun saveItemOptions(request: SaveCarItemOptionsRequest): RepositoryResult<Unit> {
        val carId = request.carId.trim()
        if (carId.isEmpty()) {
            return RepositoryResult.Failure(
                RepositoryError.RuleViolation(
                    code = "ITEM_OPTIONS_INVALID_CAR",
                    message = "保存失败：车辆标识无效。",
                ),
            )
        }

        return runCatching {
            val existingCar = dao.findCarById(carId)
                ?: return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "CAR_NOT_FOUND",
                        message = "保存失败：车辆不存在。",
                    ),
                )

            if (request.drafts.isEmpty()) {
                return RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "ITEM_OPTIONS_EMPTY",
                        message = "请至少保留一个保养项目。",
                    ),
                )
            }

            val normalizedDrafts = request.drafts.map { draft ->
                val normalizedName = draft.name.trim()
                if (normalizedName.isEmpty()) {
                    throw IllegalArgumentException("ITEM_NAME_EMPTY")
                }
                if (!draft.remindByMileage && !draft.remindByTime) {
                    throw IllegalArgumentException("ITEM_NO_REMINDER")
                }
                draft.copy(
                    id = draft.id.trim(),
                    name = normalizedName,
                    mileageInterval = if (draft.remindByMileage) draft.mileageInterval.coerceAtLeast(1) else 0,
                    monthInterval = if (draft.remindByTime) draft.monthInterval.coerceAtLeast(1) else 0,
                    warningStartPercent = draft.warningStartPercent.coerceAtLeast(0),
                    dangerStartPercent = draft.dangerStartPercent.coerceAtLeast(draft.warningStartPercent.coerceAtLeast(0)),
                )
            }

            val duplicateName = normalizedDrafts
                .groupBy { it.name }
                .entries
                .firstOrNull { it.value.size > 1 }
            if (duplicateName != null) {
                return RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "ITEM_NAME_DUPLICATED",
                        message = "存在重名项目，请先调整后再保存。",
                    ),
                )
            }

            val existingOptions = dao.listItemOptionsByCarId(carId)
            val existingById = existingOptions.associateBy { it.id }
            val targetIds = normalizedDrafts.map { it.id }.toSet()
            val removingCustomOptions = existingOptions.filter { option ->
                !option.isDefault && option.id !in targetIds
            }

            if (removingCustomOptions.isNotEmpty()) {
                val recordItemIds = dao.listRecordsByCarId(carId)
                    .flatMap { record -> parseItemIDsRaw(record.itemIDsRaw) }
                    .toSet()
                val blocked = removingCustomOptions.firstOrNull { option -> option.id in recordItemIds }
                if (blocked != null) {
                    return RepositoryResult.Failure(
                        RepositoryError.RuleViolation(
                            code = "ITEM_DELETE_BLOCKED",
                            message = "自定义项目“${blocked.name}”已有历史记录，不能删除。",
                        ),
                    )
                }
            }

            database.withTransaction {
                val nowEpoch = LocalDateTime.now().atZone(zoneId).toEpochSecond()
                normalizedDrafts.forEach { draft ->
                    val existing = existingById[draft.id]
                    if (existing == null) {
                        dao.insertItemOption(
                            entity = MaintenanceItemOptionEntity(
                                id = draft.id,
                                name = draft.name,
                                ownerCarID = carId,
                                isDefault = draft.isDefault,
                                catalogKey = draft.catalogKey,
                                remindByMileage = draft.remindByMileage,
                                mileageInterval = draft.mileageInterval,
                                remindByTime = draft.remindByTime,
                                monthInterval = draft.monthInterval,
                                warningStartPercent = draft.warningStartPercent,
                                dangerStartPercent = draft.dangerStartPercent,
                                createdAt = nowEpoch,
                            ),
                        )
                    } else {
                        dao.updateItemOptionById(
                            itemId = draft.id,
                            name = draft.name,
                            isDefault = draft.isDefault,
                            catalogKey = draft.catalogKey,
                            remindByMileage = draft.remindByMileage,
                            mileageInterval = draft.mileageInterval,
                            remindByTime = draft.remindByTime,
                            monthInterval = draft.monthInterval,
                            warningStartPercent = draft.warningStartPercent,
                            dangerStartPercent = draft.dangerStartPercent,
                        )
                    }
                }

                removingCustomOptions.forEach { option ->
                    dao.deleteItemOptionById(option.id)
                }

                val normalizedDisabledRaw = MaintenanceItemConfig.normalizeItemIDsRaw(request.disabledItemIDsRaw)
                dao.updateCarDisabledItemIDsRaw(
                    carId = existingCar.id,
                    disabledItemIDsRaw = normalizedDisabledRaw,
                )
            }

            RepositoryResult.Success(Unit)
        }.getOrElse { throwable ->
            when ((throwable as? IllegalArgumentException)?.message) {
                "ITEM_NAME_EMPTY" -> RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "ITEM_NAME_EMPTY",
                        message = "项目名称不能为空。",
                    ),
                )

                "ITEM_NO_REMINDER" -> RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "ITEM_NO_REMINDER",
                        message = "请至少开启一种提醒方式。",
                    ),
                )

                else -> RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
            }
        }
    }

    override suspend fun setupDefaultItemOptionsForCarIfAbsent(
        carId: String,
        brand: String,
        modelName: String,
    ): RepositoryResult<Unit> {
        return runCatching {
            val existing = dao.listItemOptionsByCarId(carId)
            if (existing.isNotEmpty()) {
                return RepositoryResult.Success(Unit)
            }

            val modelDefaults = modelItemDefaults(brand = brand, modelName = modelName)
            val nowEpoch = LocalDateTime.now().atZone(zoneId).toEpochSecond()
            val entities = modelDefaults.defaultItemDefinitions.map { definition ->
                MaintenanceItemOptionEntity(
                    id = UUID.randomUUID().toString(),
                    name = definition.defaultName,
                    ownerCarID = carId,
                    isDefault = true,
                    catalogKey = definition.key,
                    remindByMileage = definition.remindByMileage,
                    mileageInterval = if (definition.remindByMileage) (definition.mileageInterval ?: 5000).coerceAtLeast(1) else 0,
                    remindByTime = definition.remindByTime,
                    monthInterval = if (definition.remindByTime) (definition.monthInterval ?: 12).coerceAtLeast(1) else 0,
                    warningStartPercent = definition.warningStartPercent,
                    dangerStartPercent = definition.dangerStartPercent,
                    createdAt = nowEpoch,
                )
            }

            database.withTransaction {
                if (entities.isNotEmpty()) {
                    dao.insertItemOptions(entities)
                }
            }
            RepositoryResult.Success(Unit)
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override fun modelItemDefaults(
        brand: String,
        modelName: String,
    ): MaintenanceItemConfig.ModelConfig =
        MaintenanceItemConfig.modelConfig(brand = brand, modelName = modelName)

    override suspend fun upsertCar(request: CarUpsertRequest): RepositoryResult<CarMutationResult> {
        return runCatching {
            val existingCars = dao.listCars()
            val existingSnapshots = existingCars.map { it.toSnapshot() }
            val decision = CarManagementRules.planUpsertCar(
                input = CarUpsertInput(
                    editingCarId = request.editingCarId,
                    brand = request.brand,
                    modelName = request.modelName,
                    disabledItemIDsRaw = request.disabledItemIDsRaw,
                ),
                existingCars = existingSnapshots,
            )
            val success = decision as? CarUpsertDecision.Success
                ?: return RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "CAR_DUPLICATE_MODEL",
                        message = (decision as CarUpsertDecision.DuplicateModelConflict).let { conflict ->
                            "车型已存在：${conflict.conflictBrand} ${conflict.conflictModelName}。"
                        },
                    ),
                )

            val targetCarId = request.editingCarId ?: UUID.randomUUID().toString()
            database.withTransaction {
                if (request.editingCarId == null) {
                    dao.insertCar(
                        entity = CarEntity(
                            id = targetCarId,
                            brand = success.plan.normalizedBrand,
                            modelName = success.plan.normalizedModelName,
                            mileage = request.mileage,
                            purchaseDate = request.purchaseDateEpochSeconds,
                            disabledItemIDsRaw = success.plan.normalizedDisabledItemIDsRaw,
                        ),
                    )
                } else {
                    val updated = dao.updateCarById(
                        carId = targetCarId,
                        brand = success.plan.normalizedBrand,
                        modelName = success.plan.normalizedModelName,
                        mileage = request.mileage,
                        purchaseDate = request.purchaseDateEpochSeconds,
                        disabledItemIDsRaw = success.plan.normalizedDisabledItemIDsRaw,
                    )
                    if (updated <= 0) {
                        throw IllegalStateException("CAR_NOT_FOUND_FOR_UPDATE")
                    }
                }
            }

            val allCarIds = dao.listCars().mapNotNull { entity ->
                runCatching { UUID.fromString(entity.id) }.getOrNull()
            }
            val normalizedRawAppliedCarId = appliedCarContext.normalizeAndPersist(allCarIds)
            RepositoryResult.Success(
                CarMutationResult(
                    carId = targetCarId,
                    normalizedRawAppliedCarId = normalizedRawAppliedCarId,
                ),
            )
        }.getOrElse { throwable ->
            if (throwable is IllegalStateException && throwable.message == "CAR_NOT_FOUND_FOR_UPDATE") {
                return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "CAR_NOT_FOUND",
                        message = "未找到要更新的车辆。",
                    ),
                )
            }
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override suspend fun deleteCar(carId: String): RepositoryResult<CarMutationResult> {
        return runCatching {
            val existingCars = dao.listCars()
            val rawAppliedCarId = appliedCarContext.rawAppliedCarIdFlow.first()
            val decision = CarManagementRules.planDeleteCar(
                deletingCarId = carId,
                cars = existingCars.map { it.toSnapshot() },
                rawAppliedCarId = rawAppliedCarId,
            )
            val success = decision as? com.tx.carrecord.feature.addcar.domain.CarDeletionDecision.Success
                ?: return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "CAR_NOT_FOUND",
                        message = "未找到要删除的车辆。",
                    ),
                )

            database.withTransaction {
                dao.deleteCarById(success.plan.deletedCarId)
            }
            appliedCarContext.setRawAppliedCarId(success.plan.normalizedRawAppliedCarId)
            RepositoryResult.Success(
                CarMutationResult(
                    carId = success.plan.deletedCarId,
                    normalizedRawAppliedCarId = success.plan.normalizedRawAppliedCarId,
                ),
            )
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override suspend fun updateDisabledItemIDsRaw(
        carId: String,
        disabledItemIDsRaw: String,
    ): RepositoryResult<Unit> {
        return runCatching {
            val existingCars = dao.listCars().map { it.toSnapshot() }
            val decision = CarManagementRules.planDisabledItemIsolationUpdate(
                targetCarId = carId,
                targetDisabledItemIDsRaw = disabledItemIDsRaw,
                cars = existingCars,
            )
            val success = decision as? com.tx.carrecord.feature.addcar.domain.DisabledItemIsolationDecision.Success
                ?: return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "CAR_NOT_FOUND",
                        message = "未找到要更新禁用项目的车辆。",
                    ),
                )

            database.withTransaction {
                success.plan.disabledItemIDsRawByCarId.forEach { (targetCarId, normalizedRaw) ->
                    dao.updateCarDisabledItemIDsRaw(
                        carId = targetCarId,
                        disabledItemIDsRaw = normalizedRaw,
                    )
                }
            }
            RepositoryResult.Success(Unit)
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    private fun CarEntity.toSnapshot(): CarProfileSnapshot = CarProfileSnapshot(
        id = id,
        brand = brand,
        modelName = modelName,
        mileage = mileage,
        purchaseDate = MyDataTransferTimeCodec.fromEpochSecondsAtZone(
            epochSeconds = purchaseDate,
            zoneId = zoneId,
        ),
        disabledItemIDsRaw = disabledItemIDsRaw,
    )

    private fun MaintenanceItemOptionEntity.toSnapshot(): CarItemOptionSnapshot = CarItemOptionSnapshot(
        id = id,
        name = name,
        ownerCarId = ownerCarID,
        isDefault = isDefault,
        catalogKey = catalogKey,
        remindByMileage = remindByMileage,
        mileageInterval = mileageInterval,
        remindByTime = remindByTime,
        monthInterval = monthInterval,
        warningStartPercent = warningStartPercent,
        dangerStartPercent = dangerStartPercent,
        createdAtEpochSeconds = createdAt,
    )

    private fun parseItemIDsRaw(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ITEM_ID_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
