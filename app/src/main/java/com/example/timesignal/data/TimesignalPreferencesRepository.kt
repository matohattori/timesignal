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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TimesignalPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : TimesignalRepository {

    override val state: Flow<TimesignalState> = dataStore.data.map { preferences ->
        val quarters = QuarterSlot.values().associateWith { slot ->
            val customPattern = loadCustomPattern(preferences, slot)
            QuarterSettings(
                enabled = preferences[enabledKey(slot)] ?: false,
                vibrationPatternId = preferences[patternKey(slot)] ?: "SHORT_1",
                customPattern = customPattern
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

    override suspend fun setVibrationPattern(slot: QuarterSlot, patternId: String) {
        dataStore.edit { prefs ->
            prefs[patternKey(slot)] = patternId
        }
    }

    override suspend fun setCustomVibrationPattern(slot: QuarterSlot, customPattern: CustomVibrationPattern) {
        dataStore.edit { prefs ->
            saveCustomPattern(prefs, slot, customPattern)
        }
    }

    override suspend fun getLatestState(): TimesignalState = state.first()

    private fun enabledKey(slot: QuarterSlot) = booleanPreferencesKey("quarter_${slot.name}_enabled")
    private fun patternKey(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_pattern")
    
    private fun customVib1Key(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_custom_vib1")
    private fun customPause1Key(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_custom_pause1")
    private fun customVib2Key(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_custom_vib2")
    private fun customPause2Key(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_custom_pause2")
    private fun customVib3Key(slot: QuarterSlot) = stringPreferencesKey("quarter_${slot.name}_custom_vib3")

    private fun loadCustomPattern(preferences: Preferences, slot: QuarterSlot): CustomVibrationPattern? {
        val vib1Str = preferences[customVib1Key(slot)]
        return if (vib1Str != null) {
            CustomVibrationPattern(
                vib1 = vib1Str.toIntOrNull() ?: 200,
                pause1 = preferences[customPause1Key(slot)]?.toIntOrNull(),
                vib2 = preferences[customVib2Key(slot)]?.toIntOrNull(),
                pause2 = preferences[customPause2Key(slot)]?.toIntOrNull(),
                vib3 = preferences[customVib3Key(slot)]?.toIntOrNull()
            )
        } else {
            null
        }
    }

    private fun saveCustomPattern(prefs: androidx.datastore.preferences.core.MutablePreferences, slot: QuarterSlot, customPattern: CustomVibrationPattern) {
        prefs[customVib1Key(slot)] = customPattern.vib1.toString()
        
        if (customPattern.pause1 != null) {
            prefs[customPause1Key(slot)] = customPattern.pause1.toString()
        } else {
            prefs.remove(customPause1Key(slot))
        }
        
        if (customPattern.vib2 != null) {
            prefs[customVib2Key(slot)] = customPattern.vib2.toString()
        } else {
            prefs.remove(customVib2Key(slot))
        }
        
        if (customPattern.pause2 != null) {
            prefs[customPause2Key(slot)] = customPattern.pause2.toString()
        } else {
            prefs.remove(customPause2Key(slot))
        }
        
        if (customPattern.vib3 != null) {
            prefs[customVib3Key(slot)] = customPattern.vib3.toString()
        } else {
            prefs.remove(customVib3Key(slot))
        }
    }
}
