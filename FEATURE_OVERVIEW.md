# Custom Vibration Pattern Feature Overview

## Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Quarter Slot Card                        │
├─────────────────────────────────────────────────────────────┤
│  毎時0分                                        [ON/OFF]    │
│  振100-停200-振100ms                                        │
│                                                             │
│  「-」を選択するとこの時点で振動シーケンスが終了します       │
│                                                             │
│  振動1: [100] [200] [300] [400] [500] [600] [700] [800]... │
│  停止1: [-] [100] [200] [300] [400] [500] [600] [700]...   │
│  振動2: [-] [100] [200] [300] [400] [500] [600] [700]...   │
│  停止2: [-] [100] [200] [300] [400] [500] [600] [700]...   │
│  振動3: [-] [100] [200] [300] [400] [500] [600] [700]...   │
│                                                             │
│                        [テスト]                              │
└─────────────────────────────────────────────────────────────┘
```

## Cascading Disable Logic

### Example 1: Pause1 set to "-"
```
振動1: [200] ✓ (enabled)
停止1: [-] ✓ (selected)
     ↓
振動2: [DISABLED] ← grayed out
停止2: [DISABLED] ← grayed out  
振動3: [DISABLED] ← grayed out

Result: Pattern plays "vibrate 200ms" and stops
```

### Example 2: Vib2 set to "-"
```
振動1: [100] ✓ (enabled)
停止1: [200] ✓ (enabled)
振動2: [-] ✓ (selected)
     ↓
停止2: [DISABLED] ← grayed out
振動3: [DISABLED] ← grayed out

Result: Pattern plays "vibrate 100ms → pause 200ms" and stops
```

### Example 3: All fields enabled
```
振動1: [100] ✓
停止1: [200] ✓
振動2: [300] ✓
停止2: [200] ✓
振動3: [100] ✓

Result: Pattern plays "100ms → 200ms pause → 300ms → 200ms pause → 100ms"
```

## Data Structure

```kotlin
CustomVibrationPattern(
    vib1: Int,       // Required: 100-900ms
    pause1: Int?,    // Optional: 100-900ms or null
    vib2: Int?,      // Optional: 100-900ms or null
    pause2: Int?,    // Optional: 100-900ms or null
    vib3: Int?       // Optional: 100-900ms or null
)
```

## State Flow Diagram

```
                    ┌─────────────┐
                    │  App Start  │
                    └──────┬──────┘
                           │
                           ↓
              ┌────────────────────────┐
              │ Check for custom       │
              │ pattern in DataStore   │
              └────────┬───────────────┘
                       │
         ┌─────────────┴──────────────┐
         │                            │
         ↓                            ↓
    ┌────────┐                  ┌─────────┐
    │ Found  │                  │ Not     │
    │        │                  │ Found   │
    └───┬────┘                  └────┬────┘
        │                            │
        ↓                            ↓
    ┌────────────┐          ┌────────────────┐
    │ Load       │          │ Check legacy   │
    │ Custom     │          │ preset ID      │
    │ Pattern    │          └────────┬───────┘
    └─────┬──────┘                   │
          │                          ↓
          │                  ┌───────────────┐
          │                  │ Migrate to    │
          │                  │ custom format │
          │                  └───────┬───────┘
          │                          │
          └──────────┬───────────────┘
                     │
                     ↓
              ┌─────────────┐
              │ Display in  │
              │ UI          │
              └─────────────┘
```

## Test Button Flow

```
User presses Test Button
         │
         ↓
┌─────────────────────┐
│ Set isTestingVib    │
│ = true              │
└─────┬───────────────┘
      │
      ↓
┌─────────────────────┐
│ All test buttons    │
│ become disabled     │
└─────┬───────────────┘
      │
      ↓
┌─────────────────────┐
│ Trigger vibration   │
│ with custom pattern │
└─────┬───────────────┘
      │
      ↓
┌─────────────────────┐
│ Wait for duration   │
│ + buffer (200ms)    │
└─────┬───────────────┘
      │
      ↓
┌─────────────────────┐
│ Set isTestingVib    │
│ = false             │
└─────┬───────────────┘
      │
      ↓
┌─────────────────────┐
│ All test buttons    │
│ become enabled      │
└─────────────────────┘
```

## Migration Examples

### SHORT_1 Migration
```
Before: Preset ID = "SHORT_1"
After:  CustomVibrationPattern(
          vib1 = 100,
          pause1 = null,
          vib2 = null,
          pause2 = null,
          vib3 = null
        )
Display: "振100ms"
Plays:   100ms vibration
```

### SHORT_2 Migration (Previously Broken)
```
Before: Preset ID = "SHORT_2"
After:  CustomVibrationPattern(
          vib1 = 100,
          pause1 = 200,
          vib2 = 100,
          pause2 = null,
          vib3 = null
        )
Display: "振100-停200-振100ms"
Plays:   100ms vib → 200ms pause → 100ms vib
```

### LONG_2 Migration (Previously Broken)
```
Before: Preset ID = "LONG_2"
After:  CustomVibrationPattern(
          vib1 = 100,
          pause1 = 200,
          vib2 = 400,
          pause2 = null,
          vib3 = null
        )
Display: "振100-停200-振400ms"
Plays:   100ms vib → 200ms pause → 400ms vib
```

## Vibration Effect Generation

```kotlin
// Example: vib1=100, pause1=200, vib2=300, pause2=null, vib3=null

timings = [
    1,      // Initial delay (bug workaround)
    100,    // vib1
    200,    // pause1
    300     // vib2
    // pause2 is null, so we stop here
]

VibrationEffect.createWaveform(timings, -1)
// -1 means play once, no repeat
```

## User Journey

### First-Time User
1. Opens app
2. Sees default settings (all slots disabled)
3. Enables a quarter slot (e.g., "毎時0分")
4. Automatic test vibration with default pattern (200ms)
5. Can customize pattern using 5 field editors
6. Presses "テスト" to preview changes
7. Saves when satisfied
8. Alarm triggers at specified time with custom pattern

### Existing User (Migration)
1. Opens updated app
2. Existing preset (e.g., "SHORT_1") automatically migrated
3. Sees migrated pattern in new format: "振100ms"
4. Can now customize further if desired
5. All existing alarms continue to work

## Key Features

### ✅ Implemented
- 5-field custom pattern editor (vib1, pause1, vib2, pause2, vib3)
- 100-900ms range in 100ms increments
- "-" (disabled) option for pause1, vib2, pause2, vib3
- Cascading disable logic
- Test button per quarter slot
- Global test button state management
- Automatic migration from legacy presets
- Pattern description display
- Explanatory text

### 🎯 Benefits
- Fixes SHORT_2 and LONG_2 bugs
- Much greater flexibility than 4 presets
- Intuitive UI with immediate feedback
- Backward compatible
- No data loss during migration

### 📊 Comparison

| Feature | Old System | New System |
|---------|------------|------------|
| Patterns | 4 fixed presets | Custom (virtually unlimited) |
| Flexibility | Very limited | High |
| Bug Status | SHORT_2/LONG_2 broken | All working |
| Test Feature | No | Yes |
| Migration | N/A | Automatic |
| Max Complexity | 2 vibrations | 3 vibrations with 2 pauses |
