package com.tx.carrecord.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatastoreAppDateContext @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppDateContext {
    override val locale: Locale = Locale.forLanguageTag("zh-Hans-CN")
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val clock: Clock = Clock.system(zoneId)
    private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override val isManualNowEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[RuntimeContextPreferenceKeys.appDateUseManualNow] ?: false
    }

    override val manualNowDateFlow: Flow<LocalDate> = dataStore.data.map { prefs ->
        decodeManualNowDate(
            epochMillis = prefs[RuntimeContextPreferenceKeys.appDateManualNowTimestamp],
        )
    }

    override fun nowFlow(): Flow<LocalDate> = combine(
        isManualNowEnabledFlow,
        manualNowDateFlow,
    ) { manualEnabled, manualDate ->
        if (manualEnabled) {
            manualDate
        } else {
            LocalDate.now(clock)
        }
    }

    override suspend fun now(): LocalDate {
        val prefs = dataStore.data.first()
        val manualEnabled = prefs[RuntimeContextPreferenceKeys.appDateUseManualNow] ?: false
        if (!manualEnabled) {
            return LocalDate.now(clock)
        }
        return decodeManualNowDate(prefs[RuntimeContextPreferenceKeys.appDateManualNowTimestamp])
    }

    override suspend fun setManualNowEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[RuntimeContextPreferenceKeys.appDateUseManualNow] = enabled
        }
    }

    override suspend fun setManualNowDate(date: LocalDate) {
        val normalizedDate = normalizeToStartOfDay(date)
        val epochMillis = normalizedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        dataStore.edit { prefs ->
            prefs[RuntimeContextPreferenceKeys.appDateManualNowTimestamp] = epochMillis
        }
    }

    override fun normalizeToStartOfDay(date: LocalDate): LocalDate = date

    override fun formatShortDate(date: LocalDate): String = shortDateFormatter.withLocale(locale).format(date)

    private fun decodeManualNowDate(epochMillis: Long?): LocalDate {
        if (epochMillis == null || epochMillis <= 0L) {
            return LocalDate.now(clock)
        }

        val storedDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return normalizeToStartOfDay(storedDate)
    }
}
