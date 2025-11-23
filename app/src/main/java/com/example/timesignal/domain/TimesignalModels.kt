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
        "SHORT_1" to VibrationPattern("短1", longArrayOf(0, 100), intArrayOf(0, 255)),
        "SHORT_2" to VibrationPattern("短2", longArrayOf(0, 100, 200, 100), intArrayOf(0, 255, 0, 255)),
        "LONG_1"  to VibrationPattern("長1", longArrayOf(0, 400), intArrayOf(0, 255)),
        "LONG_2"  to VibrationPattern("長2", longArrayOf(0, 100, 200, 400), intArrayOf(0, 255, 0, 255))
    )

    fun getVibrationEffect(patternId: String): VibrationEffect? {
        val pattern = PATTERNS[patternId] ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createWaveform(pattern.timings, -1)
        }
    }

    fun getPatternDuration(patternId: String): Long {
        return PATTERNS[patternId]?.timings?.sum() ?: 0L
    }
}
