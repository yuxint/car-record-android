package com.tx.carrecord.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tx.carrecord.core.database.model.CarEntity
import com.tx.carrecord.core.database.model.MaintenanceItemOptionEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordEntity
import com.tx.carrecord.core.database.model.MaintenanceRecordItemEntity

@Dao
interface CarRecordDao {
    @Query("SELECT * FROM cars ORDER BY purchase_date ASC, id ASC")
    suspend fun listCars(): List<CarEntity>

    @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1")
    suspend fun findCarById(carId: String): CarEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCar(entity: CarEntity)

    @Query(
        """
        UPDATE cars
        SET brand = :brand,
            model_name = :modelName,
            mileage = :mileage,
            purchase_date = :purchaseDate,
            disabled_item_ids_raw = :disabledItemIDsRaw
        WHERE id = :carId
        """,
    )
    suspend fun updateCarById(
        carId: String,
        brand: String,
        modelName: String,
        mileage: Int,
        purchaseDate: Long,
        disabledItemIDsRaw: String,
    ): Int

    @Query("UPDATE cars SET mileage = :mileage WHERE id = :carId")
    suspend fun updateCarMileage(carId: String, mileage: Int): Int

    @Query("UPDATE cars SET disabled_item_ids_raw = :disabledItemIDsRaw WHERE id = :carId")
    suspend fun updateCarDisabledItemIDsRaw(carId: String, disabledItemIDsRaw: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(entity: MaintenanceRecordEntity)

    @Query("SELECT * FROM maintenance_records ORDER BY date ASC, mileage ASC, id ASC")
    suspend fun listRecords(): List<MaintenanceRecordEntity>

    @Query("SELECT * FROM maintenance_records WHERE car_id = :carId ORDER BY date ASC, mileage ASC, id ASC")
    suspend fun listRecordsByCarId(carId: String): List<MaintenanceRecordEntity>

    @Query("SELECT * FROM maintenance_records WHERE id = :recordId LIMIT 1")
    suspend fun findRecordById(recordId: String): MaintenanceRecordEntity?

    @Query(
        """
        UPDATE maintenance_records
        SET car_id = :carId,
            date = :date,
            item_ids_raw = :itemIDsRaw,
            cost = :cost,
            mileage = :mileage,
            note = :note,
            cycle_key = :cycleKey
        WHERE id = :recordId
        """,
    )
    suspend fun updateRecordById(
        recordId: String,
        carId: String,
        date: Long,
        itemIDsRaw: String,
        cost: Double,
        mileage: Int,
        note: String,
        cycleKey: String,
    ): Int

    @Query("DELETE FROM maintenance_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: String): Int

    @Query("DELETE FROM maintenance_record_items WHERE record_id = :recordId")
    suspend fun deleteRecordItemsByRecordId(recordId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecordItems(entities: List<MaintenanceRecordItemEntity>)

    @Query(
        """
        SELECT * FROM maintenance_item_options
        WHERE owner_car_id = :carId OR owner_car_id IS NULL
        ORDER BY is_default DESC, created_at ASC, id ASC
        """,
    )
    suspend fun listItemOptionsByCarId(carId: String): List<MaintenanceItemOptionEntity>

    @Query("SELECT * FROM maintenance_item_options ORDER BY created_at ASC, id ASC")
    suspend fun listItemOptions(): List<MaintenanceItemOptionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemOptions(entities: List<MaintenanceItemOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItemOption(entity: MaintenanceItemOptionEntity)

    @Query(
        """
        UPDATE maintenance_item_options
        SET name = :name,
            is_default = :isDefault,
            catalog_key = :catalogKey,
            remind_by_mileage = :remindByMileage,
            mileage_interval = :mileageInterval,
            remind_by_time = :remindByTime,
            month_interval = :monthInterval,
            warning_start_percent = :warningStartPercent,
            danger_start_percent = :dangerStartPercent
        WHERE id = :itemId
        """,
    )
    suspend fun updateItemOptionById(
        itemId: String,
        name: String,
        isDefault: Boolean,
        catalogKey: String?,
        remindByMileage: Boolean,
        mileageInterval: Int,
        remindByTime: Boolean,
        monthInterval: Int,
        warningStartPercent: Int,
        dangerStartPercent: Int,
    ): Int

    @Query("DELETE FROM maintenance_item_options WHERE id = :itemId")
    suspend fun deleteItemOptionById(itemId: String): Int

    @Transaction
    suspend fun replaceRecordItems(
        recordId: String,
        entities: List<MaintenanceRecordItemEntity>,
    ) {
        deleteRecordItemsByRecordId(recordId = recordId)
        if (entities.isNotEmpty()) {
            insertRecordItems(entities = entities)
        }
    }

    @Query("DELETE FROM cars WHERE id = :carId")
    suspend fun deleteCarById(carId: String)

    @Query("DELETE FROM maintenance_record_items")
    suspend fun clearRecordItems()

    @Query("DELETE FROM maintenance_records")
    suspend fun clearRecords()

    @Query("DELETE FROM maintenance_item_options")
    suspend fun clearItemOptions()

    @Query("DELETE FROM cars")
    suspend fun clearCars()
}
