package com.example.chronicdiseaseapp.screens.patientScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chronicdiseaseapp.viewModel.FirebaseAuthViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.chronicdiseaseapp.utils.PrivacyManager

@Composable
fun ProfileScreen(
    authViewModel: FirebaseAuthViewModel = viewModel(),
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.observeAsState()
    val userProfile by authViewModel.userProfile.observeAsState()
    val isLoading by authViewModel.isLoading.observeAsState(false)

    val privacyManager = remember { PrivacyManager(context) }
    var showPrivacyInfo by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        authViewModel.loadCurrentUserProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Profile",
                        tint = Color(0xFF64A9FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Profile Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF222222)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProfileInfoItem("Name", userProfile?.fullName ?: currentUser?.displayName ?: "—")
                ProfileInfoItem("Email", currentUser?.email ?: "—")
                ProfileInfoItem("Age", userProfile?.age?.toString() ?: "—")
                ProfileInfoItem("Phone", userProfile?.phoneNumber ?: "—")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Privacy & Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF222222)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Privacy Notice:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "This project accesses health and fitness data (heart rate, SpO2, blood pressure, steps) from Health Connect on your device to display daily activity insights for chronic disease management. No data is shared externally or stored beyond the local device.",
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showPrivacyInfo = !showPrivacyInfo },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        if (showPrivacyInfo) "Hide Privacy Details" else "Show Privacy Details",
                        fontSize = 14.sp
                    )
                }

                if (showPrivacyInfo) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Data Processing:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF222222)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val dataProcessing = listOf(
                                "• Data accessed through Health Connect API",
                                "• Information stays on your device only",
                                "• No cloud storage or external sharing",
                                "• Used only for app functionality",
                                "• You control all data permissions"
                            )

                            dataProcessing.forEach { item ->
                                Text(
                                    text = item,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Data Types:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF222222)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val dataTypes = listOf(
                                "• Heart Rate (from Galaxy Watch/Samsung Health)",
                                "• Blood Pressure measurements",
                                "• SpO2 (oxygen saturation) levels",
                                "• Daily steps and activity data",
                                "• Calories burned information"
                            )

                            dataTypes.forEach { item ->
                                Text(
                                    text = item,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "This is a college project app for educational purposes.",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Red
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Sign Out",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileInfoItem(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF888888),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF222222),
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
    }
}
