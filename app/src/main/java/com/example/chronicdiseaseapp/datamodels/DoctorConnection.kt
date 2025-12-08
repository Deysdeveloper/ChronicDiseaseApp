package com.example.chronicdiseaseapp.datamodels

/**
 * Represents a connection request between a patient and a doctor
 */
data class ConnectionRequest(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientEmail: String = "",
    val patientAge: Int = 0,
    val doctorId: String = "",
    val doctorName: String = "",
    val status: ConnectionStatus = ConnectionStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null,
    val message: String = "" // Optional message from patient
) {
    val statusDisplay: String
        get() = when (status) {
            ConnectionStatus.PENDING -> "Pending"
            ConnectionStatus.ACCEPTED -> "Connected"
            ConnectionStatus.REJECTED -> "Declined"
        }

    val timeAgo: String
        get() {
            val timeDiff = (System.currentTimeMillis() - requestedAt) / (1000 * 60)
            return when {
                timeDiff < 1 -> "Just now"
                timeDiff < 60 -> "${timeDiff.toInt()} min${if (timeDiff.toInt() > 1) "s" else ""} ago"
                timeDiff < 1440 -> "${(timeDiff / 60).toInt()} hour${if ((timeDiff / 60).toInt() > 1) "s" else ""} ago"
                else -> "${(timeDiff / 1440).toInt()} day${if ((timeDiff / 1440).toInt() > 1) "s" else ""} ago"
            }
        }
}

/**
 * Status of a connection request
 */
enum class ConnectionStatus {
    PENDING,    // Request sent, waiting for doctor's response
    ACCEPTED,   // Doctor accepted the request
    REJECTED    // Doctor declined the request
}

/**
 * Represents a doctor profile for patient view with connection status
 */
data class DoctorInfo(
    val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val medicalExpertise: String = "",
    val specialization: String = "",
    val currentHospital: String = "",
    val licenseNumber: String = "",
    val yearsOfExperience: Int = 0,
    val qualifications: List<String> = emptyList(),
    val rating: Double = 0.0,
    val totalPatients: Int = 0,
    val isVerified: Boolean = false,
    val photoUrl: String = "",
    val isConnected: Boolean = false, // Is the current patient connected to this doctor
    val connectionStatus: ConnectionStatus? = null, // Status of connection if exists
    val connectionRequestId: String? = null // ID of the connection request if exists
)

/**
 * Feedback from doctor to patient
 */
data class DoctorFeedback(
    val id: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val feedbackText: String = "",
    val feedbackType: FeedbackType = FeedbackType.GENERAL,
    val vitalsSnapshot: VitalsSnapshot? = null,
    val recommendations: List<String> = emptyList(),
    val severity: FeedbackSeverity = FeedbackSeverity.NORMAL,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    val timeAgo: String
        get() {
            val timeDiff = (System.currentTimeMillis() - createdAt) / (1000 * 60)
            return when {
                timeDiff < 1 -> "Just now"
                timeDiff < 60 -> "${timeDiff.toInt()} min${if (timeDiff.toInt() > 1) "s" else ""} ago"
                timeDiff < 1440 -> "${(timeDiff / 60).toInt()} hour${if ((timeDiff / 60).toInt() > 1) "s" else ""} ago"
                else -> "${(timeDiff / 1440).toInt()} day${if ((timeDiff / 1440).toInt() > 1) "s" else ""} ago"
            }
        }
}

/**
 * Type of feedback
 */
enum class FeedbackType(val displayName: String) {
    GENERAL("General Advice"),
    VITALS_REVIEW("Vitals Review"),
    MEDICATION("Medication"),
    LIFESTYLE("Lifestyle"),
    EMERGENCY("Emergency Alert"),
    FOLLOW_UP("Follow-up Required")
}

/**
 * Severity of feedback
 */
enum class FeedbackSeverity(val displayName: String) {
    NORMAL("Normal"),
    CAUTION("Caution"),
    WARNING("Warning"),
    URGENT("Urgent")
}

/**
 * Snapshot of patient vitals at time of feedback
 */
data class VitalsSnapshot(
    val heartRate: Int? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val spO2: Int? = null,
    val steps: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
