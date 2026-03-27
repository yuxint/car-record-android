package com.tx.carrecord.core.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object RuntimeContextPreferenceKeys {
    val appDateUseManualNow: Preferences.Key<Boolean> =
        booleanPreferencesKey(name = "app_date_use_manual_now")
    val appDateManualNowTimestamp: Preferences.Key<Long> =
        longPreferencesKey(name = "app_date_manual_now_timestamp")
    val appliedCarId: Preferences.Key<String> = stringPreferencesKey(name = "applied_car_id")
    val rootTabNavigationTarget: Preferences.Key<String> =
        stringPreferencesKey(name = "root_tab_navigation_target")
    val rootTabNavigationNonce: Preferences.Key<String> =
        stringPreferencesKey(name = "root_tab_navigation_nonce")
}
