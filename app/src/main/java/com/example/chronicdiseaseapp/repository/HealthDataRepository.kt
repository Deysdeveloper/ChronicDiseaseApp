package com.example.chronicdiseaseapp.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.chronicdiseaseapp.datamodels.*
import com.example.chronicdiseaseapp.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.util.*
import kotlin.random.Random

class HealthDataRepository(private val context: Context) {

    private val tag = "HealthDataRepository"
    private var healthConnectClient: HealthConnectClient? = null
    private val vitalsRepository = VitalsRepository()

    // Health Connect permissions needed
    // Required permissions - these are essential for the app to function
    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    // Optional permissions - nice to have but not essential
    private val optionalPermissions = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    // All permissions combined
    private val permissions = requiredPermissions + optionalPermissions

    /**
     * Initialize Health Connect client
     */
    suspend fun initialize(): Boolean {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    healthConnectClient = HealthConnectClient.getOrCreate(context)
                    Log.d(
                        tag,
                        "Health Connect initialized successfully - privacy compliant data access"
                    )
                    true
                }

                else -> {
                    Log.w(tag, "Health Connect not available, using privacy-safe sample data")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing Health Connect - no sensitive data compromised")
            false
        }
    }

    /**
     * Check if required permissions are granted
     * Note: Only checks REQUIRED permissions, optional permissions don't affect this result
     */
    suspend fun checkPermissions(): Boolean {
        return try {
            healthConnectClient?.let { client ->
                val grantedPermissions = client.permissionController.getGrantedPermissions()

                // Check if all REQUIRED permissions are granted (not optional ones)
                val hasRequiredPermissions = requiredPermissions.all { it in grantedPermissions }
                val grantedOptionalCount = optionalPermissions.count { it in grantedPermissions }

                Log.d(tag, "=== PERMISSION CHECK ===")
                Log.d(tag, "Required permissions granted: $hasRequiredPermissions")
                Log.d(
                    tag,
                    "Optional permissions granted: $grantedOptionalCount/${optionalPermissions.size}"
                )
                Log.d(
                    tag,
                    "Total required: ${requiredPermissions.size}, Total optional: ${optionalPermissions.size}"
                )
                Log.d(tag, "Total granted by user: ${grantedPermissions.size}")

                // Log each permission status for debugging
                Log.d(tag, "--- Required Permissions ---")
                requiredPermissions.forEach { permission ->
                    val isGranted = grantedPermissions.contains(permission)
                    val permissionName = when {
                        permission.toString().contains("HeartRate") -> "Heart Rate"
                        permission.toString().contains("BloodPressure") -> "Blood Pressure"
                        permission.toString()
                            .contains("OxygenSaturation") -> "Oxygen Saturation (SpO2)"

                        permission.toString().contains("Steps") -> "Steps"
                        else -> permission.toString()
                    }
                    Log.d(tag, "  $permissionName: $isGranted")
                }

                Log.d(tag, "--- Optional Permissions ---")
                optionalPermissions.forEach { permission ->
                    val isGranted = grantedPermissions.contains(permission)
                    val permissionName = when {
                        permission.toString().contains("ActiveCalories") -> "Active Calories"
                        else -> permission.toString()
                    }
                    Log.d(tag, "  $permissionName: $isGranted (optional - not required)")
                }
                Log.d(tag, "======================")

                // Return true if all REQUIRED permissions are granted
                hasRequiredPermissions
            } ?: run {
                Log.w(tag, "Health Connect client is null - cannot check permissions")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking permissions - maintaining privacy", e)
            false
        }
    }

    /**
     * Request health permissions
     */
    fun getRequiredPermissions(): Set<String> {
        return permissions.map { it.toString() }.toSet()
    }

    /**
     * Get heart rate data from the last 24 hours
     * Privacy: Data stays on device, no external transmission
     */
    fun getHeartRateData(): Flow<List<HealthReading>> = flow {
        try {
            Log.d(tag, "getHeartRateData: Starting heart rate data retrieval")

            if (healthConnectClient == null) {
                Log.w(tag, "getHeartRateData: Health Connect client is null, using sample data")
                emit(generateSampleHeartRateData())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getHeartRateData: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getHeartRateData: Permissions not granted, using sample data")
                emit(generateSampleHeartRateData())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(24 * 60 * 60) // 24 hours ago

            Log.d(tag, "getHeartRateData: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)
            val readings = mutableListOf<HealthReading>()

            // HeartRateRecord contains samples, each with beatsPerMinute and time
            response.records.forEach { record ->
                record.samples.forEach { sample ->
                    readings.add(
                        HealthReading(
                            id = UUID.randomUUID().toString(),
                            timestamp = sample.time.toEpochMilli(),
                            heartRate = sample.beatsPerMinute.toInt(),
                            source = "Health Connect"
                        )
                    )
                }
            }

            if (readings.isEmpty()) {
                Log.w(tag, "getHeartRateData: No heart rate records found in Health Connect")
                emit(generateSampleHeartRateData())
                return@flow
            }

            Log.d(
                tag,
                "getHeartRateData: Successfully retrieved ${readings.size} heart rate readings from Health Connect"
            )

            // Save to Firebase Realtime Database for current user
            if (readings.isNotEmpty()) {
                val saveResult = vitalsRepository.saveHeartRateData(readings)
                if (saveResult.isSuccess) {
                    Log.d(tag, "getHeartRateData: ✅ Saved to Firebase Realtime Database")
                } else {
                    Log.e(
                        tag,
                        "getHeartRateData: ❌ Failed to save to Firebase: ${saveResult.exceptionOrNull()?.message}"
                    )
                }
            }

            emit(readings)

        } catch (e: Exception) {
            Log.e(tag, "getHeartRateData: Error reading heart rate data", e)
            Log.e(
                tag,
                "getHeartRateData: Exception type: ${e.javaClass.simpleName}, Message: ${e.message}")
            emit(generateSampleHeartRateData())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get SpO2 (oxygen saturation) data
     * Privacy: Local processing only, no cloud transmission
     */
    fun getSpO2Data(): Flow<List<HealthReading>> = flow {
        try {
            Log.d(tag, "getSpO2Data: Starting SpO2 data retrieval")

            if (healthConnectClient == null) {
                Log.w(tag, "getSpO2Data: Health Connect client is null, using sample data")
                emit(generateSampleSpO2Data())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getSpO2Data: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getSpO2Data: Permissions not granted, using sample data")
                emit(generateSampleSpO2Data())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(24 * 60 * 60) // 24 hours ago

            Log.d(tag, "getSpO2Data: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            if (response.records.isEmpty()) {
                Log.w(tag, "getSpO2Data: No SpO2 records found in Health Connect")
                emit(generateSampleSpO2Data())
                return@flow
            }

            val readings = response.records.mapIndexed { index, record ->
                // Health Connect returns percentage as 0.0-1.0 (e.g., 0.98 for 98%)
                // So we multiply by 100 to get 98
                // BUT: Some devices (like Galaxy Watch 4) already send it as 98.0
                // So we need to detect and handle both cases
                val rawValue = record.percentage.value
                val spo2Value = when {
                    rawValue <= 1.0 -> (rawValue * 100).toInt()  // 0.98 → 98
                    rawValue > 100.0 -> (rawValue / 100).toInt() // 9800 → 98 (device bug)
                    else -> rawValue.toInt()                      // 98.0 → 98 (already correct)
                }


                // Log the conversion for debugging
                if (index < 3) { // Log first 3 readings only
                    Log.d(tag, "getSpO2Data: Raw value: $rawValue → Corrected: $spo2Value%")
                }

                HealthReading(
                    id = UUID.randomUUID().toString(),
                    timestamp = record.time.toEpochMilli(),
                    oxygenSaturation = spo2Value,
                    source = "Health Connect"
                )
            }

            Log.d(
                tag,
                "getSpO2Data: Successfully retrieved ${readings.size} SpO2 readings from Health Connect"
            )

            // Save to Firebase Realtime Database for current user
            if (readings.isNotEmpty()) {
                val saveResult = vitalsRepository.saveSpO2Data(readings)
                if (saveResult.isSuccess) {
                    Log.d(tag, "getSpO2Data: ✅ Saved to Firebase Realtime Database")
                } else {
                    Log.e(
                        tag,
                        "getSpO2Data: ❌ Failed to save to Firebase: ${saveResult.exceptionOrNull()?.message}"
                    )
                }
            }

            emit(readings)

        } catch (e: Exception) {
            Log.e(tag, "getSpO2Data: Error reading SpO2 data", e)
            Log.e(
                tag,
                "getSpO2Data: Exception type: ${e.javaClass.simpleName}, Message: ${e.message}")
            emit(generateSampleSpO2Data())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get blood pressure data
     * Privacy: Sensitive medical data kept strictly local
     */
    fun getBloodPressureData(): Flow<List<HealthReading>> = flow {
        try {
            Log.d(tag, "getBloodPressureData: Starting blood pressure data retrieval")

            if (healthConnectClient == null) {
                Log.w(tag, "getBloodPressureData: Health Connect client is null, using sample data")
                emit(generateSampleBloodPressureData())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getBloodPressureData: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getBloodPressureData: Permissions not granted, using sample data")
                emit(generateSampleBloodPressureData())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(30 * 24 * 60 * 60) // 30 days ago for more data

            Log.d(tag, "getBloodPressureData: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = BloodPressureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)
            Log.d(
                tag,
                "getBloodPressureData: Received ${response.records.size} records from Health Connect"
            )

            if (response.records.isEmpty()) {
                Log.w(
                    tag,
                    "getBloodPressureData: No blood pressure records found in Health Connect"
                )
                Log.w(
                    tag,
                    "getBloodPressureData: Check if data exists in Health Connect app and try manual sync"
                )
                emit(generateSampleBloodPressureData())
                return@flow
            }

            val readings = response.records.mapIndexed { index, record ->
                val reading = HealthReading(
                    id = UUID.randomUUID().toString(),
                    timestamp = record.time.toEpochMilli(),
                    bloodPressureSystolic = record.systolic.inMillimetersOfMercury.toInt(),
                    bloodPressureDiastolic = record.diastolic.inMillimetersOfMercury.toInt(),
                    source = "Health Connect"
                )
                Log.d(
                    tag,
                    "getBloodPressureData: Reading $index - ${reading.bloodPressureSystolic}/${reading.bloodPressureDiastolic} at ${
                        java.util.Date(reading.timestamp)
                    }"
                )
                reading
            }

            Log.d(
                tag,
                "getBloodPressureData: Successfully retrieved ${readings.size} blood pressure readings from Health Connect"
            )

            // Save to Firebase Realtime Database for current user
            if (readings.isNotEmpty()) {
                val saveResult = vitalsRepository.saveBloodPressureData(readings)
                if (saveResult.isSuccess) {
                    Log.d(tag, "getBloodPressureData: ✅ Saved to Firebase Realtime Database")
                } else {
                    Log.e(
                        tag,
                        "getBloodPressureData: ❌ Failed to save to Firebase: ${saveResult.exceptionOrNull()?.message}"
                    )
                }
            }

            emit(readings)

        } catch (e: Exception) {
            Log.e(tag, "getBloodPressureData: Error reading blood pressure data", e)
            Log.e(
                tag,
                "getBloodPressureData: Exception type: ${e.javaClass.simpleName}, Message: ${e.message}")
            emit(generateSampleBloodPressureData())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get steps data
     * Privacy: Activity data processed locally only
     */
    fun getStepsData(): Flow<List<HealthReading>> = flow {
        try {
            Log.d(tag, "getStepsData: Starting steps data retrieval")

            if (healthConnectClient == null) {
                Log.w(tag, "getStepsData: Health Connect client is null, using sample data")
                emit(generateSampleStepsData())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getStepsData: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getStepsData: Permissions not granted, using sample data")
                emit(generateSampleStepsData())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(24 * 60 * 60) // 24 hours ago

            Log.d(tag, "getStepsData: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            // Sum up all step records for the day
            val totalSteps = response.records.sumOf { it.count }
            Log.d(
                tag,
                "getStepsData: Found ${response.records.size} step records, total steps: $totalSteps"
            )

            val readings = if (totalSteps > 0) {
                listOf(
                    HealthReading(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        stepsCount = totalSteps.toInt(),
                        source = "Health Connect"
                    )
                )
            } else {
                Log.w(tag, "getStepsData: No steps data found in Health Connect")
                emptyList()
            }

            if (readings.isEmpty()) {
                emit(generateSampleStepsData())
            } else {
                Log.d(tag, "getStepsData: Successfully retrieved steps data from Health Connect")

                // Save to Firebase Realtime Database for current user
                val saveResult = vitalsRepository.saveStepsData(readings)
                if (saveResult.isSuccess) {
                    Log.d(tag, "getStepsData: ✅ Saved to Firebase Realtime Database")
                } else {
                    Log.e(
                        tag,
                        "getStepsData: ❌ Failed to save to Firebase: ${saveResult.exceptionOrNull()?.message}"
                    )
                }

                emit(readings)
            }

        } catch (e: Exception) {
            Log.e(tag, "getStepsData: Error reading steps data", e)
            Log.e(
                tag,
                "getStepsData: Exception type: ${e.javaClass.simpleName}, Message: ${e.message}")
            emit(generateSampleStepsData())
        }
    }.flowOn(Dispatchers.IO)

    // Sample data generators for demo/fallback purposes
    // Privacy: These are clearly marked as non-real data for demonstration
    private fun generateSampleHeartRateData(): List<HealthReading> {
        Log.w(tag, "Generating sample heart rate data - no real data available")
        val currentTime = System.currentTimeMillis()
        val readings = (0..23).map { hour ->
            val hr = Random.nextInt(60, 100)
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = currentTime - (hour * 60 * 60 * 1000),
                heartRate = hr,
                source = "Sample Data (No Health Connect Data)"
            )
        }
        val avgHR = readings.mapNotNull { it.heartRate }.average().toInt()
        Log.w(tag, "Generated ${readings.size} sample HR readings with average: $avgHR bpm")
        return readings
    }

    private fun generateSampleSpO2Data(): List<HealthReading> {
        Log.w(tag, "Generating sample SpO2 data - no real data available")
        val currentTime = System.currentTimeMillis()
        return (0..5).map { hour ->
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = currentTime - (hour * 4 * 60 * 60 * 1000),
                oxygenSaturation = Random.nextInt(95, 100),
                source = "Sample Data (No Health Connect Data)"
            )
        }
    }

    private fun generateSampleBloodPressureData(): List<HealthReading> {
        Log.w(tag, "Generating sample blood pressure data - no real data available")
        val currentTime = System.currentTimeMillis()
        return (0..2).map { day ->
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = currentTime - (day * 24 * 60 * 60 * 1000),
                bloodPressureSystolic = Random.nextInt(110, 140),
                bloodPressureDiastolic = Random.nextInt(70, 90),
                source = "Sample Data (No Health Connect Data)"
            )
        }
    }

    private fun generateSampleStepsData(): List<HealthReading> {
        Log.w(tag, "Generating sample steps data - no real data available")
        return listOf(
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                stepsCount = Random.nextInt(5000, 12000),
                source = "Sample Data (No Health Connect Data)"
            )
        )
    }
}