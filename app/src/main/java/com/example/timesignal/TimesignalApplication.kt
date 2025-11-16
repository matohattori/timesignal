package com.example.timesignal

import android.app.Application
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
    }

    inner class TimesignalContainer(app: TimesignalApplication) {
        val repository: TimesignalRepository = TimesignalPreferencesRepository(app.dataStore)
        val scheduler = TimesignalScheduler(app)
        val vibrator = TimesignalVibrator(app)
    }
}
