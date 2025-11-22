package com.example.timesignal.domain

data class TimesignalState(
    val quarters: Map<QuarterSlot, QuarterSettings> = emptyMap(),
    val availableDurations: List<Int> = listOf(20, 30, 40, 50, 60, 80, 100, 120, 150, 200),
    val canScheduleExactAlarms: Boolean = true,
)

enum class QuarterSlot(val minute: Int, val displayName: String) {
    ZERO(0, "毎時0分"),
    FIFTEEN(15, "毎時15分"),
    THIRTY(30, "毎時30分"),
    FORTY_FIVE(45, "毎時45分"),
}

data class QuarterSettings(
    val enabled: Boolean = false,
    val durationMs: Int = 40,
)
