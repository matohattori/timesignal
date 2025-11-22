package com.example.timesignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class QuarterChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val serviceIntent = Intent(context, TimesignalService::class.java).apply {
            // Forward the action for time changes, etc.
            action = intent.action

            // Explicitly forward the all-important EXTRA_SLOT to ensure it's not lost.
            if (intent.hasExtra(TimesignalService.EXTRA_SLOT)) {
                putExtra(TimesignalService.EXTRA_SLOT, intent.getStringExtra(TimesignalService.EXTRA_SLOT))
            }
        }
        context.startForegroundService(serviceIntent)
    }
}
