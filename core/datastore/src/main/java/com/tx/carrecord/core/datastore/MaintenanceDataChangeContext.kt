package com.tx.carrecord.core.datastore

import kotlinx.coroutines.flow.Flow

interface MaintenanceDataChangeContext {
    val changesFlow: Flow<Unit>

    fun notifyChanged()
}
