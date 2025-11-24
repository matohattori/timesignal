package com.example.timesignal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.timesignal.domain.CustomVibrationPattern
import com.example.timesignal.domain.VibrationPatterns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    fun vibrateCustom(customPattern: CustomVibrationPattern) {
        vibrator?.cancel()

        val duration = VibrationPatterns.getCustomPatternDuration(customPattern)
        if (duration == 0L) return

        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timesignal::VibrationWakeLock")
        wakeLock.acquire(duration + 200)

        // Convert pattern to segments and execute sequentially using createOneShot
        // This fixes Wear OS (Pixel Watch 4) issue where createWaveform only executes first vibration
        val segments = customPattern.toSegments()
        
        CoroutineScope(Dispatchers.Default).launch {
            for (segment in segments) {
                val (vibMs, pauseMs) = segment
                
                // Execute vibration using createOneShot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(vibMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                } else {
                    // Fallback for older API (though minSdk is 30)
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(vibMs.toLong())
                }
                
                // Wait for vibration + pause duration before next segment
                delay(vibMs.toLong() + pauseMs.toLong())
            }
        }
    }
}
