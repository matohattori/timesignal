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
import com.example.timesignal.domain.VibrationPattern

@Composable
fun TimesignalScreen(
    state: TimesignalState,
    onToggleQuarter: (QuarterSlot, Boolean) -> Unit,
    onSelectPattern: (QuarterSlot, String) -> Unit,
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
                    patterns = state.availablePatterns,
                    onToggle = { onToggleQuarter(slot, it) },
                    onPatternSelected = { onSelectPattern(slot, it) },
                )
            }
        }
    }
}

@Composable
private fun QuarterCard(
    slot: QuarterSlot,
    settings: QuarterSettings,
    patterns: List<VibrationPattern>,
    onToggle: (Boolean) -> Unit,
    onPatternSelected: (String) -> Unit,
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
                        val selectedPattern = patterns.find { it.id == settings.patternId }
                        if (selectedPattern != null) {
                            Text(text = selectedPattern.label, style = MaterialTheme.typography.caption1)
                        }
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
                    patterns.forEach { pattern ->
                        val colors = if (pattern.id == settings.patternId) {
                            ChipDefaults.secondaryChipColors()
                        } else {
                            ChipDefaults.chipColors()
                        }
                        CompactChip(
                            onClick = { onPatternSelected(pattern.id) },
                            label = { Text(text = pattern.label) },
                            enabled = true,
                            colors = colors,
                        )
                    }
                }
            }
        }
    }
}
