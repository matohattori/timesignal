package com.example.timesignal.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timesignal.TimesignalService
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

        val nextTrigger = calculateNextQuarterTime(slot, fromTime)
        val triggerMillis = nextTrigger.toInstant().toEpochMilli()
        
        // Log the scheduled alarm time for debugging
        Log.d(TAG, "Scheduling alarm: slot=${slot.minute}, " +
            "triggerTime=${nextTrigger.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}, " +
            "triggerMillis=$triggerMillis")
        
        // Intent now goes directly to the Service, not the Receiver
        val intent = Intent(context, TimesignalService::class.java).apply {
            putExtra(TimesignalService.EXTRA_SLOT, slot.name)
            putExtra(EXTRA_SCHEDULED_TIME_MILLIS, triggerMillis)
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
            triggerMillis,
            pendingIntent,
        )
    }

    companion object {
        private const val TAG = "TimesignalScheduler"
        const val EXTRA_SCHEDULED_TIME_MILLIS = "extra_scheduled_time_millis"
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

    /**
     * Calculates the next trigger time for the given quarter slot.
     * 
     * This method ensures that:
     * 1. The trigger time is always snapped to the exact calendar time (HH:MM:00.000)
     * 2. Seconds and nanoseconds are always reset to 0
     * 3. Even if the alarm fires late (e.g., at HH:00:45 instead of HH:00:00),
     *    the next trigger will be calculated as the next calendar quarter time,
     *    not based on a relative offset from the current time
     * 
     * @param slot The quarter slot (0, 15, 30, or 45 minutes)
     * @param now The reference time for calculation (typically current time)
     * @return The next trigger time as a ZonedDateTime with seconds and nanos set to 0
     */
    private fun calculateNextQuarterTime(slot: QuarterSlot, now: ZonedDateTime): ZonedDateTime {
        // Reset seconds and nanoseconds to 0 to ensure precise calendar-aligned scheduling
        val baseTime = now.withSecond(0).withNano(0)
        
        // Create candidate time with the target minute
        var candidate = baseTime.withMinute(slot.minute)
        
        // If the candidate is not strictly after 'now', advance to the next hour
        // This handles the case where we're already past or at the target minute
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusHours(1)
        }
        
        return candidate
    }
}
