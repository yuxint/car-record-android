package com.tx.carrecord.feature.addcar.domain

import java.time.LocalDate

data class CarProfileSnapshot(
    val id: String,
    val brand: String,
    val modelName: String,
    val mileage: Int,
    val purchaseDate: LocalDate,
    val disabledItemIDsRaw: String,
)

data class CarUpsertInput(
    val editingCarId: String?,
    val brand: String,
    val modelName: String,
    val disabledItemIDsRaw: String,
)

data class CarUpsertPlan(
    val normalizedBrand: String,
    val normalizedModelName: String,
    val normalizedModelKey: String,
    val normalizedDisabledItemIDsRaw: String,
)

sealed interface CarUpsertDecision {
    data class Success(val plan: CarUpsertPlan) : CarUpsertDecision

    data class DuplicateModelConflict(
        val conflictCarId: String,
        val conflictBrand: String,
        val conflictModelName: String,
    ) : CarUpsertDecision
}

data class AppliedCarResolution(
    val resolvedCarId: String?,
    val normalizedRawAppliedCarId: String,
)

data class CarDeletionPlan(
    val deletedCarId: String,
    val remainingCarIds: List<String>,
    val shouldDeleteMaintenanceRecords: Boolean,
    val shouldDeleteOwnerScopedItemOptions: Boolean,
    val nextAppliedCarId: String?,
    val normalizedRawAppliedCarId: String,
)

sealed interface CarDeletionDecision {
    data class Success(val plan: CarDeletionPlan) : CarDeletionDecision

    data class CarNotFound(val requestedCarId: String) : CarDeletionDecision
}

data class DisabledItemIsolationPlan(
    val disabledItemIDsRawByCarId: Map<String, String>,
)

sealed interface DisabledItemIsolationDecision {
    data class Success(val plan: DisabledItemIsolationPlan) : DisabledItemIsolationDecision

    data class CarNotFound(val requestedCarId: String) : DisabledItemIsolationDecision
}
