package com.tx.carrecord.feature.reminder.domain

import java.time.LocalDate
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig.ProgressColorLevel
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ReminderRulesTest {
    @Test
    fun buildReminderRow_时间和里程同时存在时采用更高进度() {
        val row = ReminderRules.buildReminderRow(
            car = ReminderCarSnapshot(
                id = "car-1",
                mileage = 12_000,
                purchaseDate = LocalDate.parse("2025-01-01"),
            ),
            option = ReminderItemOptionSnapshot(
                id = "item-1",
                name = "机油",
                remindByMileage = true,
                mileageInterval = 5_000,
                remindByTime = true,
                monthInterval = 6,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
            itemLatestRecord = ReminderRecordSnapshot(
                id = "record-1",
                carId = "car-1",
                date = LocalDate.parse("2025-04-01"),
                mileage = 10_000,
                itemIDsRaw = "item-1",
            ),
            now = LocalDate.parse("2025-10-01"),
        )

        assertEquals(1.0, row.rawProgress)
        assertEquals(180.0, row.duePriority)
        assertEquals("100%", row.progressText)
        assertEquals(ProgressColorLevel.WARNING, row.progressColorLevel)
        assertContains(row.detailTexts, "按里程提醒：距离下次约3000公里")
        assertContains(row.detailTexts, "按时间提醒：今日到期")
    }

    @Test
    fun buildReminderRow_无历史记录时使用购车日期和零里程基线() {
        val row = ReminderRules.buildReminderRow(
            car = ReminderCarSnapshot(
                id = "car-1",
                mileage = 1_200,
                purchaseDate = LocalDate.parse("2025-01-01"),
            ),
            option = ReminderItemOptionSnapshot(
                id = "item-1",
                name = "机油",
                remindByMileage = true,
                mileageInterval = 5_000,
                remindByTime = false,
                monthInterval = 0,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
            itemLatestRecord = null,
            now = LocalDate.parse("2025-02-01"),
        )

        assertEquals(0.24, row.rawProgress)
        assertEquals(5_000.0, row.duePriority)
        assertEquals("24%", row.progressText)
        assertEquals(ProgressColorLevel.NORMAL, row.progressColorLevel)
        assertContains(row.detailTexts, "按里程提醒：距离下次约3800公里")
    }

    @Test
    fun buildReminderRow_间隔无效时回退未设置提醒规则() {
        val row = ReminderRules.buildReminderRow(
            car = ReminderCarSnapshot(
                id = "car-1",
                mileage = 8_000,
                purchaseDate = LocalDate.parse("2025-01-01"),
            ),
            option = ReminderItemOptionSnapshot(
                id = "item-1",
                name = "空滤",
                remindByMileage = true,
                mileageInterval = 0,
                remindByTime = true,
                monthInterval = 0,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
            itemLatestRecord = null,
            now = LocalDate.parse("2025-02-01"),
        )

        assertEquals(0.0, row.rawProgress)
        assertEquals(Double.MAX_VALUE, row.duePriority)
        assertEquals("0%", row.progressText)
        assertEquals(listOf("未设置提醒规则"), row.detailTexts)
    }

    @Test
    fun buildReminderRow_超过百分百时文本不截断但进度条截断() {
        val row = ReminderRules.buildReminderRow(
            car = ReminderCarSnapshot(
                id = "car-1",
                mileage = 2_500,
                purchaseDate = LocalDate.parse("2025-01-01"),
            ),
            option = ReminderItemOptionSnapshot(
                id = "item-1",
                name = "机油",
                remindByMileage = true,
                mileageInterval = 1_000,
                remindByTime = false,
                monthInterval = 0,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
            itemLatestRecord = null,
            now = LocalDate.parse("2025-02-01"),
        )

        assertEquals(2.5, row.rawProgress)
        assertEquals(1.0, row.displayProgress)
        assertEquals("250%", row.progressText)
        assertEquals(ProgressColorLevel.DANGER, row.progressColorLevel)
    }

    @Test
    fun buildLatestRecordIndex_同项目取最近记录_同日取更高里程() {
        val index = ReminderRules.buildLatestRecordIndex(
            records = listOf(
                ReminderRecordSnapshot(
                    id = "record-1",
                    carId = "car-1",
                    date = LocalDate.parse("2025-06-01"),
                    mileage = 10_000,
                    itemIDsRaw = "item-1|item-2",
                ),
                ReminderRecordSnapshot(
                    id = "record-2",
                    carId = "car-1",
                    date = LocalDate.parse("2025-06-01"),
                    mileage = 10_500,
                    itemIDsRaw = "item-1",
                ),
                ReminderRecordSnapshot(
                    id = "record-3",
                    carId = "car-1",
                    date = LocalDate.parse("2025-07-01"),
                    mileage = 11_000,
                    itemIDsRaw = "item-2",
                ),
            ),
        )

        assertEquals("record-2", index[ReminderRules.latestLogKey("car-1", "item-1")]?.id)
        assertEquals("record-3", index[ReminderRules.latestLogKey("car-1", "item-2")]?.id)
    }

    @Test
    fun buildRowsForCar_按进度降序再按优先级再按项目名排序() {
        val car = ReminderCarSnapshot(
            id = "car-1",
            mileage = 8_000,
            purchaseDate = LocalDate.parse("2025-01-01"),
        )
        val rows = ReminderRules.buildRowsForCar(
            car = car,
            options = listOf(
                ReminderItemOptionSnapshot(
                    id = "item-a",
                    name = "变速箱油",
                    remindByMileage = true,
                    mileageInterval = 16_000,
                    remindByTime = false,
                    monthInterval = 0,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                ),
                ReminderItemOptionSnapshot(
                    id = "item-b",
                    name = "机油",
                    remindByMileage = true,
                    mileageInterval = 16_000,
                    remindByTime = false,
                    monthInterval = 0,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                ),
                ReminderItemOptionSnapshot(
                    id = "item-c",
                    name = "空滤",
                    remindByMileage = true,
                    mileageInterval = 10_000,
                    remindByTime = false,
                    monthInterval = 0,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                ),
            ),
            latestRecordIndex = emptyMap(),
            now = LocalDate.parse("2025-02-01"),
        )

        assertEquals(listOf("空滤", "变速箱油", "机油"), rows.map { it.itemName })
    }
}
