package com.example.timesignal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.QuietHoursConfig
import com.example.timesignal.domain.TimesignalRepository
import com.example.timesignal.domain.TimesignalState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

class TimesignalPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : TimesignalRepository {

    private val durationOptions = listOf(200, 400, 600, 800, 1200)

    override val state: Flow<TimesignalState> = dataStore.data.map { preferences ->
        val quietHours = QuietHoursConfig(
            enabled = preferences[QUIET_ENABLED_KEY] ?: false,
            start = LocalTime.ofSecondOfDay((preferences[QUIET_START_KEY] ?: 23 * 60) * 60L),
            end = LocalTime.ofSecondOfDay((preferences[QUIET_END_KEY] ?: 7 * 60) * 60L),
        )

        val quarters = QuarterSlot.values().associateWith { slot ->
            QuarterSettings(
                enabled = preferences[enabledKey(slot)] ?: false,
                durationMs = preferences[durationKey(slot)] ?: durationOptions[1],
            )
        }

        TimesignalState(
            quarters = quarters,
            quietHours = quietHours,
            availableDurations = durationOptions,
            isWithinQuietHours = false,
        )
    }

    override suspend fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[enabledKey(slot)] = enabled
        }
    }

    override suspend fun setQuarterDuration(slot: QuarterSlot, durationMs: Int) {
        dataStore.edit { prefs ->
            prefs[durationKey(slot)] = durationMs
        }
    }

    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[QUIET_ENABLED_KEY] = enabled
        }
    }

    override suspend fun setQuietHoursStart(time: LocalTime) {
        dataStore.edit { prefs ->
            prefs[QUIET_START_KEY] = time.hour * 60 + time.minute
        }
    }

    override suspend fun setQuietHoursEnd(time: LocalTime) {
        dataStore.edit { prefs ->
            prefs[QUIET_END_KEY] = time.hour * 60 + time.minute
        }
    }

    override suspend fun getLatestState(): TimesignalState = state.first()

    private fun enabledKey(slot: QuarterSlot) = booleanPreferencesKey("quarter_${slot.name}_enabled")
    private fun durationKey(slot: QuarterSlot) = intPreferencesKey("quarter_${slot.name}_duration")

    companion object {
        private val QUIET_ENABLED_KEY = booleanPreferencesKey("quiet_enabled")
        private val QUIET_START_KEY = intPreferencesKey("quiet_start")
        private val QUIET_END_KEY = intPreferencesKey("quiet_end")
    }
}
