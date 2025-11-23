package com.example.timesignal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.timesignal.domain.VibrationPatterns

class TimesignalVibrator(private val context: Context) {
    private val vibrator: Vibrator? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        }

        else -> context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun vibrate(patternId: String) {
        val effect = VibrationPatterns.getVibrationEffect(patternId)
        val duration = VibrationPatterns.getPatternDuration(patternId)

        // A null effect means the pattern is invalid; do nothing.
        if (effect == null) return

        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timesignal::VibrationWakeLock")
        try {
            // Acquire lock with a timeout slightly longer than the vibration pattern.
            wakeLock.acquire(duration + 200)

            vibrator?.vibrate(effect)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release() // Ensure the lock is always released
            }
        }
    }
}
