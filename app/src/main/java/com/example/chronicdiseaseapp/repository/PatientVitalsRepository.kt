package com.example.chronicdiseaseapp.repository

import android.util.Log
import com.example.chronicdiseaseapp.datamodels.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching all patients' vitals data for doctor dashboard
 */
class PatientVitalsRepository {

    private val tag = "PatientVitalsRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val connectionRepository = DoctorConnectionRepository()

    companion object {
        private const val USERS_NODE = "users"
        private const val PATIENTS_NODE = "patients"
        private const val VITALS_NODE = "vitals"
        private const val HEART_RATE_NODE = "heartRate"
        private const val BLOOD_PRESSURE_NODE = "bloodPressure"
        private const val SPO2_NODE = "spO2"
        private const val STEPS_NODE = "steps"
    }

    /**
     * Get all CONNECTED patients (users with userType = PATIENT) and their latest vitals
     * Only returns patients who have an accepted connection with the current doctor
     */
    suspend fun getAllPatientsWithVitals(): Result<List<PatientVitalsInfo>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(tag, "No authenticated user")
                return Result.failure(Exception("User not authenticated"))
            }

            Log.d(tag, "🔍 DEBUG: Current doctor UID: ${currentUser.uid}")

            // First, get list of connected patient IDs
            val connectedPatientsResult = connectionRepository.getConnectedPatients()
            val connectedPatientIds = connectedPatientsResult.getOrElse {
                Log.e(tag, "Failed to get connected patients: ${it.message}")
                return Result.failure(it)
            }

            if (connectedPatientIds.isEmpty()) {
                Log.w(tag, "⚠️ DEBUG: No connected patients found for doctor ${currentUser.uid}")
                Log.w(
                    tag,
                    "⚠️ DEBUG: Make sure patients have sent connection requests and doctor has accepted them"
                )
                return Result.success(emptyList())
            }

            Log.d(tag, "✅ DEBUG: Found ${connectedPatientIds.size} connected patients")
            Log.d(tag, "✅ DEBUG: Connected patient IDs: $connectedPatientIds")

            // Get connection request IDs for all connected patients
            val connectionsSnapshot = firestore.collection("connections")
                .whereEqualTo("doctorId", currentUser.uid)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()

            val connectionRequestIdMap = connectionsSnapshot.documents.associate { doc ->
                val patientId = doc.getString("patientId") ?: ""
                patientId to doc.id
            }

            // Fetch only connected users from Firestore
            val patientVitalsList = mutableListOf<PatientVitalsInfo>()

            // Process each connected patient
            for (patientId in connectedPatientIds) {
                try {
                    val document = firestore.collection("users")
                        .document(patientId)
                        .get()
                        .await()

                    val userProfile = document.toObject(UserProfile::class.java)

                    // Only include patients (not doctors)
                    if (userProfile != null && userProfile.userType == UserType.PATIENT) {
                        val userId = patientId

                        Log.d(
                            tag,
                            "📊 DEBUG: Fetching vitals for patient: ${userProfile.fullName} (ID: $userId)"
                        )

                        // Fetch latest vitals for this patient
                        val vitalsInfo = fetchLatestVitalsForPatient(
                            userId, 
                            userProfile,
                            connectionRequestIdMap[patientId]
                        )
                        patientVitalsList.add(vitalsInfo)

                        Log.d(tag, "✅ DEBUG: Fetched vitals for ${userProfile.fullName}")
                        Log.d(tag, "   - Heart Rate: ${vitalsInfo.latestHeartRate ?: "null"}")
                        Log.d(
                            tag,
                            "   - Blood Pressure: ${vitalsInfo.latestBloodPressureSystolic}/${vitalsInfo.latestBloodPressureDiastolic}"
                        )
                        Log.d(tag, "   - SpO2: ${vitalsInfo.latestSpO2 ?: "null"}")
                        Log.d(tag, "   - Steps: ${vitalsInfo.latestSteps ?: "null"}")
                        Log.d(tag, "   - Has Vitals Data: ${vitalsInfo.hasVitalsData}")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error processing patient: $patientId", e)
                    // Continue processing other patients even if one fails
                }
            }

            Log.d(tag, "✅ Successfully fetched vitals for ${patientVitalsList.size} patients")
            Result.success(patientVitalsList)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching patients with vitals", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch latest vitals for a specific patient (improved version with multi-path detection)
     */
    private suspend fun fetchLatestVitalsForPatient(
        userId: String,
        userProfile: UserProfile,
        connectionRequestId: String? = null
    ): PatientVitalsInfo {
        try {
            // Two candidate paths commonly used in apps — check both and pick the one that contains data
            val candidatePaths = listOf(
                realtimeDatabase.reference.child("users").child(userId).child("vitals"),
                realtimeDatabase.reference.child("patients").child(userId).child("vitals")
            )

            var chosenRef = candidatePaths[0]
            var chosenPathName = "users/$userId/vitals"
            var snapshot = chosenRef.get().await()

            // If first path empty, try second
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                chosenRef = candidatePaths[1]
                chosenPathName = "patients/$userId/vitals"
                snapshot = chosenRef.get().await()
            }

            Log.d(tag, "🔍 DEBUG: Using RTDB path: $chosenPathName")
            Log.d(tag, "   🔍 RAW snapshot children count: ${snapshot.childrenCount}")

            // Debug: if no children, log the raw keys/values to help debugging
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                val entries = snapshot.children.map { it.key to it.value }
                Log.w(tag, "   ⚠️ No vitals under path; raw snapshot entries: $entries")
            }

            // Use helper functions to get the latest from node children easily
            fun getLatestInt(nodeName: String): Int? {
                val nodeSnap = snapshot.child(nodeName)
                if (!nodeSnap.exists()) {
                    Log.d(tag, "   🔍 $nodeName node does not exist")
                    return null
                }
                Log.d(tag, "   🔍 $nodeName node exists, children count: ${nodeSnap.childrenCount}")
                // pick last child (children are keyed; we assume timestamps used to order with child data)
                val lastChild = nodeSnap.children.maxByOrNull {
                    it.child("timestamp").getValue(Long::class.java) ?: 0L
                } ?: return null
                val value = lastChild.child("value").getValue(Int::class.java)
                Log.d(tag, "   ✅ $nodeName latest value: $value")
                return value
            }

            fun getLatestBP(): Pair<Int?, Int?> {
                val nodeSnap = snapshot.child("bloodPressure")
                if (!nodeSnap.exists()) {
                    Log.d(tag, "   🔍 bloodPressure node does not exist")
                    return Pair(null, null)
                }
                Log.d(
                    tag,
                    "   🔍 bloodPressure node exists, children count: ${nodeSnap.childrenCount}"
                )
                val lastChild = nodeSnap.children.maxByOrNull {
                    it.child("timestamp").getValue(Long::class.java) ?: 0L
                } ?: return Pair(null, null)
                val s = lastChild.child("systolic").getValue(Int::class.java)
                val d = lastChild.child("diastolic").getValue(Int::class.java)
                Log.d(tag, "   ✅ bloodPressure latest value: $s/$d")
                return Pair(s, d)
            }

            fun getLatestTimestampFor(nodeName: String): Long {
                val nodeSnap = snapshot.child(nodeName)
                if (!nodeSnap.exists()) return 0L
                val lastChild = nodeSnap.children.maxByOrNull {
                    it.child("timestamp").getValue(Long::class.java) ?: 0L
                } ?: return 0L
                return lastChild.child("timestamp").getValue(Long::class.java) ?: 0L
            }

            val latestHeartRate = getLatestInt("heartRate")
            val (latestBPSystolic, latestBPDiastolic) = getLatestBP()
            val latestSpO2 = getLatestInt("spO2")
            val latestSteps = try {
                getLatestInt("steps")
            } catch (e: Exception) {
                Log.w(tag, "   ⚠️ Error getting steps: ${e.message}")
                null
            }

            val hrTs = getLatestTimestampFor("heartRate")
            val bpTs = getLatestTimestampFor("bloodPressure")
            val spO2Ts = getLatestTimestampFor("spO2")
            val stepsTs = getLatestTimestampFor("steps")

            val lastVitalsUpdate = maxOf(hrTs, bpTs, spO2Ts, stepsTs)

            Log.d(tag, "   📊 SUMMARY for ${userProfile.fullName}:")
            Log.d(tag, "      Heart Rate: $latestHeartRate")
            Log.d(tag, "      Blood Pressure: $latestBPSystolic/$latestBPDiastolic")
            Log.d(tag, "      SpO2: $latestSpO2")
            Log.d(tag, "      Steps: $latestSteps")
            Log.d(tag, "      Last Update: $lastVitalsUpdate")
            Log.d(tag, "      Has Vitals Data: ${lastVitalsUpdate > 0L}")

            return PatientVitalsInfo(
                patientId = userId,
                patientName = userProfile.fullName,
                patientEmail = userProfile.email,
                age = userProfile.age ?: 0,
                latestHeartRate = latestHeartRate,
                latestBloodPressureSystolic = latestBPSystolic,
                latestBloodPressureDiastolic = latestBPDiastolic,
                latestSpO2 = latestSpO2,
                latestSteps = latestSteps,
                lastVitalsUpdate = lastVitalsUpdate,
                userType = userProfile.userType,
                connectionRequestId = connectionRequestId
            )
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching vitals for patient $userId", e)
            // Return patient info with no vitals data
            return PatientVitalsInfo(
                patientId = userId,
                patientName = userProfile.fullName,
                patientEmail = userProfile.email,
                age = userProfile.age ?: 0,
                userType = userProfile.userType,
                connectionRequestId = connectionRequestId
            )
        }
    }

    /**
     * Get detailed vitals history for a specific patient
     */
    suspend fun getPatientVitalsHistory(
        patientId: String,
        limitToLast: Int = 100
    ): Result<PatientVitalsHistory> {
        return try {
            val vitalsRef = realtimeDatabase.reference
                .child(USERS_NODE)
                .child(patientId)
                .child(VITALS_NODE)

            // Get user profile for patient name
            val userProfile = firestore.collection("users")
                .document(patientId)
                .get()
                .await()
                .toObject(UserProfile::class.java)

            val patientName = userProfile?.fullName ?: "Unknown Patient"

            // Fetch heart rate history
            val heartRateHistory = mutableListOf<HealthReading>()
            try {
                val hrSnapshot = vitalsRef.child(HEART_RATE_NODE)
                    .orderByChild("timestamp")
                    .limitToLast(limitToLast)
                    .get()
                    .await()

                for (child in hrSnapshot.children) {
                    val value = child.child("value").getValue(Int::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    val source = child.child("source").getValue(String::class.java)
                    val id = child.child("id").getValue(String::class.java)

                    if (value != null && timestamp != null) {
                        heartRateHistory.add(
                            HealthReading(
                                id = id ?: "",
                                userId = patientId,
                                timestamp = timestamp,
                                heartRate = value,
                                source = source ?: "Firebase"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Error fetching heart rate history for $patientId: ${e.message}")
            }

            // Fetch blood pressure history
            val bloodPressureHistory = mutableListOf<HealthReading>()
            try {
                val bpSnapshot = vitalsRef.child(BLOOD_PRESSURE_NODE)
                    .orderByChild("timestamp")
                    .limitToLast(limitToLast)
                    .get()
                    .await()

                for (child in bpSnapshot.children) {
                    val systolic = child.child("systolic").getValue(Int::class.java)
                    val diastolic = child.child("diastolic").getValue(Int::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    val source = child.child("source").getValue(String::class.java)
                    val id = child.child("id").getValue(String::class.java)

                    if (systolic != null && diastolic != null && timestamp != null) {
                        bloodPressureHistory.add(
                            HealthReading(
                                id = id ?: "",
                                userId = patientId,
                                timestamp = timestamp,
                                bloodPressureSystolic = systolic,
                                bloodPressureDiastolic = diastolic,
                                source = source ?: "Firebase"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Error fetching blood pressure history for $patientId: ${e.message}")
            }

            // Fetch SpO2 history
            val spO2History = mutableListOf<HealthReading>()
            try {
                val spO2Snapshot = vitalsRef.child(SPO2_NODE)
                    .orderByChild("timestamp")
                    .limitToLast(limitToLast)
                    .get()
                    .await()

                for (child in spO2Snapshot.children) {
                    val value = child.child("value").getValue(Int::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    val source = child.child("source").getValue(String::class.java)
                    val id = child.child("id").getValue(String::class.java)

                    if (value != null && timestamp != null) {
                        spO2History.add(
                            HealthReading(
                                id = id ?: "",
                                userId = patientId,
                                timestamp = timestamp,
                                oxygenSaturation = value,
                                source = source ?: "Firebase"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Error fetching SpO2 history for $patientId: ${e.message}")
            }

            // Fetch steps history
            val stepsHistory = mutableListOf<HealthReading>()
            try {
                val stepsSnapshot = vitalsRef.child(STEPS_NODE)
                    .orderByChild("timestamp")
                    .limitToLast(limitToLast)
                    .get()
                    .await()

                for (child in stepsSnapshot.children) {
                    val value = child.child("value").getValue(Long::class.java)?.toInt()
                        ?: child.child("value").getValue(Int::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    val source = child.child("source").getValue(String::class.java)
                    val id = child.child("id").getValue(String::class.java)

                    if (value != null && timestamp != null) {
                        stepsHistory.add(
                            HealthReading(
                                id = id ?: "",
                                userId = patientId,
                                timestamp = timestamp,
                                stepsCount = value,
                                source = source ?: "Firebase"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Error fetching steps history for $patientId: ${e.message}")
            }

            val vitalsHistory = PatientVitalsHistory(
                patientId = patientId,
                patientName = patientName,
                heartRateHistory = heartRateHistory,
                bloodPressureHistory = bloodPressureHistory,
                spO2History = spO2History,
                stepsHistory = stepsHistory
            )

            Log.d(tag, "✅ Fetched vitals history for patient: $patientName")
            Result.success(vitalsHistory)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching vitals history for patient: $patientId", e)
            Result.failure(e)
        }
    }

    /**
     * Add mock vitals data for testing purposes
     * This helps isolate whether the issue is with data fetching or data not existing
     */
    suspend fun addMockVitalsTo(patientId: String): Result<Unit> {
        return try {
            Log.d(tag, "🧪 Adding mock vitals for patient: $patientId")

            // Try both paths - first check which one patient normally uses
            val pathA =
                realtimeDatabase.reference.child(USERS_NODE).child(patientId).child(VITALS_NODE)
            val pathB =
                realtimeDatabase.reference.child(PATIENTS_NODE).child(patientId).child(VITALS_NODE)

            // Prefer 'users' path as that's what VitalsRepository uses for writing
            val chosenPath = pathA
            val pathName = "users/$patientId/vitals"

            Log.d(tag, "   📍 Writing mock data to: $pathName")

            // Add mock heart rate
            val hrRef = chosenPath.child(HEART_RATE_NODE).push()
            hrRef.setValue(
                mapOf(
                    "value" to 75,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "source" to "MockData",
                    "id" to "mock-hr-${System.currentTimeMillis()}"
                )
            ).await()

            // Add mock blood pressure
            val bpRef = chosenPath.child(BLOOD_PRESSURE_NODE).push()
            bpRef.setValue(
                mapOf(
                    "systolic" to 120,
                    "diastolic" to 80,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "source" to "MockData",
                    "id" to "mock-bp-${System.currentTimeMillis()}"
                )
            ).await()

            // Add mock SpO2
            val spO2Ref = chosenPath.child(SPO2_NODE).push()
            spO2Ref.setValue(
                mapOf(
                    "value" to 98,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "source" to "MockData",
                    "id" to "mock-spo2-${System.currentTimeMillis()}"
                )
            ).await()

            // Add mock steps
            val stepsRef = chosenPath.child(STEPS_NODE).push()
            stepsRef.setValue(
                mapOf(
                    "value" to 5000,
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "source" to "MockData",
                    "id" to "mock-steps-${System.currentTimeMillis()}"
                )
            ).await()

            Log.d(tag, "✅ Mock vitals added successfully to $pathName")
            Log.d(tag, "   ❤️ Heart Rate: 75 bpm")
            Log.d(tag, "   🩸 Blood Pressure: 120/80 mmHg")
            Log.d(tag, "   🫁 SpO2: 98%")
            Log.d(tag, "   👟 Steps: 5000")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error adding mock vitals for patient: $patientId", e)
            Result.failure(e)
        }
    }

    /**
     * Check which RTDB path actually contains vitals data for a patient
     * Useful for debugging path mismatches
     */
    suspend fun debugVitalsPath(patientId: String): Result<String> {
        return try {
            val pathA =
                realtimeDatabase.reference.child(USERS_NODE).child(patientId).child(VITALS_NODE)
            val pathB =
                realtimeDatabase.reference.child(PATIENTS_NODE).child(patientId).child(VITALS_NODE)

            val snapshotA = pathA.get().await()
            val snapshotB = pathB.get().await()

            Log.d(tag, "🔍 DEBUG PATH CHECK for patient: $patientId")
            Log.d(
                tag,
                "   📍 users/$patientId/vitals - exists: ${snapshotA.exists()}, children: ${snapshotA.childrenCount}"
            )
            Log.d(
                tag,
                "   📍 patients/$patientId/vitals - exists: ${snapshotB.exists()}, children: ${snapshotB.childrenCount}"
            )

            val result = when {
                snapshotA.exists() && snapshotA.childrenCount > 0 -> {
                    Log.d(tag, "   ✅ Data found at: users/$patientId/vitals")
                    "users/$patientId/vitals"
                }

                snapshotB.exists() && snapshotB.childrenCount > 0 -> {
                    Log.d(tag, "   ✅ Data found at: patients/$patientId/vitals")
                    "patients/$patientId/vitals"
                }

                else -> {
                    Log.w(tag, "   ⚠️ No vitals data found at either path")
                    "none"
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error checking vitals paths", e)
            Result.failure(e)
        }
    }
}
