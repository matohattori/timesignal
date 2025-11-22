package com.example.timesignal.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.timesignal.TimesignalService
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class TimesignalScheduler(
    private val context: Context,
) {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun reschedule(state: TimesignalState) {
        QuarterSlot.values().forEach { slot ->
            cancel(slot)
            if (state.quarters[slot]?.enabled == true) {
                scheduleNext(slot)
            }
        }
    }

    fun scheduleNext(slot: QuarterSlot, fromTime: ZonedDateTime = ZonedDateTime.now()) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() == false) {
                return
            }
        }

        val nextTrigger = calculateNextTrigger(slot, fromTime)
        // Intent now goes directly to the Service, not the Receiver
        val intent = Intent(context, TimesignalService::class.java).apply {
            putExtra(TimesignalService.EXTRA_SLOT, slot.name)
        }

        // Use getForegroundService to directly start the service from the background
        val pendingIntent = PendingIntent.getForegroundService(
            context,
            slot.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTrigger.toInstant().toEpochMilli(),
            pendingIntent,
        )
    }

    fun cancelAll() {
        QuarterSlot.values().forEach { cancel(it) }
    }

    private fun cancel(slot: QuarterSlot) {
        val intent = Intent(context, TimesignalService::class.java)
        val pendingIntent = PendingIntent.getForegroundService(
            context,
            slot.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.cancel(pendingIntent)
    }

    private fun calculateNextTrigger(slot: QuarterSlot, fromTime: ZonedDateTime): ZonedDateTime {
        var candidate = fromTime.truncatedTo(ChronoUnit.MINUTES)
            .withMinute(slot.minute)
            .withSecond(0)
            .withNano(0)
        if (!candidate.isAfter(fromTime)) {
            candidate = candidate.plusHours(1)
        }
        return candidate
    }
}
