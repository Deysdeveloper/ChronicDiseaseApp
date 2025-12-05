package com.example.chronicdiseaseapp.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.chronicdiseaseapp.datamodels.*
import com.example.chronicdiseaseapp.repository.VitalsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.*
import kotlin.random.Random

class HealthDataRepository(private val context: Context) {

    private val tag = "HealthDataRepository"
    private var healthConnectClient: HealthConnectClient? = null
    private val vitalsRepository = VitalsRepository()
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Get device ID for tracking where data came from
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // ✅ Get account creation timestamp to filter out pre-account device data
    private fun accountCreationMillis(): Long {
        return try {
            auth.currentUser?.metadata?.creationTimestamp ?: 0L
        } catch (e: Exception) {
            Log.w(tag, "Could not get account creation timestamp", e)
            0L
        }
    }

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
                Log.w(tag, "getHeartRateData: Health Connect client is null")
                emit(emptyList())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getHeartRateData: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getHeartRateData: Permissions not granted")
                emit(emptyList())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(7 * 24 * 60 * 60) // 7 days ago

            Log.d(tag, "getHeartRateData: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)
            val readings = mutableListOf<HealthReading>()

            // Check if user is authenticated BEFORE processing data
            val currentUid = auth.currentUser?.uid
            if (currentUid == null) {
                Log.e(
                    tag,
                    "getHeartRateData: ⚠️ No authenticated user — skipping Firebase save (prevents data mix-up)"
                )
                emit(emptyList())
                return@flow
            }

            val deviceId = getDeviceId()
            val importedAt = System.currentTimeMillis()
            val acctCreated = accountCreationMillis()

            Log.d(
                tag,
                "getHeartRateData: Account created at: $acctCreated, filtering pre-account data"
            )

            // HeartRateRecord contains samples, each with beatsPerMinute and time
            response.records.forEach { record ->
                record.samples.forEach { sample ->
                    val sampleMillis = sample.time.toEpochMilli()

                    // ✅ Skip readings that predate account creation
                    if (sampleMillis < acctCreated) {
                        Log.d(
                            tag,
                            "Skipping HR sample at $sampleMillis — predates account creation $acctCreated"
                        )
                        return@forEach
                    }

                    readings.add(
                        HealthReading(
                            id = UUID.randomUUID().toString(),
                            timestamp = sampleMillis,
                            heartRate = sample.beatsPerMinute.toInt(),
                            source = "Health Connect",
                            deviceId = deviceId,
                            importedAt = importedAt,
                            importedByUser = currentUid
                        )
                    )
                }
            }

            if (readings.isEmpty()) {
                Log.w(tag, "getHeartRateData: No heart rate records found in Health Connect")
                emit(emptyList())
                return@flow
            }

            Log.d(
                tag,
                "getHeartRateData: Successfully retrieved ${readings.size} heart rate readings from Health Connect for user: $currentUid"
            )

            // Save to Firebase Realtime Database with proper suspend context
            if (readings.isNotEmpty()) {
                val saveResult = withContext(Dispatchers.IO) {
                    vitalsRepository.saveHeartRateData(readings)
                }
                if (saveResult.isSuccess) {
                    Log.d(
                        tag,
                        "getHeartRateData: ✅ Saved ${readings.size} readings to Firebase for uid=$currentUid, device=$deviceId"
                    )
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
            emit(emptyList())
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
                Log.w(tag, "getSpO2Data: Health Connect client is null")
                emit(emptyList())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getSpO2Data: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getSpO2Data: Permissions not granted")
                emit(emptyList())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(7 * 24 * 60 * 60) // 7 days ago

            Log.d(tag, "getSpO2Data: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            // Check if user is authenticated BEFORE processing data
            val currentUid = auth.currentUser?.uid
            if (currentUid == null) {
                Log.e(
                    tag,
                    "getSpO2Data: ⚠️ No authenticated user — skipping Firebase save (prevents data mix-up)"
                )
                emit(emptyList())
                return@flow
            }

            val deviceId = getDeviceId()
            val importedAt = System.currentTimeMillis()
            val acctCreated = accountCreationMillis()

            if (response.records.isEmpty()) {
                Log.w(tag, "getSpO2Data: No SpO2 records found in Health Connect")
                emit(emptyList())
                return@flow
            }

            Log.d(tag, "getSpO2Data: Account created at: $acctCreated, filtering pre-account data")

            val readings = response.records.mapIndexedNotNull { index, record ->
                val recordMillis = record.time.toEpochMilli()

                // ✅ Skip readings that predate account creation
                if (recordMillis < acctCreated) {
                    Log.d(
                        tag,
                        "Skipping SpO2 record at $recordMillis — predates account creation $acctCreated"
                    )
                    return@mapIndexedNotNull null
                }

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
                    timestamp = recordMillis,
                    oxygenSaturation = spo2Value,
                    source = "Health Connect",
                    deviceId = deviceId,
                    importedAt = importedAt,
                    importedByUser = currentUid
                )
            }

            Log.d(
                tag,
                "getSpO2Data: Successfully retrieved ${readings.size} SpO2 readings from Health Connect for user: $currentUid"
            )

            // Save to Firebase Realtime Database with proper suspend context
            if (readings.isNotEmpty()) {
                val saveResult = withContext(Dispatchers.IO) {
                    vitalsRepository.saveSpO2Data(readings)
                }
                if (saveResult.isSuccess) {
                    Log.d(
                        tag,
                        "getSpO2Data: ✅ Saved ${readings.size} readings to Firebase for uid=$currentUid, device=$deviceId")
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
            emit(emptyList())
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
                Log.w(tag, "getBloodPressureData: Health Connect client is null")
                emit(emptyList())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getBloodPressureData: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getBloodPressureData: Permissions not granted")
                emit(emptyList())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(7 * 24 * 60 * 60) // 7 days ago

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

            // Check if user is authenticated BEFORE processing data
            val currentUid = auth.currentUser?.uid
            if (currentUid == null) {
                Log.e(
                    tag,
                    "getBloodPressureData: ⚠️ No authenticated user — skipping Firebase save (prevents data mix-up)"
                )
                emit(emptyList())
                return@flow
            }

            val deviceId = getDeviceId()
            val importedAt = System.currentTimeMillis()
            val acctCreated = accountCreationMillis()

            if (response.records.isEmpty()) {
                Log.w(
                    tag,
                    "getBloodPressureData: No blood pressure records found in Health Connect"
                )
                emit(emptyList())
                return@flow
            }

            Log.d(
                tag,
                "getBloodPressureData: Account created at: $acctCreated, filtering pre-account data"
            )

            val readings = response.records.mapIndexedNotNull { index, record ->
                val recordMillis = record.time.toEpochMilli()

                // ✅ Skip readings that predate account creation
                if (recordMillis < acctCreated) {
                    Log.d(
                        tag,
                        "Skipping BP record at $recordMillis — predates account creation $acctCreated"
                    )
                    return@mapIndexedNotNull null
                }

                val reading = HealthReading(
                    id = UUID.randomUUID().toString(),
                    timestamp = recordMillis,
                    bloodPressureSystolic = record.systolic.inMillimetersOfMercury.toInt(),
                    bloodPressureDiastolic = record.diastolic.inMillimetersOfMercury.toInt(),
                    source = "Health Connect",
                    deviceId = deviceId,
                    importedAt = importedAt,
                    importedByUser = currentUid
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
                "getBloodPressureData: Successfully retrieved ${readings.size} blood pressure readings from Health Connect for user: $currentUid"
            )

            // Save to Firebase Realtime Database with proper suspend context
            if (readings.isNotEmpty()) {
                val saveResult = withContext(Dispatchers.IO) {
                    vitalsRepository.saveBloodPressureData(readings)
                }
                if (saveResult.isSuccess) {
                    Log.d(
                        tag,
                        "getBloodPressureData: ✅ Saved ${readings.size} readings to Firebase for uid=$currentUid, device=$deviceId")
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
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get steps data - SIMPLIFIED VERSION - NO FILTERING
     * Just fetch whatever steps exist in Health Connect
     */
    fun getStepsData(): Flow<List<HealthReading>> = flow {
        try {
            Log.d(tag, "getStepsData: 🚀 Starting SIMPLIFIED steps data retrieval (no filters)")

            if (healthConnectClient == null) {
                Log.w(tag, "getStepsData: Health Connect client is null")
                emit(emptyList())
                return@flow
            }

            val client = healthConnectClient!!
            val hasPermissions = checkPermissions()
            Log.d(tag, "getStepsData: Permission check result: $hasPermissions")

            if (!hasPermissions) {
                Log.w(tag, "getStepsData: Permissions not granted")
                emit(emptyList())
                return@flow
            }

            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(30 * 24 * 60 * 60) // 30 days ago for more data

            Log.d(tag, "getStepsData: Querying Health Connect from $startTime to $endTime")

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            Log.d(
                tag,
                "getStepsData: 📊 Received ${response.records.size} step records from Health Connect"
            )

            // NO FILTERING - Just get ALL the steps
            val totalSteps = response.records.sumOf { it.count }

            Log.d(tag, "getStepsData: 🎯 Total steps from ALL records: $totalSteps")

            // Get current user for Firebase save (but don't block if not authenticated)
            val currentUid = auth.currentUser?.uid
            val deviceId = getDeviceId()
            val importedAt = System.currentTimeMillis()

            val readings = if (totalSteps > 0) {
                listOf(
                    HealthReading(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        stepsCount = totalSteps.toInt(),
                        source = "Health Connect",
                        deviceId = deviceId,
                        importedAt = importedAt,
                        importedByUser = currentUid
                    )
                )
            } else {
                Log.w(tag, "getStepsData: ⚠️ No steps data found in Health Connect")
                emptyList()
            }

            if (readings.isNotEmpty()) {
                Log.d(
                    tag,
                    "getStepsData: ✅ Created reading with ${readings[0].stepsCount} steps"
                )

                // Save to Firebase (only if user is authenticated)
                if (currentUid != null) {
                    val saveResult = withContext(Dispatchers.IO) {
                        vitalsRepository.saveStepsData(readings)
                    }
                    if (saveResult.isSuccess) {
                        Log.d(tag, "getStepsData: ✅ Saved to Firebase for uid=$currentUid")
                    } else {
                        Log.e(
                            tag,
                            "getStepsData: ❌ Failed to save to Firebase: ${saveResult.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    Log.w(tag, "getStepsData: User not authenticated, skipping Firebase save")
                }
            }

            emit(readings)

        } catch (e: Exception) {
            Log.e(tag, "getStepsData: ❌ Error reading steps data", e)
            Log.e(tag, "getStepsData: Exception: ${e.javaClass.simpleName}, Message: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

}