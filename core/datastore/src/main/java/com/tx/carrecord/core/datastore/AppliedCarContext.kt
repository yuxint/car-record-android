package com.tx.carrecord.core.datastore

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AppliedCarContext {
    val rawAppliedCarIdFlow: Flow<String>

    val appliedCarIdFlow: Flow<UUID?>

    suspend fun setRawAppliedCarId(rawId: String)

    suspend fun setAppliedCarId(carId: UUID?)

    fun decodeCarId(rawId: String): UUID?

    fun encodeCarId(carId: UUID?): String

    fun resolveAppliedCarId(rawId: String, availableCarIds: List<UUID>): UUID?

    fun normalizedRawId(rawId: String, availableCarIds: List<UUID>): String

    suspend fun normalizeAndPersist(availableCarIds: List<UUID>): String
}
