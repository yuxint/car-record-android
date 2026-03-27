package com.tx.carrecord.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_record_items",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["record_id"]),
        Index(value = ["cycle_item_key"], unique = true),
    ],
)
data class MaintenanceRecordItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "record_id")
    val recordId: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "cycle_item_key")
    val cycleItemKey: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
