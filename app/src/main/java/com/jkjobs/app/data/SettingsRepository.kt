package com.jkjobs.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "jk_jobs_settings")

/** Frequency options exposed in Settings - kept as a single source of truth so the UI and
 *  the WorkManager scheduling logic can never drift out of sync with each other. */
enum class AlertFrequency(val hours: Long, val label: String) {
    EVERY_3_HOURS(3, "Every 3 hours"),
    EVERY_12_HOURS(12, "Every 12 hours"),
    DAILY(24, "Daily");

    companion object {
        fun fromHours(hours: Long): AlertFrequency = entries.find { it.hours == hours } ?: EVERY_3_HOURS
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val INTERVAL_HOURS = longPreferencesKey("interval_hours")
    }

    val notificationsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val frequency: Flow<AlertFrequency> =
        context.settingsDataStore.data.map { AlertFrequency.fromHours(it[Keys.INTERVAL_HOURS] ?: 3L) }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setFrequency(freq: AlertFrequency) {
        context.settingsDataStore.edit { it[Keys.INTERVAL_HOURS] = freq.hours }
    }

    /** One-shot synchronous-style read for use inside JobCheckWorker, which needs the current
     *  value at the start of doWork() rather than an ongoing subscription. */
    suspend fun isNotificationsEnabledOnce(): Boolean = notificationsEnabled.first()
}
