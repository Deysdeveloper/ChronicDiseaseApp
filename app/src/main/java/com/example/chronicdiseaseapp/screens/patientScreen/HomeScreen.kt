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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.example.chronicdiseaseapp.viewModel.FirebaseAuthViewModel
import com.example.chronicdiseaseapp.viewModel.HealthDataViewModel
import com.example.chronicdiseaseapp.viewModel.AnomalyDetectionViewModel
import com.example.chronicdiseaseapp.utils.*
import com.example.chronicdiseaseapp.datamodels.HealthReading
import com.example.chronicdiseaseapp.ml.AnomalyResult
import com.example.chronicdiseaseapp.ml.AnomalySeverity
import com.example.chronicdiseaseapp.ml.ModelTester
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    authViewModel: FirebaseAuthViewModel = viewModel(),
    healthDataViewModel: HealthDataViewModel = viewModel(),
    anomalyViewModel: AnomalyDetectionViewModel = viewModel(),
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

    // Trend detail dialog state
    var showTrendDetail by remember { mutableStateOf<TrendType?>(null) }

    // Observe all health data for trends
    val heartRateData by healthDataViewModel.heartRateData.observeAsState(emptyList())
    val bloodPressureData by healthDataViewModel.bloodPressureData.observeAsState(emptyList())
    val spO2Data by healthDataViewModel.spO2Data.observeAsState(emptyList())
    val stepsData by healthDataViewModel.stepsData.observeAsState(emptyList())

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
                    selected = selectedTab == BottomTab.Analysis,
                    onClick = { selectedTab = BottomTab.Analysis },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Analysis") },
                    label = { Text("Analysis") }
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
                } else if (selectedTab == BottomTab.Analysis) {
                    Text(
                        text = "Health Analysis",
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
                    // Make entire dashboard scrollable
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                                val syncTime =
                                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
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
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFFFF8E1
                                        )
                                    )
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
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFE8F5E9
                                        )
                                    )
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

                        // All Trend Cards (no separate scroll - part of main scroll)
                        // Heart Rate Trend Card
                        TrendCard(
                            title = "Heart Rate Trends",
                            currentValue = healthMetrics?.getDisplayText("heartRate") ?: "—",
                            unit = "bpm",
                            trend = healthMetrics?.weeklyTrend,
                            color = Color(0xFFFF6B6B),
                            dataPoints = heartRateData.takeLast(7)
                                .mapNotNull { it.heartRate?.toFloat() },
                            onClick = { showTrendDetail = TrendType.HEART_RATE }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // SpO2 Trend Card
                        TrendCard(
                            title = "SpO2 Trends",
                            currentValue = healthMetrics?.getDisplayText("spO2") ?: "—",
                            unit = "%",
                            trend = healthMetrics?.weeklyTrend,
                            color = Color(0xFF4ECDC4),
                            dataPoints = spO2Data.takeLast(7)
                                .mapNotNull { it.oxygenSaturation?.toFloat() },
                            onClick = { showTrendDetail = TrendType.SPO2 }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Blood Pressure Trend Card
                        TrendCard(
                            title = "Blood Pressure Trends",
                            currentValue = healthMetrics?.getDisplayText("bloodPressure") ?: "—/—",
                            unit = "mmHg",
                            trend = healthMetrics?.weeklyTrend,
                            color = Color(0xFF95E1D3),
                            dataPoints = bloodPressureData.takeLast(7)
                                .mapNotNull { it.bloodPressureSystolic?.toFloat() },
                            onClick = { showTrendDetail = TrendType.BLOOD_PRESSURE }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Steps Trend Card
                        TrendCard(
                            title = "Steps Trends",
                            currentValue = healthMetrics?.getDisplayText("steps") ?: "—",
                            unit = "steps",
                            trend = healthMetrics?.weeklyTrend,
                            color = Color(0xFFFFBE76),
                            dataPoints = stepsData.takeLast(7)
                                .mapNotNull { it.stepsCount?.toFloat() },
                            onClick = { showTrendDetail = TrendType.STEPS }
                        )

                        // Add bottom padding for better scrolling experience
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Trend Detail Dialog
                    showTrendDetail?.let { trendType ->
                        TrendDetailDialog(
                            trendType = trendType,
                            data = when (trendType) {
                                TrendType.HEART_RATE -> heartRateData.sortedBy { it.timestamp }
                                TrendType.SPO2 -> spO2Data.sortedBy { it.timestamp }
                                TrendType.BLOOD_PRESSURE -> bloodPressureData.sortedBy { it.timestamp }
                                TrendType.STEPS -> stepsData.sortedBy { it.timestamp }
                            },
                            onDismiss = { showTrendDetail = null }
                        )
                    }
                }

                BottomTab.Insights -> {
                    InsightsScreen()
                }

                BottomTab.Analysis -> {
                    HealthAnalysisScreen(
                        heartRateData = heartRateData,
                        bloodPressureData = bloodPressureData,
                        spO2Data = spO2Data,
                        stepsData = stepsData,
                        anomalyViewModel = anomalyViewModel
                    )
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

@Composable
private fun TrendCard(
    title: String,
    currentValue: String,
    unit: String,
    trend: com.example.chronicdiseaseapp.datamodels.HealthTrend?,
    color: Color,
    dataPoints: List<Float>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        title,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currentValue $unit",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val trendText = when (trend) {
                        com.example.chronicdiseaseapp.datamodels.HealthTrend.IMPROVING -> "+2%"
                        com.example.chronicdiseaseapp.datamodels.HealthTrend.DECLINING -> "-3%"
                        else -> "0%"
                    }
                    val trendColor = when (trend) {
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text("Last 7 Days", color = Color(0xFF999999), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini trend chart
            if (dataPoints.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Normalize data points to 0-1 range
                    val minValue = dataPoints.minOrNull() ?: 0f
                    val maxValue = dataPoints.maxOrNull() ?: 1f
                    val range = maxValue - minValue

                    val normalizedPoints = if (range > 0) {
                        dataPoints.map { (it - minValue) / range }
                    } else {
                        dataPoints.map { 0.5f }
                    }

                    val path = Path()
                    normalizedPoints.forEachIndexed { index, v ->
                        val x =
                            width * (index.toFloat() / (normalizedPoints.size - 1).coerceAtLeast(1))
                        val y = height * (1f - v)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 4f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data available",
                        color = Color(0xFF999999),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Tap to view details",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TrendDetailDialog(
    trendType: TrendType,
    data: List<HealthReading>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (trendType) {
                            TrendType.HEART_RATE -> "Heart Rate History"
                            TrendType.SPO2 -> "SpO2 History"
                            TrendType.BLOOD_PRESSURE -> "Blood Pressure History"
                            TrendType.STEPS -> "Steps History"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Showing ${data.size} readings from oldest to latest",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Data list
                if (data.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No data",
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No data available",
                                color = Color(0xFF999999),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        data.forEachIndexed { index, reading ->
                            TrendDataItem(
                                reading = reading,
                                trendType = trendType,
                                index = index + 1,
                                total = data.size
                            )
                            if (index < data.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64A9FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun TrendDataItem(
    reading: HealthReading,
    trendType: TrendType,
    index: Int,
    total: Int
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(reading.timestamp))

    val (value, unit, color) = when (trendType) {
        TrendType.HEART_RATE -> Triple(
            reading.heartRate?.toString() ?: "—",
            "bpm",
            Color(0xFFFF6B6B)
        )

        TrendType.SPO2 -> Triple(
            reading.oxygenSaturation?.toString() ?: "—",
            "%",
            Color(0xFF4ECDC4)
        )

        TrendType.BLOOD_PRESSURE -> Triple(
            if (reading.bloodPressureSystolic != null && reading.bloodPressureDiastolic != null) {
                "${reading.bloodPressureSystolic}/${reading.bloodPressureDiastolic}"
            } else "—/—",
            "mmHg",
            Color(0xFF95E1D3)
        )

        TrendType.STEPS -> Triple(
            reading.stepsCount?.toString() ?: "—",
            "steps",
            Color(0xFFFFBE76)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reading number and date
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$index",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reading.source,
                    fontSize = 10.sp,
                    color = Color(0xFF999999)
                )
            }

            // Value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
private fun HealthAnalysisScreen(
    heartRateData: List<HealthReading>,
    bloodPressureData: List<HealthReading>,
    spO2Data: List<HealthReading>,
    stepsData: List<HealthReading>,
    anomalyViewModel: AnomalyDetectionViewModel
) {
    // Observe ML analysis results
    val anomalies by anomalyViewModel.anomalies.observeAsState(emptyList())
    val isAnalyzing by anomalyViewModel.isAnalyzing.observeAsState(false)
    val modelInitialized by anomalyViewModel.modelInitialized.observeAsState(false)
    val errorMessage by anomalyViewModel.errorMessage.observeAsState()
    val hasAnalyzed by anomalyViewModel.hasAnalyzed.observeAsState(false)

    val context = LocalContext.current
    var testResults by remember { mutableStateOf<String?>(null) }

    // Calculate valid reading counts (filter corrupted data)
    val validHeartRateCount = remember(heartRateData) {
        heartRateData.count { reading ->
            val hr = reading.heartRate?.toFloat()
            hr != null && hr >= 20f && hr <= 250f
        }
    }

    val validSpO2Count = remember(spO2Data) {
        spO2Data.count { reading ->
            val spo2 = reading.oxygenSaturation?.toFloat()
            spo2 != null && spo2 >= 0f && spo2 <= 100f
        }
    }

    val validSystolicCount = remember(bloodPressureData) {
        bloodPressureData.count { reading ->
            val sys = reading.bloodPressureSystolic?.toFloat()
            sys != null && sys >= 60f && sys <= 250f
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {

        // Model Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (modelInitialized) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (modelInitialized) "✅" else "⏳",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (modelInitialized) "ML Model Ready" else "Initializing ML Model...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (modelInitialized) Color(0xFF2E7D32) else Color(0xFFF57C00)
                    )
                    Text(
                        text = if (modelInitialized)
                            "AI-powered anomaly detection active"
                        else
                            "Loading TensorFlow Lite model",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Test Model Button
        if (modelInitialized) {
            Button(
                onClick = {
                    testResults = "Running tests..."
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val tester = ModelTester(context)
                        val results = tester.runTests()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            testResults = if (results.allPassed()) {
                                "✅ ALL TESTS PASSED (${results.passedCount()}/5)"
                            } else {
                                "⚠️ ${results.passedCount()}/5 TESTS PASSED - Check Logcat"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🧪 Run Model Tests", fontSize = 14.sp)
            }

            // Test Results Display
            testResults?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.contains("ALL TESTS PASSED"))
                            Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = result,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Summary Section
        Text(
            text = "Available Data for Analysis",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Heart Rate Data
        AnalysisDataCard(
            title = "Heart Rate Readings",
            dataCount = heartRateData.size,
            validCount = validHeartRateCount,
            icon = "❤️",
            color = Color(0xFFFF6B6B),
            description = "BPM measurements",
            onAnalyze = if (modelInitialized && validHeartRateCount >= 10) {
                { anomalyViewModel.analyzeHeartRate(heartRateData) }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Blood Pressure Data
        AnalysisDataCard(
            title = "Blood Pressure Readings",
            dataCount = bloodPressureData.size,
            validCount = validSystolicCount,
            icon = "🩺",
            color = Color(0xFF95E1D3),
            description = "Systolic/Diastolic measurements",
            onAnalyze = if (modelInitialized && validSystolicCount >= 10) {
                { anomalyViewModel.analyzeBloodPressure(bloodPressureData, useSystolic = true) }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // SpO2 Data
        AnalysisDataCard(
            title = "SpO2 Readings",
            dataCount = spO2Data.size,
            validCount = validSpO2Count,
            icon = "🫁",
            color = Color(0xFF4ECDC4),
            description = "Oxygen saturation levels",
            onAnalyze = if (modelInitialized && validSpO2Count >= 10) {
                { anomalyViewModel.analyzeSpO2(spO2Data) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Analyze All Button
        if (modelInitialized) {
            Button(
                onClick = {
                    anomalyViewModel.analyzeAllVitals(
                        heartRateData = heartRateData,
                        spO2Data = spO2Data,
                        bloodPressureData = bloodPressureData
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Text("🔍 Analyze All Vitals", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Error",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        fontSize = 13.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ML Analysis Results
        Text(
            text = "Anomaly Detection Results",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (anomalies.isEmpty() && !isAnalyzing && !hasAnalyzed) {
            // No analysis has been run yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Analysis Yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click 'Analyze All Vitals' or individual cards to detect anomalies",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (anomalies.isEmpty() && !isAnalyzing && hasAnalyzed) {
            // Analysis completed with 0 anomalies - show success message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✅",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All Clear!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analysis complete: No anomalies detected. Your health data appears normal! 🎉",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The ML model analyzed your vitals and found no concerning patterns.",
                        fontSize = 11.sp,
                        color = Color(0xFF666666),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (anomalies.isEmpty() && isAnalyzing) {
            // Analyzing
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing Your Health Data...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )
                }
            }
        } else {
            // Display anomalies
            anomalies.forEach { anomaly ->
                AnomalyCard(anomaly = anomaly)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AnomalyCard(anomaly: AnomalyResult) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(anomaly.timestamp))

    val (bgColor, borderColor) = when (anomaly.severity) {
        AnomalySeverity.CRITICAL -> Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
        AnomalySeverity.HIGH -> Pair(Color(0xFFFFF3E0), Color(0xFFF57C00))
        AnomalySeverity.MEDIUM -> Pair(Color(0xFFFFF9C4), Color(0xFFFBC02D))
        AnomalySeverity.LOW -> Pair(Color(0xFFE8F5E9), Color(0xFF4CAF50))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anomaly.getMessage(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Time",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Deviation",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "±${String.format("%.1f", anomaly.deviation)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Confidence: ${anomaly.votes} votes",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "Expected: ${String.format("%.1f", anomaly.localMedian)}",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun AnalysisDataCard(
    title: String,
    dataCount: Int,
    validCount: Int? = null,
    icon: String,
    color: Color,
    description: String,
    onAnalyze: (() -> Unit)? = null
) {
    val hasInvalidData = validCount != null && validCount < dataCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onAnalyze != null) Modifier.clickable { onAnalyze() }
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222)
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                // Show valid count if different from total
                if (hasInvalidData) {
                    Text(
                        text = "${validCount}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "valid / $dataCount total",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800)
                    )
                } else {
                    Text(
                        text = dataCount.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "readings",
                        fontSize = 11.sp,
                        color = Color(0xFF999999)
                    )
                }
                if (onAnalyze != null) {
                    Text(
                        text = "Tap to analyze",
                        fontSize = 10.sp,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                } else if (hasInvalidData && (validCount ?: 0) < 10) {
                    Text(
                        text = "Need ${10 - (validCount ?: 0)} more",
                        fontSize = 10.sp,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
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

private enum class BottomTab { Dashboard, Insights, Analysis, Profile }

private enum class TrendType { HEART_RATE, SPO2, BLOOD_PRESSURE, STEPS }