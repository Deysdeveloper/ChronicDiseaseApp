package com.example.chronicdiseaseapp.datamodels

/**
 * Health data retrieved from Samsung Health/Galaxy Watch 4
 */
data class HealthReading(
    val id: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "", // "Samsung Health", "Galaxy Watch 4", etc.
    val heartRate: Int? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val oxygenSaturation: Int? = null, // SpO2
    val stepsCount: Int? = null,
    val caloriesBurned: Double? = null,
    val sleepDuration: Long? = null, // in minutes
    val stressLevel: Int? = null, // 0-100
    val bodyTemperature: Double? = null,
    val syncedAt: Long = System.currentTimeMillis(),
    // NEW: Track device and import metadata to prevent cross-user data contamination
    val deviceId: String? = null,
    val importedAt: Long? = null,
    val importedByUser: String? = null  // UID of user who imported this device data
) {
    val bloodPressureReading: String
        get() = if (bloodPressureSystolic != null && bloodPressureDiastolic != null) {
            "$bloodPressureSystolic/$bloodPressureDiastolic"
        } else "—/—"
}

/**
 * Aggregated health metrics for dashboard display
 */
data class HealthMetrics(
    val averageHeartRate: Int = 0, // Now stores the LATEST heart rate (most recent reading) for real-time updates
    val latestBloodPressure: String = "—/—",
    val latestSpO2: Int = 0,
    val dailySteps: Int = 0,
    val weeklyTrend: HealthTrend = HealthTrend.STABLE,
    val lastSyncTime: Long = 0,
    val isDataAvailable: Boolean = false
) {
    fun getDisplayText(metric: String): String {
        return when (metric) {
            "heartRate" -> if (averageHeartRate > 0) averageHeartRate.toString() else "—"
            "bloodPressure" -> latestBloodPressure
            "spO2" -> if (latestSpO2 > 0) latestSpO2.toString() else "—"
            "steps" -> if (dailySteps > 0) dailySteps.toString() else "—"
            else -> "—"
        }
    }
}

enum class HealthTrend {
    IMPROVING, STABLE, DECLINING
}

/**
 * Samsung Health data types we're interested in
 */
enum class SamsungHealthDataType(val identifier: String) {
    HEART_RATE("com.samsung.health.heart_rate"),
    BLOOD_PRESSURE("com.samsung.health.blood_pressure"),
    OXYGEN_SATURATION("com.samsung.health.oxygen_saturation"),
    STEP_COUNT("com.samsung.health.step_count"),
    CALORIES("com.samsung.health.calories_burned"),
    SLEEP("com.samsung.health.sleep"),
    STRESS("com.samsung.health.stress")
}

/**
 * Health Connect data types for cross-platform compatibility
 */
enum class HealthConnectDataType {
    HEART_RATE,
    BLOOD_PRESSURE,
    OXYGEN_SATURATION,
    STEPS,
    ACTIVE_CALORIES_BURNED,
    SLEEP_SESSION
}

/**
 * Health data sync status
 */
data class SyncStatus(
    val isConnected: Boolean = false,
    val lastSyncTime: Long = 0,
    val errorMessage: String? = null,
    val pendingSyncCount: Int = 0
)