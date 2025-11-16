package com.example.timesignal.domain

import java.time.LocalTime

object QuietHoursEvaluator {
    fun isWithinQuietHours(config: QuietHoursConfig, now: LocalTime = LocalTime.now()): Boolean {
        if (!config.enabled) return false
        return if (config.start <= config.end) {
            now >= config.start && now < config.end
        } else {
            now >= config.start || now < config.end
        }
    }
}
