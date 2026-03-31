package com.tx.carrecord.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object AppTimeCodec {
    private val strictDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)

    fun formatDate(date: LocalDate): String = date.format(strictDateFormatter)

    fun parseDateOrNull(value: String): LocalDate? {
        val text = value.trim()
        if (text.isEmpty()) return null

        return try {
            val parsed = LocalDate.parse(text, strictDateFormatter)
            if (formatDate(parsed) == text) parsed else null
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun toEpochSecondsAtStartOfDay(
        date: LocalDate,
        zoneId: ZoneId,
    ): Long = date.atStartOfDay(zoneId).toEpochSecond()

    fun fromEpochSecondsAtZone(
        epochSeconds: Long,
        zoneId: ZoneId,
    ): LocalDate = Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()

    fun payloadCreatedAtToEpochSeconds(createdAt: Double): Long = createdAt.toLong()

    fun epochSecondsToPayloadCreatedAt(epochSeconds: Long): Double = epochSeconds.toDouble()
}
