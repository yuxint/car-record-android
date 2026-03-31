package com.tx.carrecord.feature.reminder.data

import com.tx.carrecord.core.common.RepositoryResult
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.error.RoomRepositoryErrorMapper
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.feature.reminder.domain.ReminderCarSnapshot
import com.tx.carrecord.feature.reminder.domain.ReminderItemOptionSnapshot
import com.tx.carrecord.feature.reminder.domain.ReminderRecordSnapshot
import com.tx.carrecord.feature.reminder.domain.ReminderRow
import com.tx.carrecord.feature.reminder.domain.ReminderRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface ReminderRepository {
    suspend fun loadAppliedCarRows(): RepositoryResult<ReminderLoadResult>
}

data class ReminderLoadResult(
    val rows: List<ReminderRow>,
    val emptyMessage: String? = null,
    val canAddRecord: Boolean,
    val carDisplayName: String? = null,
)

class RoomReminderRepository @Inject constructor(
    private val dao: CarRecordDao,
    private val appDateContext: AppDateContext,
    private val appliedCarContext: AppliedCarContext,
) : ReminderRepository {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    override suspend fun loadAppliedCarRows(): RepositoryResult<ReminderLoadResult> = runCatching {
        val cars = dao.listCars()
        if (cars.isEmpty()) {
            return@runCatching ReminderLoadResult(
                rows = emptyList(),
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
        val resolvedCarId = resolvedAppliedCarId?.toString() ?: cars.first().id

        val normalizedRawId = resolvedAppliedCarId?.toString().orEmpty()
        if (rawAppliedCarId != normalizedRawId && availableCarIds.isNotEmpty()) {
            appliedCarContext.setRawAppliedCarId(normalizedRawId)
        }

        val targetCar = cars.firstOrNull { it.id == resolvedCarId }
            ?: cars.first()
        val now = appDateContext.now()
        val targetCarRecords = dao.listRecordsByCarId(targetCar.id)
        if (targetCarRecords.isEmpty()) {
            return@runCatching ReminderLoadResult(
                rows = emptyList(),
                emptyMessage = "当前车辆还没有保养记录，点击右下角“+”开始新增。",
                canAddRecord = true,
                carDisplayName = targetCar.brand,
            )
        }

        val sortedItemOptions = MaintenanceItemConfig.sortItemOptionsByDefaultOrder(
            options = dao.listItemOptionsByCarId(targetCar.id),
            defaultOrderByKey = MaintenanceItemConfig.modelConfig(targetCar.brand, targetCar.modelName).defaultOrderByKey,
            catalogKeySelector = { it.catalogKey },
        )
        val latestIndex = ReminderRules.buildLatestRecordIndex(
            records = targetCarRecords.map { record ->
                ReminderRecordSnapshot(
                    id = record.id,
                    carId = record.carId,
                    date = fromEpochSeconds(record.date),
                    mileage = record.mileage,
                    itemIDsRaw = record.itemIDsRaw,
                )
            },
        )

        ReminderLoadResult(
            rows = ReminderRules.buildRowsForCar(
                car = ReminderCarSnapshot(
                    id = targetCar.id,
                    mileage = targetCar.mileage,
                    purchaseDate = fromEpochSeconds(targetCar.purchaseDate),
                    brand = targetCar.brand,
                    modelName = targetCar.modelName,
                ),
                options = sortedItemOptions.map { option ->
                    ReminderItemOptionSnapshot(
                        id = option.id,
                        name = option.name,
                        catalogKey = option.catalogKey,
                        remindByMileage = option.remindByMileage,
                        mileageInterval = option.mileageInterval,
                        remindByTime = option.remindByTime,
                        monthInterval = option.monthInterval,
                        warningStartPercent = option.warningStartPercent,
                        dangerStartPercent = option.dangerStartPercent,
                    )
                },
                latestRecordIndex = latestIndex,
                now = now,
            ),
            canAddRecord = true,
            carDisplayName = targetCar.brand,
        )
    }.fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Failure(RoomRepositoryErrorMapper.map(it)) },
    )

    private fun fromEpochSeconds(epochSeconds: Long): LocalDate =
        Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()
}
