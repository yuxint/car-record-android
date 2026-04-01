package com.tx.carrecord.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatastoreDebugModeContext @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : DebugModeContext {
    override val isDebugModeEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[RuntimeContextPreferenceKeys.appDebugModeEnabled] ?: false
    }

    override suspend fun setDebugModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[RuntimeContextPreferenceKeys.appDebugModeEnabled] = enabled
        }
    }
}
