package com.tx.carrecord.feature.records.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class RecordSaveInput(
    val recordId: String?,
    val carId: String,
    val dateTime: LocalDateTime,
    val itemIDsRaw: String,
    val mileage: Int,
)

data class ExistingRecordSnapshot(
    val id: String,
    val carId: String,
    val dateTime: LocalDateTime,
)

data class RecordItemRelationDraft(
    val id: String,
    val recordId: String,
    val itemId: String,
    val cycleItemKey: String,
    val createdAtEpochSeconds: Long,
)

data class RecordSavePlan(
    val targetRecordId: String,
    val normalizedDate: LocalDate,
    val cycleKey: String,
    val normalizedItemIDsRaw: String,
    val relationDrafts: List<RecordItemRelationDraft>,
    val syncedCarMileage: Int,
)

sealed interface RecordSaveDecision {
    data class Success(val plan: RecordSavePlan) : RecordSaveDecision

    data class DuplicateCycleConflict(
        val conflictRecordId: String,
        val conflictDate: LocalDate,
    ) : RecordSaveDecision
}
