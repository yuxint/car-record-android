package com.tx.carrecord.feature.records.data.di

import com.tx.carrecord.feature.records.data.RecordRepository
import com.tx.carrecord.feature.records.data.RoomRecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRecordRepository(impl: RoomRecordRepository): RecordRepository
}
