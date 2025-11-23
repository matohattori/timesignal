package com.example.timesignal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import com.example.timesignal.domain.VibrationPatterns

class TimesignalVibrator(private val context: Context) {
    private val vibrator: Vibrator? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        }

        else -> @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    }

    private val hasAmplitudeControl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.hasAmplitudeControl() ?: false
    } else {
        false
    }

    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun vibrate(patternId: String) {
        vibrator?.cancel()

        val effect = VibrationPatterns.getVibrationEffect(patternId, hasAmplitudeControl)
        val duration = VibrationPatterns.getPatternDuration(patternId)

        if (effect == null || duration == 0L) return

        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timesignal::VibrationWakeLock")
        wakeLock.acquire(duration + 200)

        vibrator?.vibrate(effect)
    }
}
