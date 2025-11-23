# Custom Vibration Pattern Implementation Notes

## Overview
This document describes the implementation details of the custom vibration pattern feature that replaces the preset-based system.

## Architecture Changes

### Data Layer

#### 1. CustomVibrationPattern Data Class
Located in: `TimesignalModels.kt`

```kotlin
data class CustomVibrationPattern(
    val vib1: Int = 200,        // Required, 100-900ms
    val pause1: Int? = null,    // Optional, null = disabled
    val vib2: Int? = null,      // Optional, null = disabled
    val pause2: Int? = null,    // Optional, null = disabled
    val vib3: Int? = null       // Optional, null = disabled
)
```

Key features:
- `vib1` is always required (non-null)
- Other fields use `Int?` where `null` means "disabled"
- Includes validation method `isValid()` to ensure proper termination

#### 2. VibrationPatterns Object Extensions
Located in: `TimesignalModels.kt`

New methods:
- `migratePresetToCustom(patternId: String)`: Converts legacy presets to CustomVibrationPattern
- `getCustomVibrationEffect(CustomVibrationPattern)`: Creates VibrationEffect from custom pattern
- `getCustomPatternDuration(CustomVibrationPattern)`: Calculates total duration

#### 3. Data Persistence
Located in: `TimesignalPreferencesRepository.kt`

Storage strategy:
- Each field stored as separate string preference
- Keys: `quarter_{SLOT}_custom_vib1`, `quarter_{SLOT}_custom_pause1`, etc.
- Null values stored by removing the preference key
- Legacy `vibrationPatternId` field retained for backwards compatibility

Methods added:
- `loadCustomPattern()`: Reads custom pattern from preferences
- `saveCustomPattern()`: Writes custom pattern to preferences
- `setCustomVibrationPattern()`: Public API for saving

### Domain Layer

#### 1. TimesignalRepository Interface
Located in: `TimesignalRepository.kt`

New method:
```kotlin
suspend fun setCustomVibrationPattern(slot: QuarterSlot, customPattern: CustomVibrationPattern)
```

#### 2. TimesignalVibrator
Located in: `TimesignalVibrator.kt`

New method:
```kotlin
fun vibrateCustom(customPattern: CustomVibrationPattern)
```

Builds vibration timing array dynamically based on non-null fields.

#### 3. TimesignalService
Located in: `TimesignalService.kt`

Updated to check for custom pattern before falling back to preset:
```kotlin
if (slotSettings.customPattern != null) {
    vibrator.vibrateCustom(slotSettings.customPattern)
} else {
    vibrator.vibrate(slotSettings.vibrationPatternId)
}
```

### UI Layer

#### 1. TimesignalScreen
Located in: `ui/TimesignalScreen.kt`

Major rewrite with:
- `CustomPatternEditor`: Composable for editing 5-field pattern
- `DurationSelector`: Reusable component for each field (100-900ms + disabled option)
- Cascading disable logic based on null selections
- Test button integration per quarter slot

#### 2. TimesignalViewModel
Located in: `ui/TimesignalViewModel.kt`

New features:
- `isTestingVibration` StateFlow to track test state
- `setCustomVibrationPattern()`: Updates custom pattern
- `testVibration()`: Triggers test vibration and manages button state
- Auto-migration when switching on a quarter (uses preset → custom migration)

#### 3. MainActivity
Located in: `MainActivity.kt`

Updated to pass new callbacks:
- `onUpdateCustomPattern`
- `onTestVibration`
- `isTestingVibration` state

## UI Behavior

### Cascading Disable Logic
Implemented in `CustomPatternEditor`:

1. If `pause1 == null`: Disable vib2, pause2, vib3
2. If `vib2 == null`: Disable pause2, vib3
3. If `pause2 == null`: Disable vib3

This ensures patterns are properly terminated at any point.

### Test Button Behavior
- Single global `isTestingVibration` state
- All test buttons disabled during any test
- Test duration calculated from pattern
- Automatic re-enable after completion

### Duration Selector
- Horizontal scrollable list of chips
- First chip: "-（無効）" if field supports null
- Remaining chips: 100, 200, 300, ..., 900 (ms)
- Selected chip highlighted with secondary color
- Disabled chips grayed out

## Migration Strategy

### Preset to Custom Mapping
```
SHORT_1 -> CustomVibrationPattern(vib1=100, pause1=null, ...)
SHORT_2 -> CustomVibrationPattern(vib1=100, pause1=200, vib2=100, pause2=null, vib3=null)
LONG_1  -> CustomVibrationPattern(vib1=400, pause1=null, ...)
LONG_2  -> CustomVibrationPattern(vib1=100, pause1=200, vib2=400, pause2=null, vib3=null)
```

### Data Loading Priority
1. Check for `customPattern` in preferences
2. If null, check `vibrationPatternId` and auto-migrate
3. Display migrated pattern in UI
4. Save as custom pattern on first edit

## Vibration Pattern Execution

### Legacy (Preset)
```kotlin
timings = [1, vib_duration]
```

### Custom Pattern
```kotlin
timings = [1]  // Initial 1ms delay (bug workaround)
timings += vib1
if (pause1 != null) {
    timings += pause1
    if (vib2 != null) {
        timings += vib2
        if (pause2 != null) {
            timings += pause2
            if (vib3 != null) {
                timings += vib3
            }
        }
    }
}
```

Uses `VibrationEffect.createWaveform(timings, -1)` for one-shot playback.

## Testing Checklist

- [ ] Custom pattern saves and loads correctly
- [ ] Preset migration works for all 4 presets
- [ ] Cascading disable logic works correctly
- [ ] Test button triggers correct pattern
- [ ] Test button disables during vibration
- [ ] Actual alarm triggers custom pattern
- [ ] UI displays pattern summary correctly
- [ ] Null fields handled correctly in execution
- [ ] Migration preserves enabled state
- [ ] Data persists across app restarts

## Known Limitations

1. Android SDK required for actual testing
2. Wear OS emulator needed for UI verification
3. Build system requires proper configuration
4. No automated tests yet (would require Android test framework)

## Future Enhancements

Possible improvements:
- Preset templates for common patterns
- Import/export patterns
- Per-hour custom patterns
- Visual waveform preview
- Intensity/amplitude control (if hardware supports)
