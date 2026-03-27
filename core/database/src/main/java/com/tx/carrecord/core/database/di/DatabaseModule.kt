package com.tx.carrecord.core.database.di

import android.content.Context
import androidx.room.Room
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.room.CarRecordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val CAR_RECORD_DATABASE_NAME: String = "car_record.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCarRecordDatabase(
        @ApplicationContext context: Context,
    ): CarRecordDatabase = Room.databaseBuilder(
        context = context,
        klass = CarRecordDatabase::class.java,
        name = CAR_RECORD_DATABASE_NAME,
    ).build()

    @Provides
    fun provideCarRecordDao(database: CarRecordDatabase): CarRecordDao = database.carRecordDao()
}
