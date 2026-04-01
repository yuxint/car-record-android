package com.tx.carrecord.core.datastore

import kotlinx.coroutines.flow.Flow

interface DebugModeContext {
    val isDebugModeEnabledFlow: Flow<Boolean>

    suspend fun setDebugModeEnabled(enabled: Boolean)
}
