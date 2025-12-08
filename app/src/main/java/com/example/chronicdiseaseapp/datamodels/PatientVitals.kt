package com.example.chronicdiseaseapp.datamodels

/**
 * Combined patient information and vitals for doctor dashboard
 */
data class PatientVitalsInfo(
    val patientId: String = "",
    val patientName: String = "",
    val patientEmail: String = "",
    val age: Int = 0,
    val latestHeartRate: Int? = null,
    val latestBloodPressureSystolic: Int? = null,
    val latestBloodPressureDiastolic: Int? = null,
    val latestSpO2: Int? = null,
    val latestSteps: Int? = null,
    val lastVitalsUpdate: Long = 0L,
    val userType: UserType = UserType.PATIENT
) {
    val bloodPressureDisplay: String
        get() = if (latestBloodPressureSystolic != null && latestBloodPressureDiastolic != null) {
            "$latestBloodPressureSystolic/$latestBloodPressureDiastolic mmHg"
        } else "—"

    val heartRateDisplay: String
        get() = latestHeartRate?.let { "$it bpm" } ?: "—"

    val spO2Display: String
        get() = latestSpO2?.let { "$it%" } ?: "—"

    val stepsDisplay: String
        get() = latestSteps?.toString() ?: "—"

    val lastUpdateDisplay: String
        get() = if (lastVitalsUpdate > 0) {
            val timeDiff = System.currentTimeMillis() - lastVitalsUpdate
            val minutesDiff = timeDiff / (1000 * 60)
            val hoursDiff = minutesDiff / 60
            val daysDiff = hoursDiff / 24

            when {
                minutesDiff < 1 -> "Just now"
                minutesDiff < 60 -> "$minutesDiff min${if (minutesDiff > 1) "s" else ""} ago"
                hoursDiff < 24 -> "$hoursDiff hour${if (hoursDiff > 1) "s" else ""} ago"
                else -> "$daysDiff day${if (daysDiff > 1) "s" else ""} ago"
            }
        } else "No data"

    /**
     * Check if patient has any vital data
     */
    val hasVitalsData: Boolean
        get() = latestHeartRate != null || latestBloodPressureSystolic != null ||
                latestSpO2 != null || latestSteps != null
}

/**
 * Detailed vitals history for a specific patient
 */
data class PatientVitalsHistory(
    val patientId: String = "",
    val patientName: String = "",
    val heartRateHistory: List<HealthReading> = emptyList(),
    val bloodPressureHistory: List<HealthReading> = emptyList(),
    val spO2History: List<HealthReading> = emptyList(),
    val stepsHistory: List<HealthReading> = emptyList()
)
