package com.example.timesignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.domain.QuietHoursEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuarterChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TimesignalApplication
        val slot = intent.getStringExtra(EXTRA_SLOT)?.let { runCatching { QuarterSlot.valueOf(it) }.getOrNull() }
            ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val state = app.container.repository.getLatestState()
            val withinQuietHours = QuietHoursEvaluator.isWithinQuietHours(state.quietHours)
            val slotSettings = state.quarters[slot]
            if (!withinQuietHours && slotSettings?.enabled == true) {
                app.container.vibrator.vibrate(slotSettings.durationMs)
            }
            app.container.scheduler.scheduleNext(slot)
            pendingResult.finish()
        }
    }

    companion object {
        const val EXTRA_SLOT = "extra_slot"
    }
}
