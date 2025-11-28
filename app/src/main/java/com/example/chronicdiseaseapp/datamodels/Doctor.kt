package com.example.chronicdiseaseapp.datamodels

/**
 * Represents a doctor's extended profile information
 */
data class DoctorProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val medicalExpertise: String = "",
    val currentHospital: String = "",
    val licenseNumber: String = "",
    val specialization: String = "",
    val yearsOfExperience: Int = 0,
    val qualifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val consultationFee: Double = 0.0,
    val availableHours: String = "",
    val biography: String = "",
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val isVerified: Boolean = false,
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Statistics for doctor dashboard
 */
data class DoctorStats(
    val totalPatients: Int = 0,
    val activePatients: Int = 0,
    val appointmentsToday: Int = 0,
    val appointmentsThisWeek: Int = 0,
    val appointmentsThisMonth: Int = 0,
    val criticalPatients: Int = 0,
    val pendingAppointments: Int = 0,
    val completedAppointments: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Recent activity for doctor dashboard
 */
data class RecentActivity(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val activity: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: ActivityType = ActivityType.VITALS_UPDATE,
    val priority: ActivityPriority = ActivityPriority.NORMAL,
    val isRead: Boolean = false
) {
    val timeAgo: String
        get() {
            val timeDiff = (System.currentTimeMillis() - timestamp) / (1000 * 60)
            return when {
                timeDiff < 1 -> "Just now"
                timeDiff < 60 -> "${timeDiff.toInt()} min${if (timeDiff.toInt() > 1) "s" else ""} ago"
                timeDiff < 1440 -> "${(timeDiff / 60).toInt()} hour${if ((timeDiff / 60).toInt() > 1) "s" else ""} ago"
                else -> "${(timeDiff / 1440).toInt()} day${if ((timeDiff / 1440).toInt() > 1) "s" else ""} ago"
            }
        }
}

/**
 * Types of activities that can occur
 */
enum class ActivityType(val displayName: String) {
    VITALS_UPDATE("Vitals Updated"),
    APPOINTMENT("Appointment"),
    MEDICATION_CHANGE("Medication Changed"),
    EMERGENCY_ALERT("Emergency Alert"),
    NEW_PATIENT("New Patient"),
    LAB_RESULT("Lab Result"),
    PRESCRIPTION("Prescription"),
    NOTE_ADDED("Note Added"),
    CHECKUP_COMPLETED("Checkup Completed")
}

/**
 * Priority levels for activities
 */
enum class ActivityPriority(val displayName: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent"),
    EMERGENCY("Emergency")
}

/**
 * Appointment information
 */
data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val dateTime: Long = 0L,
    val duration: Int = 30, // minutes
    val type: AppointmentType = AppointmentType.CONSULTATION,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val reason: String = "",
    val notes: String = "",
    val prescription: String = "",
    val followUpRequired: Boolean = false,
    val followUpDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val dateTimeDisplay: String
        get() {
            val date = java.util.Date(dateTime)
            val formatter =
                java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
            return formatter.format(date)
        }

    val timeDisplay: String
        get() {
            val date = java.util.Date(dateTime)
            val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            return formatter.format(date)
        }
}

/**
 * Types of appointments
 */
enum class AppointmentType(val displayName: String) {
    CONSULTATION("Consultation"),
    FOLLOW_UP("Follow-up"),
    EMERGENCY("Emergency"),
    CHECKUP("Regular Checkup"),
    LAB_REVIEW("Lab Review"),
    PRESCRIPTION_REVIEW("Prescription Review"),
    TELEMEDICINE("Telemedicine")
}

/**
 * Status of appointments
 */
enum class AppointmentStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    CONFIRMED("Confirmed"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_SHOW("No Show"),
    RESCHEDULED("Rescheduled")
}

/**
 * Notification for doctors
 */
data class DoctorNotification(
    val id: String = "",
    val doctorId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val priority: ActivityPriority = ActivityPriority.NORMAL,
    val isRead: Boolean = false,
    val actionRequired: Boolean = false,
    val relatedPatientId: String? = null,
    val relatedAppointmentId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTime: Long? = null
)

/**
 * Types of notifications
 */
enum class NotificationType(val displayName: String) {
    INFO("Information"),
    WARNING("Warning"),
    ERROR("Error"),
    SUCCESS("Success"),
    APPOINTMENT("Appointment"),
    EMERGENCY("Emergency"),
    MEDICATION("Medication"),
    LAB_RESULT("Lab Result"),
    SYSTEM("System")
}