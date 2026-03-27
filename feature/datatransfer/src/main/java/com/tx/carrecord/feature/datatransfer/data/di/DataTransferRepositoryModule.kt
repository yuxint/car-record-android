package com.tx.carrecord.feature.datatransfer.data.di

import com.tx.carrecord.feature.datatransfer.data.BackupRepository
import com.tx.carrecord.feature.datatransfer.data.RoomBackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataTransferRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository
}
