package com.example.chronicdiseaseapp.screens.patientScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chronicdiseaseapp.datamodels.ConnectionStatus
import com.example.chronicdiseaseapp.datamodels.DoctorInfo
import com.example.chronicdiseaseapp.datamodels.DoctorFeedback
import com.example.chronicdiseaseapp.datamodels.FeedbackSeverity
import com.example.chronicdiseaseapp.viewModel.DoctorConnectionViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class DoctorScreenTab { ALL_DOCTORS, MY_CONNECTIONS, FEEDBACK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsListScreen(
    viewModel: DoctorConnectionViewModel = viewModel()
) {
    val doctors by viewModel.filteredDoctors.collectAsState(initial = emptyList())
    val feedback by viewModel.feedback.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
    val successMessage by viewModel.successMessage.collectAsState(initial = null)
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")

    var selectedTab by remember { mutableStateOf(DoctorScreenTab.ALL_DOCTORS) }
    var showConnectionDialog by remember { mutableStateOf<DoctorInfo?>(null) }

    val unreadFeedbackCount = feedback.count { !it.isRead }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search doctors by name, specialization, hospital...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF6A5ACD),
                unfocusedBorderColor = Color.LightGray
            )
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.White,
            contentColor = Color(0xFF6A5ACD)
        ) {
            Tab(
                selected = selectedTab == DoctorScreenTab.ALL_DOCTORS,
                onClick = { selectedTab = DoctorScreenTab.ALL_DOCTORS },
                text = { Text("All Doctors") }
            )
            Tab(
                selected = selectedTab == DoctorScreenTab.MY_CONNECTIONS,
                onClick = { selectedTab = DoctorScreenTab.MY_CONNECTIONS },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Connected")
                        val connectedCount = doctors.count { it.isConnected }
                        if (connectedCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(
                                containerColor = Color(0xFF4CAF50)
                            ) {
                                Text("$connectedCount", color = Color.White)
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == DoctorScreenTab.FEEDBACK,
                onClick = { selectedTab = DoctorScreenTab.FEEDBACK },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Feedback")
                        if (unreadFeedbackCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(
                                containerColor = Color.Red
                            ) {
                                Text("$unreadFeedbackCount", color = Color.White)
                            }
                        }
                    }
                }
            )
        }

        // Success message
        successMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = message,
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearSuccess() }) {
                        Text("Dismiss", color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

        // Content based on selected tab
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF6A5ACD))
                    Text(
                        text = "Loading...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            when (selectedTab) {
                DoctorScreenTab.ALL_DOCTORS -> {
                    AllDoctorsTab(
                        doctors = doctors,
                        onConnect = { showConnectionDialog = it }
                    )
                }

                DoctorScreenTab.MY_CONNECTIONS -> {
                    MyConnectionsTab(
                        doctors = doctors.filter { it.isConnected },
                        onCancelRequest = { viewModel.cancelConnectionRequest(it) }
                    )
                }

                DoctorScreenTab.FEEDBACK -> {
                    FeedbackTab(
                        feedback = feedback,
                        onMarkAsRead = { viewModel.markFeedbackAsRead(it) }
                    )
                }
            }
        }
    }

    // Connection request dialog
    showConnectionDialog?.let { doctor ->
        ConnectionRequestDialog(
            doctor = doctor,
            onDismiss = { showConnectionDialog = null },
            onConfirm = { message ->
                viewModel.sendConnectionRequest(doctor.id, doctor.fullName, message)
                showConnectionDialog = null
            }
        )
    }
}

@Composable
private fun AllDoctorsTab(
    doctors: List<DoctorInfo>,
    onConnect: (DoctorInfo) -> Unit
) {
    if (doctors.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No doctors found",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(doctors) { doctor ->
                DoctorCard(
                    doctor = doctor,
                    onConnect = { onConnect(doctor) }
                )
            }
        }
    }
}

@Composable
private fun MyConnectionsTab(
    doctors: List<DoctorInfo>,
    onCancelRequest: (String) -> Unit
) {
    if (doctors.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No connections yet",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Connect with doctors to see them here",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(doctors) { doctor ->
                ConnectedDoctorCard(
                    doctor = doctor,
                    onCancel = { doctor.connectionRequestId?.let { onCancelRequest(it) } }
                )
            }
        }
    }
}

@Composable
private fun FeedbackTab(
    feedback: List<DoctorFeedback>,
    onMarkAsRead: (String) -> Unit
) {
    if (feedback.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No feedback yet",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Your doctors will provide feedback here",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(feedback) { item ->
                FeedbackCard(
                    feedback = item,
                    onTap = { onMarkAsRead(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoctorCard(
    doctor: DoctorInfo,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Doctor photo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8E0F5)),
                contentAlignment = Alignment.Center
            ) {
                if (doctor.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = doctor.photoUrl,
                        contentDescription = "Doctor Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF6A5ACD),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Doctor info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = doctor.fullName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF6A5ACD),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = doctor.specialization,
                        fontSize = 14.sp,
                        color = Color(0xFF6A5ACD),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = doctor.currentHospital,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = doctor.medicalExpertise,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Connection button
                when {
                    doctor.isConnected -> {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connected", fontSize = 14.sp)
                        }
                    }

                    doctor.connectionStatus == ConnectionStatus.PENDING -> {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                disabledContainerColor = Color(0xFFFF9800).copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pending", fontSize = 14.sp)
                        }
                    }

                    doctor.connectionStatus == ConnectionStatus.REJECTED -> {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                disabledContainerColor = Color.Red.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Declined", fontSize = 14.sp)
                        }
                    }

                    else -> {
                        Button(
                            onClick = onConnect,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A5ACD)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedDoctorCard(
    doctor: DoctorInfo,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (doctor.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = doctor.photoUrl,
                        contentDescription = "Doctor Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = doctor.fullName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
                Text(
                    text = doctor.specialization,
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = doctor.currentHospital,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Connected",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: DoctorFeedback,
    onTap: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(feedback.createdAt))

    val (bgColor, borderColor) = when (feedback.severity) {
        FeedbackSeverity.URGENT -> Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
        FeedbackSeverity.WARNING -> Pair(Color(0xFFFFF3E0), Color(0xFFF57C00))
        FeedbackSeverity.CAUTION -> Pair(Color(0xFFFFF9C4), Color(0xFFFBC02D))
        FeedbackSeverity.NORMAL -> Pair(Color(0xFFE8F5E9), Color(0xFF4CAF50))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(
            containerColor = if (feedback.isRead) Color.White else bgColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (!feedback.isRead) {
            androidx.compose.foundation.BorderStroke(2.dp, borderColor)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Dr. ${feedback.doctorName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                        if (!feedback.isRead) {
                            Badge(
                                containerColor = Color.Red
                            ) {
                                Text("New", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                    Text(
                        text = feedback.feedbackType.displayName,
                        fontSize = 12.sp,
                        color = Color(0xFF6A5ACD),
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = feedback.timeAgo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feedback.feedbackText,
                fontSize = 14.sp,
                color = Color(0xFF222222)
            )

            if (feedback.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recommendations:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF666666)
                )
                feedback.recommendations.forEach { rec ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("•", color = Color(0xFF6A5ACD))
                        Text(
                            text = rec,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            feedback.vitalsSnapshot?.let { vitals ->
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vitals at time of feedback:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    vitals.heartRate?.let {
                        VitalChip("HR: $it bpm", Color(0xFFE91E63))
                    }
                    if (vitals.bloodPressureSystolic != null && vitals.bloodPressureDiastolic != null) {
                        VitalChip(
                            "BP: ${vitals.bloodPressureSystolic}/${vitals.bloodPressureDiastolic}",
                            Color(0xFF2196F3)
                        )
                    }
                    vitals.spO2?.let {
                        VitalChip("SpO2: $it%", Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ConnectionRequestDialog(
    doctor: DoctorInfo,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connect with Dr. ${doctor.fullName}?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )

                Text(
                    text = "Send a connection request to this doctor. Once accepted, they can view your health vitals and provide medical feedback.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (Optional)") },
                    placeholder = { Text("Tell the doctor why you want to connect...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(message) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A5ACD)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send Request")
                    }
                }
            }
        }
    }
}
