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
    val vibrationPatternId: String = "SHORT_1" // Default to a valid pattern
)

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
    val PATTERNS = linkedMapOf(
        // Patterns use timing-only mode where arrays alternate: [off, on, off, on, ...]
        // Initial 0ms delay, then vibration, then pause, then vibration again for multi-part patterns
        "SHORT_1" to VibrationPattern("短1", longArrayOf(0, 100), intArrayOf(0, 255)),
        "SHORT_2" to VibrationPattern("短2", longArrayOf(0, 100, 100, 100), intArrayOf(0, 255, 0, 255)),
        "LONG_1"  to VibrationPattern("長1", longArrayOf(0, 400), intArrayOf(0, 255)),
        "LONG_2"  to VibrationPattern("長2", longArrayOf(0, 100, 100, 400), intArrayOf(0, 255, 0, 255))
    )

    fun getVibrationEffect(patternId: String, hasAmplitudeControl: Boolean): VibrationEffect? {
        val pattern = PATTERNS[patternId] ?: return null

        // Always use amplitude-based waveform for proper multi-part pattern support.
        // The hasAmplitudeControl parameter is currently unused but kept for API compatibility.
        // Even on devices where hasAmplitudeControl might be false, using the amplitude-based
        // API provides better compatibility with Wear OS for complex patterns like SHORT_2 and LONG_2.
        return VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
    }

    fun getPatternDuration(patternId: String): Long {
        return PATTERNS[patternId]?.timings?.sum() ?: 0L
    }
}
