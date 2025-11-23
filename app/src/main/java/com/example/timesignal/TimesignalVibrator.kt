package com.example.timesignal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.timesignal.domain.VibrationPattern

class TimesignalVibrator(private val context: Context) {
    private val vibrator: Vibrator? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        }
        else -> context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val powerManager: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun vibrate(pattern: VibrationPattern) {
        val totalDuration = pattern.timings.sum() + 200L
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timesignal::VibrationWakeLock")
        try {
            wakeLock.acquire(totalDuration) // Acquire lock with a timeout

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern.timings, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern.timings, -1)
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release() // Ensure the lock is always released
            }
        }
    }
}
