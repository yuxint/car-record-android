package com.tx.carrecord.feature.addcar.domain

import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig

object CarManagementRules {
    private const val MODEL_KEY_SEPARATOR: Char = '|'

    fun modelKey(brand: String, modelName: String): String {
        val normalizedBrand = normalizeBrand(brand)
        val normalizedModel = normalizeModel(modelName)
        return "$normalizedBrand$MODEL_KEY_SEPARATOR$normalizedModel"
    }

    fun normalizeBrand(brand: String): String = brand.trim()

    fun normalizeModel(modelName: String): String = modelName.trim()

    fun planUpsertCar(
        input: CarUpsertInput,
        existingCars: List<CarProfileSnapshot>,
    ): CarUpsertDecision {
        val normalizedBrand = normalizeBrand(input.brand)
        val normalizedModelName = normalizeModel(input.modelName)
        val normalizedModelKey = modelKey(normalizedBrand, normalizedModelName)

        val conflict = existingCars.firstOrNull { car ->
            if (car.id == input.editingCarId) return@firstOrNull false
            modelKey(car.brand, car.modelName) == normalizedModelKey
        }
        if (conflict != null) {
            return CarUpsertDecision.DuplicateModelConflict(
                conflictCarId = conflict.id,
                conflictBrand = conflict.brand,
                conflictModelName = conflict.modelName,
            )
        }

        return CarUpsertDecision.Success(
            plan = CarUpsertPlan(
                normalizedBrand = normalizedBrand,
                normalizedModelName = normalizedModelName,
                normalizedModelKey = normalizedModelKey,
                normalizedDisabledItemIDsRaw = MaintenanceItemConfig.normalizeItemIDsRaw(input.disabledItemIDsRaw),
            ),
        )
    }

    fun resolveAppliedCar(
        rawAppliedCarId: String?,
        cars: List<CarProfileSnapshot>,
    ): AppliedCarResolution {
        val normalizedRaw = rawAppliedCarId?.trim().orEmpty()
        val resolvedCarId = if (normalizedRaw.isNotEmpty() && cars.any { it.id == normalizedRaw }) {
            normalizedRaw
        } else {
            cars.firstOrNull()?.id
        }
        return AppliedCarResolution(
            resolvedCarId = resolvedCarId,
            normalizedRawAppliedCarId = resolvedCarId.orEmpty(),
        )
    }

    fun planDeleteCar(
        deletingCarId: String,
        cars: List<CarProfileSnapshot>,
        rawAppliedCarId: String?,
    ): CarDeletionDecision {
        val targetCarExists = cars.any { it.id == deletingCarId }
        if (!targetCarExists) {
            return CarDeletionDecision.CarNotFound(requestedCarId = deletingCarId)
        }

        val remainingCars = cars.filter { it.id != deletingCarId }
        val appliedCarResolution = resolveAppliedCar(
            rawAppliedCarId = rawAppliedCarId,
            cars = remainingCars,
        )
        return CarDeletionDecision.Success(
            plan = CarDeletionPlan(
                deletedCarId = deletingCarId,
                remainingCarIds = remainingCars.map { it.id },
                shouldDeleteMaintenanceRecords = true,
                shouldDeleteOwnerScopedItemOptions = true,
                nextAppliedCarId = appliedCarResolution.resolvedCarId,
                normalizedRawAppliedCarId = appliedCarResolution.normalizedRawAppliedCarId,
            ),
        )
    }

    fun planDisabledItemIsolationUpdate(
        targetCarId: String,
        targetDisabledItemIDsRaw: String,
        cars: List<CarProfileSnapshot>,
    ): DisabledItemIsolationDecision {
        if (cars.none { it.id == targetCarId }) {
            return DisabledItemIsolationDecision.CarNotFound(requestedCarId = targetCarId)
        }

        val disabledRawByCar = cars.associate { car ->
            val raw = if (car.id == targetCarId) {
                targetDisabledItemIDsRaw
            } else {
                car.disabledItemIDsRaw
            }
            car.id to MaintenanceItemConfig.normalizeItemIDsRaw(raw)
        }
        return DisabledItemIsolationDecision.Success(
            plan = DisabledItemIsolationPlan(disabledItemIDsRawByCarId = disabledRawByCar),
        )
    }
}
