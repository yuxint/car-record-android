package com.tx.carrecord.feature.records.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object RecordsDomainRules {
    private const val ITEM_ID_SEPARATOR: Char = '|'
    private val cycleDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun cycleKey(carId: String, date: LocalDate): String = "$carId|${date.format(cycleDateFormatter)}"

    fun cycleItemKey(cycleKey: String, itemId: String): String = "$cycleKey|$itemId"

    fun uniqueItemIDsPreservingOrder(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        val seen = mutableSetOf<String>()
        val unique = mutableListOf<String>()
        for (itemId in raw.split(ITEM_ID_SEPARATOR).map { it.trim() }) {
            if (itemId.isEmpty()) continue
            if (seen.add(itemId)) {
                unique += itemId
            }
        }
        return unique
    }

    fun joinItemIDs(itemIds: List<String>): String = itemIds.joinToString(separator = ITEM_ID_SEPARATOR.toString())

    fun normalizeItemIDsRaw(raw: String): String = joinItemIDs(uniqueItemIDsPreservingOrder(raw))

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
