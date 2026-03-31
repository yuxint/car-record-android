package com.tx.carrecord.core.datastore

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class DatastoreMaintenanceDataChangeContext @Inject constructor() : MaintenanceDataChangeContext {
    private val changes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val changesFlow: Flow<Unit> = changes.asSharedFlow()

    override fun notifyChanged() {
        changes.tryEmit(Unit)
    }
}
