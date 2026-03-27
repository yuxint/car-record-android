package com.tx.carrecord.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatastoreAppliedCarContext @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppliedCarContext {
    override val rawAppliedCarIdFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[RuntimeContextPreferenceKeys.appliedCarId].orEmpty()
    }

    override val appliedCarIdFlow: Flow<UUID?> = rawAppliedCarIdFlow.map(::decodeCarId)

    override suspend fun setRawAppliedCarId(rawId: String) {
        val normalized = rawId.trim()
        dataStore.edit { prefs ->
            prefs[RuntimeContextPreferenceKeys.appliedCarId] = normalized
        }
    }

    override suspend fun setAppliedCarId(carId: UUID?) {
        setRawAppliedCarId(encodeCarId(carId))
    }

    override fun decodeCarId(rawId: String): UUID? {
        val normalized = rawId.trim()
        if (normalized.isEmpty()) return null
        return runCatching { UUID.fromString(normalized) }.getOrNull()
    }

    override fun encodeCarId(carId: UUID?): String = carId?.toString().orEmpty()

    override fun resolveAppliedCarId(rawId: String, availableCarIds: List<UUID>): UUID? {
        val preferredId = decodeCarId(rawId)
        if (preferredId != null && availableCarIds.contains(preferredId)) {
            return preferredId
        }
        return availableCarIds.firstOrNull()
    }

    override fun normalizedRawId(rawId: String, availableCarIds: List<UUID>): String {
        val resolvedId = resolveAppliedCarId(rawId = rawId, availableCarIds = availableCarIds)
        return encodeCarId(resolvedId)
    }

    override suspend fun normalizeAndPersist(availableCarIds: List<UUID>): String {
        val currentRawId = rawAppliedCarIdFlow.first()
        val normalizedRawId = normalizedRawId(
            rawId = currentRawId,
            availableCarIds = availableCarIds,
        )
        if (currentRawId != normalizedRawId) {
            setRawAppliedCarId(normalizedRawId)
        }
        return normalizedRawId
    }
}
