package com.tx.carrecord.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tx.carrecord.core.datastore.AppDateContext
import com.tx.carrecord.core.datastore.AppNavigationContext
import com.tx.carrecord.core.datastore.AppliedCarContext
import com.tx.carrecord.core.datastore.DatastoreAppDateContext
import com.tx.carrecord.core.datastore.DatastoreAppNavigationContext
import com.tx.carrecord.core.datastore.DatastoreAppliedCarContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val RUNTIME_CONTEXT_DATASTORE_NAME: String = "runtime_context_store"

private val Context.runtimeContextDataStore: DataStore<Preferences> by preferencesDataStore(
    name = RUNTIME_CONTEXT_DATASTORE_NAME,
)

@Module
@InstallIn(SingletonComponent::class)
object RuntimeContextDatastoreProviderModule {
    @Provides
    @Singleton
    fun provideRuntimeContextDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.runtimeContextDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeContextBindingModule {
    @Binds
    @Singleton
    abstract fun bindAppDateContext(impl: DatastoreAppDateContext): AppDateContext

    @Binds
    @Singleton
    abstract fun bindAppliedCarContext(impl: DatastoreAppliedCarContext): AppliedCarContext

    @Binds
    @Singleton
    abstract fun bindAppNavigationContext(impl: DatastoreAppNavigationContext): AppNavigationContext
}
