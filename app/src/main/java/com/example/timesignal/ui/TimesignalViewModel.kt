package com.example.timesignal.ui

import android.app.AlarmManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timesignal.TimesignalVibrator
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalRepository
import com.example.timesignal.domain.TimesignalScheduler
import com.example.timesignal.domain.TimesignalState
import com.example.timesignal.domain.VibrationPatterns
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimesignalViewModel(
    private val repository: TimesignalRepository,
    private val scheduler: TimesignalScheduler,
    private val vibrator: TimesignalVibrator,
    private val alarmManager: AlarmManager,
) : ViewModel() {

    val uiState: StateFlow<TimesignalState> = repository.state
        .map { state ->
            val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            state.copy(canScheduleExactAlarms = canSchedule)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimesignalState())

    fun setQuarterEnabled(slot: QuarterSlot, enabled: Boolean) {
        viewModelScope.launch {
            val previous = repository.getLatestState()
            repository.setQuarterEnabled(slot, enabled)
            val latest = repository.getLatestState()
            scheduler.reschedule(latest)

            val wasEnabled = previous.quarters[slot]?.enabled == true
            if (!wasEnabled && enabled) {
                val patternId = latest.quarters[slot]?.patternId ?: return@launch
                val pattern = VibrationPatterns.findById(patternId)
                vibrator.vibrate(pattern)
            }
        }
    }

    fun setQuarterPattern(slot: QuarterSlot, patternId: String) {
        viewModelScope.launch {
            repository.setQuarterPattern(slot, patternId)
            val latest = repository.getLatestState()
            scheduler.reschedule(latest)
            val pattern = VibrationPatterns.findById(patternId)
            vibrator.vibrate(pattern)
        }
    }

    class Factory(
        private val repository: TimesignalRepository,
        private val scheduler: TimesignalScheduler,
        private val vibrator: TimesignalVibrator,
        private val alarmManager: AlarmManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimesignalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TimesignalViewModel(repository, scheduler, vibrator, alarmManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
