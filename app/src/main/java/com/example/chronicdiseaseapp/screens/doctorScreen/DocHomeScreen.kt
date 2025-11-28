package com.example.chronicdiseaseapp.screens.doctorScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chronicdiseaseapp.datamodels.*
import com.example.chronicdiseaseapp.viewModel.DoctorHomeViewModel

private enum class DoctorTab { Dashboard, Patients, Profile }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocHomeScreen(
    viewModel: DoctorHomeViewModel = viewModel(),
    onNavigateToPatientDetails: (String) -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToPatientList: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    // Collect state from ViewModel
    val doctorProfile by viewModel.doctorProfile.collectAsState()
    val doctorStats by viewModel.doctorStats.collectAsState()
    val recentPatients by viewModel.recentPatients.collectAsState()
    val recentActivities by viewModel.recentActivities.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(DoctorTab.Dashboard) }

    // Calculate unread notifications count
    val unreadNotificationsCount = notifications.count { !it.isRead }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == DoctorTab.Dashboard,
                    onClick = { selectedTab = DoctorTab.Dashboard },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == DoctorTab.Patients,
                    onClick = { selectedTab = DoctorTab.Patients },
                    icon = {
                        if (unreadNotificationsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    ) {
                                        Text(unreadNotificationsCount.toString())
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Patients")
                            }
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Patients")
                        }
                    },
                    label = { Text("Patients") }
                )
                NavigationBarItem(
                    selected = selectedTab == DoctorTab.Profile,
                    onClick = { selectedTab = DoctorTab.Profile },
                    icon = { Icon(Icons.Default.AccountBox, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF6A5ACD))
            }
        } else {
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
                        text = "Doctor Portal",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            .background(Color(0xFFE8E0F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Doctor Profile",
                            tint = Color(0xFF6A5ACD),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = doctorProfile?.fullName ?: "Doctor",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                        Text(
                            text = doctorProfile?.medicalExpertise ?: "Medical Professional",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error message
                errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red
                            )
                            Text(
                                text = error,
                                color = Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss", color = Color.Red)
                            }
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    DoctorTab.Dashboard -> {
                        DashboardContent(
                            doctorStats = doctorStats,
                            recentActivities = recentActivities,
                            onRefresh = { viewModel.refreshData() }
                        )
                    }
                    DoctorTab.Patients -> {
                        PatientsContent(
                            recentPatients = recentPatients,
                            notifications = notifications,
                            onNavigateToPatientDetails = onNavigateToPatientDetails,
                            onNavigateToPatientList = onNavigateToPatientList,
                            onMarkNotificationRead = { viewModel.markNotificationAsRead(it) }
                        )
                    }
                    DoctorTab.Profile -> {
                        ProfileContent(
                            doctorProfile = doctorProfile,
                            onSignOut = onSignOut,
                            onNavigateToProfile = onNavigateToProfile,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    doctorStats: DoctorStats,
    recentActivities: List<RecentActivity>,
    onRefresh: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Custom Dashboard heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dashboard grid 2x2 - matching patient home design
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DashboardCard(
                        title = "Total Patients",
                        value = doctorStats.totalPatients.toString(),
                        unit = "patients",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    DashboardCard(
                        title = "Active Patients",
                        value = doctorStats.activePatients.toString(),
                        unit = "active",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DashboardCard(
                        title = "Today's Appointments",
                        value = doctorStats.appointmentsToday.toString(),
                        unit = "scheduled",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    DashboardCard(
                        title = "Critical Patients",
                        value = doctorStats.criticalPatients.toString(),
                        unit = "critical",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Activity Chart Card - matching patient home design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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
                                "Patient Activities",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF555555)
                            )
                            Text(
                                "${recentActivities.size} activities",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF222222)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "+${recentActivities.count { it.priority == ActivityPriority.URGENT }}",
                                color = Color(0xFFD64545),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Urgent", color = Color(0xFF999999), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Simple activity chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val points = listOf(0.3f, 0.5f, 0.2f, 0.8f, 0.4f, 0.6f, 0.3f)
                        val path = Path()
                        points.forEachIndexed { index, v ->
                            val x = width * (index.toFloat() / (points.size - 1))
                            val y = height * (1f - v)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF6A5ACD),
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientsContent(
    recentPatients: List<Patient>,
    notifications: List<DoctorNotification>,
    onNavigateToPatientDetails: (String) -> Unit,
    onNavigateToPatientList: () -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Patients",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )
                TextButton(onClick = onNavigateToPatientList) {
                    Text("View All")
                }
            }
        }

        if (recentPatients.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(recentPatients) { patient ->
                        PatientCard(
                            patient = patient,
                            onClick = { onNavigateToPatientDetails(patient.id) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Notifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(notifications) { notification ->
            NotificationCard(
                notification = notification,
                onMarkAsRead = { onMarkNotificationRead(notification.id) }
            )
        }
    }
}

@Composable
private fun ProfileContent(
    doctorProfile: UserProfile?,
    onSignOut: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: DoctorHomeViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Doctor Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )
                IconButton(onClick = { viewModel.refreshDoctorProfile() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Profile",
                        tint = Color(0xFF6A5ACD)
                    )
                }
            }
        }

        // Debug Information Card (can be commented out for production)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Debug Info",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Profile Status: ${viewModel.getDoctorProfileStatus()}",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                    Text(
                        text = "UID: ${doctorProfile?.uid ?: "null"}",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                    Text(
                        text = "Firestore Data Loaded: ${doctorProfile != null}",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        item {
            // Personal Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Personal Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ProfileInfoRow(
                        label = "Name",
                        value = doctorProfile?.fullName ?: "—",
                        icon = Icons.Default.Person
                    )

                    ProfileInfoRow(
                        label = "Email",
                        value = doctorProfile?.email ?: "—",
                        icon = Icons.Default.Email
                    )

                    ProfileInfoRow(
                        label = "Phone",
                        value = doctorProfile?.phoneNumber ?: "—",
                        icon = Icons.Default.Phone
                    )

                    ProfileInfoRow(
                        label = "Age",
                        value = doctorProfile?.age?.toString() ?: "—",
                        icon = Icons.Default.DateRange
                    )
                }
            }
        }

        item {
            // Professional Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Professional Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ProfileInfoRow(
                        label = "Medical Expertise",
                        value = doctorProfile?.medicalExpertise ?: "—",
                        icon = Icons.Default.Settings
                    )

                    ProfileInfoRow(
                        label = "Current Hospital",
                        value = doctorProfile?.currentHospital ?: "—",
                        icon = Icons.Default.Place
                    )

                    ProfileInfoRow(
                        label = "License Number",
                        value = doctorProfile?.licenseNumber ?: "—",
                        icon = Icons.Default.Info
                    )

                    ProfileInfoRow(
                        label = "Specialization",
                        value = doctorProfile?.specialization ?: "—",
                        icon = Icons.Default.Star
                    )
                }
            }
        }

        item {
            // Account Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ProfileInfoRow(
                        label = "User Type",
                        value = doctorProfile?.userType?.name ?: "—",
                        icon = Icons.Default.AccountBox
                    )

                    ProfileInfoRow(
                        label = "Terms Accepted",
                        value = if (doctorProfile?.termsAccepted == true) "Yes" else "No",
                        icon = Icons.Default.CheckCircle
                    )

                    val createdAt = doctorProfile?.createdAt?.let { timestamp ->
                        val date = java.util.Date(timestamp)
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(date)
                    } ?: "—"

                    ProfileInfoRow(
                        label = "Account Created",
                        value = createdAt,
                        icon = Icons.Default.DateRange
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

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
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6A5ACD),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF777777),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color(0xFF222222),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun PatientCard(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = patient.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Age ${patient.age}",
                        fontSize = 12.sp,
                        color = Color(0xFF777777)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(patient.riskLevel.color)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = patient.primaryCondition,
                fontSize = 14.sp,
                color = Color(0xFF555555),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Last: ${patient.lastVisitDisplay}",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: DoctorNotification,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                Color.White
            else
                Color(0xFFF3E5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = if (!notification.isRead) onMarkAsRead else {
            {}
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            val iconColor = when (notification.type) {
                NotificationType.EMERGENCY -> Color(0xFFF44336)
                NotificationType.WARNING -> Color(0xFFFF9800)
                NotificationType.LAB_RESULT -> Color(0xFF2196F3)
                NotificationType.APPOINTMENT -> Color(0xFF4CAF50)
                else -> Color(0xFF666666)
            }

            val icon = when (notification.type) {
                NotificationType.EMERGENCY -> Icons.Default.Warning
                NotificationType.LAB_RESULT -> Icons.Default.Info
                NotificationType.APPOINTMENT -> Icons.Default.DateRange
                else -> Icons.Default.Notifications
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6A5ACD))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DocHomeScreenPreview() {
    DocHomeScreen()
}
