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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Bedtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimePicker
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.picker.rememberTimePickerState
import com.example.timesignal.R
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalState
import java.time.LocalTime

@Composable
@OptIn(ExperimentalWearMaterialApi::class)
fun TimesignalScreen(
    state: TimesignalState,
    onToggleQuarter: (QuarterSlot, Boolean) -> Unit,
    onSelectDuration: (QuarterSlot, Int) -> Unit,
    onQuietHoursEnabled: (Boolean) -> Unit,
    onQuietHoursStart: (LocalTime) -> Unit,
    onQuietHoursEnd: (LocalTime) -> Unit,
) {
    var editingTarget by remember { mutableStateOf<QuietHoursTarget?>(null) }
    if (editingTarget != null) {
        val targetTime = when (editingTarget) {
            QuietHoursTarget.START -> state.quietHours.start
            QuietHoursTarget.END -> state.quietHours.end
            null -> LocalTime.MIDNIGHT
        }
        val pickerState = rememberTimePickerState(
            initialHour = targetTime.hour,
            initialMinute = targetTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { editingTarget = null },
            title = {
                Text(text = if (editingTarget == QuietHoursTarget.START) stringResource(R.string.quiet_hours_start) else stringResource(R.string.quiet_hours_end))
            },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        when (editingTarget) {
                            QuietHoursTarget.START -> onQuietHoursStart(newTime)
                            QuietHoursTarget.END -> onQuietHoursEnd(newTime)
                            null -> Unit
                        }
                        editingTarget = null
                    }
                ) { Text(text = "決定") }
            },
            dismissButton = {
                TextButton(onClick = { editingTarget = null }) { Text(text = "キャンセル") }
            },
        )
    }

    Scaffold(timeText = { }) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isWithinQuietHours) {
                item {
                    Card(onClick = { }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(id = R.string.quiet_hours_active))
                        }
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

            item {
                QuietHoursCard(
                    enabled = state.quietHours.enabled,
                    start = state.quietHours.start,
                    end = state.quietHours.end,
                    onEnabledChange = onQuietHoursEnabled,
                    onSelectStart = { editingTarget = QuietHoursTarget.START },
                    onSelectEnd = { editingTarget = QuietHoursTarget.END },
                )
            }
        }
    }
}

private enum class QuietHoursTarget { START, END }

@Composable
private fun QuarterCard(
    slot: QuarterSlot,
    settings: QuarterSettings,
    durations: List<Int>,
    onToggle: (Boolean) -> Unit,
    onDurationSelected: (Int) -> Unit,
) {
    Card(onClick = { if (!settings.enabled) onToggle(true) }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = slot.displayName, style = MaterialTheme.typography.title3)
                    Text(text = "${settings.durationMs} ms", style = MaterialTheme.typography.caption1)
                }
                Switch(checked = settings.enabled, onCheckedChange = onToggle)
            }
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

@Composable
private fun QuietHoursCard(
    enabled: Boolean,
    start: LocalTime,
    end: LocalTime,
    onEnabledChange: (Boolean) -> Unit,
    onSelectStart: () -> Unit,
    onSelectEnd: () -> Unit,
) {
    Card(onClick = { onEnabledChange(!enabled) }) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = stringResource(id = R.string.quiet_hours_title), fontWeight = FontWeight.Bold)
            ToggleChip(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                label = { Text(text = stringResource(id = R.string.quiet_hours_enabled)) },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Chip(
                    onClick = onSelectStart,
                    label = { Text(text = "${stringResource(id = R.string.quiet_hours_start)} ${formatTime(start)}") },
                )
                Chip(
                    onClick = onSelectEnd,
                    label = { Text(text = "${stringResource(id = R.string.quiet_hours_end)} ${formatTime(end)}") },
                )
            }
        }
    }
}

private fun formatTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)
