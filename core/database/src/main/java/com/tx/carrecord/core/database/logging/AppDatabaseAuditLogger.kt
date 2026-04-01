package com.tx.carrecord.core.database.logging

import com.tx.carrecord.core.datastore.logging.AppLogger
import com.tx.carrecord.core.database.model.CarEntity
import com.tx.carrecord.core.database.model.MaintenanceItemOptionEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordItemEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class AppDatabaseAuditLogger @Inject constructor(
    private val appLogger: AppLogger,
) {
    suspend fun logInsert(entity: String, data: Map<String, Any?>) {
        appLogger.info("数据库新增", payload = makePayload("insert", entity, data))
    }

    suspend fun logDelete(entity: String, data: Map<String, Any?>) {
        appLogger.info("数据库删除", payload = makePayload("delete", entity, data))
    }

    suspend fun logUpdate(entity: String, before: Map<String, Any?>, after: Map<String, Any?>) {
        appLogger.info(
            "数据库修改",
            payload = makePayload(
                action = "update",
                entity = entity,
                data = mapOf(
                    "before" to before,
                    "after" to after,
                ),
            ),
        )
    }

    private fun makePayload(
        action: String,
        entity: String,
        data: Map<String, Any?>,
    ): String {
        return JSONObject(
            mapOf(
                "action" to action,
                "entity" to entity,
                "data" to data,
            ),
        ).toString()
    }
}

object AppDatabaseSnapshot {
    fun car(entity: CarEntity): Map<String, Any?> = mapOf(
        "id" to entity.id,
        "brand" to entity.brand,
        "modelName" to entity.modelName,
        "mileage" to entity.mileage,
        "purchaseDate" to dateString(entity.purchaseDate),
        "disabledItemIDsRaw" to entity.disabledItemIDsRaw,
    )

    fun maintenanceItemOption(entity: MaintenanceItemOptionEntity): Map<String, Any?> = mapOf(
        "id" to entity.id,
        "name" to entity.name,
        "ownerCarID" to entity.ownerCarID,
        "isDefault" to entity.isDefault,
        "catalogKey" to entity.catalogKey,
        "remindByMileage" to entity.remindByMileage,
        "mileageInterval" to entity.mileageInterval,
        "remindByTime" to entity.remindByTime,
        "monthInterval" to entity.monthInterval,
        "warningStartPercent" to entity.warningStartPercent,
        "dangerStartPercent" to entity.dangerStartPercent,
        "createdAt" to dateString(entity.createdAt),
    )

    fun maintenanceRecord(entity: MaintenanceRecordEntity): Map<String, Any?> = mapOf(
        "id" to entity.id,
        "carID" to entity.carId,
        "cycleKey" to entity.cycleKey,
        "date" to dateString(entity.date),
        "itemIDsRaw" to entity.itemIDsRaw,
        "cost" to entity.cost,
        "mileage" to entity.mileage,
        "note" to entity.note,
    )

    fun maintenanceRecordItem(entity: MaintenanceRecordItemEntity): Map<String, Any?> = mapOf(
        "id" to entity.id,
        "recordID" to entity.recordId,
        "itemID" to entity.itemId,
        "cycleItemKey" to entity.cycleItemKey,
        "createdAt" to dateString(entity.createdAt),
    )

    private fun dateString(epochSeconds: Long): String {
        return Instant.ofEpochSecond(epochSeconds).toString()
    }
}
