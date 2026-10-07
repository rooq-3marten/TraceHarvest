package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.TraceHarvestApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TraceHarvestViewModel

/**
 * Main entry activity for the TraceHarvest Android application.
 *
 * Designed with strict Single Responsibility Principle (SRP):
 * delegates UI layout, responsive navigation, and state rendering
 * to [TraceHarvestApp] within the centralized [MyApplicationTheme].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: TraceHarvestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if recovered from unexpected failure
        if (com.example.core.resilience.CrashGuard.wasRecoveredFromCrash(this) ||
            intent.getBooleanExtra("EXTRA_RECOVERED_FROM_CRASH", false)
        ) {
            android.widget.Toast.makeText(
                this,
                "✓ Session restored safely. Offline database and records are intact.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            MyApplicationTheme {
                TraceHarvestApp(viewModel = viewModel)
            }
        }
    }
}
