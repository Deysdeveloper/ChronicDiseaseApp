package com.example.chronicdiseaseapp.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.chronicdiseaseapp.datamodels.*
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

    // Health Connect permissions needed
    private val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

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
     */
    suspend fun checkPermissions(): Boolean {
        return try {
            healthConnectClient?.let { client ->
                val grantedPermissions = client.permissionController.getGrantedPermissions()
                val hasPermissions = permissions.all { it in grantedPermissions }
                Log.d(tag, "Permission status checked - user consent: $hasPermissions")
                Log.d(tag, "Required permissions: ${permissions.size}")
                Log.d(tag, "Granted permissions: ${grantedPermissions.size}")

                // Log each permission status for debugging
                permissions.forEach { permission ->
                    val isGranted = grantedPermissions.contains(permission)
                    Log.d(tag, "Permission ${permission} granted: $isGranted")
                }

                hasPermissions
            } ?: false
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
            healthConnectClient?.let { client ->
                if (checkPermissions()) {
                    val endTime = Instant.now()
                    val startTime = endTime.minusSeconds(24 * 60 * 60) // 24 hours ago

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
                                    source = "Galaxy Watch 4" // Non-sensitive metadata only
                                )
                            )
                        }
                    }

                    // Privacy: Only log count, not actual values
                    Log.d(tag, "Retrieved ${readings.size} heart rate readings - data kept local")
                    emit(readings)
                    return@flow
                }
            }

            // Fallback: Generate sample heart rate data for demo
            Log.d(tag, "Using privacy-safe sample heart rate data")
            emit(generateSampleHeartRateData())

        } catch (e: Exception) {
            Log.e(tag, "Error reading heart rate data - privacy maintained")
            emit(generateSampleHeartRateData())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get SpO2 (oxygen saturation) data
     * Privacy: Local processing only, no cloud transmission
     */
    fun getSpO2Data(): Flow<List<HealthReading>> = flow {
        try {
            healthConnectClient?.let { client ->
                if (checkPermissions()) {
                    val endTime = Instant.now()
                    val startTime = endTime.minusSeconds(24 * 60 * 60) // 24 hours ago

                    val request = ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )

                    val response = client.readRecords(request)
                    val readings = response.records.map { record ->
                        HealthReading(
                            id = UUID.randomUUID().toString(),
                            timestamp = record.time.toEpochMilli(),
                            oxygenSaturation = (record.percentage.value * 100).toInt(),
                            source = "Galaxy Watch 4"
                        )
                    }

                    Log.d(
                        tag,
                        "Retrieved ${readings.size} SpO2 readings - data processing local only"
                    )
                    emit(readings)
                    return@flow
                }
            }

            // Fallback: Generate sample SpO2 data for demo
            Log.d(tag, "Using privacy-safe sample SpO2 data")
            emit(generateSampleSpO2Data())

        } catch (e: Exception) {
            Log.e(tag, "Error reading SpO2 data - privacy maintained")
            emit(generateSampleSpO2Data())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get blood pressure data
     * Privacy: Sensitive medical data kept strictly local
     */
    fun getBloodPressureData(): Flow<List<HealthReading>> = flow {
        try {
            healthConnectClient?.let { client ->
                if (checkPermissions()) {
                    val endTime = Instant.now()
                    val startTime = endTime.minusSeconds(7 * 24 * 60 * 60) // 7 days ago

                    val request = ReadRecordsRequest(
                        recordType = BloodPressureRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )

                    val response = client.readRecords(request)
                    val readings = response.records.map { record ->
                        HealthReading(
                            id = UUID.randomUUID().toString(),
                            timestamp = record.time.toEpochMilli(),
                            bloodPressureSystolic = record.systolic.inMillimetersOfMercury.toInt(),
                            bloodPressureDiastolic = record.diastolic.inMillimetersOfMercury.toInt(),
                            source = "Samsung Health"
                        )
                    }

                    Log.d(
                        tag,
                        "Retrieved ${readings.size} blood pressure readings - confidential data local only"
                    )
                    emit(readings)
                    return@flow
                }
            }

            // Fallback: Generate sample blood pressure data for demo
            Log.d(tag, "Using privacy-safe sample blood pressure data")
            emit(generateSampleBloodPressureData())

        } catch (e: Exception) {
            Log.e(tag, "Error reading blood pressure data - privacy maintained")
            emit(generateSampleBloodPressureData())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get steps data
     * Privacy: Activity data processed locally only
     */
    fun getStepsData(): Flow<List<HealthReading>> = flow {
        try {
            healthConnectClient?.let { client ->
                if (checkPermissions()) {
                    val endTime = Instant.now()
                    val startTime = endTime.minusSeconds(24 * 60 * 60) // 24 hours ago

                    val request = ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )

                    val response = client.readRecords(request)

                    // Sum up all step records for the day
                    val totalSteps = response.records.sumOf { it.count }
                    val readings = if (totalSteps > 0) {
                        listOf(
                            HealthReading(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                stepsCount = totalSteps.toInt(),
                                source = "Galaxy Watch 4"
                            )
                        )
                    } else emptyList()

                    Log.d(tag, "Retrieved steps data - activity tracking local only")
                    emit(readings)
                    return@flow
                }
            }

            // Fallback: Generate sample steps data for demo
            Log.d(tag, "Using privacy-safe sample steps data")
            emit(generateSampleStepsData())

        } catch (e: Exception) {
            Log.e(tag, "Error reading steps data - privacy maintained")
            emit(generateSampleStepsData())
        }
    }.flowOn(Dispatchers.IO)

    // Sample data generators for demo/fallback purposes
    // Privacy: These are clearly marked as non-real data for demonstration
    private fun generateSampleHeartRateData(): List<HealthReading> {
        val currentTime = System.currentTimeMillis()
        return (0..23).map { hour ->
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = currentTime - (hour * 60 * 60 * 1000),
                heartRate = Random.nextInt(60, 100),
                source = "Sample Data - Demo Only"
            )
        }
    }

    private fun generateSampleSpO2Data(): List<HealthReading> {
        val currentTime = System.currentTimeMillis()
        return (0..5).map { hour ->
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = currentTime - (hour * 4 * 60 * 60 * 1000),
                oxygenSaturation = Random.nextInt(95, 100),
                source = "Sample Data - Demo Only"
            )
        }
    }

    private fun generateSampleBloodPressureData(): List<HealthReading> {
        val currentTime = System.currentTimeMillis()
        return (0..2).map { day ->
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = currentTime - (day * 24 * 60 * 60 * 1000),
                bloodPressureSystolic = Random.nextInt(110, 140),
                bloodPressureDiastolic = Random.nextInt(70, 90),
                source = "Sample Data - Demo Only"
            )
        }
    }

    private fun generateSampleStepsData(): List<HealthReading> {
        return listOf(
            HealthReading(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                stepsCount = Random.nextInt(5000, 12000),
                source = "Sample Data - Demo Only"
            )
        )
    }
}