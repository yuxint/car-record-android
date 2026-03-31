package com.tx.carrecord.core.common.maintenance

import java.util.Locale

object MaintenanceItemConfig {
    private const val MODEL_KEY_SEPARATOR: Char = '|'
    private const val ITEM_ID_SEPARATOR: Char = '|'

    data class DefaultItemDefinition(
        val key: String,
        val defaultName: String,
        val mileageInterval: Int?,
        val monthInterval: Int?,
        val remindByMileage: Boolean,
        val remindByTime: Boolean,
        val warningStartPercent: Int,
        val dangerStartPercent: Int,
    )

    data class ModelConfig(
        val defaultItemDefinitions: List<DefaultItemDefinition>,
        val preferredKeysWhenNoLog: List<String>,
        val defaultWarningStartPercent: Int,
        val defaultDangerStartPercent: Int,
    ) {
        val defaultOrderByKey: Map<String, Int> by lazy {
            defaultItemDefinitions
                .withIndex()
                .associate { (index, definition) -> definition.key to index }
        }
    }

    const val fuelCleanerKey = "fuel_cleaner"
    const val engineOilKey = "engine_oil"
    const val acFilterKey = "ac_filter"
    const val airFilterKey = "air_filter"
    const val transmissionOilKey = "transmission_oil"
    const val brakeFluidKey = "brake_fluid"
    const val sparkPlugKey = "spark_plug"
    const val driveBeltKey = "drive_belt"
    const val valveClearanceKey = "valve_clearance"
    const val brakeKey = "brake"
    const val antifreezeKey = "antifreeze"
    const val gasFilterKey = "gas_filter"
    const val tireRotationKey = "tire_rotation"

    val warningRangeStartPercent: Int
        get() = civic2022WarningStartPercent

    val warningRangeEndExclusivePercent: Int
        get() = civic2022DangerStartPercent

    val dangerStartPercent: Int
        get() = civic2022DangerStartPercent

    private const val civic2022WarningStartPercent = 100
    private const val civic2022DangerStartPercent = 125
    private const val sylphy2022WarningStartPercent = 100
    private const val sylphy2022DangerStartPercent = 125

    private val civic2022DefaultItemDefinitions = listOf(
        DefaultItemDefinition(fuelCleanerKey, "汽油发动机清洁剂（燃油宝）", 5000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(engineOilKey, "机油、机滤", 5000, 6, true, true, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(acFilterKey, "空调滤芯", 20_000, 12, true, true, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(airFilterKey, "空气滤芯", 20_000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(transmissionOilKey, "变速箱油", 40_000, 24, true, true, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(brakeFluidKey, "制动液（刹车油）", null, 36, false, true, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(sparkPlugKey, "火花塞", 100_000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(driveBeltKey, "检查传动皮带", 40_000, 24, true, true, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(valveClearanceKey, "检查气门间隙", 120_000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(brakeKey, "检查制动器（刹车）", 120_000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(antifreezeKey, "冷却液（防冻液）", 200_000, 120, true, true, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(gasFilterKey, "汽油滤芯", 140_000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
        DefaultItemDefinition(tireRotationKey, "轮胎换位", 10_000, null, true, false, civic2022WarningStartPercent, civic2022DangerStartPercent),
    )

    private val sylphy2022DefaultItemDefinitions = listOf(
        DefaultItemDefinition(engineOilKey, "机油", 10_000, 6, true, true, sylphy2022WarningStartPercent, sylphy2022DangerStartPercent),
        DefaultItemDefinition(acFilterKey, "空调滤芯", 20_000, 12, true, true, sylphy2022WarningStartPercent, sylphy2022DangerStartPercent),
        DefaultItemDefinition(airFilterKey, "空气滤芯", 20_000, null, true, false, sylphy2022WarningStartPercent, sylphy2022DangerStartPercent),
        DefaultItemDefinition(transmissionOilKey, "变速箱油", null, 24, false, true, sylphy2022WarningStartPercent, sylphy2022DangerStartPercent),
        DefaultItemDefinition(brakeFluidKey, "刹车油", null, 36, false, true, sylphy2022WarningStartPercent, sylphy2022DangerStartPercent),
    )

    private val fallbackModelConfig = ModelConfig(
        defaultItemDefinitions = civic2022DefaultItemDefinitions,
        preferredKeysWhenNoLog = listOf(engineOilKey, fuelCleanerKey, acFilterKey),
        defaultWarningStartPercent = civic2022WarningStartPercent,
        defaultDangerStartPercent = civic2022DangerStartPercent,
    )

    fun modelConfig(brand: String?, modelName: String?): ModelConfig {
        val key = modelKey(brand, modelName)
        return when (key) {
            modelKey("本田", "22款思域") -> ModelConfig(
                defaultItemDefinitions = civic2022DefaultItemDefinitions,
                preferredKeysWhenNoLog = listOf(engineOilKey, fuelCleanerKey, acFilterKey),
                defaultWarningStartPercent = civic2022WarningStartPercent,
                defaultDangerStartPercent = civic2022DangerStartPercent,
            )
            modelKey("日产", "22款轩逸") -> ModelConfig(
                defaultItemDefinitions = sylphy2022DefaultItemDefinitions,
                preferredKeysWhenNoLog = listOf(engineOilKey, acFilterKey, airFilterKey),
                defaultWarningStartPercent = sylphy2022WarningStartPercent,
                defaultDangerStartPercent = sylphy2022DangerStartPercent,
            )
            else -> fallbackModelConfig
        }
    }

    fun defaultItemDefinitions(brand: String?, modelName: String?): List<DefaultItemDefinition> =
        modelConfig(brand, modelName).defaultItemDefinitions

    fun joinItemIDs(itemIDs: List<String>): String =
        itemIDs.joinToString(separator = ITEM_ID_SEPARATOR.toString())

    fun parseItemIDs(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        val seen = mutableSetOf<String>()
        val unique = mutableListOf<String>()
        for (itemId in raw.split(ITEM_ID_SEPARATOR).map { it.trim() }) {
            if (itemId.isEmpty()) continue
            if (seen.add(itemId)) {
                unique += itemId
            }
        }
        return unique
    }

    fun normalizeItemIDsRaw(raw: String): String = joinItemIDs(parseItemIDs(raw))

    fun <T> filterDisabledOptions(
        options: List<T>,
        disabledItemIDsRaw: String,
        includeDisabled: Boolean,
        itemIDSelector: (T) -> String,
    ): List<T> {
        if (includeDisabled) return options
        val disabledItemIDs = parseItemIDs(disabledItemIDsRaw).toSet()
        if (disabledItemIDs.isEmpty()) return options
        return options.filter { itemIDSelector(it) !in disabledItemIDs }
    }

    fun <T> sortItemOptionsByDefaultOrder(
        options: List<T>,
        defaultOrderByKey: Map<String, Int>,
        catalogKeySelector: (T) -> String?,
    ): List<T> {
        return options
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<T>> {
                    defaultOrderByKey[catalogKeySelector(it.value).orEmpty()] ?: Int.MAX_VALUE
                }.thenBy { it.index },
            )
            .map { it.value }
    }

    fun <T> reminderSummaryText(
        option: T,
        remindByMileageSelector: (T) -> Boolean,
        mileageIntervalSelector: (T) -> Int,
        remindByTimeSelector: (T) -> Boolean,
        monthIntervalSelector: (T) -> Int,
        mileageTextFormatter: (Int) -> String = ::formatReminderMileageText,
        monthTextFormatter: (Int) -> String = ::formatMonthsAsYearsText,
    ): String {
        val parts = buildList {
            if (remindByMileageSelector(option) && mileageIntervalSelector(option) > 0) {
                add(mileageTextFormatter(mileageIntervalSelector(option)))
            }
            if (remindByTimeSelector(option) && monthIntervalSelector(option) > 0) {
                add(monthTextFormatter(monthIntervalSelector(option)))
            }
        }
        return if (parts.isEmpty()) "未设置" else parts.joinToString(separator = " / ")
    }

    fun reminderDetailTexts(
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

    fun normalizedProgressThresholds(
        warning: Int,
        danger: Int,
    ): Pair<Int, Int> {
        val normalizedWarning = warning.coerceAtLeast(0)
        val normalizedDanger = danger.coerceAtLeast(normalizedWarning)
        return normalizedWarning to normalizedDanger
    }

    fun progressColorLevel(
        rawPercent: Double,
        warningStartPercent: Int,
        dangerStartPercent: Int,
    ): ProgressColorLevel {
        val (warningStart, dangerStart) = normalizedProgressThresholds(
            warning = warningStartPercent,
            danger = dangerStartPercent,
        )
        return when {
            rawPercent >= dangerStart.toDouble() -> ProgressColorLevel.DANGER
            rawPercent >= warningStart.toDouble() -> ProgressColorLevel.WARNING
            else -> ProgressColorLevel.NORMAL
        }
    }

    enum class ProgressColorLevel {
        NORMAL,
        WARNING,
        DANGER,
    }

    private fun normalizeText(value: String?): String = value.orEmpty().trim()

    fun formatReminderMileageText(value: Int): String {
        val safeValue = value.coerceAtLeast(0)
        if (safeValue < 10_000) {
            return "${safeValue}公里"
        }
        return formattedMileageByWanQian(safeValue)
    }

    private fun modelKey(brand: String?, modelName: String?): String {
        return "${normalizeText(brand)}$MODEL_KEY_SEPARATOR${normalizeText(modelName)}"
    }

    private fun buildMileageDetailText(remaining: Int): String {
        if (remaining > 0) {
            return "按里程提醒：距离下次约${formatReminderMileageText(remaining)}"
        }
        if (remaining == 0) {
            return "按里程提醒：今日到期"
        }
        return "按里程提醒：已超${formatReminderMileageText(kotlin.math.abs(remaining))}"
    }

    private fun formattedMileageByWanQian(value: Int): String {
        val wan = value / 10_000
        val remainder = value % 10_000
        val qian = remainder / 1_000
        val bai = (remainder % 1_000) / 100

        if (wan > 0) {
            if (qian > 0 || bai > 0) {
                val decimalValue = (qian * 1_000 + bai * 100) / 10_000.0
                val fullString = String.format(Locale.CHINA, "%.1f", decimalValue)
                val decimalPart = fullString
                    .substringAfter('.', missingDelimiterValue = "")
                    .replace(Regex("^0+|0+$"), "")
                if (decimalPart.isEmpty()) {
                    return "${wan}万公里"
                }
                return "${wan}.${decimalPart}万公里"
            }
            return "${wan}万公里"
        }

        if (qian > 0 || bai > 0) {
            return "${value}公里"
        }

        return "0公里"
    }

    private fun buildTimeDetailText(remainingDays: Int): String {
        if (remainingDays > 0) {
            return "按时间提醒：距离下次约${formatTimeDistanceText(remainingDays)}"
        }
        if (remainingDays == 0) {
            return "按时间提醒：今日到期"
        }
        return "按时间提醒：已超${formatTimeDistanceText(kotlin.math.abs(remainingDays))}"
    }

    private fun formatTimeDistanceText(days: Int): String {
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

    private fun formatMonthsAsYearsText(monthInterval: Int): String {
        val years = monthInterval.coerceAtLeast(1) / 12.0
        return if (years % 1.0 == 0.0) {
            "${years.toInt()}年"
        } else {
            String.format("%.1f年", years)
        }
    }
}
