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
import kotlinx.coroutines.Job
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
    
    private var vibrationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

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
        // Cancel any ongoing vibration and job
        vibrationJob?.cancel()
        vibrator?.cancel()

        val duration = VibrationPatterns.getCustomPatternDuration(customPattern)
        if (duration == 0L) return

        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timesignal::VibrationWakeLock")
        // Add extra buffer to wakeLock duration to account for coroutine scheduling delays
        wakeLock.acquire(duration + 500)

        // Convert pattern to segments and execute sequentially using createOneShot
        // This fixes Wear OS (Pixel Watch 4) issue where createWaveform only executes first vibration
        val segments = customPattern.toSegments()
        
        vibrationJob = coroutineScope.launch {
            try {
                for (segment in segments) {
                    val (vibMs, pauseMs) = segment
                    
                    // Execute vibration using createOneShot (minSdk is 30, so O check is sufficient)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createOneShot(vibMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator?.vibrate(effect)
                    }
                    
                    // Wait for vibration + pause duration before next segment
                    delay(vibMs.toLong() + pauseMs.toLong())
                }
            } finally {
                // Ensure wakeLock is released even if coroutine is cancelled
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
}
