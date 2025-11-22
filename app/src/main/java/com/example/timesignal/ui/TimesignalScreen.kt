package com.example.timesignal.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import com.example.timesignal.R
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalState

@Composable
fun TimesignalScreen(
    state: TimesignalState,
    onToggleQuarter: (QuarterSlot, Boolean) -> Unit,
    onSelectDuration: (QuarterSlot, Int) -> Unit,
    onNavigateToExactAlarmSettings: () -> Unit,
) {
    Scaffold(timeText = { }) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!state.canScheduleExactAlarms) {
                item {
                    Card(onClick = onNavigateToExactAlarmSettings) {
                        Text(text = stringResource(R.string.permission_required_warning))
                    }
                }
            }

            items(QuarterSlot.values()) { slot ->
                val settings = state.quarters[slot] ?: QuarterSettings()
                QuarterCard(
                    slot = slot,
                    settings = settings,
                    durations = state.availableDurations,
                    onToggle = { onToggleQuarter(slot, it) },
                    onDurationSelected = { onSelectDuration(slot, it) },
                )
            }
        }
    }
}

@Composable
private fun QuarterCard(
    slot: QuarterSlot,
    settings: QuarterSettings,
    durations: List<Int>,
    onToggle: (Boolean) -> Unit,
    onDurationSelected: (Int) -> Unit,
) {
    Card(onClick = { onToggle(!settings.enabled) }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = slot.displayName, style = MaterialTheme.typography.title3)
                    if (settings.enabled) {
                        Text(text = "${settings.durationMs} ms", style = MaterialTheme.typography.caption1)
                    }
                }
                Switch(checked = settings.enabled, onCheckedChange = null)
            }
            if (settings.enabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    durations.forEach { duration ->
                        val colors = if (duration == settings.durationMs) {
                            ChipDefaults.secondaryChipColors()
                        } else {
                            ChipDefaults.chipColors()
                        }
                        CompactChip(
                            onClick = { onDurationSelected(duration) },
                            label = { Text(text = "$duration ms") },
                            enabled = true,
                            colors = colors,
                        )
                    }
                }
            }
        }
    }
}
