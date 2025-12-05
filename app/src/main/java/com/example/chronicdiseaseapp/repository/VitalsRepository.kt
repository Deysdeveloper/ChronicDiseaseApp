package com.example.chronicdiseaseapp.repository

import android.util.Log
import com.example.chronicdiseaseapp.datamodels.HealthReading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for managing user health vitals in Firebase Realtime Database
 * Data structure: users/{userId}/vitals/{vitalType}/{timestamp}
 */
class VitalsRepository {

    private val tag = "VitalsRepository"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val USERS_NODE = "users"
        private const val VITALS_NODE = "vitals"
        private const val HEART_RATE_NODE = "heartRate"
        private const val BLOOD_PRESSURE_NODE = "bloodPressure"
        private const val SPO2_NODE = "spO2"
        private const val STEPS_NODE = "steps"
    }

    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Get user's vitals reference path
     */
    private fun getUserVitalsRef(userId: String) =
        database.reference.child(USERS_NODE).child(userId).child(VITALS_NODE)

    /**
     * Save heart rate data to Firebase Realtime Database
     */
    suspend fun saveHeartRateData(readings: List<HealthReading>): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val heartRateRef = getUserVitalsRef(userId).child(HEART_RATE_NODE)

            readings.forEach { reading ->
                if (reading.heartRate != null) {
                    val data = mutableMapOf(
                        "value" to reading.heartRate,
                        "timestamp" to reading.timestamp,
                        "source" to (reading.source ?: "Unknown"),
                        "id" to reading.id
                    )
                    // Add device tracking metadata
                    reading.deviceId?.let { data["deviceId"] = it }
                    reading.importedAt?.let { data["importedAt"] = it }
                    reading.importedByUser?.let { data["importedByUser"] = it }

                    // ✅ Use push() to avoid overwrites from identical timestamps
                    heartRateRef.push().setValue(data).await()
                }
            }

            Log.d(tag, "✅ Saved ${readings.size} heart rate readings for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saving heart rate data for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Save blood pressure data to Firebase Realtime Database
     */
    suspend fun saveBloodPressureData(readings: List<HealthReading>): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val bpRef = getUserVitalsRef(userId).child(BLOOD_PRESSURE_NODE)

            readings.forEach { reading ->
                if (reading.bloodPressureSystolic != null && reading.bloodPressureDiastolic != null) {
                    val data = mutableMapOf(
                        "systolic" to reading.bloodPressureSystolic,
                        "diastolic" to reading.bloodPressureDiastolic,
                        "timestamp" to reading.timestamp,
                        "source" to (reading.source ?: "Unknown"),
                        "id" to reading.id
                    )
                    // Add device tracking metadata
                    reading.deviceId?.let { data["deviceId"] = it }
                    reading.importedAt?.let { data["importedAt"] = it }
                    reading.importedByUser?.let { data["importedByUser"] = it }

                    // ✅ Use push() to avoid overwrites from identical timestamps
                    bpRef.push().setValue(data).await()
                }
            }

            Log.d(tag, "✅ Saved ${readings.size} blood pressure readings for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saving blood pressure data for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Save SpO2 data to Firebase Realtime Database
     */
    suspend fun saveSpO2Data(readings: List<HealthReading>): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val spO2Ref = getUserVitalsRef(userId).child(SPO2_NODE)

            readings.forEach { reading ->
                if (reading.oxygenSaturation != null) {
                    val data = mutableMapOf(
                        "value" to reading.oxygenSaturation,
                        "timestamp" to reading.timestamp,
                        "source" to (reading.source ?: "Unknown"),
                        "id" to reading.id
                    )
                    // Add device tracking metadata
                    reading.deviceId?.let { data["deviceId"] = it }
                    reading.importedAt?.let { data["importedAt"] = it }
                    reading.importedByUser?.let { data["importedByUser"] = it }

                    // ✅ Use push() to avoid overwrites from identical timestamps
                    spO2Ref.push().setValue(data).await()
                }
            }

            Log.d(tag, "✅ Saved ${readings.size} SpO2 readings for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saving SpO2 data for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Save steps data to Firebase Realtime Database
     */
    suspend fun saveStepsData(readings: List<HealthReading>): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val stepsRef = getUserVitalsRef(userId).child(STEPS_NODE)

            readings.forEach { reading ->
                if (reading.stepsCount != null) {
                    val data = mutableMapOf<String, Any>(
                        "value" to reading.stepsCount,
                        "timestamp" to reading.timestamp,
                        "source" to (reading.source ?: "Unknown"),
                        "id" to reading.id
                    )
                    // Add device tracking metadata
                    reading.deviceId?.let { data["deviceId"] = it }
                    reading.importedAt?.let { data["importedAt"] = it }
                    reading.importedByUser?.let { data["importedByUser"] = it }

                    Log.d(
                        tag,
                        "Saving steps data: value=${reading.stepsCount}, timestamp=${reading.timestamp}, source=${reading.source}"
                    )

                    // ✅ Use push() to avoid overwrites from identical timestamps
                    stepsRef.push().setValue(data).await()
                }
            }

            Log.d(tag, "✅ Saved ${readings.size} steps readings for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saving steps data for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get heart rate data from Firebase for current user
     */
    fun getHeartRateDataFromFirebase(limitToLast: Int = 100): Flow<List<HealthReading>> =
        callbackFlow {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(tag, "Cannot fetch heart rate data - user not logged in")
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val heartRateRef = getUserVitalsRef(userId).child(HEART_RATE_NODE)
                .orderByChild("timestamp")
                .limitToLast(limitToLast)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val readings = mutableListOf<HealthReading>()
                    for (child in snapshot.children) {
                        try {
                            val value = child.child("value").getValue(Int::class.java)
                            val timestamp = child.child("timestamp").getValue(Long::class.java)
                            val source = child.child("source").getValue(String::class.java)
                            val id = child.child("id").getValue(String::class.java)

                            if (value != null && timestamp != null) {
                                readings.add(
                                    HealthReading(
                                        id = id ?: "",
                                        timestamp = timestamp,
                                        heartRate = value,
                                        source = source ?: "Firebase"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing heart rate data", e)
                        }
                    }
                    Log.d(
                        tag,
                        "📥 Fetched ${readings.size} heart rate readings from Firebase for user: $userId"
                    )
                    trySend(readings)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag, "Error fetching heart rate data from Firebase", error.toException())
                    close(error.toException())
                }
            }

            heartRateRef.addValueEventListener(listener)

            awaitClose {
                heartRateRef.removeEventListener(listener)
            }
        }

    /**
     * Get blood pressure data from Firebase for current user
     */
    fun getBloodPressureDataFromFirebase(limitToLast: Int = 100): Flow<List<HealthReading>> =
        callbackFlow {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(tag, "Cannot fetch blood pressure data - user not logged in")
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val bpRef = getUserVitalsRef(userId).child(BLOOD_PRESSURE_NODE)
                .orderByChild("timestamp")
                .limitToLast(limitToLast)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val readings = mutableListOf<HealthReading>()
                    for (child in snapshot.children) {
                        try {
                            val systolic = child.child("systolic").getValue(Int::class.java)
                            val diastolic = child.child("diastolic").getValue(Int::class.java)
                            val timestamp = child.child("timestamp").getValue(Long::class.java)
                            val source = child.child("source").getValue(String::class.java)
                            val id = child.child("id").getValue(String::class.java)

                            if (systolic != null && diastolic != null && timestamp != null) {
                                readings.add(
                                    HealthReading(
                                        id = id ?: "",
                                        timestamp = timestamp,
                                        bloodPressureSystolic = systolic,
                                        bloodPressureDiastolic = diastolic,
                                        source = source ?: "Firebase"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing blood pressure data", e)
                        }
                    }
                    Log.d(
                        tag,
                        "📥 Fetched ${readings.size} blood pressure readings from Firebase for user: $userId"
                    )
                    trySend(readings)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        tag,
                        "Error fetching blood pressure data from Firebase",
                        error.toException()
                    )
                    close(error.toException())
                }
            }

            bpRef.addValueEventListener(listener)

            awaitClose {
                bpRef.removeEventListener(listener)
            }
        }

    /**
     * Get SpO2 data from Firebase for current user
     */
    fun getSpO2DataFromFirebase(limitToLast: Int = 100): Flow<List<HealthReading>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w(tag, "Cannot fetch SpO2 data - user not logged in")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val spO2Ref = getUserVitalsRef(userId).child(SPO2_NODE)
            .orderByChild("timestamp")
            .limitToLast(limitToLast)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readings = mutableListOf<HealthReading>()
                for (child in snapshot.children) {
                    try {
                        val value = child.child("value").getValue(Int::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)
                        val source = child.child("source").getValue(String::class.java)
                        val id = child.child("id").getValue(String::class.java)

                        if (value != null && timestamp != null) {
                            readings.add(
                                HealthReading(
                                    id = id ?: "",
                                    timestamp = timestamp,
                                    oxygenSaturation = value,
                                    source = source ?: "Firebase"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing SpO2 data", e)
                    }
                }
                Log.d(
                    tag,
                    "📥 Fetched ${readings.size} SpO2 readings from Firebase for user: $userId"
                )
                trySend(readings)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Error fetching SpO2 data from Firebase", error.toException())
                close(error.toException())
            }
        }

        spO2Ref.addValueEventListener(listener)

        awaitClose {
            spO2Ref.removeEventListener(listener)
        }
    }

    /**
     * Get steps data from Firebase for current user
     */
    fun getStepsDataFromFirebase(limitToLast: Int = 100): Flow<List<HealthReading>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w(tag, "Cannot fetch steps data - user not logged in")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val stepsRef = getUserVitalsRef(userId).child(STEPS_NODE)
            .orderByChild("timestamp")
            .limitToLast(limitToLast)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readings = mutableListOf<HealthReading>()

                Log.d(
                    tag,
                    "Steps snapshot exists: ${snapshot.exists()}, child count: ${snapshot.childrenCount}"
                )

                for (child in snapshot.children) {
                    try {
                        // Log raw data for debugging
                        Log.d(tag, "Steps child key: ${child.key}, value: ${child.value}")

                        val value = child.child("value").getValue(Long::class.java)?.toInt()
                            ?: child.child("value").getValue(Int::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)
                        val source = child.child("source").getValue(String::class.java)
                        val id = child.child("id").getValue(String::class.java)

                        Log.d(
                            tag,
                            "Parsed steps - value: $value, timestamp: $timestamp, source: $source"
                        )

                        if (value != null && timestamp != null) {
                            readings.add(
                                HealthReading(
                                    id = id ?: "",
                                    timestamp = timestamp,
                                    stepsCount = value,
                                    source = source ?: "Firebase"
                                )
                            )
                        } else {
                            Log.w(tag, "Skipping steps record - missing value or timestamp")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing steps data for child ${child.key}", e)
                    }
                }

                if (readings.isNotEmpty()) {
                    val totalSteps = readings.sumOf { it.stepsCount ?: 0 }
                    Log.d(
                        tag,
                        "📥 Fetched ${readings.size} steps readings from Firebase for user: $userId, total steps: $totalSteps"
                    )
                } else {
                    Log.w(tag, "📥 No steps readings found in Firebase for user: $userId")
                }

                trySend(readings)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Error fetching steps data from Firebase", error.toException())
                close(error.toException())
            }
        }

        stepsRef.addValueEventListener(listener)

        awaitClose {
            stepsRef.removeEventListener(listener)
        }
    }

    /**
     * Delete old vitals data (keep only last N days)
     */
    suspend fun cleanupOldData(daysToKeep: Int = 30): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

        return try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            val vitalsRef = getUserVitalsRef(userId)

            val vitalTypes = listOf(HEART_RATE_NODE, BLOOD_PRESSURE_NODE, SPO2_NODE, STEPS_NODE)

            vitalTypes.forEach { vitalType ->
                val snapshot = vitalsRef.child(vitalType)
                    .orderByChild("timestamp")
                    .endAt(cutoffTime.toDouble())
                    .get()
                    .await()

                snapshot.children.forEach { child ->
                    child.ref.removeValue().await()
                }

                Log.d(tag, "🧹 Cleaned up old $vitalType data for user: $userId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error cleaning up old data for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get vitals summary for current user (latest values)
     */
    suspend fun getVitalsSummary(): Map<String, Any?> {
        val userId = getCurrentUserId() ?: return emptyMap()

        return try {
            val vitalsRef = getUserVitalsRef(userId)

            val latestHeartRate = vitalsRef.child(HEART_RATE_NODE)
                .orderByChild("timestamp")
                .limitToLast(1)
                .get()
                .await()
                .children
                .firstOrNull()
                ?.child("value")
                ?.getValue(Int::class.java)

            val latestBP = vitalsRef.child(BLOOD_PRESSURE_NODE)
                .orderByChild("timestamp")
                .limitToLast(1)
                .get()
                .await()
                .children
                .firstOrNull()

            val latestSpO2 = vitalsRef.child(SPO2_NODE)
                .orderByChild("timestamp")
                .limitToLast(1)
                .get()
                .await()
                .children
                .firstOrNull()
                ?.child("value")
                ?.getValue(Int::class.java)

            val latestSteps = vitalsRef.child(STEPS_NODE)
                .orderByChild("timestamp")
                .limitToLast(1)
                .get()
                .await()
                .children
                .firstOrNull()
                ?.child("value")
                ?.getValue(Int::class.java)

            mapOf(
                "heartRate" to latestHeartRate,
                "bloodPressureSystolic" to latestBP?.child("systolic")?.getValue(Int::class.java),
                "bloodPressureDiastolic" to latestBP?.child("diastolic")?.getValue(Int::class.java),
                "spO2" to latestSpO2,
                "steps" to latestSteps
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching vitals summary", e)
            emptyMap()
        }
    }
}
