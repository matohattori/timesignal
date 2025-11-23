package com.example.timesignal.domain

data class TimesignalState(
    val quarters: Map<QuarterSlot, QuarterSettings> = emptyMap(),
    val availablePatterns: List<VibrationPattern> = VibrationPatterns.all,
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
    val patternId: String = VibrationPatterns.default.id,
)

data class VibrationPattern(
    val id: String,
    val label: String,
    val timings: LongArray,
)

object VibrationPatterns {
    private const val SHORT_VIBRATION_MS = 200L
    private const val LONG_VIBRATION_MS = 800L
    private const val GAP_MS = 200L

    val all: List<VibrationPattern> = listOf(
        VibrationPattern(
            id = "SHORT_1",
            label = "短1",
            timings = longArrayOf(0, SHORT_VIBRATION_MS),
        ),
        VibrationPattern(
            id = "SHORT_2",
            label = "短2",
            timings = longArrayOf(0, SHORT_VIBRATION_MS, GAP_MS, SHORT_VIBRATION_MS),
        ),
        VibrationPattern(
            id = "LONG_1",
            label = "長1",
            timings = longArrayOf(0, LONG_VIBRATION_MS),
        ),
        VibrationPattern(
            id = "LONG_2",
            label = "長2",
            timings = longArrayOf(0, SHORT_VIBRATION_MS, GAP_MS, LONG_VIBRATION_MS),
        ),
    )

    val default: VibrationPattern = all.first()

    fun findById(id: String?): VibrationPattern = all.find { it.id == id } ?: default
}
