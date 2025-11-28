package com.example.chronicdiseaseapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class PrivacyManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "privacy_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_PRIVACY_ACKNOWLEDGED = "privacy_acknowledged"
        private const val KEY_HEALTH_CONSENT_GIVEN = "health_consent_given"
    }

    /**
     * Check if user has acknowledged privacy notice
     */
    fun hasAcknowledgedPrivacy(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_ACKNOWLEDGED, false)
    }

    /**
     * Mark privacy notice as acknowledged
     */
    fun acknowledgePrivacy() {
        prefs.edit().putBoolean(KEY_PRIVACY_ACKNOWLEDGED, true).apply()
    }

    /**
     * Check if user has given health data consent
     */
    fun hasGivenHealthConsent(): Boolean {
        return prefs.getBoolean(KEY_HEALTH_CONSENT_GIVEN, false)
    }

    /**
     * Mark health data consent as given
     */
    fun giveHealthConsent() {
        prefs.edit().putBoolean(KEY_HEALTH_CONSENT_GIVEN, true).apply()
    }

    /**
     * Reset all privacy preferences (for testing or settings)
     */
    fun resetPrivacyPreferences() {
        prefs.edit().clear().apply()
    }
}

@Composable
fun PrivacyNoticeDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(onDismissRequest = { /* Prevent dismissal without choice */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Notice",
                    tint = Color(0xFF64A9FF),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Privacy Notice",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "This app uses Health Connect to read your health data from Samsung Health and Galaxy Watch for chronic disease management.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Data Usage:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val dataPoints = listOf(
                            "• Heart rate from Galaxy Watch",
                            "• Blood pressure measurements",
                            "• SpO2 (oxygen saturation) data",
                            "• Daily steps and activity",
                            "• Calories burned information"
                        )

                        dataPoints.forEach { point ->
                            Text(
                                text = point,
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Data Protection:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val protections = listOf(
                            "• Data stays on your device only",
                            "• Not shared with external servers",
                            "• Used only for app functionality",
                            "• You control all permissions"
                        )

                        protections.forEach { protection ->
                            Text(
                                text = protection,
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        )
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF64A9FF)
                        )
                    ) {
                        Text("Accept & Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun HealthDataConsentDialog(
    onGrantPermissions: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(onDismissRequest = onSkip) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Health Connect",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect Health Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "To show your real health data from Samsung Health and Galaxy Watch, we need your permission to access Health Connect.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This will open Health Connect settings where you can grant permissions for:",
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val permissions = listOf(
                    "Heart Rate", "Blood Pressure", "SpO2", "Steps", "Calories"
                )

                Text(
                    text = permissions.joinToString(" • "),
                    fontSize = 13.sp,
                    color = Color(0xFF64A9FF),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip for Now", color = Color(0xFF666666))
                    }

                    Button(
                        onClick = onGrantPermissions,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    }
}