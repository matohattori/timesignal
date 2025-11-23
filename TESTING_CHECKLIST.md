# Testing Checklist for Vibration Pattern Feature

## Prerequisites
- [ ] Android SDK installed (API 30+)
- [ ] Wear OS device or emulator available
- [ ] Project builds successfully in Android Studio

## Data Persistence Tests

### Custom Pattern Save/Load
- [ ] Create a custom pattern (e.g., vib1=200, pause1=300, vib2=400)
- [ ] Close the app
- [ ] Reopen the app
- [ ] Verify the pattern is preserved

### Migration from Legacy Presets
- [ ] Use adb to clear app data: `adb shell pm clear com.example.timesignal`
- [ ] Manually set legacy preset in preferences (if possible via adb)
- [ ] Open app
- [ ] Verify preset is converted to equivalent custom pattern

## UI Behavior Tests

### Field Cascading Disable
- [ ] Enable a quarter slot
- [ ] Verify all 5 fields are initially configurable
- [ ] Set pause1 to "-"
- [ ] Verify vib2, pause2, vib3 are grayed out and disabled
- [ ] Set pause1 to a value (e.g., 200ms)
- [ ] Verify vib2 becomes enabled
- [ ] Set vib2 to "-"
- [ ] Verify pause2, vib3 are grayed out and disabled
- [ ] Set vib2 to a value
- [ ] Verify pause2 becomes enabled
- [ ] Set pause2 to "-"
- [ ] Verify vib3 is grayed out and disabled
- [ ] Set pause2 to a value
- [ ] Verify vib3 becomes enabled

### Duration Selector
- [ ] Verify vib1 selector shows options: 100, 200, 300, ..., 900 (no "-" option)
- [ ] Verify pause1 selector shows: "-(無効)", 100, 200, 300, ..., 900
- [ ] Verify vib2 selector shows: "-(無効)", 100, 200, 300, ..., 900
- [ ] Verify pause2 selector shows: "-(無効)", 100, 200, 300, ..., 900
- [ ] Verify vib3 selector shows: "-(無効)", 100, 200, 300, ..., 900
- [ ] Select each value and verify it's highlighted
- [ ] Verify scrolling works for all options

### Pattern Description Display
- [ ] Set vib1=100, all others disabled
- [ ] Verify caption shows "振100ms"
- [ ] Set vib1=100, pause1=200, vib2=100, others disabled
- [ ] Verify caption shows "振100-停200-振100ms"
- [ ] Set all 5 fields to non-null values
- [ ] Verify caption shows "振XXX-停XXX-振XXX-停XXX-振XXXms"

### Explanatory Text
- [ ] Enable a quarter slot
- [ ] Verify text "「-」を選択するとこの時点で振動シーケンスが終了します" is displayed
- [ ] Verify text is visible and readable

## Test Button Tests

### Test Button Functionality
- [ ] Set a simple pattern (vib1=200, others disabled)
- [ ] Press test button
- [ ] Verify vibration occurs for ~200ms
- [ ] Verify all test buttons become disabled during vibration
- [ ] Wait for vibration to complete
- [ ] Verify all test buttons become enabled again

### Test Button Multi-Pattern
- [ ] Test with different patterns:
  - [ ] vib1=100 only
  - [ ] vib1=100, pause1=200, vib2=100
  - [ ] vib1=100, pause1=200, vib2=300, pause2=200, vib3=100
- [ ] Verify each pattern vibrates correctly
- [ ] Verify test button behavior is consistent

### Test Button During Slot Toggle
- [ ] Disable a quarter slot
- [ ] Enable it again
- [ ] Verify test vibration is triggered automatically
- [ ] Verify all test buttons are disabled during this vibration

## Vibration Execution Tests

### Actual Alarm Triggering
- [ ] Set a quarter slot to a specific minute (e.g., next 15-minute mark)
- [ ] Configure a custom pattern
- [ ] Wait for the alarm to trigger
- [ ] Verify vibration matches the configured pattern
- [ ] Verify pattern terminates correctly at "-" selections

### Multiple Slots
- [ ] Enable multiple quarter slots with different patterns
- [ ] Verify each slot vibrates with its own pattern at the correct time

### Pattern Validation
Test these specific patterns to verify correct timing:
- [ ] vib1=100: Total duration should be ~101ms (1ms initial delay + 100ms)
- [ ] vib1=100, pause1=200, vib2=100: Total should be ~401ms
- [ ] All 5 fields set to 100: Total should be ~501ms

## Migration Tests

### Legacy Preset Conversion
For each preset, verify the migration:

- [ ] SHORT_1 → vib1=100, others null
  - Expected pattern: 100ms vibration only
  
- [ ] SHORT_2 → vib1=100, pause1=200, vib2=100, others null
  - Expected pattern: 100ms vib, 200ms pause, 100ms vib
  
- [ ] LONG_1 → vib1=400, others null
  - Expected pattern: 400ms vibration only
  
- [ ] LONG_2 → vib1=100, pause1=200, vib2=400, others null
  - Expected pattern: 100ms vib, 200ms pause, 400ms vib

### First-Time User Experience
- [ ] Install app fresh
- [ ] Enable a quarter slot
- [ ] Verify default pattern is applied
- [ ] Verify test vibration works

## Edge Cases

### Boundary Values
- [ ] Set vib1 to minimum (100ms) - verify it works
- [ ] Set vib1 to maximum (900ms) - verify it works
- [ ] Set all fields to 900ms - verify total vibration works

### Rapid Toggling
- [ ] Rapidly press test button multiple times
- [ ] Verify only one vibration occurs at a time
- [ ] Verify buttons re-enable after completion

### Configuration Changes
- [ ] Rotate device during configuration (if applicable)
- [ ] Verify settings are preserved
- [ ] Background/foreground the app
- [ ] Verify settings are preserved

## Regression Tests

### Existing Functionality
- [ ] Verify alarm permission prompt still works
- [ ] Verify quarter slot enable/disable still works
- [ ] Verify notification channel still works
- [ ] Verify foreground service behavior unchanged

## Performance Tests

### UI Responsiveness
- [ ] Scroll through duration selectors quickly
- [ ] Verify no lag or stuttering
- [ ] Switch between quarter slots rapidly
- [ ] Verify UI remains responsive

### Memory Leaks
- [ ] Open/close app multiple times
- [ ] Enable/disable patterns repeatedly
- [ ] Verify no memory growth in Android Profiler

## Accessibility Tests

### Wear OS Specific
- [ ] Test on small watch screen
- [ ] Verify all UI elements are accessible
- [ ] Verify scrolling works smoothly
- [ ] Test with different font sizes (if Wear OS supports it)

## Bug Reproduction Tests

### Original Issues
- [ ] Verify SHORT_2 now works correctly (was reported as not working)
- [ ] Verify LONG_2 now works correctly (was reported as not working)
- [ ] Compare with original implementation to ensure fix

## Documentation Verification

- [ ] Verify MIGRATION_GUIDE.md matches actual behavior
- [ ] Verify IMPLEMENTATION_NOTES.md is accurate
- [ ] Verify README.md describes features correctly

## Security Checks

- [ ] Verify no sensitive data is logged
- [ ] Verify preferences are stored securely
- [ ] Verify no unnecessary permissions requested
- [ ] Verify vibration duration cannot be exploited

## Final Acceptance

- [ ] All test cases above pass
- [ ] No crashes observed
- [ ] UI is intuitive and clear
- [ ] Performance is acceptable
- [ ] Documentation is complete
- [ ] Code review feedback addressed

---

**Test Date:** ____________

**Tester:** ____________

**Device/Emulator:** ____________

**Android Version:** ____________

**Test Result:** ☐ PASS  ☐ FAIL

**Notes:**
