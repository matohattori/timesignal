package com.example.timesignal.domain

import kotlinx.coroutines.flow.Flow

interface TimesignalRepository {
    val state: Flow<TimesignalState>

    suspend fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean)
    suspend fun setQuarterDuration(slot: QuarterSlot, durationMs: Int)
    suspend fun getLatestState(): TimesignalState
}
