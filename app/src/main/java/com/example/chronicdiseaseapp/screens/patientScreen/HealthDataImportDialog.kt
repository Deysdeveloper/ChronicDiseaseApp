package com.example.chronicdiseaseapp.screens.patientScreen

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight

/**
 * Dialog to confirm importing device Health Connect data into user's account
 *
 * This is important because Health Connect data is device-scoped, not user-scoped.
 * We want explicit user consent before importing potentially sensitive health data.
 */
@Composable
fun HealthDataImportDialog(
    onImportConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Health Data?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Would you like to import health readings from this device into your account?\n\n" +
                        "This includes:\n" +
                        "• Heart rate data\n" +
                        "• Blood pressure readings\n" +
                        "• Oxygen saturation (SpO2)\n" +
                        "• Step count\n\n" +
                        "Note: Health data is stored on the device and may include readings from previous users of this device.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onImportConfirmed) {
                Text("Yes, Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}

/**
 * Preference key for tracking if user was asked about import
 */
const val PREF_HEALTH_IMPORT_ASKED = "health_import_asked"
