package com.tx.carrecord.core.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tx.carrecord.core.database.dao.CarRecordDao
import com.tx.carrecord.core.database.model.CarEntity
import com.tx.carrecord.core.database.model.MaintenanceItemOptionEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordItemEntity

@Database(
    entities = [
        CarEntity::class,
        MaintenanceRecordEntity::class,
        MaintenanceRecordItemEntity::class,
        MaintenanceItemOptionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CarRecordDatabase : RoomDatabase() {
    abstract fun carRecordDao(): CarRecordDao
}
