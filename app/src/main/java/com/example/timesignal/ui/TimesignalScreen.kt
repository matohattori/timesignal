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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import com.example.timesignal.R
import com.example.timesignal.domain.CustomVibrationPattern
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalState
import com.example.timesignal.domain.VibrationPatterns

private val LABEL_WIDTH = 60.dp

@Composable
fun TimesignalScreen(
    state: TimesignalState,
    isTestingVibration: Boolean,
    onToggleQuarter: (QuarterSlot, Boolean) -> Unit,
    onSelectVibrationPattern: (QuarterSlot, String) -> Unit,
    onUpdateCustomPattern: (QuarterSlot, CustomVibrationPattern) -> Unit,
    onTestVibration: (QuarterSlot) -> Unit,
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
                    isTestingDisabled = isTestingVibration,
                    onToggle = { onToggleQuarter(slot, it) },
                    onPatternSelected = { onSelectVibrationPattern(slot, it) },
                    onUpdateCustomPattern = { onUpdateCustomPattern(slot, it) },
                    onTestVibration = { onTestVibration(slot) },
                )
            }
        }
    }
}

@Composable
private fun QuarterCard(
    slot: QuarterSlot,
    settings: QuarterSettings,
    isTestingDisabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onPatternSelected: (String) -> Unit,
    onUpdateCustomPattern: (CustomVibrationPattern) -> Unit,
    onTestVibration: () -> Unit,
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
                        val pattern = settings.customPattern ?: VibrationPatterns.migratePresetToCustom(settings.vibrationPatternId)
                        Text(
                            text = formatPatternDescription(pattern),
                            style = MaterialTheme.typography.caption1
                        )
                    }
                }
                Switch(checked = settings.enabled, onCheckedChange = null)
            }
            
            if (settings.enabled) {
                Spacer(modifier = Modifier.height(6.dp))
                
                // Show explanation text
                Text(
                    text = stringResource(R.string.vibration_explanation),
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Get current pattern or migrate from preset
                val currentPattern = settings.customPattern ?: VibrationPatterns.migratePresetToCustom(settings.vibrationPatternId)
                
                CustomPatternEditor(
                    pattern = currentPattern,
                    onPatternChange = onUpdateCustomPattern
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = onTestVibration,
                    enabled = !isTestingDisabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(text = stringResource(R.string.test_button))
                }
            }
        }
    }
}

@Composable
private fun CustomPatternEditor(
    pattern: CustomVibrationPattern,
    onPatternChange: (CustomVibrationPattern) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Vibration 1 (required, no disable option)
        DurationSelector(
            label = stringResource(R.string.vib1_label),
            value = pattern.vib1,
            hasDisabledOption = false,
            enabled = true,
            onValueChange = { newValue ->
                if (newValue != null) {
                    onPatternChange(pattern.copy(vib1 = newValue))
                }
            }
        )
        
        // Pause 1 (optional)
        val pause1Enabled = pattern.pause1 != null
        DurationSelector(
            label = stringResource(R.string.pause1_label),
            value = pattern.pause1,
            hasDisabledOption = true,
            enabled = true,
            onValueChange = { newValue ->
                onPatternChange(pattern.copy(
                    pause1 = newValue,
                    vib2 = if (newValue == null) null else pattern.vib2,
                    pause2 = if (newValue == null) null else pattern.pause2,
                    vib3 = if (newValue == null) null else pattern.vib3
                ))
            }
        )
        
        // Vibration 2 (optional, disabled if pause1 is null)
        DurationSelector(
            label = stringResource(R.string.vib2_label),
            value = pattern.vib2,
            hasDisabledOption = true,
            enabled = pause1Enabled,
            onValueChange = { newValue ->
                onPatternChange(pattern.copy(
                    vib2 = newValue,
                    pause2 = if (newValue == null) null else pattern.pause2,
                    vib3 = if (newValue == null) null else pattern.vib3
                ))
            }
        )
        
        // Pause 2 (optional, disabled if vib2 is null)
        val vib2Enabled = pause1Enabled && pattern.vib2 != null
        DurationSelector(
            label = stringResource(R.string.pause2_label),
            value = pattern.pause2,
            hasDisabledOption = true,
            enabled = vib2Enabled,
            onValueChange = { newValue ->
                onPatternChange(pattern.copy(
                    pause2 = newValue,
                    vib3 = if (newValue == null) null else pattern.vib3
                ))
            }
        )
        
        // Vibration 3 (optional, disabled if pause2 is null)
        val pause2Enabled = vib2Enabled && pattern.pause2 != null
        DurationSelector(
            label = stringResource(R.string.vib3_label),
            value = pattern.vib3,
            hasDisabledOption = true,
            enabled = pause2Enabled,
            onValueChange = { newValue ->
                onPatternChange(pattern.copy(vib3 = newValue))
            }
        )
    }
}

@Composable
private fun DurationSelector(
    label: String,
    value: Int?,
    hasDisabledOption: Boolean,
    enabled: Boolean,
    onValueChange: (Int?) -> Unit
) {
    val selectedChipColors = remember { ChipDefaults.secondaryChipColors() }
    val defaultChipColors = remember { ChipDefaults.chipColors() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption1,
            modifier = Modifier.width(LABEL_WIDTH)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Add disabled option if applicable
            if (hasDisabledOption) {
                val isSelected = value == null
                CompactChip(
                    onClick = { if (enabled) onValueChange(null) },
                    label = { Text(stringResource(R.string.disabled_option)) },
                    enabled = enabled,
                    colors = if (isSelected) selectedChipColors else defaultChipColors
                )
            }
            
            // Add duration options (100-900 ms in 100 ms increments)
            for (duration in 100..900 step 100) {
                val isSelected = value == duration
                CompactChip(
                    onClick = { if (enabled) onValueChange(duration) },
                    label = { Text("${duration}") },
                    enabled = enabled,
                    colors = if (isSelected) selectedChipColors else defaultChipColors
                )
            }
        }
    }
}

private fun formatPatternDescription(pattern: CustomVibrationPattern): String {
    val parts = mutableListOf<String>()
    parts.add("振${pattern.vib1}")
    
    if (pattern.pause1 != null) {
        parts.add("停${pattern.pause1}")
        
        if (pattern.vib2 != null) {
            parts.add("振${pattern.vib2}")
            
            if (pattern.pause2 != null) {
                parts.add("停${pattern.pause2}")
                
                if (pattern.vib3 != null) {
                    parts.add("振${pattern.vib3}")
                }
            }
        }
    }
    
    return parts.joinToString("-") + "ms"
}
