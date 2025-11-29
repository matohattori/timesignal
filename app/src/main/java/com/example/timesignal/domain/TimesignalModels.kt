package com.example.timesignal.domain

import android.os.Build
import android.os.VibrationEffect

/**
 * Represents the entire state of the Timesignal application.
 * `availableDurations` is removed to align with the new vibration pattern system.
 */
data class TimesignalState(
    val quarters: Map<QuarterSlot, QuarterSettings> = emptyMap(),
    val canScheduleExactAlarms: Boolean = true,
)

/**
 * Defines the trigger points within an hour.
 * The `displayName` is used for the UI and `minute` is for scheduling calculations.
 */
enum class QuarterSlot(val minute: Int, val displayName: String) {
    ZERO(0, "毎時0分"),
    FIFTEEN(15, "毎時15分"),
    THIRTY(30, "毎時30分"),
    FORTY_FIVE(45, "毎時45分"),
}

/**
 * Holds settings for each quarter slot.
 * Changed from `durationMs` to `vibrationPatternId` to support custom patterns.
 */
data class QuarterSettings(
    val enabled: Boolean = false,
    val vibrationPatternId: String = "SHORT_1", // Default to a valid pattern (kept for migration)
    val customPattern: CustomVibrationPattern? = null // New custom pattern settings
)

/**
 * Custom vibration pattern with up to 5 segments.
 * vib1: First vibration (required, 50/100/200/300/500ms)
 * pause1: First pause (optional, 50/100/200/300/500ms or null for disabled)
 * vib2: Second vibration (optional, 50/100/200/300/500ms or null for disabled)
 * pause2: Second pause (optional, 50/100/200/300/500ms or null for disabled)
 * vib3: Third vibration (optional, 50/100/200/300/500ms or null for disabled)
 * When a field is null, all subsequent fields are ignored.
 */
data class CustomVibrationPattern(
    val vib1: Int = 200, // Required, default DEFAULT_DURATION (200ms)
    val pause1: Int? = null, // Optional
    val vib2: Int? = null, // Optional
    val pause2: Int? = null, // Optional
    val vib3: Int? = null // Optional
) {
    /**
     * Validates that the sequence is properly terminated.
     * If pause1 is null, vib2/pause2/vib3 must be null.
     * If vib2 is null, pause2/vib3 must be null.
     * If pause2 is null, vib3 must be null.
     */
    fun isValid(): Boolean {
        return when {
            pause1 == null -> vib2 == null && pause2 == null && vib3 == null
            vib2 == null -> pause2 == null && vib3 == null
            pause2 == null -> vib3 == null
            else -> true
        }
    }

    companion object {
        /**
         * Valid duration options for vibration/pause.
         */
        val VALID_DURATIONS = listOf(50, 100, 200, 300, 500)
        
        /**
         * Default duration for vibration patterns.
         */
        const val DEFAULT_DURATION = 200

        /**
         * Migrates a duration value to the nearest valid option.
         * If the value is null, returns null.
         * If the value is already valid, returns it unchanged.
         * Otherwise, returns the nearest valid value.
         */
        fun migrateToNearestValidDuration(value: Int?): Int? {
            if (value == null) return null
            if (value in VALID_DURATIONS) return value
            return VALID_DURATIONS.minByOrNull { kotlin.math.abs(it - value) } ?: DEFAULT_DURATION
        }
    }
}

/**
 * A data class to hold all information about a vibration pattern.
 */
data class VibrationPattern(
    val displayName: String,
    val timings: LongArray,
    val amplitudes: IntArray
)

/**
 * A singleton object that centralizes all vibration pattern definitions and logic.
 */
object VibrationPatterns {
    // Legacy preset patterns kept for migration compatibility
    val PATTERNS = linkedMapOf(
        // By changing the initial 0ms delay to 1ms, we can work around a bug in some
        // vibrator drivers that causes multi-part waveforms to fail.
        "SHORT_1" to VibrationPattern("短1", longArrayOf(1, 100), intArrayOf(0, 255)),
        "SHORT_2" to VibrationPattern("短2", longArrayOf(1, 100, 200, 100), intArrayOf(0, 255, 0, 255)),
        "LONG_1"  to VibrationPattern("長1", longArrayOf(1, 400), intArrayOf(0, 255)),
        "LONG_2"  to VibrationPattern("長2", longArrayOf(1, 100, 200, 400), intArrayOf(0, 255, 0, 255))
    )

    /**
     * Migrates a legacy preset pattern ID to a CustomVibrationPattern.
     * SHORT_1 -> vib1=100ms, others disabled
     * SHORT_2 -> vib1=100ms, pause1=200ms, vib2=100ms, others disabled
     * LONG_1 -> vib1=500ms (migrated from 400ms), others disabled
     * LONG_2 -> vib1=100ms, pause1=200ms, vib2=500ms (migrated from 400ms), others disabled
     */
    fun migratePresetToCustom(patternId: String): CustomVibrationPattern {
        return when (patternId) {
            "SHORT_1" -> CustomVibrationPattern(vib1 = 100)
            "SHORT_2" -> CustomVibrationPattern(vib1 = 100, pause1 = 200, vib2 = 100)
            "LONG_1" -> CustomVibrationPattern(vib1 = 500)
            "LONG_2" -> CustomVibrationPattern(vib1 = 100, pause1 = 200, vib2 = 500)
            else -> CustomVibrationPattern(vib1 = CustomVibrationPattern.DEFAULT_DURATION)
        }
    }

    /**
     * Creates a VibrationEffect from a CustomVibrationPattern.
     * Builds the timing array dynamically based on which fields are set.
     */
    fun getCustomVibrationEffect(customPattern: CustomVibrationPattern): VibrationEffect? {
        val timings = mutableListOf<Long>()
        
        // Start with 1ms delay to work around vibrator driver bugs
        timings.add(1)
        
        // Add vib1 (always present)
        timings.add(customPattern.vib1.toLong())
        
        // Add pause1 and subsequent segments if present
        if (customPattern.pause1 != null) {
            timings.add(customPattern.pause1.toLong())
            
            if (customPattern.vib2 != null) {
                timings.add(customPattern.vib2.toLong())
                
                if (customPattern.pause2 != null) {
                    timings.add(customPattern.pause2.toLong())
                    
                    if (customPattern.vib3 != null) {
                        timings.add(customPattern.vib3.toLong())
                    }
                }
            }
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(timings.toLongArray(), -1)
        } else {
            null
        }
    }

    /**
     * Calculates the total duration of a custom vibration pattern.
     */
    fun getCustomPatternDuration(customPattern: CustomVibrationPattern): Long {
        var duration = 1L + customPattern.vib1 // Initial delay + vib1
        
        if (customPattern.pause1 != null) {
            duration += customPattern.pause1
            
            if (customPattern.vib2 != null) {
                duration += customPattern.vib2
                
                if (customPattern.pause2 != null) {
                    duration += customPattern.pause2
                    
                    if (customPattern.vib3 != null) {
                        duration += customPattern.vib3
                    }
                }
            }
        }
        
        return duration
    }

    fun getVibrationEffect(patternId: String, hasAmplitudeControl: Boolean): VibrationEffect? {
        val pattern = PATTERNS[patternId] ?: return null

        // HYPOTHESIS: The device's amplitude control implementation is buggy.
        // To fix this, we ignore amplitude control and use the simpler createWaveform version
        // for all modern devices. This version only uses timings.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(pattern.timings, -1)
        } else {
            // Older APIs don't have amplitude control anyway.
            @Suppress("DEPRECATION")
            VibrationEffect.createWaveform(pattern.timings, -1)
        }
    }

    fun getPatternDuration(patternId: String): Long {
        return PATTERNS[patternId]?.timings?.sum() ?: 0L
    }
}
