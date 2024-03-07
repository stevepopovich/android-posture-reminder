package com.stevepopovich.posture_reminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

val MINUTES_KEY = intPreferencesKey("minutes_key")
val SECONDS_KEY = intPreferencesKey("seconds_key")
val LAST_NOTIF_TIME = stringPreferencesKey("last_notif_time")