package com.tx.carrecord.feature.addcar.data.di

import com.tx.carrecord.feature.addcar.data.CarRepository
import com.tx.carrecord.feature.addcar.data.RoomCarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AddCarRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCarRepository(impl: RoomCarRepository): CarRepository
}
