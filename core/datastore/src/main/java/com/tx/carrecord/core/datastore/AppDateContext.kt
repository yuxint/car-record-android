package com.tx.carrecord.core.datastore

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Locale

interface AppDateContext {
    val locale: Locale

    val isManualNowEnabledFlow: Flow<Boolean>

    val manualNowDateFlow: Flow<LocalDate>

    fun nowFlow(): Flow<LocalDate>

    suspend fun now(): LocalDate

    suspend fun setManualNowEnabled(enabled: Boolean)

    suspend fun setManualNowDate(date: LocalDate)

    fun normalizeToStartOfDay(date: LocalDate): LocalDate

    fun formatShortDate(date: LocalDate): String
}
