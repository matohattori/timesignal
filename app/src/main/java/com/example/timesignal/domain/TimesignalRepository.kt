package com.example.timesignal.domain

import kotlinx.coroutines.flow.Flow

interface TimesignalRepository {
    val state: Flow<TimesignalState>

    suspend fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean)
    suspend fun setVibrationPattern(slot: QuarterSlot, patternId: String)
    suspend fun setCustomVibrationPattern(slot: QuarterSlot, customPattern: CustomVibrationPattern)
    suspend fun getLatestState(): TimesignalState
}
