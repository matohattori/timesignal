package com.example.timesignal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalRepository
import com.example.timesignal.domain.TimesignalState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TimesignalPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : TimesignalRepository {

    private val durationOptions = listOf(200, 400, 600, 800, 1200)

    override val state: Flow<TimesignalState> = dataStore.data.map { preferences ->
        val quarters = QuarterSlot.values().associateWith { slot ->
            QuarterSettings(
                enabled = preferences[enabledKey(slot)] ?: false,
                durationMs = preferences[durationKey(slot)] ?: durationOptions[1],
            )
        }

        TimesignalState(
            quarters = quarters,
            availableDurations = durationOptions,
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

    override suspend fun getLatestState(): TimesignalState = state.first()

    private fun enabledKey(slot: QuarterSlot) = booleanPreferencesKey("quarter_${slot.name}_enabled")
    private fun durationKey(slot: QuarterSlot) = intPreferencesKey("quarter_${slot.name}_duration")
}
