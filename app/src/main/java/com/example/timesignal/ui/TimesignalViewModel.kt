package com.example.timesignal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timesignal.TimesignalVibrator
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.QuietHoursEvaluator
import com.example.timesignal.domain.TimesignalRepository
import com.example.timesignal.domain.TimesignalScheduler
import com.example.timesignal.domain.TimesignalState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class TimesignalViewModel(
    private val repository: TimesignalRepository,
    private val scheduler: TimesignalScheduler,
    private val vibrator: TimesignalVibrator,
) : ViewModel() {

    val uiState: StateFlow<TimesignalState> = repository.state
        .map { state ->
            state.copy(isWithinQuietHours = QuietHoursEvaluator.isWithinQuietHours(state.quietHours))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimesignalState())

    fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean) {
        viewModelScope.launch {
            repository.setQuarterEnabled(slot, enabled)
            val latest = repository.getLatestState()
            scheduler.reschedule(latest)
            if (enabled) {
                val duration = latest.quarters[slot]?.durationMs ?: return@launch
                vibrator.vibrate(duration)
            }
        }
    }

    fun setQuarterDuration(slot: QuarterSlot, durationMs: Int) {
        viewModelScope.launch {
            repository.setQuarterDuration(slot, durationMs)
            val latest = repository.getLatestState()
            scheduler.reschedule(latest)
            vibrator.vibrate(durationMs)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setQuietHoursEnabled(enabled)
        }
    }

    fun setQuietHoursStart(time: LocalTime) {
        viewModelScope.launch {
            repository.setQuietHoursStart(time)
        }
    }

    fun setQuietHoursEnd(time: LocalTime) {
        viewModelScope.launch {
            repository.setQuietHoursEnd(time)
        }
    }

    class Factory(
        private val repository: TimesignalRepository,
        private val scheduler: TimesignalScheduler,
        private val vibrator: TimesignalVibrator,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimesignalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TimesignalViewModel(repository, scheduler, vibrator) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
