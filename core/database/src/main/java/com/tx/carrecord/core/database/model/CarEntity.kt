package com.tx.carrecord.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "brand")
    val brand: String,
    @ColumnInfo(name = "model_name")
    val modelName: String,
    @ColumnInfo(name = "mileage")
    val mileage: Int,
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Long,
    @ColumnInfo(name = "disabled_item_ids_raw")
    val disabledItemIDsRaw: String,
)
