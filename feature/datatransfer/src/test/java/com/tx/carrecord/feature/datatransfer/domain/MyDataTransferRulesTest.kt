package com.tx.carrecord.feature.datatransfer.domain

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

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

//    @Test
//    fun buildExportPayload_导出结果应稳定排序并使用catalogKey() {
//        val payload = MyDataTransferRules.buildExportPayload(
//            cars = listOf(
//                BackupExportCarSnapshot(
//                    id = "22222222-2222-2222-2222-222222222222",
//                    brand = "本田",
//                    modelName = "思域",
//                    mileage = 10000,
//                    disabledItemIDsRaw = "",
//                    purchaseDate = LocalDate.parse("2026-05-01"),
//                ),
//                BackupExportCarSnapshot(
//                    id = "11111111-1111-1111-1111-111111111111",
//                    brand = "日产",
//                    modelName = "轩逸",
//                    mileage = 8000,
//                    disabledItemIDsRaw = "",
//                    purchaseDate = LocalDate.parse("2026-03-01"),
//                ),
//            ),
//            itemOptions = listOf(
//                BackupExportItemOptionSnapshot(
//                    id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
//                    name = "机油",
//                    ownerCarId = "22222222-2222-2222-2222-222222222222",
//                    isDefault = true,
//                    catalogKey = "engine-oil",
//                    remindByMileage = true,
//                    mileageInterval = 5000,
//                    remindByTime = false,
//                    monthInterval = 0,
//                    warningStartPercent = 80,
//                    dangerStartPercent = 100,
//                    createdAtEpochSeconds = 10,
//                ),
//            ),
//            records = listOf(
//                BackupExportRecordSnapshot(
//                    id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
//                    carId = "22222222-2222-2222-2222-222222222222",
//                    date = LocalDate.parse("2026-05-05"),
//                    itemIDsRaw = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
//                    cost = 300.0,
//                    mileage = 12000,
//                    note = "",
//                ),
//            ),
//        )
//
//        assertEquals("日产", payload.vehicles.first().car.brand)
//        assertEquals(listOf("engine-oil"), payload.vehicles[1].serviceLogs.first().itemNames)
//        assertEquals("本田", payload.modelProfiles.first().brand)
//    }

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
