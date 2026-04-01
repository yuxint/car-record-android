package com.tx.carrecord.feature.datatransfer.domain

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse

class MyDataTransferRulesTest {
    @Test
    fun decodePayload_缺少根节点字段应失败() {
        val json = """
            {
              "vehicles": []
            }
        """.trimIndent()

        val result = MyDataTransferRules.decodePayload(json)

        val invalid = assertIs<BackupImportDecision.InvalidPayload>(result)
        assertEquals(1001, invalid.code)
        assertContains(invalid.message, "modelProfiles 或 vehicles")
    }

    @Test
    fun planImport_车型重复应失败() {
        val payload = MyDataTransferPayload(
            modelProfiles = listOf(
                MyDataTransferModelProfilePayload(
                    brand = "本田",
                    modelName = "思域",
                    serviceItems = emptyList(),
                ),
                MyDataTransferModelProfilePayload(
                    brand = " 本田 ",
                    modelName = " 思域 ",
                    serviceItems = emptyList(),
                ),
            ),
            vehicles = emptyList(),
        )

        val result = MyDataTransferRules.planImport(payload)

        val invalid = assertIs<BackupImportDecision.InvalidPayload>(result)
        assertEquals(1010, invalid.code)
    }

    @Test
    fun planImport_车辆日期必须严格yyyyMMdd格式() {
        val payload = MyDataTransferPayload(
            modelProfiles = listOf(
                MyDataTransferModelProfilePayload(
                    brand = "本田",
                    modelName = "思域",
                    serviceItems = listOf(
                        sampleItemPayload(id = "11111111-1111-1111-1111-111111111111"),
                    ),
                ),
            ),
            vehicles = listOf(
                MyDataTransferVehiclePayload(
                    car = MyDataTransferCarPayload(
                        id = "22222222-2222-2222-2222-222222222222",
                        brand = "本田",
                        modelName = "思域",
                        mileage = 12000,
                        disabledItemIDsRaw = "",
                        purchaseDate = "2026-2-03",
                    ),
                    serviceLogs = emptyList(),
                ),
            ),
        )

        val result = MyDataTransferRules.planImport(payload)

        val invalid = assertIs<BackupImportDecision.InvalidPayload>(result)
        assertEquals(1002, invalid.code)
    }

    @Test
    fun planImport_项目映射应优先catalogKey并去重() {
        val payload = MyDataTransferPayload(
            modelProfiles = listOf(
                MyDataTransferModelProfilePayload(
                    brand = "本田",
                    modelName = "思域",
                    serviceItems = listOf(
                        sampleItemPayload(
                            id = "11111111-1111-1111-1111-111111111111",
                            name = "机油(旧名)",
                            catalogKey = "engine-oil",
                        ),
                        sampleItemPayload(
                            id = "33333333-3333-3333-3333-333333333333",
                            name = "空滤",
                            catalogKey = null,
                        ),
                    ),
                ),
            ),
            vehicles = listOf(
                MyDataTransferVehiclePayload(
                    car = MyDataTransferCarPayload(
                        id = "22222222-2222-2222-2222-222222222222",
                        brand = "本田",
                        modelName = "思域",
                        mileage = 12000,
                        disabledItemIDsRaw = "",
                        purchaseDate = "2026-02-03",
                    ),
                    serviceLogs = listOf(
                        MyDataTransferLogPayload(
                            id = "44444444-4444-4444-4444-444444444444",
                            date = "2026-03-03",
                            itemNames = listOf("engine-oil", "机油(旧名)", "空滤", "空滤"),
                            cost = 328.5,
                            mileage = 15000,
                            note = "",
                        ),
                    ),
                ),
            ),
        )

        val result = MyDataTransferRules.planImport(payload)

        val success = assertIs<BackupImportDecision.Success>(result)
        val mapped = success.plan.carDrafts.first().recordDrafts.first().mappedItemIds
        assertEquals(
            listOf(
                "11111111-1111-1111-1111-111111111111".uppercase(),
                "33333333-3333-3333-3333-333333333333".uppercase(),
            ),
            mapped,
        )
    }

    @Test
    fun encodePayload_数值字段应使用普通十进制文本() {
        val payload = MyDataTransferPayload(
            modelProfiles = listOf(
                MyDataTransferModelProfilePayload(
                    brand = "本田",
                    modelName = "思域",
                    serviceItems = listOf(
                        MyDataTransferItemPayload(
                            id = "11111111-1111-1111-1111-111111111111",
                            name = "机油",
                            isDefault = true,
                            catalogKey = "engine-oil",
                            remindByMileage = true,
                            mileageInterval = 5000,
                            remindByTime = false,
                            monthInterval = 0,
                            warningStartPercent = 80,
                            dangerStartPercent = 100,
                            createdAt = 1700000000.0,
                        ),
                    ),
                ),
            ),
            vehicles = listOf(
                MyDataTransferVehiclePayload(
                    car = MyDataTransferCarPayload(
                        id = "22222222-2222-2222-2222-222222222222",
                        brand = "本田",
                        modelName = "思域",
                        mileage = 12000,
                        disabledItemIDsRaw = "",
                        purchaseDate = "2026-02-03",
                    ),
                    serviceLogs = listOf(
                        MyDataTransferLogPayload(
                            id = "33333333-3333-3333-3333-333333333333",
                            date = "2026-03-03",
                            itemNames = listOf("engine-oil"),
                            cost = 328.5,
                            mileage = 15000,
                            note = "",
                        ),
                    ),
                ),
            ),
        )

        val json = MyDataTransferRules.encodePayload(payload)

        assertContains(json, "\"createdAt\": 1700000000")
        assertContains(json, "\"cost\": 328.5")
        assertFalse(Regex("""\b\d+(?:\.\d+)?[eE][+-]?\d+\b""").containsMatchIn(json))
    }

    private fun sampleItemPayload(
        id: String,
        name: String = "机油",
        catalogKey: String? = "engine-oil",
    ): MyDataTransferItemPayload =
        MyDataTransferItemPayload(
            id = id,
            name = name,
            isDefault = true,
            catalogKey = catalogKey,
            remindByMileage = true,
            mileageInterval = 5000,
            remindByTime = false,
            monthInterval = 0,
            warningStartPercent = 80,
            dangerStartPercent = 100,
            createdAt = 0.0,
        )
}
