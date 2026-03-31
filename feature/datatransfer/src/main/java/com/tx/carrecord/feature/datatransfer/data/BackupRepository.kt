package com.tx.carrecord.feature.datatransfer.data

import androidx.room.withTransaction
import com.tx.carrecord.core.common.RepositoryError
import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.error.RoomRepositoryErrorMapper
import com.tx.carrecord.core.database.model.CarEntity
import com.tx.carrecord.core.database.model.MaintenanceItemOptionEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordItemEntity
import com.tx.carrecord.core.database.room.CarRecordDatabase
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.core.common.time.AppTimeCodec
import com.tx.carrecord.feature.datatransfer.domain.BackupExportCarSnapshot
import com.tx.carrecord.feature.datatransfer.domain.BackupExportItemOptionSnapshot
import com.tx.carrecord.feature.datatransfer.domain.BackupExportRecordSnapshot
import com.tx.carrecord.feature.datatransfer.domain.BackupImportDecision
import com.tx.carrecord.feature.datatransfer.domain.MyDataTransferRules
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

private const val ITEM_ID_SEPARATOR: Char = '|'

data class BackupImportSummary(
    val importedCarCount: Int,
    val importedItemOptionCount: Int,
    val importedRecordCount: Int,
)

interface BackupRepository {
    suspend fun exportBackupJson(): RepositoryResult<String>

    suspend fun importBackupJson(jsonText: String): RepositoryResult<BackupImportSummary>

    suspend fun resetBusinessData(): RepositoryResult<Unit>

    suspend fun hasAnyBusinessData(): RepositoryResult<Boolean>
}

class RoomBackupRepository @Inject constructor(
    private val database: CarRecordDatabase,
    private val dao: CarRecordDao,
    private val appliedCarContext: AppliedCarContext,
) : BackupRepository {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun exportBackupJson(): RepositoryResult<String> = runCatching {
        val payload = MyDataTransferRules.buildExportPayload(
            cars = dao.listCars().map { it.toExportSnapshot() },
            itemOptions = dao.listItemOptions().map { it.toExportSnapshot() },
            records = dao.listRecords().map { it.toExportSnapshot() },
        )
        MyDataTransferRules.encodePayload(payload)
    }.fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
    )

    override suspend fun importBackupJson(jsonText: String): RepositoryResult<BackupImportSummary> {
        val decision = MyDataTransferRules.decodePayload(jsonText)
        val success = decision as? BackupImportDecision.Success
            ?: return RepositoryResult.Failure(
                RepositoryError.RuleViolation(
                    code = "BACKUP_INVALID_PAYLOAD_${(decision as BackupImportDecision.InvalidPayload).code}",
                    message = decision.message,
                ),
            )

        return runCatching {
            var importedCarCount = 0
            var importedItemOptionCount = 0
            var importedRecordCount = 0

            database.withTransaction {
                if (success.plan.shouldClearBusinessDataBeforeImport) {
                    clearBusinessData()
                }

                for (carDraft in success.plan.carDrafts) {
                    dao.insertCar(
                        entity = CarEntity(
                            id = carDraft.car.id,
                            brand = carDraft.car.brand,
                            modelName = carDraft.car.modelName,
                            mileage = carDraft.car.mileage,
                            purchaseDate = AppTimeCodec.toEpochSecondsAtStartOfDay(
                                date = carDraft.normalizedPurchaseDate,
                                zoneId = zoneId,
                            ),
                            disabledItemIDsRaw = carDraft.car.disabledItemIDsRaw,
                        ),
                    )
                    importedCarCount += 1

                    val optionEntities = carDraft.itemDrafts.map { itemDraft ->
                        MaintenanceItemOptionEntity(
                            id = itemDraft.item.id,
                            name = itemDraft.item.name,
                            ownerCarID = carDraft.car.id,
                            isDefault = itemDraft.item.isDefault,
                            catalogKey = itemDraft.item.catalogKey,
                            remindByMileage = itemDraft.item.remindByMileage,
                            mileageInterval = itemDraft.item.mileageInterval,
                            remindByTime = itemDraft.item.remindByTime,
                            monthInterval = itemDraft.item.monthInterval,
                            warningStartPercent = itemDraft.item.warningStartPercent,
                            dangerStartPercent = itemDraft.item.dangerStartPercent,
                            createdAt = AppTimeCodec.payloadCreatedAtToEpochSeconds(
                                itemDraft.item.createdAt,
                            ),
                        )
                    }
                    if (optionEntities.isNotEmpty()) {
                        dao.insertItemOptions(optionEntities)
                    }
                    importedItemOptionCount += optionEntities.size

                    for (recordDraft in carDraft.recordDrafts) {
                        val cycleKey = "${carDraft.car.id}|${AppTimeCodec.formatDate(recordDraft.normalizedDate)}"
                        dao.insertRecord(
                            entity = MaintenanceRecordEntity(
                                id = recordDraft.record.id,
                                carId = carDraft.car.id,
                                date = AppTimeCodec.toEpochSecondsAtStartOfDay(
                                    date = recordDraft.normalizedDate,
                                    zoneId = zoneId,
                                ),
                                itemIDsRaw = recordDraft.mappedItemIDsRaw,
                                cost = recordDraft.record.cost,
                                mileage = recordDraft.record.mileage,
                                note = recordDraft.record.note,
                                cycleKey = cycleKey,
                            ),
                        )

                        val recordItems = recordDraft.mappedItemIds.map { itemId ->
                            MaintenanceRecordItemEntity(
                                id = UUID.randomUUID().toString(),
                                recordId = recordDraft.record.id,
                                itemId = itemId,
                                cycleItemKey = "$cycleKey$ITEM_ID_SEPARATOR$itemId",
                                createdAt = AppTimeCodec.toEpochSecondsAtStartOfDay(
                                    date = recordDraft.normalizedDate,
                                    zoneId = zoneId,
                                ),
                            )
                        }
                        if (recordItems.isNotEmpty()) {
                            dao.insertRecordItems(recordItems)
                        }
                        importedRecordCount += 1
                    }
                }
            }

            val carUUIDs = dao.listCars().mapNotNull { entity ->
                runCatching { UUID.fromString(entity.id) }.getOrNull()
            }
            appliedCarContext.normalizeAndPersist(carUUIDs)

            RepositoryResult.Success(
                BackupImportSummary(
                    importedCarCount = importedCarCount,
                    importedItemOptionCount = importedItemOptionCount,
                    importedRecordCount = importedRecordCount,
                ),
            )
        }.getOrElse { throwable ->
            RepositoryResult.Failure(RoomRepositoryErrorMapper.map(throwable))
        }
    }

    override suspend fun resetBusinessData(): RepositoryResult<Unit> {
        return runCatching {
            database.withTransaction {
                clearBusinessData()
            }
            appliedCarContext.normalizeAndPersist(emptyList())
        }.fold(
            onSuccess = { RepositoryResult.Success(Unit) },
            onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
        )
    }

    override suspend fun hasAnyBusinessData(): RepositoryResult<Boolean> = runCatching {
        dao.hasCars() || dao.hasRecords() || dao.hasItemOptions() || dao.hasRecordItems()
    }.fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
    )

    private suspend fun clearBusinessData() {
        dao.clearRecordItems()
        dao.clearRecords()
        dao.clearItemOptions()
        dao.clearCars()
    }

    private fun CarEntity.toExportSnapshot(): BackupExportCarSnapshot = BackupExportCarSnapshot(
        id = id,
        brand = brand,
        modelName = modelName,
        mileage = mileage,
        disabledItemIDsRaw = disabledItemIDsRaw,
        purchaseDate = AppTimeCodec.fromEpochSecondsAtZone(
            epochSeconds = purchaseDate,
            zoneId = zoneId,
        ),
    )

    private fun MaintenanceItemOptionEntity.toExportSnapshot(): BackupExportItemOptionSnapshot =
        BackupExportItemOptionSnapshot(
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

    private fun MaintenanceRecordEntity.toExportSnapshot(): BackupExportRecordSnapshot = BackupExportRecordSnapshot(
        id = id,
        carId = carId,
        date = AppTimeCodec.fromEpochSecondsAtZone(
            epochSeconds = date,
            zoneId = zoneId,
        ),
        itemIDsRaw = itemIDsRaw,
        cost = cost,
        mileage = mileage,
        note = note,
    )
}
