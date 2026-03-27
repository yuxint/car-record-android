package com.tx.carrecord.feature.reminder.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object ReminderRules {
    private const val ITEM_ID_SEPARATOR: Char = '|'

    fun latestLogKey(carId: String, itemId: String): String = "$carId|$itemId"

    fun buildLatestRecordIndex(
        records: List<ReminderRecordSnapshot>,
        itemIdSeparator: Char = ITEM_ID_SEPARATOR,
    ): Map<String, ReminderRecordSnapshot> {
        val index = mutableMapOf<String, ReminderRecordSnapshot>()
        for (record in records) {
            val itemIds = parseItemIds(record.itemIDsRaw, itemIdSeparator)
            for (itemId in itemIds) {
                val key = latestLogKey(carId = record.carId, itemId = itemId)
                val existing = index[key]
                if (existing == null || shouldReplace(existing, record)) {
                    index[key] = record
                }
            }
        }
        return index
    }

    fun buildRowsForCar(
        car: ReminderCarSnapshot,
        options: List<ReminderItemOptionSnapshot>,
        latestRecordIndex: Map<String, ReminderRecordSnapshot>,
        now: LocalDate,
    ): List<ReminderRow> {
        return options
            .map { option ->
                val key = latestLogKey(carId = car.id, itemId = option.id)
                buildReminderRow(
                    car = car,
                    option = option,
                    itemLatestRecord = latestRecordIndex[key],
                    now = now,
                )
            }
            .sortedWith(
                compareByDescending<ReminderRow> { it.rawProgress }
                    .thenBy { it.duePriority }
                    .thenBy { it.itemName },
            )
    }

    fun buildReminderRow(
        car: ReminderCarSnapshot,
        option: ReminderItemOptionSnapshot,
        itemLatestRecord: ReminderRecordSnapshot?,
        now: LocalDate,
    ): ReminderRow {
        val timeBaselineDate = itemLatestRecord?.date ?: car.purchaseDate
        val mileageBaseline = itemLatestRecord?.mileage ?: 0

        val mileageProgress: Double?
        val mileageRemaining: Int?
        if (option.remindByMileage && option.mileageInterval > 0) {
            val usedMileage = (car.mileage - mileageBaseline).coerceAtLeast(0)
            mileageProgress = usedMileage.toDouble() / option.mileageInterval.toDouble()
            mileageRemaining = option.mileageInterval - usedMileage
        } else {
            mileageProgress = null
            mileageRemaining = null
        }

        val timeProgress: Double?
        val daysRemaining: Int?
        if (option.remindByTime && option.monthInterval > 0) {
            val dueDate = timeBaselineDate.plusMonths(option.monthInterval.toLong())
            val totalIntervalDays = ChronoUnit.DAYS.between(timeBaselineDate, dueDate).toDouble()
            val elapsedDays = ChronoUnit.DAYS.between(timeBaselineDate, now).toDouble()

            timeProgress = if (totalIntervalDays > 0.0) {
                elapsedDays / totalIntervalDays
            } else {
                1.0
            }
            daysRemaining = ChronoUnit.DAYS.between(now, dueDate).toInt()
        } else {
            timeProgress = null
            daysRemaining = null
        }

        val (rawProgress, duePriority) = when {
            mileageProgress != null && timeProgress != null -> {
                if (mileageProgress >= timeProgress) {
                    mileageProgress to option.mileageInterval.toDouble()
                } else {
                    timeProgress to option.monthInterval * 30.0
                }
            }

            mileageProgress != null -> mileageProgress to option.mileageInterval.toDouble()
            timeProgress != null -> timeProgress to option.monthInterval * 30.0
            else -> 0.0 to Double.MAX_VALUE
        }

        val rawPercent = (rawProgress * 100.0).coerceAtLeast(0.0)
        val warningStart = option.warningStartPercent.coerceAtLeast(0)
        val dangerStart = option.dangerStartPercent.coerceAtLeast(warningStart)
        val colorLevel = when {
            rawPercent >= dangerStart.toDouble() -> ReminderProgressColorLevel.DANGER
            rawPercent >= warningStart.toDouble() -> ReminderProgressColorLevel.WARNING
            else -> ReminderProgressColorLevel.NORMAL
        }

        return ReminderRow(
            id = latestLogKey(carId = car.id, itemId = option.id),
            itemName = option.name,
            rawProgress = rawProgress,
            duePriority = duePriority,
            displayProgress = rawProgress.coerceIn(0.0, 1.0),
            progressText = "${rawPercent.roundToInt()}%",
            detailTexts = buildDetailTexts(
                mileageRemaining = mileageRemaining,
                daysRemaining = daysRemaining,
            ),
            progressColorLevel = colorLevel,
        )
    }

    private fun shouldReplace(existing: ReminderRecordSnapshot, incoming: ReminderRecordSnapshot): Boolean {
        if (incoming.date.isAfter(existing.date)) {
            return true
        }
        if (incoming.date.isBefore(existing.date)) {
            return false
        }
        return incoming.mileage > existing.mileage
    }

    private fun parseItemIds(raw: String, separator: Char): Set<String> {
        if (raw.isBlank()) return emptySet()
        return raw
            .split(separator)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun buildDetailTexts(
        mileageRemaining: Int?,
        daysRemaining: Int?,
    ): List<String> {
        val detailTexts = mutableListOf<String>()
        mileageRemaining?.let { detailTexts.add(buildMileageDetailText(it)) }
        daysRemaining?.let { detailTexts.add(buildTimeDetailText(it)) }
        if (detailTexts.isEmpty()) {
            detailTexts += "未设置提醒规则"
        }
        return detailTexts
    }

    private fun buildMileageDetailText(remaining: Int): String {
        if (remaining > 0) {
            return "按里程提醒：距离下次约${remaining}公里"
        }
        if (remaining == 0) {
            return "按里程提醒：今日到期"
        }
        return "按里程提醒：已超${kotlin.math.abs(remaining)}公里"
    }

    private fun buildTimeDetailText(remainingDays: Int): String {
        if (remainingDays > 0) {
            return "按时间提醒：距离下次约${timeDistanceText(remainingDays)}"
        }
        if (remainingDays == 0) {
            return "按时间提醒：今日到期"
        }
        return "按时间提醒：已超${timeDistanceText(kotlin.math.abs(remainingDays))}"
    }

    private fun timeDistanceText(days: Int): String {
        if (days < 30) return "${days}天"
        if (days < 365) {
            val months = (days / 30).coerceAtLeast(1)
            return "${months}个月"
        }
        val totalMonths = (days / 30).coerceAtLeast(12)
        val years = totalMonths / 12
        val months = totalMonths % 12
        return if (months == 0) {
            "${years}年"
        } else {
            "${years}年${months}个月"
        }
    }
}
