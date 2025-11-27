package com.example.timesignal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.timesignal.data.TimesignalPreferencesRepository
import com.example.timesignal.domain.TimesignalRepository
import com.example.timesignal.domain.TimesignalScheduler

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timesignal")

class TimesignalApplication : Application() {
    lateinit var container: TimesignalContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = TimesignalContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(TimesignalService.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(false)
                vibrationPattern = null
                setSound(null, null)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    inner class TimesignalContainer(app: TimesignalApplication) {
        val repository: TimesignalRepository = TimesignalPreferencesRepository(app.dataStore)
        val scheduler = TimesignalScheduler(app)
        val vibrator = TimesignalVibrator(app)
    }
}
