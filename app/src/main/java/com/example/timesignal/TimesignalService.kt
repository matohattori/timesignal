package com.example.timesignal

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.timesignal.domain.QuarterSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimesignalService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as TimesignalApplication

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent?.action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        val state = app.container.repository.getLatestState()
                        app.container.scheduler.reschedule(state)
                    }
                    else -> {
                        val slot = intent?.getStringExtra(EXTRA_SLOT)?.let { runCatching { QuarterSlot.valueOf(it) }.getOrNull() }
                            ?: return@launch
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

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a standard system icon
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "timesignal_service_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_SLOT = "extra_slot"
    }
}
