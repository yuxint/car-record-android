package com.tx.carrecord.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_item_options",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["owner_car_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["owner_car_id"]),
        Index(value = ["catalog_key"]),
    ],
)
data class MaintenanceItemOptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "owner_car_id")
    val ownerCarID: String?,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "catalog_key")
    val catalogKey: String?,
    @ColumnInfo(name = "remind_by_mileage")
    val remindByMileage: Boolean,
    @ColumnInfo(name = "mileage_interval")
    val mileageInterval: Int,
    @ColumnInfo(name = "remind_by_time")
    val remindByTime: Boolean,
    @ColumnInfo(name = "month_interval")
    val monthInterval: Int,
    @ColumnInfo(name = "warning_start_percent")
    val warningStartPercent: Int,
    @ColumnInfo(name = "danger_start_percent")
    val dangerStartPercent: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
