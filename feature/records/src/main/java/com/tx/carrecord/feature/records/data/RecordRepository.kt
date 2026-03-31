package com.tx.carrecord.feature.records.data

import androidx.room.withTransaction
import com.tx.carrecord.core.common.RepositoryError
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.error.RoomRepositoryErrorMapper
import com.tx.carrecord.core.database.model.MaintenanceRecordEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordItemEntity
import com.tx.carrecord.core.database.room.CarRecordDatabase
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.feature.records.domain.ExistingRecordSnapshot
import com.tx.carrecord.feature.records.domain.RecordSaveDecision
import com.tx.carrecord.feature.records.domain.RecordSaveInput
import com.tx.carrecord.feature.records.domain.RecordsDomainRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SaveRecordRequest(
    val recordId: String? = null,
    val carId: String? = null,
    val date: LocalDate? = null,
    val itemIDsRaw: String,
    val cost: Double,
    val mileage: Int,
    val note: String,
    val intervalDrafts: List<SaveRecordIntervalDraft> = emptyList(),
)

data class SaveRecordIntervalDraft(
    val itemId: String,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
)

data class SaveRecordResult(
    val recordId: String,
    val cycleKey: String,
    val normalizedItemIDsRaw: String,
    val syncedCarMileage: Int,
)

data class RecordSnapshot(
    val id: String,
    val carId: String,
    val date: LocalDate,
    val itemIDsRaw: String,
    val cost: Double,
    val mileage: Int,
    val note: String,
    val cycleKey: String,
)

interface RecordRepository {
    suspend fun listRecords(carId: String? = null): RepositoryResult<List<RecordSnapshot>>

    suspend fun saveRecord(request: SaveRecordRequest): RepositoryResult<SaveRecordResult>

    suspend fun deleteRecord(recordId: String): RepositoryResult<Unit>
}

class RoomRecordRepository @Inject constructor(
    private val database: CarRecordDatabase,
    private val dao: CarRecordDao,
    private val appDateContext: AppDateContext,
    private val appliedCarContext: AppliedCarContext,
) : RecordRepository {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun listRecords(carId: String?): RepositoryResult<List<RecordSnapshot>> = runCatching {
        val records = if (carId.isNullOrBlank()) {
            dao.listRecords()
        } else {
            dao.listRecordsByCarId(carId)
        }
        records.map { entity ->
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
        }
    }.fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
    )

    override suspend fun saveRecord(request: SaveRecordRequest): RepositoryResult<SaveRecordResult> {
        val resolvedCarId = resolveCarId(request.carId)
            ?: return RepositoryResult.Failure(
                RepositoryError.NotFound(
                    code = "RECORD_CAR_NOT_SELECTED",
                    message = "保存失败：未选择车辆。",
                ),
            )

        return runCatching {
            val car = dao.findCarById(resolvedCarId)
                ?: return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "RECORD_CAR_NOT_FOUND",
                        message = "保存失败：车辆不存在。",
                    ),
                )

            if (request.recordId != null && dao.findRecordById(request.recordId) == null) {
                return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "RECORD_NOT_FOUND",
                        message = "保存失败：要编辑的记录不存在。",
                    ),
                )
            }

            val normalizedDate = request.date ?: appDateContext.now()
            val decision = RecordsDomainRules.planSave(
                input = RecordSaveInput(
                    recordId = request.recordId,
                    carId = resolvedCarId,
                    dateTime = normalizedDate.atStartOfDay(),
                    itemIDsRaw = request.itemIDsRaw,
                    mileage = request.mileage,
                ),
                existingRecords = dao.listRecordsByCarId(resolvedCarId).map { record ->
                    ExistingRecordSnapshot(
                        id = record.id,
                        carId = record.carId,
                        dateTime = fromEpochSeconds(record.date).atStartOfDay(),
                    )
                },
                carCurrentMileage = car.mileage,
                createdAtEpochSeconds = toEpochSeconds(normalizedDate),
            )
            val success = decision as? RecordSaveDecision.Success
                ?: return RepositoryResult.Failure(
                    RepositoryError.RuleViolation(
                        code = "RECORD_DUPLICATE_CYCLE",
                        message = "保存失败：同车同日只允许一条记录。",
                    ),
                )

            database.withTransaction {
                val recordEntity = MaintenanceRecordEntity(
                    id = success.plan.targetRecordId,
                    carId = resolvedCarId,
                    date = toEpochSeconds(success.plan.normalizedDate),
                    itemIDsRaw = success.plan.normalizedItemIDsRaw,
                    cost = request.cost,
                    mileage = request.mileage,
                    note = request.note,
                    cycleKey = success.plan.cycleKey,
                )
                if (request.recordId == null) {
                    dao.insertRecord(recordEntity)
                } else {
                    dao.updateRecordById(
                        recordId = success.plan.targetRecordId,
                        carId = recordEntity.carId,
                        date = recordEntity.date,
                        itemIDsRaw = recordEntity.itemIDsRaw,
                    cost = recordEntity.cost,
                    mileage = recordEntity.mileage,
                    note = recordEntity.note,
                    cycleKey = recordEntity.cycleKey,
                    )
                }

                dao.replaceRecordItems(
                    recordId = success.plan.targetRecordId,
                    entities = success.plan.relationDrafts.map { draft ->
                        MaintenanceRecordItemEntity(
                            id = draft.id,
                            recordId = draft.recordId,
                            itemId = draft.itemId,
                            cycleItemKey = draft.cycleItemKey,
                            createdAt = draft.createdAtEpochSeconds,
                        )
                    },
                )

                if (request.intervalDrafts.isNotEmpty()) {
                    val optionIDs = request.intervalDrafts.map { it.itemId }.toSet()
                    val availableOptions = dao.listItemOptionsByCarId(resolvedCarId)
                        .filter { option -> option.id in optionIDs }
                        .associateBy { it.id }
                    request.intervalDrafts.forEach { draft ->
                        val option = availableOptions[draft.itemId] ?: return@forEach
                        dao.updateItemOptionById(
                            itemId = option.id,
                            name = option.name,
                            isDefault = option.isDefault,
                            catalogKey = option.catalogKey,
                            remindByMileage = draft.remindByMileage,
                            mileageInterval = if (draft.remindByMileage) draft.mileageInterval.coerceAtLeast(1) else 0,
                            remindByTime = draft.remindByTime,
                            monthInterval = if (draft.remindByTime) draft.monthInterval.coerceAtLeast(1) else 0,
                            warningStartPercent = option.warningStartPercent,
                            dangerStartPercent = option.dangerStartPercent,
                        )
                    }
                }

                if (success.plan.syncedCarMileage > car.mileage) {
                    dao.updateCarMileage(
                        carId = resolvedCarId,
                        mileage = success.plan.syncedCarMileage,
                    )
                }
            }

            RepositoryResult.Success(
                SaveRecordResult(
                    recordId = success.plan.targetRecordId,
                    cycleKey = success.plan.cycleKey,
                    normalizedItemIDsRaw = success.plan.normalizedItemIDsRaw,
                    syncedCarMileage = success.plan.syncedCarMileage,
                ),
            )
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override suspend fun deleteRecord(recordId: String): RepositoryResult<Unit> {
        return runCatching {
            val existingRecord = dao.findRecordById(recordId)
                ?: return RepositoryResult.Failure(
                    RepositoryError.NotFound(
                        code = "RECORD_NOT_FOUND",
                        message = "未找到要删除的记录。",
                    ),
                )
            database.withTransaction {
                dao.deleteRecordById(existingRecord.id)
            }
            RepositoryResult.Success(Unit)
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    private suspend fun resolveCarId(preferredCarId: String?): String? {
        if (!preferredCarId.isNullOrBlank()) return preferredCarId
        return appliedCarContext.appliedCarIdFlow.first()?.toString()
    }

    private fun fromEpochSeconds(epochSeconds: Long): LocalDate =
        Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()

    private fun toEpochSeconds(date: LocalDate): Long = date.atStartOfDay(zoneId).toEpochSecond()
}
