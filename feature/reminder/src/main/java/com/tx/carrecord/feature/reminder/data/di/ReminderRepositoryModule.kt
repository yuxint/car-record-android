package com.tx.carrecord.feature.reminder.data.di

import com.tx.carrecord.feature.reminder.data.ReminderRepository
import com.tx.carrecord.feature.reminder.data.RoomReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: RoomReminderRepository): ReminderRepository
}
