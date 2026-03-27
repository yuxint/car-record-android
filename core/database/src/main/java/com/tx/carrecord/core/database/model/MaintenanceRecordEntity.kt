package com.tx.carrecord.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["car_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["car_id"]),
        Index(value = ["cycle_key"], unique = true),
    ],
)
data class MaintenanceRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "car_id")
    val carId: String,
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "item_ids_raw")
    val itemIDsRaw: String,
    @ColumnInfo(name = "cost")
    val cost: Double,
    @ColumnInfo(name = "mileage")
    val mileage: Int,
    @ColumnInfo(name = "note")
    val note: String,
    @ColumnInfo(name = "cycle_key")
    val cycleKey: String,
)
