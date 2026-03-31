package com.tx.carrecord.feature.addcar.domain

import com.tx.carrecord.feature.addcar.data.CarItemOptionSnapshot
import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CarManagementRulesTest {
    @Test
    fun planUpsertCar_新增同品牌同车型应返回冲突() {
        val result = CarManagementRules.planUpsertCar(
            input = CarUpsertInput(
                editingCarId = null,
                brand = " HONDA ",
                modelName = " Civic ",
                disabledItemIDsRaw = "",
            ),
            existingCars = listOf(
                CarProfileSnapshot(
                    id = "car-1",
                    brand = "HONDA",
                    modelName = "Civic",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "",
                ),
            ),
        )

        val conflict = assertIs<CarUpsertDecision.DuplicateModelConflict>(result)
        assertEquals("car-1", conflict.conflictCarId)
    }

    @Test
    fun planUpsertCar_编辑同一辆车不应误判冲突并归一化禁用项() {
        val result = CarManagementRules.planUpsertCar(
            input = CarUpsertInput(
                editingCarId = "car-1",
                brand = " HONDA ",
                modelName = " Civic ",
                disabledItemIDsRaw = "item-a|item-b|item-a||item-c",
            ),
            existingCars = listOf(
                CarProfileSnapshot(
                    id = "car-1",
                    brand = "HONDA",
                    modelName = "Civic",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "item-z",
                ),
            ),
        )

        val success = assertIs<CarUpsertDecision.Success>(result)
        assertEquals("HONDA", success.plan.normalizedBrand)
        assertEquals("Civic", success.plan.normalizedModelName)
        assertEquals("HONDA|Civic", success.plan.normalizedModelKey)
        assertEquals("item-a|item-b|item-c", success.plan.normalizedDisabledItemIDsRaw)
    }

    @Test
    fun resolveAppliedCar_无效已应用车型应回退第一辆车并规范化存储值() {
        val resolved = CarManagementRules.resolveAppliedCar(
            rawAppliedCarId = "missing-car",
            cars = listOf(
                CarProfileSnapshot(
                    id = "car-2",
                    brand = "BYD",
                    modelName = "秦",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "",
                ),
                CarProfileSnapshot(
                    id = "car-3",
                    brand = "Tesla",
                    modelName = "Model 3",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "",
                ),
            ),
        )

        assertEquals("car-2", resolved.resolvedCarId)
        assertEquals("car-2", resolved.normalizedRawAppliedCarId)
    }

    @Test
    fun resolveAppliedCar_无车辆时返回空应用车型() {
        val resolved = CarManagementRules.resolveAppliedCar(
            rawAppliedCarId = "any-id",
            cars = emptyList(),
        )

        assertEquals(null, resolved.resolvedCarId)
        assertEquals("", resolved.normalizedRawAppliedCarId)
    }

    @Test
    fun planDeleteCar_删除当前应用车型时应触发回退并声明级联删除() {
        val result = CarManagementRules.planDeleteCar(
            deletingCarId = "car-1",
            cars = listOf(
                CarProfileSnapshot(
                    id = "car-1",
                    brand = "Honda",
                    modelName = "Civic",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "",
                ),
                CarProfileSnapshot(
                    id = "car-2",
                    brand = "Toyota",
                    modelName = "Corolla",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "",
                ),
            ),
            rawAppliedCarId = "car-1",
        )

        val success = assertIs<CarDeletionDecision.Success>(result)
        assertEquals("car-1", success.plan.deletedCarId)
        assertEquals(listOf("car-2"), success.plan.remainingCarIds)
        assertEquals(true, success.plan.shouldDeleteMaintenanceRecords)
        assertEquals(true, success.plan.shouldDeleteOwnerScopedItemOptions)
        assertEquals("car-2", success.plan.nextAppliedCarId)
        assertEquals("car-2", success.plan.normalizedRawAppliedCarId)
    }

    @Test
    fun planDisabledItemIsolationUpdate_仅更新目标车辆禁用项不影响其他车辆() {
        val result = CarManagementRules.planDisabledItemIsolationUpdate(
            targetCarId = "car-1",
            targetDisabledItemIDsRaw = "item-b|item-c|item-b",
            cars = listOf(
                CarProfileSnapshot(
                    id = "car-1",
                    brand = "Honda",
                    modelName = "Civic",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "item-a",
                ),
                CarProfileSnapshot(
                    id = "car-2",
                    brand = "Toyota",
                    modelName = "Corolla",
                    mileage = 0,
                    purchaseDate = LocalDate.of(2024, 1, 1),
                    disabledItemIDsRaw = "item-x|item-y",
                ),
            ),
        )

        val success = assertIs<DisabledItemIsolationDecision.Success>(result)
        assertEquals("item-b|item-c", success.plan.disabledItemIDsRawByCarId["car-1"])
        assertEquals("item-x|item-y", success.plan.disabledItemIDsRawByCarId["car-2"])
    }

    @Test
    fun sortItemOptionsByDefaultOrder_已知项按车型顺序未知项保持原始顺序() {
        val result = MaintenanceItemConfig.sortItemOptionsByDefaultOrder(
            options = listOf(
                CarItemOptionSnapshot(
                    id = "option-1",
                    name = "未知A",
                    ownerCarId = "car-1",
                    isDefault = false,
                    catalogKey = "custom-a",
                    remindByMileage = true,
                    mileageInterval = 5000,
                    remindByTime = false,
                    monthInterval = 0,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                    createdAtEpochSeconds = 1L,
                ),
                CarItemOptionSnapshot(
                    id = "option-2",
                    name = "机油",
                    ownerCarId = "car-1",
                    isDefault = true,
                    catalogKey = "engine_oil",
                    remindByMileage = true,
                    mileageInterval = 5000,
                    remindByTime = true,
                    monthInterval = 6,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                    createdAtEpochSeconds = 2L,
                ),
                CarItemOptionSnapshot(
                    id = "option-3",
                    name = "未知B",
                    ownerCarId = "car-1",
                    isDefault = false,
                    catalogKey = null,
                    remindByMileage = true,
                    mileageInterval = 5000,
                    remindByTime = false,
                    monthInterval = 0,
                    warningStartPercent = 100,
                    dangerStartPercent = 125,
                    createdAtEpochSeconds = 3L,
                ),
            ),
            defaultOrderByKey = mapOf(
                "engine_oil" to 0,
                "ac_filter" to 1,
            ),
            catalogKeySelector = { it.catalogKey },
        )

        assertEquals(listOf("option-2", "option-1", "option-3"), result.map { it.id })
    }

    @Test
    fun normalizeItemIDsRaw_应去重并清理空白() {
        val normalized = MaintenanceItemConfig.normalizeItemIDsRaw(" item-a | item-b | item-a || item-c ")

        assertEquals("item-a|item-b|item-c", normalized)
    }

    @Test
    fun reminderSummaryText_无规则时显示未设置_有规则时合并摘要() {
        val emptySummary = MaintenanceItemConfig.reminderSummaryText(
            option = CarItemOptionSnapshot(
                id = "option-0",
                name = "测试",
                ownerCarId = "car-1",
                isDefault = true,
                catalogKey = "test",
                remindByMileage = false,
                mileageInterval = 0,
                remindByTime = false,
                monthInterval = 0,
                warningStartPercent = 100,
                dangerStartPercent = 125,
                createdAtEpochSeconds = 0L,
            ),
            remindByMileageSelector = { it.remindByMileage },
            mileageIntervalSelector = { it.mileageInterval },
            remindByTimeSelector = { it.remindByTime },
            monthIntervalSelector = { it.monthInterval },
        )
        val summary = MaintenanceItemConfig.reminderSummaryText(
            option = CarItemOptionSnapshot(
                id = "option-1",
                name = "测试",
                ownerCarId = "car-1",
                isDefault = true,
                catalogKey = "test",
                remindByMileage = true,
                mileageInterval = 20_000,
                remindByTime = true,
                monthInterval = 6,
                warningStartPercent = 100,
                dangerStartPercent = 125,
                createdAtEpochSeconds = 0L,
            ),
            remindByMileageSelector = { it.remindByMileage },
            mileageIntervalSelector = { it.mileageInterval },
            remindByTimeSelector = { it.remindByTime },
            monthIntervalSelector = { it.monthInterval },
        )

        assertEquals("未设置", emptySummary)
        assertEquals("2万公里 / 0.5年", summary)
    }

    @Test
    fun progressColorLevel_阈值应固定为100和125() {
        assertEquals(
            MaintenanceItemConfig.ProgressColorLevel.NORMAL,
            MaintenanceItemConfig.progressColorLevel(
                rawPercent = 99.9,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
        )
        assertEquals(
            MaintenanceItemConfig.ProgressColorLevel.WARNING,
            MaintenanceItemConfig.progressColorLevel(
                rawPercent = 100.0,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
        )
        assertEquals(
            MaintenanceItemConfig.ProgressColorLevel.DANGER,
            MaintenanceItemConfig.progressColorLevel(
                rawPercent = 125.0,
                warningStartPercent = 100,
                dangerStartPercent = 125,
            ),
        )
    }

    @Test
    fun filterDisabledOptions_默认过滤禁用项_可按需保留禁用项() {
        val options = listOf(
            CarItemOptionSnapshot(
                id = "option-1",
                name = "机油",
                ownerCarId = "car-1",
                isDefault = true,
                catalogKey = "engine_oil",
                remindByMileage = true,
                mileageInterval = 5000,
                remindByTime = true,
                monthInterval = 6,
                warningStartPercent = 100,
                dangerStartPercent = 125,
                createdAtEpochSeconds = 0L,
            ),
            CarItemOptionSnapshot(
                id = "option-2",
                name = "空调滤芯",
                ownerCarId = "car-1",
                isDefault = true,
                catalogKey = "ac_filter",
                remindByMileage = true,
                mileageInterval = 20000,
                remindByTime = true,
                monthInterval = 12,
                warningStartPercent = 100,
                dangerStartPercent = 125,
                createdAtEpochSeconds = 0L,
            ),
        )

        val filtered = MaintenanceItemConfig.filterDisabledOptions(
            options = options,
            disabledItemIDsRaw = "option-2",
            includeDisabled = false,
            itemIDSelector = { it.id },
        )

        assertEquals(listOf("option-1"), filtered.map { it.id })
        assertEquals(
            listOf("option-1", "option-2"),
            MaintenanceItemConfig.filterDisabledOptions(
                options = options,
                disabledItemIDsRaw = "option-2",
                includeDisabled = true,
                itemIDSelector = { it.id },
            ).map { it.id },
        )
    }
}
