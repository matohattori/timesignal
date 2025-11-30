package com.example.timesignal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.example.timesignal.R
import com.example.timesignal.domain.CustomVibrationPattern
import com.example.timesignal.domain.QuarterSettings
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalState
import com.example.timesignal.domain.VibrationPatterns
import kotlinx.coroutines.launch

private val LABEL_WIDTH = 60.dp

@OptIn(ExperimentalFoundationApi::class)
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
    val slots = QuarterSlot.entries
    val pagerState = rememberPagerState(pageCount = { slots.size })

    Scaffold(timeText = { }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission warning at the top if needed
            if (!state.canScheduleExactAlarms) {
                Card(
                    onClick = onNavigateToExactAlarmSettings,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = stringResource(R.string.permission_required_warning))
                }
            }

            // Horizontal pager for each quarter slot
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val slot = slots[pageIndex]
                val settings = state.quarters[slot] ?: QuarterSettings()
                QuarterPage(
                    slot = slot,
                    settings = settings,
                    isTestingDisabled = isTestingVibration,
                    onToggle = { onToggleQuarter(slot, it) },
                    onPatternSelected = { onSelectVibrationPattern(slot, it) },
                    onUpdateCustomPattern = { onUpdateCustomPattern(slot, it) },
                    onTestVibration = { onTestVibration(slot) },
                )
            }

            // Page indicator
            PageIndicator(
                pageCount = slots.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun QuarterPage(
    slot: QuarterSlot,
    settings: QuarterSettings,
    isTestingDisabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onPatternSelected: (String) -> Unit,
    onUpdateCustomPattern: (CustomVibrationPattern) -> Unit,
    onTestVibration: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Request focus to enable rotary input
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    listState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Page title centered at top with margins for circular screen
            Text(
                text = slot.displayName,
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        item {
            // "振動ON" toggle switch below title
            ToggleChip(
                checked = settings.enabled,
                onCheckedChange = { onToggle(it) },
                label = {
                    Text(
                        text = stringResource(R.string.vibration_on_label),
                        style = MaterialTheme.typography.body1
                    )
                },
                toggleControl = {
                    // Switch with null onCheckedChange is the standard pattern for ToggleChip
                    // The ToggleChip handles the toggle action and passes it to onCheckedChange
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = ToggleChipDefaults.toggleChipColors()
            )
        }

        // Settings UI visible only when enabled
        if (settings.enabled) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                // Custom pattern editor
                val currentPattern = settings.customPattern ?: VibrationPatterns.migratePresetToCustom(settings.vibrationPatternId)
                CustomPatternEditor(
                    pattern = currentPattern,
                    onPatternChange = onUpdateCustomPattern
                )
            }

            item {
                // Test button with vibration icon instead of text
                Button(
                    onClick = onTestVibration,
                    enabled = !isTestingDisabled,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(48.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = stringResource(R.string.test_vibration_description),
                        modifier = Modifier.size(24.dp)
                    )
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

/**
 * Duration selector that shows current value as text.
 * When tapped, opens a dialog with vertical scroll picker for selection.
 */
@Composable
private fun DurationSelector(
    label: String,
    value: Int?,
    hasDisabledOption: Boolean,
    enabled: Boolean,
    onValueChange: (Int?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption1,
            color = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(LABEL_WIDTH)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Display current value as tappable text
        val displayText = if (value == null) {
            stringResource(R.string.disabled_option)
        } else {
            stringResource(R.string.duration_format, value)
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (enabled) {
                        MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                    }
                )
                .clickable(enabled = enabled) { showDialog = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.body1,
                color = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Duration picker dialog
    if (showDialog) {
        DurationPickerDialog(
            currentValue = value,
            hasDisabledOption = hasDisabledOption,
            onValueSelected = { newValue ->
                onValueChange(newValue)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog with vertical scroll picker for duration selection.
 * Includes a confirmation button at the bottom.
 * Supports rotary input (crown rotation) for scrolling.
 */
@Composable
private fun DurationPickerDialog(
    currentValue: Int?,
    hasDisabledOption: Boolean,
    onValueSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    // Build list of options
    val options = buildList {
        if (hasDisabledOption) {
            add(null) // Disabled option
        }
        addAll(listOf(50, 100, 200, 300, 500))
    }
    
    // Find initial index
    val initialIndex = options.indexOf(currentValue).coerceAtLeast(0)
    var selectedIndex by remember { mutableIntStateOf(initialIndex) }
    val listState = rememberScalingLazyListState(initialCenterItemIndex = initialIndex)
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Request focus to enable rotary input
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = stringResource(R.string.select_duration_title),
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                
                // Vertical scroll picker with rotary input support
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onRotaryScrollEvent { event ->
                            coroutineScope.launch {
                                listState.scrollBy(event.verticalScrollPixels)
                            }
                            true
                        }
                        .focusRequester(focusRequester)
                        .focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(options.size) { index ->
                        val option = options[index]
                        val isSelected = index == selectedIndex
                        
                        val displayText = if (option == null) {
                            stringResource(R.string.disabled_option)
                        } else {
                            stringResource(R.string.duration_format, option)
                        }
                        
                        Card(
                            onClick = { selectedIndex = index },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            backgroundPainter = if (isSelected) {
                                CardDefaults.cardBackgroundPainter(
                                    startBackgroundColor = MaterialTheme.colors.primary,
                                    endBackgroundColor = MaterialTheme.colors.primary
                                )
                            } else {
                                CardDefaults.cardBackgroundPainter()
                            }
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.body1,
                                color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Confirmation button at the bottom
                val confirmButtonDescription = stringResource(R.string.confirm_selection)
                Button(
                    onClick = { onValueSelected(options[selectedIndex]) },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(48.dp)
                        .semantics { contentDescription = confirmButtonDescription },
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text(
                        text = "✔",
                        style = MaterialTheme.typography.title2
                    )
                }
            }
        }
    }
}
