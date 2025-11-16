package com.example.timesignal.domain

import java.time.LocalTime

/** Quarter slots that can trigger a time signal. */
enum class QuarterSlot(val minute: Int, val displayName: String) {
    TOP_OF_HOUR(0, "00分"),
    QUARTER_PAST(15, "15分"),
    HALF_PAST(30, "30分"),
    QUARTER_TO(45, "45分");

    companion object {
        fun fromMinute(minute: Int): QuarterSlot? = values().firstOrNull { it.minute == minute }
    }
}

data class QuarterSettings(
    val enabled: Boolean = false,
    val durationMs: Int = 400,
)

data class QuietHoursConfig(
    val enabled: Boolean = false,
    val start: LocalTime = LocalTime.of(23, 0),
    val end: LocalTime = LocalTime.of(7, 0),
)

data class TimesignalState(
    val quarters: Map<QuarterSlot, QuarterSettings> = QuarterSlot.values().associateWith { QuarterSettings() },
    val quietHours: QuietHoursConfig = QuietHoursConfig(),
    val availableDurations: List<Int> = listOf(200, 400, 600, 800, 1200),
    val isWithinQuietHours: Boolean = false,
)
