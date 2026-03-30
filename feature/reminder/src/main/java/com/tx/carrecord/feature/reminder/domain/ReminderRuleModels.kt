package com.tx.carrecord.feature.reminder.domain

import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig.ProgressColorLevel
import java.time.LocalDate

data class ReminderCarSnapshot(
    val id: String,
    val mileage: Int,
    val purchaseDate: LocalDate,
)

data class ReminderItemOptionSnapshot(
    val id: String,
    val name: String,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
    val warningStartPercent: Int,
    val dangerStartPercent: Int,
)

data class ReminderRecordSnapshot(
    val id: String,
    val carId: String,
    val date: LocalDate,
    val mileage: Int,
    val itemIDsRaw: String,
)

data class ReminderRow(
    val id: String,
    val itemName: String,
    val rawProgress: Double,
    val duePriority: Double,
    val displayProgress: Double,
    val progressText: String,
    val detailTexts: List<String>,
    val progressColorLevel: ProgressColorLevel,
)
