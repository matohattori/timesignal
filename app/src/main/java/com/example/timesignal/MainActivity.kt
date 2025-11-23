package com.example.timesignal

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timesignal.domain.QuarterSlot
import com.example.timesignal.ui.TimesignalScreen
import com.example.timesignal.ui.TimesignalTheme
import com.example.timesignal.ui.TimesignalViewModel

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // You can check here if the permission was granted and show a toast or something.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TimesignalApplication

        setContent {
            val factory = TimesignalViewModel.Factory(
                repository = app.container.repository,
                scheduler = app.container.scheduler,
                vibrator = app.container.vibrator,
                alarmManager = getSystemService<AlarmManager>()!!,
            )

            TimesignalTheme {
                val viewModel: TimesignalViewModel = viewModel(factory = factory)
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                val isTestingVibration = viewModel.isTestingVibration.collectAsStateWithLifecycle()
                TimesignalScreen(
                    state = uiState.value,
                    isTestingVibration = isTestingVibration.value,
                    onToggleQuarter = { slot: QuarterSlot, enabled: Boolean ->
                        viewModel.setQuarterEnabled(slot, enabled)
                    },
                    onSelectVibrationPattern = { slot: QuarterSlot, patternId ->
                        viewModel.setVibrationPattern(slot, patternId)
                    },
                    onUpdateCustomPattern = { slot: QuarterSlot, customPattern ->
                        viewModel.setCustomVibrationPattern(slot, customPattern)
                    },
                    onTestVibration = { slot: QuarterSlot ->
                        viewModel.testVibration(slot)
                    },
                    onNavigateToExactAlarmSettings = {
                        startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName"),
                            )
                        )
                    },
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
