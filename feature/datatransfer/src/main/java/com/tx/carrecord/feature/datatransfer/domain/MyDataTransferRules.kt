package com.tx.carrecord.feature.datatransfer.domain

import com.tx.carrecord.core.common.maintenance.MaintenanceItemConfig
import com.tx.carrecord.core.common.time.AppTimeCodec
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.util.UUID

object MyDataTransferRules {
    val jsonCodec: Json = Json {
        prettyPrint = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun decodePayload(jsonText: String): BackupImportDecision {
        val rootElement = try {
            jsonCodec.parseToJsonElement(jsonText)
        } catch (_: Exception) {
            return BackupImportDecision.InvalidPayload(
                code = 1000,
                message = "恢复失败：JSON 格式非法。",
            )
        }

        val rootObject = rootElement as? JsonObject
            ?: return BackupImportDecision.InvalidPayload(
                code = 1001,
                message = "恢复失败：JSON 根节点必须为对象。",
            )

        if ("modelProfiles" !in rootObject || "vehicles" !in rootObject) {
            return BackupImportDecision.InvalidPayload(
                code = 1001,
                message = "恢复失败：JSON 缺少 modelProfiles 或 vehicles，文件结构与 App 当前备份不一致。",
            )
        }

        val modelProfiles = rootObject["modelProfiles"]
        val vehicles = rootObject["vehicles"]
        if (modelProfiles !is JsonArray || vehicles !is JsonArray) {
            return BackupImportDecision.InvalidPayload(
                code = 1001,
                message = "恢复失败：modelProfiles/vehicles 必须为数组。",
            )
        }

        val payload = try {
            jsonCodec.decodeFromString(MyDataTransferPayload.serializer(), jsonText)
        } catch (_: SerializationException) {
            return BackupImportDecision.InvalidPayload(
                code = 1000,
                message = "恢复失败：JSON 字段结构与当前版本不兼容。",
            )
        }

        return planImport(payload)
    }

    fun encodePayload(payload: MyDataTransferPayload): String =
        jsonCodec.encodeToString(MyDataTransferPayload.serializer(), payload)

    fun buildExportPayload(
        cars: List<BackupExportCarSnapshot>,
        itemOptions: List<BackupExportItemOptionSnapshot>,
        records: List<BackupExportRecordSnapshot>,
    ): MyDataTransferPayload {
        val sortedCars = cars.sortedWith(
            compareBy<BackupExportCarSnapshot>({ it.purchaseDate }, { it.id.lowercase() }),
        )
        val optionsByCarId = itemOptions
            .groupBy { it.ownerCarID.orEmpty() }
            .mapValues { (_, scoped) ->
                scoped.sortedWith(
                    compareBy<BackupExportItemOptionSnapshot>({ it.createdAtEpochSeconds }, { it.id.lowercase() }),
                )
            }
        val recordsByCarId = records
            .groupBy { it.carID }
            .mapValues { (_, scoped) ->
                scoped.sortedWith(
                    compareBy<BackupExportRecordSnapshot>({ it.date }, { it.mileage }, { it.id.lowercase() }),
                )
            }

        val profileByKey = linkedMapOf<String, MyDataTransferModelProfilePayload>()
        val vehicles = sortedCars.map { car ->
            val scopedOptions = optionsByCarId[car.id].orEmpty()
            val exportTokenByItemId = scopedOptions.associate { option ->
                val normalizedCatalogKey = option.catalogKey.orEmpty().trim()
                val token = if (normalizedCatalogKey.isNotEmpty()) {
                    normalizedCatalogKey
                } else {
                    option.name
                }
                option.id.trim().uppercase() to token
            }

            val profileKey = modelProfileKey(car.brand, car.modelName)
            if (!profileByKey.containsKey(profileKey)) {
                profileByKey[profileKey] = MyDataTransferModelProfilePayload(
                    brand = car.brand,
                    modelName = car.modelName,
                    serviceItems = scopedOptions.map { option ->
                        MyDataTransferItemPayload(
                            id = normalizeUuid(option.id),
                            name = option.name,
                            isDefault = option.isDefault,
                            catalogKey = option.catalogKey,
                            remindByMileage = option.remindByMileage,
                            mileageInterval = option.mileageInterval,
                            remindByTime = option.remindByTime,
                            monthInterval = option.monthInterval,
                            warningStartPercent = option.warningStartPercent,
                            dangerStartPercent = option.dangerStartPercent,
                            createdAt = AppTimeCodec.epochSecondsToPayloadCreatedAt(
                                option.createdAtEpochSeconds,
                            ),
                        )
                    },
                )
            }

            val serviceLogs = recordsByCarId[car.id].orEmpty().map { record ->
                val itemNames = parseItemIDsRaw(record.itemIDsRaw)
                    .mapNotNull { itemId -> exportTokenByItemId[itemId.trim().uppercase()] }
                MyDataTransferLogPayload(
                    id = normalizeUuid(record.id),
                    date = AppTimeCodec.formatDate(record.date),
                    itemNames = itemNames,
                    cost = record.cost,
                    mileage = record.mileage,
                    note = record.note,
                )
            }

            MyDataTransferVehiclePayload(
                car = MyDataTransferCarPayload(
                    id = normalizeUuid(car.id),
                    brand = car.brand,
                    modelName = car.modelName,
                    mileage = car.mileage,
                    disabledItemIDsRaw = car.disabledItemIDsRaw,
                    purchaseDate = AppTimeCodec.formatDate(car.purchaseDate),
                ),
                serviceLogs = serviceLogs,
            )
        }

        val sortedProfiles = profileByKey
            .toList()
            .sortedBy { it.first.lowercase() }
            .map { it.second }

        return MyDataTransferPayload(
            modelProfiles = sortedProfiles,
            vehicles = vehicles,
        )
    }

    fun planImport(payload: MyDataTransferPayload): BackupImportDecision {
        val profileByKey = mutableMapOf<String, MyDataTransferModelProfilePayload>()
        for (profile in payload.modelProfiles) {
            val key = modelProfileKey(profile.brand, profile.modelName)
            if (profileByKey.containsKey(key)) {
                return BackupImportDecision.InvalidPayload(
                    code = 1010,
                    message = "恢复失败：车型“${profile.brand} ${profile.modelName}”重复。",
                )
            }
            profileByKey[key] = profile
        }

        if (profileByKey.isEmpty() && payload.vehicles.isNotEmpty()) {
            return BackupImportDecision.InvalidPayload(
                code = 1011,
                message = "恢复失败：备份缺少车型保养项目配置。",
            )
        }

        val importedCarIDs = mutableSetOf<String>()
        val importedModelKeys = mutableSetOf<String>()
        val importedRecordIDs = mutableSetOf<String>()
        val carDrafts = mutableListOf<BackupImportCarDraft>()

        for (vehicle in payload.vehicles) {
            val car = vehicle.car
            val normalizedCarId = normalizeUuidOrNull(car.id)
                ?: return BackupImportDecision.InvalidPayload(
                    code = 1004,
                    message = "恢复失败：车辆 ID 格式非法：${car.id}。",
                )

            val normalizedPurchaseDate = AppTimeCodec.parseDateOrNull(car.purchaseDate)
            if (normalizedPurchaseDate == null) {
                return BackupImportDecision.InvalidPayload(
                    code = 1002,
                    message = "车辆上路日期格式错误：${car.purchaseDate}。请使用 yyyy-MM-dd。",
                )
            }
            if (!importedCarIDs.add(normalizedCarId)) {
                return BackupImportDecision.InvalidPayload(
                    code = 1004,
                    message = "恢复失败：备份内车辆ID $normalizedCarId 重复。",
                )
            }

            val profileKey = modelProfileKey(car.brand, car.modelName)
            val profile = profileByKey[profileKey]
                ?: return BackupImportDecision.InvalidPayload(
                    code = 1013,
                    message = "恢复失败：车型“${car.brand} ${car.modelName}”缺少保养项目配置。",
                )

            if (!importedModelKeys.add(profileKey)) {
                return BackupImportDecision.InvalidPayload(
                    code = 1014,
                    message = "恢复失败：车型“${car.brand} ${car.modelName}”重复，单一车型仅允许一辆车。",
                )
            }

            val profileItemNames = mutableSetOf<String>()
            val profileItemKeys = mutableSetOf<String>()
            val itemByName = mutableMapOf<String, MyDataTransferItemPayload>()
            val itemByCatalogKey = mutableMapOf<String, MyDataTransferItemPayload>()
            val itemDrafts = mutableListOf<BackupImportItemDraft>()

            for (item in profile.serviceItems) {
                val normalizedItemName = item.name.trim()
                if (normalizedItemName.isEmpty()) {
                    return BackupImportDecision.InvalidPayload(
                        code = 1008,
                        message = "恢复失败：保养项目名称不能为空。",
                    )
                }
                if (!profileItemNames.add(normalizedItemName)) {
                    return BackupImportDecision.InvalidPayload(
                        code = 1009,
                        message = "恢复失败：车型“${profile.brand} ${profile.modelName}”存在重复项目“$normalizedItemName”。",
                    )
                }

                val normalizedItemId = normalizeUuidOrNull(item.id)
                    ?: return BackupImportDecision.InvalidPayload(
                        code = 1008,
                        message = "恢复失败：项目 ID 格式非法：${item.id}。",
                    )

                val normalizedCatalogKey = item.catalogKey.orEmpty().trim()
                if (normalizedCatalogKey.isNotEmpty()) {
                    if (!profileItemKeys.add(normalizedCatalogKey)) {
                        return BackupImportDecision.InvalidPayload(
                            code = 1012,
                            message = "恢复失败：车型“${profile.brand} ${profile.modelName}”存在重复项目 key “$normalizedCatalogKey”。",
                        )
                    }
                }

                val normalizedItem = item.copy(
                    id = normalizedItemId,
                    name = normalizedItemName,
                )
                itemByName[normalizedItemName] = normalizedItem
                if (normalizedCatalogKey.isNotEmpty()) {
                    itemByCatalogKey[normalizedCatalogKey] = normalizedItem
                }
                itemDrafts += BackupImportItemDraft(
                    item = normalizedItem,
                    normalizedName = normalizedItemName,
                    normalizedCatalogKey = normalizedCatalogKey,
                )
            }

            val recordDrafts = mutableListOf<BackupImportRecordDraft>()
            for (record in vehicle.serviceLogs) {
                val normalizedRecordId = normalizeUuidOrNull(record.id)
                    ?: return BackupImportDecision.InvalidPayload(
                        code = 1005,
                        message = "恢复失败：保养记录 ID 格式非法：${record.id}。",
                    )
                val normalizedDate = AppTimeCodec.parseDateOrNull(record.date)
                if (normalizedDate == null) {
                    return BackupImportDecision.InvalidPayload(
                        code = 1003,
                        message = "保养日期格式错误：${record.date}。请使用 yyyy-MM-dd。",
                    )
                }
                if (!importedRecordIDs.add(normalizedRecordId)) {
                    return BackupImportDecision.InvalidPayload(
                        code = 1005,
                        message = "恢复失败：备份内保养记录ID $normalizedRecordId 重复。",
                    )
                }

                val normalizedNames = record.itemNames
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (normalizedNames.isEmpty()) {
                    return BackupImportDecision.InvalidPayload(
                        code = 1006,
                        message = "恢复失败：保养项目不能为空。",
                    )
                }

                val mappedItemIds = mutableListOf<String>()
                val seenItemIds = mutableSetOf<String>()
                for (name in normalizedNames) {
                    val matched = itemByCatalogKey[name] ?: itemByName[name]
                    if (matched == null) {
                        return BackupImportDecision.InvalidPayload(
                            code = 1007,
                            message = "恢复失败：项目“$name”未在车型配置中声明。",
                        )
                    }
                    if (seenItemIds.add(matched.id)) {
                        mappedItemIds += matched.id
                    }
                }

                recordDrafts += BackupImportRecordDraft(
                    record = record.copy(id = normalizedRecordId),
                    normalizedDate = normalizedDate,
                    mappedItemIds = mappedItemIds,
                    mappedItemIDsRaw = MaintenanceItemConfig.joinItemIDs(mappedItemIds),
                )
            }

            carDrafts += BackupImportCarDraft(
                car = car.copy(id = normalizedCarId),
                normalizedPurchaseDate = normalizedPurchaseDate,
                itemDrafts = itemDrafts,
                recordDrafts = recordDrafts,
            )
        }

        return BackupImportDecision.Success(
            plan = BackupImportPlan(
                shouldClearBusinessDataBeforeImport = true,
                carDrafts = carDrafts,
            ),
        )
    }

    private fun parseItemIDsRaw(raw: String): List<String> {
        return MaintenanceItemConfig.parseItemIDs(raw)
            .map { it.uppercase() }
    }

    private fun normalizeUuid(value: String): String = UUID.fromString(value.trim()).toString().uppercase()

    private fun normalizeUuidOrNull(value: String): String? = try {
        normalizeUuid(value)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun modelProfileKey(brand: String, modelName: String): String {
        val normalizedBrand = brand.trim()
        val normalizedModel = modelName.trim()
        return "$normalizedBrand|$normalizedModel"
    }
}
