package com.tx.carrecord.feature.addcar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import java.time.LocalDate

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
}
