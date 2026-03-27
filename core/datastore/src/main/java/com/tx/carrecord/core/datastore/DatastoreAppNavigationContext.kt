package com.tx.carrecord.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatastoreAppNavigationContext @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppNavigationContext {
    override val navigationRequestFlow: Flow<RootTabNavigationRequest?> = dataStore.data.map { prefs ->
        val targetRawValue = prefs[RuntimeContextPreferenceKeys.rootTabNavigationTarget].orEmpty()
        val nonce = prefs[RuntimeContextPreferenceKeys.rootTabNavigationNonce].orEmpty()
        val route = RootTabRoute.fromRawValue(targetRawValue) ?: return@map null
        if (nonce.isBlank()) return@map null
        RootTabNavigationRequest(route = route, nonce = nonce)
    }

    override suspend fun requestNavigation(to: RootTabRoute) {
        dataStore.edit { prefs ->
            prefs[RuntimeContextPreferenceKeys.rootTabNavigationTarget] = to.rawValue
            prefs[RuntimeContextPreferenceKeys.rootTabNavigationNonce] = UUID.randomUUID().toString()
        }
    }

    override suspend fun clearNavigationRequest() {
        dataStore.edit { prefs ->
            prefs.remove(RuntimeContextPreferenceKeys.rootTabNavigationTarget)
            prefs.remove(RuntimeContextPreferenceKeys.rootTabNavigationNonce)
        }
    }
}
