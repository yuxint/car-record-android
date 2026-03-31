package com.tx.carrecord.feature.records.domain

import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object RecordsDomainRules {
    private val cycleDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun cycleKey(carId: String, date: LocalDate): String = "$carId|${date.format(cycleDateFormatter)}"

    fun cycleItemKey(cycleKey: String, itemId: String): String = "$cycleKey|$itemId"

    fun uniqueItemIDsPreservingOrder(raw: String): List<String> {
        return MaintenanceItemConfig.parseItemIDs(raw)
    }

    fun joinItemIDs(itemIds: List<String>): String = MaintenanceItemConfig.joinItemIDs(itemIds)

    fun normalizeItemIDsRaw(raw: String): String = MaintenanceItemConfig.normalizeItemIDsRaw(raw)

    fun itemIDsAfterRemoval(raw: String, removingItemId: String): List<String> {
        return uniqueItemIDsPreservingOrder(raw).filterNot { it == removingItemId }
    }

    fun shouldDeleteWholeRecordAfterRemovingItem(raw: String, removingItemId: String): Boolean {
        return uniqueItemIDsPreservingOrder(raw).size <= 1 || uniqueItemIDsPreservingOrder(raw).none { it == removingItemId }
    }

    fun planSave(
        input: RecordSaveInput,
        existingRecords: List<ExistingRecordSnapshot>,
        carCurrentMileage: Int,
        createdAtEpochSeconds: Long,
        relationIdProvider: () -> String = { UUID.randomUUID().toString() },
    ): RecordSaveDecision {
        val normalizedDate = input.dateTime.toLocalDate()
        val normalizedCycleKey = cycleKey(carId = input.carId, date = normalizedDate)

        val conflict = existingRecords.firstOrNull { existing ->
            if (existing.id == input.recordId) return@firstOrNull false
            cycleKey(carId = existing.carId, date = existing.dateTime.toLocalDate()) == normalizedCycleKey
        }
        if (conflict != null) {
            return RecordSaveDecision.DuplicateCycleConflict(
                conflictRecordId = conflict.id,
                conflictDate = conflict.dateTime.toLocalDate(),
            )
        }

        val uniqueItemIDs = uniqueItemIDsPreservingOrder(input.itemIDsRaw)
        val normalizedItemIDsRaw = joinItemIDs(uniqueItemIDs)
        val targetRecordId = input.recordId ?: relationIdProvider()

        val relationDrafts = uniqueItemIDs.map { itemId ->
            RecordItemRelationDraft(
                id = relationIdProvider(),
                recordId = targetRecordId,
                itemId = itemId,
                cycleItemKey = cycleItemKey(cycleKey = normalizedCycleKey, itemId = itemId),
                createdAtEpochSeconds = createdAtEpochSeconds,
            )
        }

        return RecordSaveDecision.Success(
            plan = RecordSavePlan(
                targetRecordId = targetRecordId,
                normalizedDate = normalizedDate,
                cycleKey = normalizedCycleKey,
                normalizedItemIDsRaw = normalizedItemIDsRaw,
                relationDrafts = relationDrafts,
                syncedCarMileage = maxOf(carCurrentMileage, input.mileage),
            ),
        )
    }
}
