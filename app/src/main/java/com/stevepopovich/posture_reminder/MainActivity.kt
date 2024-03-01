package com.stevepopovich.posture_reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.edit
import androidx.work.WorkManager
import com.stevepopovich.posture_reminder.ui.theme.PostureReminderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }

        val reminderServiceIntent = Intent(this, ReminderForegroundService::class.java)

        setContent {
            var minuteValue by remember { mutableIntStateOf(1) }
            var secondValue by remember { mutableIntStateOf(0) }

            val coroutineScope = rememberCoroutineScope()
            PostureReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            this@MainActivity.startService(reminderServiceIntent)

                            val totalSeconds = (minuteValue * 60) + secondValue
                            val formatted = "${(totalSeconds / 60).toString().padStart(2, '0')}MM : ${(totalSeconds % 60).toString().padStart(2, '0')}SS"
                            val toast = Toast.makeText(this@MainActivity, "Started with $formatted interval", Toast.LENGTH_SHORT)
                            toast.show()
                        }) {
                            Text("Start Reminder Service")
                        }
                        Button(onClick = {
                            this@MainActivity.stopService(reminderServiceIntent)
                            WorkManager.getInstance(applicationContext).cancelAllWork()
                        }) {
                            Text("Stop Reminder Service")
                        }
                        Row {
                            val textFieldWidth = 80.dp
                            TextField(
                                modifier = Modifier.width(textFieldWidth),
                                value = minuteValue.toString(),
                                onValueChange = {
                                    if (it.toIntOrNull() != null) {
                                        minuteValue = it.toInt()
                                        coroutineScope.launch {
                                            saveMinutes(it.toInt())
                                        }
                                    }
                                },
                            )
                            Text(text = ":")
                            TextField(
                                modifier = Modifier.width(textFieldWidth),
                                value = secondValue.toString(),
                                onValueChange = {
                                    if (it.toIntOrNull() != null) {
                                        secondValue = it.toInt()
                                        coroutineScope.launch {
                                            saveSeconds(it.toInt())
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun saveMinutes(minutes: Int) {
       applicationContext.reminderDataStore.edit { settings ->
           settings[MINUTES_KEY] = minutes
       }
    }

    private suspend fun saveSeconds(seconds: Int) {
         applicationContext.reminderDataStore.edit { settings ->
              settings[SECONDS_KEY] = seconds
         }
    }
}