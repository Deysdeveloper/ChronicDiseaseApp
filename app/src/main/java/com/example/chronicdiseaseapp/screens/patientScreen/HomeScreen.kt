package com.example.chronicdiseaseapp.screens.patientScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.example.chronicdiseaseapp.viewModel.FirebaseAuthViewModel
import com.example.chronicdiseaseapp.viewModel.HealthDataViewModel
import com.example.chronicdiseaseapp.utils.*
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

@Composable
fun HomeScreen(
    authViewModel: FirebaseAuthViewModel = viewModel(),
    healthDataViewModel: HealthDataViewModel = viewModel(),
    onSignOut: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.observeAsState()
    val userProfile by authViewModel.userProfile.observeAsState()

    // Samsung Health data from Galaxy Watch 4
    val healthMetrics by healthDataViewModel.healthMetrics.observeAsState()
    val isHealthDataLoading by healthDataViewModel.isLoading.observeAsState(false)
    val healthError by healthDataViewModel.errorMessage.observeAsState()
    val syncStatus by healthDataViewModel.syncStatus.observeAsState()

    // Privacy and permission management
    val privacyManager = remember { PrivacyManager(context) }
    val permissionHandler = remember { HealthConnectPermissionHandler(context) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHealthConsentDialog by remember { mutableStateOf(false) }

    // Health Connect permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = permissionHandler.getPermissionRequestContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(permissionHandler.requiredPermissions.map { it.toString() }
                .toSet())) {
            privacyManager.giveHealthConsent()
            healthDataViewModel.refreshData()
        }
    }

    // Check privacy status on startup and load user profile
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            // Always load the user profile when component mounts or user changes
            Log.d("HomeScreen", "Loading profile for user: $uid")
            authViewModel.loadCurrentUserProfile()
        }

        // Check if privacy notice has been acknowledged
        if (!privacyManager.hasAcknowledgedPrivacy()) {
            showPrivacyDialog = true
        } else if (!privacyManager.hasGivenHealthConsent()) {
            // Check if Health Connect permissions are available and not granted
            val permissionStatus = permissionHandler.getPermissionStatusSummary()
            if (permissionStatus == PermissionStatus.NotGranted) {
                showHealthConsentDialog = true
            }
        }
    }

    // Log profile data when it changes
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            Log.d("HomeScreen", "Profile loaded - Name: ${profile.fullName}, Age: ${profile.age}")
        } ?: Log.d("HomeScreen", "Profile is null")
    }

    // Privacy Notice Dialog
    if (showPrivacyDialog) {
        PrivacyNoticeDialog(
            onAccept = {
                privacyManager.acknowledgePrivacy()
                showPrivacyDialog = false
                showHealthConsentDialog = true // Directly show health consent dialog
            },
            onDecline = {
                showPrivacyDialog = false
                // User declined privacy, app can still work with sample data
            }
        )
    }

    // Health Data Consent Dialog
    if (showHealthConsentDialog) {
        HealthDataConsentDialog(
            onGrantPermissions = {
                showHealthConsentDialog = false
                permissionLauncher.launch(permissionHandler.requiredPermissions.map { it.toString() }
                    .toSet())
            },
            onSkip = {
                showHealthConsentDialog = false
                // User skipped health permissions, use sample data
            }
        )
    }

    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.Dashboard,
                    onClick = { selectedTab = BottomTab.Dashboard },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.Insights,
                    onClick = { selectedTab = BottomTab.Insights },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Insights") },
                    label = { Text("Insights") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.Profile,
                    onClick = { selectedTab = BottomTab.Profile },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Health",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val userPhotoUrl = userProfile?.photoUrl
            val isLoading by authViewModel.isLoading.observeAsState(false)
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    uri?.let {
                        authViewModel.updateProfilePhoto(it) { success, error ->
                        }
                    }
                }
            )

            // Profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8E0F5))
                        .clickable(enabled = !isLoading) { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (userPhotoUrl != null && userPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(userPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF6A5ACD),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(start = 16.dp)) {
                    // Display name
                    val displayName = userProfile?.fullName
                        ?: currentUser?.displayName
                        ?: if (isLoading) "Loading..." else "Your Name"

                    Text(
                        text = displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )

                    // Display age with proper formatting
                    val age = userProfile?.age
                    val ageText = when {
                        age != null && age > 0 -> "Age $age"
                        isLoading -> "Loading..."
                        else -> "Your Age"
                    }

                    Text(
                        text = ageText,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Dashboard heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTab == BottomTab.Dashboard) {
                    Text(
                        text = "Health Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222)
                    )

                    // Sync status and refresh button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        syncStatus?.let { status ->
                            if (status.isConnected) {
                                Text(
                                    text = "Galaxy Watch 4",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                healthDataViewModel.refreshData()
                                healthDataViewModel.clearError()
                            },
                            enabled = !isHealthDataLoading
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh health data",
                                tint = if (isHealthDataLoading) Color.Gray else Color(0xFF666666)
                            )
                        }

                        // Debug button for troubleshooting
                        IconButton(
                            onClick = {
                                // Manual permission check and data refresh for debugging
                                healthDataViewModel.debugRefreshPermissions()
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Debug permissions",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                } else if (selectedTab == BottomTab.Insights) {
                    Text(
                        text = "Insights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                } else {
                    Text(
                        text = "Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                }
            }

            when (selectedTab) {
                BottomTab.Dashboard -> {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Health data sync status and error handling
                    healthError?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Text(
                                text = "Health data sync issue: $error",
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    syncStatus?.let { status ->
                        if (status.lastSyncTime > 0) {
                            val syncTime = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                .format(Date(status.lastSyncTime))
                            Text(
                                text = "Last sync: $syncTime",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dashboard grid 2x2 - Updated with real Samsung Health data
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Data source indicator
                        val heartRateData by healthDataViewModel.heartRateData.observeAsState()
                        val bloodPressureData by healthDataViewModel.bloodPressureData.observeAsState()

                        val isSampleData =
                            heartRateData?.firstOrNull()?.source?.contains("Sample Data") == true

                        if (isSampleData) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFFF57C00),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Using Sample Data",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF57C00)
                                        )
                                        Text(
                                            text = "No Health Connect data found. Check logs for details.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF795548)
                                        )
                                    }
                                }
                            }
                        } else if (bloodPressureData?.isNotEmpty() == true) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Connected to Health Connect",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            DashboardCard(
                                title = "Heart Rate",
                                value = healthMetrics?.getDisplayText("heartRate") ?: "—",
                                unit = "bpm",
                                isLoading = isHealthDataLoading,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            DashboardCard(
                                title = "Blood Pressure",
                                value = healthMetrics?.getDisplayText("bloodPressure") ?: "—/—",
                                unit = "mmHg",
                                isLoading = isHealthDataLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DashboardCard(
                                title = "SpO2",
                                value = healthMetrics?.getDisplayText("spO2") ?: "—",
                                unit = "%",
                                isLoading = isHealthDataLoading,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            DashboardCard(
                                title = "Steps",
                                value = healthMetrics?.getDisplayText("steps") ?: "—",
                                unit = "steps",
                                isLoading = isHealthDataLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Trends",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Trends Card with updated data
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Heart Rate Trends",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF555555)
                                    )
                                    Text(
                                        healthMetrics?.getDisplayText("heartRate") + " bpm"
                                            ?: "— bpm",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF222222)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val trendText = when (healthMetrics?.weeklyTrend) {
                                        com.example.chronicdiseaseapp.datamodels.HealthTrend.IMPROVING -> "+2%"
                                        com.example.chronicdiseaseapp.datamodels.HealthTrend.DECLINING -> "-3%"
                                        else -> "0%"
                                    }
                                    val trendColor = when (healthMetrics?.weeklyTrend) {
                                        com.example.chronicdiseaseapp.datamodels.HealthTrend.IMPROVING -> Color(
                                            0xFF4CAF50
                                        )

                                        com.example.chronicdiseaseapp.datamodels.HealthTrend.DECLINING -> Color(
                                            0xFFD64545
                                        )

                                        else -> Color(0xFF666666)
                                    }
                                    Text(
                                        trendText,
                                        color = trendColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("Last 7 Days", color = Color(0xFF999999), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                val width = size.width
                                val height = size.height
                                val points = listOf(0f, 0.2f, 0.1f, 0.4f, 0.15f, 0.8f, 0.5f, 0.7f)
                                val path = Path()
                                points.forEachIndexed { index, v ->
                                    val x = width * (index.toFloat() / (points.size - 1))
                                    val y = height * (1f - v)
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF64A9FF),
                                    style = Stroke(width = 6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    "Mon",
                                    "Tue",
                                    "Wed",
                                    "Thu",
                                    "Fri",
                                    "Sat",
                                    "Sun"
                                ).forEach { day ->
                                    Text(day, color = Color(0xFF999999), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                BottomTab.Insights -> {
                    InsightsScreen()
                }

                BottomTab.Profile -> {
                    ProfileScreen(authViewModel = authViewModel, onSignOut = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isLoading: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                color = Color(0xFF777777),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF64A9FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading...", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(unit, color = Color(0xFFAAAAAA))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}

private enum class BottomTab { Dashboard, Insights, Profile }