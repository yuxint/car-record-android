package com.tx.carrecord.feature.records.domain

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecordsDomainRulesTest {
    @Test
    fun planSave_同日不同时间应归一化为同一cycleKey并重建关系() {
        val idSeed = mutableListOf(
            "record-generated-id",
            "relation-1",
            "relation-2",
        )

        val result = RecordsDomainRules.planSave(
            input = RecordSaveInput(
                recordId = null,
                carId = "car-1",
                dateTime = LocalDateTime.parse("2026-03-01T18:30:45"),
                itemIDsRaw = "item-a|item-b|item-a",
                mileage = 13_000,
            ),
            existingRecords = emptyList(),
            carCurrentMileage = 12_000,
            createdAtEpochSeconds = 1_742_345_678L,
            relationIdProvider = { idSeed.removeAt(0) },
        )

        val success = assertIs<RecordSaveDecision.Success>(result)
        assertEquals("record-generated-id", success.plan.targetRecordId)
        assertEquals(LocalDate.parse("2026-03-01"), success.plan.normalizedDate)
        assertEquals("car-1|2026-03-01", success.plan.cycleKey)
        assertEquals("item-a|item-b", success.plan.normalizedItemIDsRaw)
        assertEquals(13_000, success.plan.syncedCarMileage)

        assertEquals(2, success.plan.relationDrafts.size)
        assertEquals("record-generated-id", success.plan.relationDrafts[0].recordId)
        assertEquals("car-1|2026-03-01|item-a", success.plan.relationDrafts[0].cycleItemKey)
        assertEquals("car-1|2026-03-01|item-b", success.plan.relationDrafts[1].cycleItemKey)
    }

    @Test
    fun planSave_新增遇到同车同日记录返回冲突错误() {
        val result = RecordsDomainRules.planSave(
            input = RecordSaveInput(
                recordId = null,
                carId = "car-1",
                dateTime = LocalDateTime.parse("2026-03-01T09:00:00"),
                itemIDsRaw = "item-a",
                mileage = 10_000,
            ),
            existingRecords = listOf(
                ExistingRecordSnapshot(
                    id = "record-exist",
                    carId = "car-1",
                    dateTime = LocalDateTime.parse("2026-03-01T20:00:00"),
                ),
            ),
            carCurrentMileage = 9_000,
            createdAtEpochSeconds = 1L,
        )

        val conflict = assertIs<RecordSaveDecision.DuplicateCycleConflict>(result)
        assertEquals("record-exist", conflict.conflictRecordId)
        assertEquals(LocalDate.parse("2026-03-01"), conflict.conflictDate)
    }

    @Test
    fun planSave_编辑时应忽略自身记录避免误判冲突() {
        val result = RecordsDomainRules.planSave(
            input = RecordSaveInput(
                recordId = "record-self",
                carId = "car-1",
                dateTime = LocalDateTime.parse("2026-03-01T09:00:00"),
                itemIDsRaw = "item-a|item-b",
                mileage = 8_000,
            ),
            existingRecords = listOf(
                ExistingRecordSnapshot(
                    id = "record-self",
                    carId = "car-1",
                    dateTime = LocalDateTime.parse("2026-03-01T20:00:00"),
                ),
            ),
            carCurrentMileage = 9_000,
            createdAtEpochSeconds = 1L,
            relationIdProvider = { "id-fixed" },
        )

        val success = assertIs<RecordSaveDecision.Success>(result)
        assertEquals("record-self", success.plan.targetRecordId)
        assertEquals("record-self", success.plan.relationDrafts.first().recordId)
        assertEquals(9_000, success.plan.syncedCarMileage)
    }

    @Test
    fun uniqueItemIDsPreservingOrder_应去重并保持原顺序() {
        val unique = RecordsDomainRules.uniqueItemIDsPreservingOrder("item-b|item-a|item-b||item-c|item-a")

        assertEquals(listOf("item-b", "item-a", "item-c"), unique)
        assertEquals("item-b|item-a|item-c", RecordsDomainRules.joinItemIDs(unique))
    }
}
