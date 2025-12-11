package com.example.chronicdiseaseapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.chronicdiseaseapp.datamodels.*
import com.example.chronicdiseaseapp.repository.DoctorConnectionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for Doctor Home Screen
 * Manages doctor dashboard data, patient information, and activities
 */
class DoctorHomeViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val connectionRepository = DoctorConnectionRepository()

    // Doctor profile
    private val _doctorProfile = MutableStateFlow<UserProfile?>(null)
    val doctorProfile: StateFlow<UserProfile?> = _doctorProfile.asStateFlow()

    // Doctor statistics
    private val _doctorStats = MutableStateFlow(DoctorStats())
    val doctorStats: StateFlow<DoctorStats> = _doctorStats.asStateFlow()

    // Recent patients
    private val _recentPatients = MutableStateFlow<List<Patient>>(emptyList())
    val recentPatients: StateFlow<List<Patient>> = _recentPatients.asStateFlow()

    // Recent activities
    private val _recentActivities = MutableStateFlow<List<RecentActivity>>(emptyList())
    val recentActivities: StateFlow<List<RecentActivity>> = _recentActivities.asStateFlow()

    // Today's appointments
    private val _todaysAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val todaysAppointments: StateFlow<List<Appointment>> = _todaysAppointments.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<DoctorNotification>>(emptyList())
    val notifications: StateFlow<List<DoctorNotification>> = _notifications.asStateFlow()

    // Connection requests
    private val _connectionRequests = MutableStateFlow<List<ConnectionRequest>>(emptyList())
    val connectionRequests: StateFlow<List<ConnectionRequest>> = _connectionRequests.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDoctorDashboardData()
        observeConnectionRequests()
    }

    /**
     * Observe connection requests in real-time
     */
    private fun observeConnectionRequests() {
        viewModelScope.launch {
            try {
                connectionRepository.getPendingConnectionRequests().collect { requests ->
                    _connectionRequests.value = requests
                }
            } catch (e: Exception) {
                // Silently fail if Firestore index is missing or other errors
                _connectionRequests.value = emptyList()
                // Don't show error for connection requests - it's not critical
            }
        }
    }

    /**
     * Load all dashboard data
     */
    private fun loadDoctorDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadDoctorProfile()
                loadDoctorStats()
                loadRecentPatients()
                loadRecentActivities()
                loadTodaysAppointments()
                loadNotifications()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load dashboard data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load doctor profile information from Firebase
     */
    private suspend fun loadDoctorProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                val snapshot = firestore.collection("users").document(currentUser.uid).get().await()
                val userProfile = snapshot.toObject(UserProfile::class.java)
                if (userProfile != null) {
                    _doctorProfile.value = userProfile
                } else {
                    // If no profile document found, fallback to basic info
                    _doctorProfile.value = UserProfile(
                        uid = currentUser.uid,
                        fullName = currentUser.displayName ?: "Doctor",
                        email = currentUser.email ?: "",
                        userType = UserType.DOCTOR
                    )
                }
            } catch (e: Exception) {
                // Fallback to basic user info in case of error
                _doctorProfile.value = UserProfile(
                    uid = currentUser.uid,
                    fullName = currentUser.displayName ?: "Doctor",
                    email = currentUser.email ?: "",
                    userType = UserType.DOCTOR
                )
                _errorMessage.value = "Failed to load doctor profile: ${e.message}"
            }
        }
    }

    /**
     * Load doctor statistics
     */
    private suspend fun loadDoctorStats() {
        // TODO: Replace with actual repository call to get doctor-specific stats
        _doctorStats.value = getSampleDoctorStats()
    }

    /**
     * Load recent patients
     */
    private suspend fun loadRecentPatients() {
        // TODO: Replace with actual repository call to get doctor's patients
        _recentPatients.value = getSampleRecentPatients()
    }

    /**
     * Load recent activities
     */
    private suspend fun loadRecentActivities() {
        // TODO: Replace with actual repository call
        _recentActivities.value = getSampleRecentActivities()
    }

    /**
     * Load today's appointments
     */
    private suspend fun loadTodaysAppointments() {
        // TODO: Replace with actual repository call
        _todaysAppointments.value = getSampleTodaysAppointments()
    }

    /**
     * Load notifications
     */
    private suspend fun loadNotifications() {
        // TODO: Replace with actual repository call
        _notifications.value = getSampleNotifications()
    }

    /**
     * Refresh all data
     */
    fun refreshData() {
        loadDoctorDashboardData()
    }

    /**
     * Refresh only doctor profile data
     */
    fun refreshDoctorProfile() {
        viewModelScope.launch {
            try {
                loadDoctorProfile()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh doctor profile: ${e.message}"
            }
        }
    }

    /**
     * Get current doctor profile loading status for debugging
     */
    fun getDoctorProfileStatus(): String {
        val profile = _doctorProfile.value
        return when {
            profile == null -> "No profile loaded"
            profile.medicalExpertise.isNullOrBlank() -> "Profile loaded but missing expertise data"
            profile.currentHospital.isNullOrBlank() -> "Profile loaded but missing hospital data"
            profile.phoneNumber.isNullOrBlank() -> "Profile loaded but missing phone data"
            else -> "Profile complete with all data"
        }
    }

    /**
     * Mark notification as read
     */
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            // TODO: Update in repository
            val updatedNotifications = _notifications.value.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            _notifications.value = updatedNotifications
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Accept connection request
     */
    fun acceptConnectionRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = connectionRepository.acceptConnectionRequest(requestId)
                result.onFailure { exception ->
                    _errorMessage.value = "Failed to accept request: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error accepting request: ${e.message}"
            }
        }
    }

    /**
     * Reject connection request
     */
    fun rejectConnectionRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = connectionRepository.rejectConnectionRequest(requestId)
                result.onFailure { exception ->
                    _errorMessage.value = "Failed to reject request: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error rejecting request: ${e.message}"
            }
        }
    }

    // Sample data methods (to be replaced with actual repository calls)
    private fun getSampleDoctorStats(): DoctorStats {
        return DoctorStats(
            totalPatients = 2,
            activePatients = 1,
            appointmentsToday = 8,
            appointmentsThisWeek = 42,
            appointmentsThisMonth = 178,
            criticalPatients = 3,
            pendingAppointments = 15,
            completedAppointments = 1250
        )
    }

    private fun getSampleRecentPatients(): List<Patient> {
        return listOf(
            Patient(
                id = "1",
                uid = "patient_1",
                name = "John Smith",
                age = 45,
                primaryCondition = "Diabetes Type 2",
                lastVisit = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                riskLevel = RiskLevel.MEDIUM,
                doctorId = auth.currentUser?.uid ?: ""
            ),
            Patient(
                id = "2",
                uid = "patient_2",
                name = "Sarah Johnson",
                age = 62,
                primaryCondition = "Hypertension",
                lastVisit = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                riskLevel = RiskLevel.HIGH,
                doctorId = auth.currentUser?.uid ?: ""
            ),
            Patient(
                id = "3",
                uid = "patient_3",
                name = "Michael Brown",
                age = 38,
                primaryCondition = "Heart Disease",
                lastVisit = System.currentTimeMillis() - (0 * 24 * 60 * 60 * 1000), // Today
                riskLevel = RiskLevel.CRITICAL,
                doctorId = auth.currentUser?.uid ?: ""
            ),
            Patient(
                id = "4",
                uid = "patient_4",
                name = "Lisa Davis",
                age = 55,
                primaryCondition = "COPD",
                lastVisit = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                riskLevel = RiskLevel.LOW,
                doctorId = auth.currentUser?.uid ?: ""
            )
        )
    }

    private fun getSampleRecentActivities(): List<RecentActivity> {
        return listOf(
            RecentActivity(
                id = "1",
                patientId = "patient_1",
                patientName = "John Smith",
                activity = "Blood glucose levels updated",
                description = "New reading: 145 mg/dL",
                timestamp = System.currentTimeMillis() - (10 * 60 * 1000), // 10 min ago
                type = ActivityType.VITALS_UPDATE,
                priority = ActivityPriority.NORMAL
            ),
            RecentActivity(
                id = "2",
                patientId = "patient_2",
                patientName = "Sarah Johnson",
                activity = "Appointment scheduled",
                description = "Follow-up appointment for tomorrow 10:00 AM",
                timestamp = System.currentTimeMillis() - (60 * 60 * 1000), // 1 hour ago
                type = ActivityType.APPOINTMENT,
                priority = ActivityPriority.NORMAL
            ),
            RecentActivity(
                id = "3",
                patientId = "patient_3",
                patientName = "Michael Brown",
                activity = "Emergency alert - High BP",
                description = "Blood pressure: 180/120 mmHg",
                timestamp = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 hours ago
                type = ActivityType.EMERGENCY_ALERT,
                priority = ActivityPriority.URGENT
            )
        )
    }

    private fun getSampleTodaysAppointments(): List<Appointment> {
        val today = System.currentTimeMillis()
        return listOf(
            Appointment(
                id = "1",
                patientId = "patient_1",
                patientName = "John Smith",
                doctorId = auth.currentUser?.uid ?: "",
                doctorName = auth.currentUser?.displayName ?: "Dr. Doctor",
                dateTime = today + (2 * 60 * 60 * 1000), // 2 hours from now
                type = AppointmentType.FOLLOW_UP,
                status = AppointmentStatus.CONFIRMED,
                reason = "Diabetes follow-up"
            ),
            Appointment(
                id = "2",
                patientId = "patient_2",
                patientName = "Sarah Johnson",
                doctorId = auth.currentUser?.uid ?: "",
                doctorName = auth.currentUser?.displayName ?: "Dr. Doctor",
                dateTime = today + (4 * 60 * 60 * 1000), // 4 hours from now
                type = AppointmentType.CHECKUP,
                status = AppointmentStatus.SCHEDULED,
                reason = "Regular checkup"
            )
        )
    }

    private fun getSampleNotifications(): List<DoctorNotification> {
        return listOf(
            DoctorNotification(
                id = "1",
                doctorId = auth.currentUser?.uid ?: "",
                title = "Critical Patient Alert",
                message = "Michael Brown's BP reading is critically high",
                type = NotificationType.EMERGENCY,
                priority = ActivityPriority.URGENT,
                relatedPatientId = "patient_3"
            ),
            DoctorNotification(
                id = "2",
                doctorId = auth.currentUser?.uid ?: "",
                title = "New Lab Results",
                message = "Lab results available for John Smith",
                type = NotificationType.LAB_RESULT,
                priority = ActivityPriority.NORMAL,
                relatedPatientId = "patient_1"
            ),
            DoctorNotification(
                id = "3",
                doctorId = auth.currentUser?.uid ?: "",
                title = "Appointment Reminder",
                message = "You have 2 appointments scheduled for today",
                type = NotificationType.APPOINTMENT,
                priority = ActivityPriority.NORMAL
            )
        )
    }
}