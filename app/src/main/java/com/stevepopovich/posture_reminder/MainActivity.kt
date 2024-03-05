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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.edit
import androidx.work.WorkManager
import com.example.compose.AppTheme
import kotlinx.coroutines.flow.firstOrNull
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
            var minuteValue: Int? by remember { mutableStateOf(null) }
            var secondValue: Int? by remember { mutableStateOf(null) }

            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val storedInterval = applicationContext.reminderDataStore.data.firstOrNull()
                minuteValue = storedInterval?.get(MINUTES_KEY)
                secondValue = storedInterval?.get(SECONDS_KEY)
            }

            AppTheme {
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

                            val minute = minuteValue ?: 0
                            val second = secondValue ?: 0
                            val totalSeconds = (minute * 60) + second
                            val formatted = "${(totalSeconds / 60).toString().padStart(2, '0')}MM : ${(totalSeconds % 60).toString().padStart(2, '0')}SS"
                            val toast = Toast.makeText(this@MainActivity, getString(R.string.started_with_interval, formatted), Toast.LENGTH_SHORT)
                            toast.show()
                        }) {
                            Text(getString(R.string.start_reminder_service))
                        }

                        Button(onClick = {
                            this@MainActivity.stopService(reminderServiceIntent)
                            WorkManager.getInstance(applicationContext).cancelAllWork()
                        }) {
                            Text(getString(R.string.stop_reminder_service))
                        }

                        Row {
                            val textFieldWidth = 80.dp
                            TextField(
                                modifier = Modifier.width(textFieldWidth),
                                value = if (minuteValue == null) "" else minuteValue.toString(),
                                onValueChange = {
                                    if (it.toIntOrNull() != null) {
                                        minuteValue = it.toInt()
                                        coroutineScope.launch {
                                            saveMinutes(it.toInt())
                                        }
                                    } else if (it.isEmpty()) {
                                        minuteValue = null
                                        coroutineScope.launch {
                                            saveMinutes(0)
                                        }
                                    }
                                },
                                placeholder = { Text("MM") }
                            )
                            Text(text = ":")
                            TextField(
                                modifier = Modifier.width(textFieldWidth),
                                value = if (secondValue == null) "" else secondValue.toString(),
                                onValueChange = {
                                    if (it.toIntOrNull() != null) {
                                        secondValue = it.toInt()
                                        coroutineScope.launch {
                                            saveSeconds(it.toInt())
                                        }
                                    } else if (it.isEmpty()) {
                                        secondValue = null
                                        coroutineScope.launch {
                                            saveSeconds(0)
                                        }
                                    }
                                },
                                placeholder = { Text("SS") }
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