package com.example.timesignal.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface TimesignalRepository {
    val state: Flow<TimesignalState>

    suspend fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean)
    suspend fun setQuarterDuration(slot: QuarterSlot, durationMs: Int)
    suspend fun setQuietHoursEnabled(enabled: Boolean)
    suspend fun setQuietHoursStart(time: LocalTime)
    suspend fun setQuietHoursEnd(time: LocalTime)
    suspend fun getLatestState(): TimesignalState
}
