package com.example.timesignal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalRepository
import com.example.timesignal.domain.TimesignalState
import com.example.timesignal.domain.VibrationPatterns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TimesignalPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : TimesignalRepository {

    override val state: Flow<TimesignalState> = dataStore.data.map { preferences ->
        val quarters = QuarterSlot.values().associateWith { slot ->
            QuarterSettings(
                enabled = preferences[enabledKey(slot)] ?: false,
                patternId = preferences[patternKey(slot)] ?: VibrationPatterns.default.id,
            )
        }

        TimesignalState(
            quarters = quarters,
        )
    }

    override suspend fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[enabledKey(slot)] = enabled
        }
    }

    override suspend fun setQuarterPattern(slot: QuarterSlot, patternId: String) {
        val targetPattern = VibrationPatterns.findById(patternId)
        dataStore.edit { prefs ->
            prefs[patternKey(slot)] = targetPattern.id
        }
    }

    override suspend fun getLatestState(): TimesignalState = state.first()

    private fun enabledKey(slot: QuarterSlot) = booleanPreferencesKey("quarter_${slot.name}_enabled")
    private fun patternKey(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_pattern")
}
