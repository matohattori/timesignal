package com.example.timesignal

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.TimesignalScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimesignalService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as TimesignalApplication
        val receiveTimeMillis = System.currentTimeMillis()

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent?.action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        Log.d(TAG, "Received system event: ${intent.action}")
                        val state = app.container.repository.getLatestState()
                        app.container.scheduler.reschedule(state)
                    }
                    else -> {
                        val slot = intent?.getStringExtra(EXTRA_SLOT)?.let { runCatching { QuarterSlot.valueOf(it) }.getOrNull() }
                            ?: return@launch
                        
                        // Log the delay between scheduled time and actual receipt time
                        val scheduledTimeMillis = intent.getLongExtra(
                            TimesignalScheduler.EXTRA_SCHEDULED_TIME_MILLIS, 0L
                        )
                        logAlarmDelay(slot, scheduledTimeMillis, receiveTimeMillis)
                        
                        val state = app.container.repository.getLatestState()
                        val slotSettings = state.quarters[slot]
                        if (slotSettings?.enabled == true) {
                            if (slotSettings.customPattern != null) {
                                app.container.vibrator.vibrateCustom(slotSettings.customPattern)
                            } else {
                                app.container.vibrator.vibrate(slotSettings.vibrationPatternId)
                            }
                        }
                        app.container.scheduler.scheduleNext(slot)
                    }
                }
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Logs the delay between the scheduled alarm time and the actual receipt time.
     * This helps diagnose timing issues with quarter-hour chime alarms.
     */
    private fun logAlarmDelay(slot: QuarterSlot, scheduledTimeMillis: Long, receiveTimeMillis: Long) {
        val receiveTime = Instant.ofEpochMilli(receiveTimeMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        if (scheduledTimeMillis > 0) {
            val delaySeconds = (receiveTimeMillis - scheduledTimeMillis) / 1000.0
            val scheduledTime = Instant.ofEpochMilli(scheduledTimeMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            
            Log.d(TAG, "Alarm received: slot=${slot.minute}, " +
                "scheduledTime=$scheduledTime, " +
                "receiveTime=$receiveTime, " +
                "delaySeconds=${"%.2f".format(delaySeconds)}")
        } else {
            Log.d(TAG, "Alarm received: slot=${slot.minute}, " +
                "receiveTime=$receiveTime, " +
                "scheduledTime=unknown")
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a standard system icon
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "TimesignalService"
        const val NOTIFICATION_CHANNEL_ID = "timesignal_service_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_SLOT = "extra_slot"
    }
}
