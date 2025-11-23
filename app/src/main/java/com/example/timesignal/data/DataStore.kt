package com.example.timesignal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Top-level property delegate to create a DataStore instance for the whole app.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timesignal_preferences")
