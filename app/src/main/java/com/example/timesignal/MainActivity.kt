package com.example.timesignal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.ui.TimesignalScreen
import com.example.timesignal.ui.TimesignalTheme
import com.example.timesignal.ui.TimesignalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TimesignalApplication
        val factory = TimesignalViewModel.Factory(
            repository = app.container.repository,
            scheduler = app.container.scheduler,
            vibrator = app.container.vibrator,
        )
        setContent {
            TimesignalTheme {
                val viewModel: TimesignalViewModel = viewModel(factory = factory)
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                TimesignalScreen(
                    state = uiState.value,
                    onToggleQuarter = { slot: QuarterSlot, enabled: Boolean ->
                        viewModel.setQuarterEnabled(slot, enabled)
                    },
                    onSelectDuration = { slot: QuarterSlot, duration ->
                        viewModel.setQuarterDuration(slot, duration)
                    },
                    onQuietHoursEnabled = viewModel::setQuietHoursEnabled,
                    onQuietHoursStart = viewModel::setQuietHoursStart,
                    onQuietHoursEnd = viewModel::setQuietHoursEnd,
                )
            }
        }
    }
}
