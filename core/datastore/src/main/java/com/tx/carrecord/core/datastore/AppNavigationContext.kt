package com.tx.carrecord.core.datastore

import kotlinx.coroutines.flow.Flow

enum class RootTabRoute(val rawValue: String) {
    REMINDER("reminder"),
    RECORDS("records"),
    MY("my"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): RootTabRoute? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

data class RootTabNavigationRequest(
    val route: RootTabRoute,
    val nonce: String,
)

interface AppNavigationContext {
    val navigationRequestFlow: Flow<RootTabNavigationRequest?>

    suspend fun requestNavigation(to: RootTabRoute)

    suspend fun clearNavigationRequest()
}
