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
import com.tx.carrecord.core.database.logging.AppDatabaseAuditLogger
import com.tx.carrecord.core.database.logging.AppDatabaseSnapshot
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.core.datastore.MaintenanceDataChangeContext
import com.tx.carrecord.core.common.time.AppTimeCodec
import com.tx.carrecord.core.datastore.logging.AppLogger
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
    private val maintenanceDataChangeContext: MaintenanceDataChangeContext,
    private val appLogger: AppLogger,
    private val auditLogger: AppDatabaseAuditLogger,
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
            var clearedCars: List<CarEntity> = emptyList()
            var clearedItemOptions: List<MaintenanceItemOptionEntity> = emptyList()
            var clearedRecords: List<MaintenanceRecordEntity> = emptyList()
            var clearedRecordItems: List<MaintenanceRecordItemEntity> = emptyList()

            if (success.plan.shouldClearBusinessDataBeforeImport) {
                clearedCars = dao.listCars()
                clearedItemOptions = dao.listItemOptions()
                clearedRecords = dao.listRecords()
                clearedRecordItems = clearedRecords.flatMap { record ->
                    dao.listRecordItemsByRecordId(record.id)
                }
                appLogger.info(
                    "开始清空恢复前业务数据",
                    payload = "cars=${clearedCars.size}, items=${clearedItemOptions.size}, records=${clearedRecords.size}, recordItems=${clearedRecordItems.size}",
                )
            }

            database.withTransaction {
                if (success.plan.shouldClearBusinessDataBeforeImport) {
                    clearBusinessData()
                }

                for (carDraft in success.plan.carDrafts) {
                    val insertedCar = CarEntity(
                        id = carDraft.car.id,
                        brand = carDraft.car.brand,
                        modelName = carDraft.car.modelName,
                        mileage = carDraft.car.mileage,
                        purchaseDate = AppTimeCodec.toEpochSecondsAtStartOfDay(
                            date = carDraft.normalizedPurchaseDate,
                            zoneId = zoneId,
                        ),
                        disabledItemIDsRaw = carDraft.car.disabledItemIDsRaw,
                    )
                    dao.insertCar(entity = insertedCar)
                    auditLogger.logInsert("Car", AppDatabaseSnapshot.car(insertedCar))
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
                        optionEntities.forEach { optionEntity ->
                            auditLogger.logInsert(
                                entity = "MaintenanceItemOption",
                                data = AppDatabaseSnapshot.maintenanceItemOption(optionEntity),
                            )
                        }
                    }
                    importedItemOptionCount += optionEntities.size

                    for (recordDraft in carDraft.recordDrafts) {
                        val cycleKey = "${carDraft.car.id}|${AppTimeCodec.formatDate(recordDraft.normalizedDate)}"
                        val insertedRecord = MaintenanceRecordEntity(
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
                        )
                        dao.insertRecord(entity = insertedRecord)
                        auditLogger.logInsert("MaintenanceRecord", AppDatabaseSnapshot.maintenanceRecord(insertedRecord))

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
                            recordItems.forEach { recordItem ->
                                auditLogger.logInsert(
                                    entity = "MaintenanceRecordItem",
                                    data = AppDatabaseSnapshot.maintenanceRecordItem(recordItem),
                                )
                            }
                        }
                        importedRecordCount += 1
                    }
                }
            }

            if (success.plan.shouldClearBusinessDataBeforeImport) {
                clearedCars.forEach { car ->
                    auditLogger.logDelete("Car", AppDatabaseSnapshot.car(car))
                }
                clearedItemOptions.forEach { option ->
                    auditLogger.logDelete(
                        entity = "MaintenanceItemOption",
                        data = AppDatabaseSnapshot.maintenanceItemOption(option),
                    )
                }
                clearedRecords.forEach { record ->
                    auditLogger.logDelete(
                        entity = "MaintenanceRecord",
                        data = AppDatabaseSnapshot.maintenanceRecord(record),
                    )
                }
                clearedRecordItems.forEach { recordItem ->
                    auditLogger.logDelete(
                        entity = "MaintenanceRecordItem",
                        data = AppDatabaseSnapshot.maintenanceRecordItem(recordItem),
                    )
                }
                appLogger.info(
                    "恢复前业务数据已清空",
                    payload = "cars=${clearedCars.size}, items=${clearedItemOptions.size}, records=${clearedRecords.size}, recordItems=${clearedRecordItems.size}",
                )
            }

            val carUUIDs = dao.listCars().mapNotNull { entity ->
                runCatching { UUID.fromString(entity.id) }.getOrNull()
            }
            appliedCarContext.normalizeAndPersist(carUUIDs)
            maintenanceDataChangeContext.notifyChanged()

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
            val existingCars = dao.listCars()
            val existingItemOptions = dao.listItemOptions()
            val existingRecords = dao.listRecords()
            val existingRecordItems = existingRecords.flatMap { record ->
                dao.listRecordItemsByRecordId(record.id)
            }
            database.withTransaction {
                clearBusinessData()
            }
            existingCars.forEach { car ->
                auditLogger.logDelete("Car", AppDatabaseSnapshot.car(car))
            }
            existingItemOptions.forEach { option ->
                auditLogger.logDelete(
                    entity = "MaintenanceItemOption",
                    data = AppDatabaseSnapshot.maintenanceItemOption(option),
                )
            }
            existingRecords.forEach { record ->
                auditLogger.logDelete(
                    entity = "MaintenanceRecord",
                    data = AppDatabaseSnapshot.maintenanceRecord(record),
                )
            }
            existingRecordItems.forEach { recordItem ->
                auditLogger.logDelete(
                    entity = "MaintenanceRecordItem",
                    data = AppDatabaseSnapshot.maintenanceRecordItem(recordItem),
                )
            }
            appliedCarContext.normalizeAndPersist(emptyList())
            maintenanceDataChangeContext.notifyChanged()
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
            ownerCarID = ownerCarID,
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
        carID = carId,
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
