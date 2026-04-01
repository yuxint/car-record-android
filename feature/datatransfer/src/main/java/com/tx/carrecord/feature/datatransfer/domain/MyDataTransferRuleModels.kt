package com.tx.carrecord.feature.datatransfer.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class MyDataTransferPayload(
    val modelProfiles: List<MyDataTransferModelProfilePayload>,
    val vehicles: List<MyDataTransferVehiclePayload>,
)

@Serializable
data class MyDataTransferModelProfilePayload(
    val brand: String,
    val modelName: String,
    val serviceItems: List<MyDataTransferItemPayload>,
)

@Serializable
data class MyDataTransferItemPayload(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val catalogKey: String?,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
    val warningStartPercent: Int,
    val dangerStartPercent: Int,
    @Serializable(with = PlainNumberDoubleSerializer::class)
    val createdAt: Double,
)

@Serializable
data class MyDataTransferVehiclePayload(
    val car: MyDataTransferCarPayload,
    val serviceLogs: List<MyDataTransferLogPayload>,
)

@Serializable
data class MyDataTransferCarPayload(
    val id: String,
    val brand: String,
    val modelName: String,
    val mileage: Int,
    val disabledItemIDsRaw: String,
    val purchaseDate: String,
)

@Serializable
data class MyDataTransferLogPayload(
    val id: String,
    val date: String,
    val itemNames: List<String>,
    @Serializable(with = PlainNumberDoubleSerializer::class)
    val cost: Double,
    val mileage: Int,
    val note: String,
)

/**
 * 导出所需的最小业务快照，后续 Step 09 接数据层时直接映射 Room 实体。
 */
data class BackupExportCarSnapshot(
    val id: String,
    val brand: String,
    val modelName: String,
    val mileage: Int,
    val disabledItemIDsRaw: String,
    val purchaseDate: LocalDate,
)

data class BackupExportItemOptionSnapshot(
    val id: String,
    val name: String,
    val ownerCarID: String?,
    val isDefault: Boolean,
    val catalogKey: String?,
    val remindByMileage: Boolean,
    val mileageInterval: Int,
    val remindByTime: Boolean,
    val monthInterval: Int,
    val warningStartPercent: Int,
    val dangerStartPercent: Int,
    val createdAtEpochSeconds: Long,
)

data class BackupExportRecordSnapshot(
    val id: String,
    val carID: String,
    val date: LocalDate,
    val itemIDsRaw: String,
    val cost: Double,
    val mileage: Int,
    val note: String,
)

data class BackupImportPlan(
    val shouldClearBusinessDataBeforeImport: Boolean,
    val carDrafts: List<BackupImportCarDraft>,
)

data class BackupImportCarDraft(
    val car: MyDataTransferCarPayload,
    val normalizedPurchaseDate: LocalDate,
    val itemDrafts: List<BackupImportItemDraft>,
    val recordDrafts: List<BackupImportRecordDraft>,
)

data class BackupImportItemDraft(
    val item: MyDataTransferItemPayload,
    val normalizedName: String,
    val normalizedCatalogKey: String,
)

data class BackupImportRecordDraft(
    val record: MyDataTransferLogPayload,
    val normalizedDate: LocalDate,
    val mappedItemIds: List<String>,
    val mappedItemIDsRaw: String,
)

object PlainNumberDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PlainNumberDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            val plainText = BigDecimal.valueOf(value).toPlainString()
            jsonEncoder.encodeJsonElement(JsonPrimitive(BigDecimal(plainText)))
            return
        }
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

sealed interface BackupImportDecision {
    data class Success(val plan: BackupImportPlan) : BackupImportDecision

    data class InvalidPayload(
        val code: Int,
        val message: String,
    ) : BackupImportDecision
}
