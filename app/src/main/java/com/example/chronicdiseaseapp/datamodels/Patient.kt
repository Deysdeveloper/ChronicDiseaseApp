package com.example.chronicdiseaseapp.datamodels

import androidx.compose.ui.graphics.Color

/**
 * Represents a patient in the doctor's care
 */
data class Patient(
    val id: String = "",
    val uid: String = "", // Firebase UID of the patient
    val name: String = "",
    val age: Int = 0,
    val email: String = "",
    val phoneNumber: String = "",
    val primaryCondition: String = "",
    val conditions: List<String> = emptyList(),
    val lastVisit: Long = 0L, // Timestamp
    val nextAppointment: Long? = null, // Timestamp
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val doctorId: String = "", // UID of assigned doctor
    val avatar: String = "",
    val address: String = "",
    val emergencyContact: EmergencyContact? = null,
    val vitals: Vitals? = null,
    val medications: List<Medication> = emptyList(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Computed property for display
    val lastVisitDisplay: String
        get() = if (lastVisit > 0) {
            val daysDiff = (System.currentTimeMillis() - lastVisit) / (1000 * 60 * 60 * 24)
            when {
                daysDiff == 0L -> "Today"
                daysDiff == 1L -> "1 day ago"
                daysDiff < 7 -> "$daysDiff days ago"
                daysDiff < 30 -> "${daysDiff / 7} week${if (daysDiff / 7 > 1) "s" else ""} ago"
                else -> "${daysDiff / 30} month${if (daysDiff / 30 > 1) "s" else ""} ago"
            }
        } else "Never"
}

/**
 * Risk level for patients
 */
enum class RiskLevel(val color: Color, val label: String) {
    LOW(Color(0xFF4CAF50), "Low Risk"),
    MEDIUM(Color(0xFFFF9800), "Medium Risk"),
    HIGH(Color(0xFFF44336), "High Risk"),
    CRITICAL(Color(0xFF9C27B0), "Critical")
}

/**
 * Emergency contact information
 */
data class EmergencyContact(
    val name: String = "",
    val relationship: String = "",
    val phoneNumber: String = "",
    val email: String = ""
)

/**
 * Patient vital signs
 */
data class Vitals(
    val bloodPressureSystolic: Int = 0,
    val bloodPressureDiastolic: Int = 0,
    val heartRate: Int = 0,
    val temperature: Double = 0.0,
    val weight: Double = 0.0,
    val height: Double = 0.0,
    val bloodSugar: Double = 0.0,
    val oxygenSaturation: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val bmi: Double
        get() = if (height > 0 && weight > 0) {
            val heightInMeters = height / 100
            weight / (heightInMeters * heightInMeters)
        } else 0.0

    val bloodPressureReading: String
        get() = "$bloodPressureSystolic/$bloodPressureDiastolic"
}

/**
 * Medication information
 */
data class Medication(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val prescribedBy: String = "", // Doctor UID
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val instructions: String = "",
    val sideEffects: List<String> = emptyList(),
    val isActive: Boolean = true
)