package com.example.chronicdiseaseapp.repository

import android.util.Log
import com.example.chronicdiseaseapp.datamodels.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing doctor-patient connections
 */
class DoctorConnectionRepository {

    private val tag = "DoctorConnectionRepo"
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDatabase = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val CONNECTIONS_COLLECTION = "connections"
        private const val USERS_COLLECTION = "users"
        private const val FEEDBACK_COLLECTION = "feedback"
    }

    /**
     * Get all available doctors
     */
    suspend fun getAllDoctors(): Result<List<DoctorInfo>> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Log.w(tag, "User not authenticated")
                return Result.success(emptyList()) // Return empty list instead of failure
            }

            // Fetch all users with userType = DOCTOR
            val doctorsSnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("userType", "DOCTOR")
                .get()
                .await()

            // Get all connection requests for current user
            val connectionsSnapshot = firestore.collection(CONNECTIONS_COLLECTION)
                .whereEqualTo("patientId", currentUserId)
                .get()
                .await()

            val connectionsMap = connectionsSnapshot.documents.associate { doc ->
                val doctorId = doc.getString("doctorId") ?: ""
                val status = doc.getString("status")?.let {
                    ConnectionStatus.valueOf(it)
                } ?: ConnectionStatus.PENDING
                doctorId to Pair(status, doc.id)
            }

            val doctors = doctorsSnapshot.documents.mapNotNull { doc ->
                try {
                    val userProfile = doc.toObject(UserProfile::class.java)
                    if (userProfile != null && userProfile.userType == UserType.DOCTOR) {
                        val doctorId = doc.id
                        val connection = connectionsMap[doctorId]

                        DoctorInfo(
                            id = doctorId,
                            uid = userProfile.uid,
                            fullName = userProfile.fullName,
                            email = userProfile.email,
                            phoneNumber = userProfile.phoneNumber ?: "",
                            medicalExpertise = userProfile.medicalExpertise ?: "General Medicine",
                            specialization = userProfile.specialization ?: "General Practitioner",
                            currentHospital = userProfile.currentHospital ?: "Not specified",
                            licenseNumber = userProfile.licenseNumber ?: "",
                            yearsOfExperience = 0, // Can be added to UserProfile
                            qualifications = emptyList(), // Can be added to UserProfile
                            rating = 0.0, // Can be calculated from feedback
                            totalPatients = 0, // Can be calculated from connections
                            isVerified = userProfile.termsAccepted,
                            photoUrl = userProfile.photoUrl ?: "",
                            isConnected = connection?.first == ConnectionStatus.ACCEPTED,
                            connectionStatus = connection?.first,
                            connectionRequestId = connection?.second
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing doctor: ${doc.id}", e)
                    null
                }
            }

            Log.d(tag, "✅ Fetched ${doctors.size} doctors")
            Result.success(doctors)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching doctors", e)
            Result.failure(e)
        }
    }

    /**
     * Send connection request to a doctor
     */
    suspend fun sendConnectionRequest(
        doctorId: String,
        doctorName: String,
        message: String = ""
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            // Get current user profile
            val userProfileDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            val userProfile = userProfileDoc.toObject(UserProfile::class.java)

            // If profile doesn't exist, create a basic one
            if (userProfile == null) {
                Log.w(tag, "User profile not found, creating basic profile")
                val basicProfile = UserProfile(
                    uid = currentUser.uid,
                    fullName = currentUser.displayName ?: "Patient",
                    email = currentUser.email ?: "",
                    userType = UserType.PATIENT,
                    age = 0,
                    termsAccepted = true,
                    createdAt = System.currentTimeMillis()
                )

                // Save the basic profile
                try {
                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .set(basicProfile)
                        .await()
                    Log.d(tag, "✅ Created basic user profile")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to create basic profile", e)
                    return Result.failure(Exception("Please complete your profile first"))
                }

            }

            // Use the found or created profile
            val profileToUse = userProfile

            // Check if connection request already exists
            val existingRequest = firestore.collection(CONNECTIONS_COLLECTION)
                .whereEqualTo("patientId", currentUser.uid)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Connection request already exists"))
            }

            // Create connection request
            val connectionRequest = ConnectionRequest(
                patientId = currentUser.uid,
                patientName = profileToUse?.fullName ?: currentUser.displayName ?: "Patient",
                patientEmail = profileToUse?.email ?: currentUser.email ?: "",
                patientAge = profileToUse?.age ?: 0,
                doctorId = doctorId,
                doctorName = doctorName,
                status = ConnectionStatus.PENDING,
                requestedAt = System.currentTimeMillis(),
                message = message
            )

            val docRef = firestore.collection(CONNECTIONS_COLLECTION)
                .add(connectionRequest)
                .await()

            Log.d(tag, "✅ Connection request sent: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error sending connection request", e)
            Result.failure(e)
        }
    }

    /**
     * Cancel connection request
     */
    suspend fun cancelConnectionRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(CONNECTIONS_COLLECTION)
                .document(requestId)
                .delete()
                .await()

            Log.d(tag, "✅ Connection request cancelled: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error cancelling connection request", e)
            Result.failure(e)
        }
    }

    /**
     * Get pending connection requests for a doctor
     */
    fun getPendingConnectionRequests(): Flow<List<ConnectionRequest>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(tag, "No authenticated user")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CONNECTIONS_COLLECTION)
            .whereEqualTo("doctorId", currentUser.uid)
            .whereEqualTo("status", ConnectionStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to connection requests", error)
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ConnectionRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing connection request: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(tag, "📥 Received ${requests.size} pending connection requests")
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Accept connection request (with RTDB mirroring for security rules)
     */
    suspend fun acceptConnectionRequest(requestId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            // 1. Get connection details from Firestore
            val connectionDoc = firestore.collection(CONNECTIONS_COLLECTION)
                .document(requestId)
                .get()
                .await()

            val patientId = connectionDoc.getString("patientId")
            val doctorId = connectionDoc.getString("doctorId")

            if (patientId == null || doctorId == null) {
                Log.e(tag, "Invalid connection document - missing patientId or doctorId")
                return Result.failure(Exception("Invalid connection document"))
            }

            Log.d(tag, "🔗 Accepting connection: Doctor=$doctorId, Patient=$patientId")

            // 2. Update Firestore
            firestore.collection(CONNECTIONS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to ConnectionStatus.ACCEPTED.name,
                        "respondedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            // 3. Mirror to RTDB for security rules
            // Structure: /connections/{doctorId}/{patientId}/status = "ACCEPTED"
            val rtdbConnectionRef = realtimeDatabase.reference
                .child("connections")
                .child(doctorId)
                .child(patientId)

            rtdbConnectionRef.setValue(
                mapOf(
                    "status" to "ACCEPTED",
                    "acceptedAt" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "patientId" to patientId,
                    "doctorId" to doctorId
                )
            ).await()

            Log.d(tag, "✅ Connection accepted and mirrored to RTDB: $requestId")
            Log.d(tag, "   📍 RTDB Path: connections/$doctorId/$patientId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error accepting connection request", e)
            Result.failure(e)
        }
    }

    /**
     * Reject connection request (with RTDB cleanup)
     */
    suspend fun rejectConnectionRequest(requestId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            // 1. Get connection details from Firestore
            val connectionDoc = firestore.collection(CONNECTIONS_COLLECTION)
                .document(requestId)
                .get()
                .await()

            val patientId = connectionDoc.getString("patientId")
            val doctorId = connectionDoc.getString("doctorId")

            Log.d(tag, "🔗 Rejecting connection: Doctor=$doctorId, Patient=$patientId")

            // 2. Update Firestore
            firestore.collection(CONNECTIONS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to ConnectionStatus.REJECTED.name,
                        "respondedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            // 3. Remove from RTDB if exists
            if (patientId != null && doctorId != null) {
                realtimeDatabase.reference
                    .child("connections")
                    .child(doctorId)
                    .child(patientId)
                    .removeValue()
                    .await()

                Log.d(tag, "   🗑️ Removed from RTDB: connections/$doctorId/$patientId")
            }

            Log.d(tag, "✅ Connection rejected and removed from RTDB: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error rejecting connection request", e)
            Result.failure(e)
        }
    }

    /**
     * Remove connection (can be called by either doctor or patient)
     * This will delete the connection from both Firestore and RTDB
     */
    suspend fun removeConnection(connectionRequestId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            // 1. Get connection details from Firestore
            val connectionDoc = firestore.collection(CONNECTIONS_COLLECTION)
                .document(connectionRequestId)
                .get()
                .await()

            val patientId = connectionDoc.getString("patientId")
            val doctorId = connectionDoc.getString("doctorId")

            if (patientId == null || doctorId == null) {
                Log.e(tag, "Invalid connection document - missing patientId or doctorId")
                return Result.failure(Exception("Invalid connection document"))
            }

            Log.d(tag, "🔗 Removing connection: Doctor=$doctorId, Patient=$patientId")

            // 2. Delete from Firestore
            firestore.collection(CONNECTIONS_COLLECTION)
                .document(connectionRequestId)
                .delete()
                .await()

            // 3. Remove from RTDB
            realtimeDatabase.reference
                .child("connections")
                .child(doctorId)
                .child(patientId)
                .removeValue()
                .await()

            Log.d(tag, "✅ Connection removed from both Firestore and RTDB: $connectionRequestId")
            Log.d(tag, "   🗑️ Removed from RTDB: connections/$doctorId/$patientId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error removing connection", e)
            Result.failure(e)
        }
    }

    /**
     * Get connected patients for a doctor
     */
    suspend fun getConnectedPatients(): Result<List<String>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(tag, "🔍 DEBUG: Getting connected patients for doctor: ${currentUser.uid}")

            val connectionsSnapshot = firestore.collection(CONNECTIONS_COLLECTION)
                .whereEqualTo("doctorId", currentUser.uid)
                .whereEqualTo("status", ConnectionStatus.ACCEPTED.name)
                .get()
                .await()

            Log.d(tag, "🔍 DEBUG: Total connection documents found: ${connectionsSnapshot.size()}")

            connectionsSnapshot.documents.forEach { doc ->
                Log.d(
                    tag,
                    "   📄 Connection: doctorId=${doc.getString("doctorId")}, patientId=${
                        doc.getString("patientId")
                    }, status=${doc.getString("status")}"
                )
            }

            val patientIds = connectionsSnapshot.documents.mapNotNull { doc ->
                doc.getString("patientId")
            }

            Log.d(tag, "✅ Found ${patientIds.size} connected patients: $patientIds")
            Result.success(patientIds)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching connected patients", e)
            Result.failure(e)
        }
    }

    /**
     * Add feedback for a patient
     */
    suspend fun addFeedback(
        patientId: String,
        patientName: String,
        feedbackText: String,
        feedbackType: FeedbackType,
        vitalsSnapshot: VitalsSnapshot? = null,
        recommendations: List<String> = emptyList(),
        severity: FeedbackSeverity = FeedbackSeverity.NORMAL
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            val userProfile = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()
                .toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Doctor profile not found"))

            val feedback = DoctorFeedback(
                doctorId = currentUser.uid,
                doctorName = userProfile.fullName,
                patientId = patientId,
                patientName = patientName,
                feedbackText = feedbackText,
                feedbackType = feedbackType,
                vitalsSnapshot = vitalsSnapshot,
                recommendations = recommendations,
                severity = severity,
                createdAt = System.currentTimeMillis(),
                isRead = false
            )

            val docRef = firestore.collection(FEEDBACK_COLLECTION)
                .add(feedback)
                .await()

            Log.d(tag, "✅ Feedback added: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error adding feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Get feedback for a patient
     */
    fun getPatientFeedback(): Flow<List<DoctorFeedback>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(tag, "No authenticated user")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FEEDBACK_COLLECTION)
            .whereEqualTo("patientId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to feedback", error)
                    close(error)
                    return@addSnapshotListener
                }

                val feedbackList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(DoctorFeedback::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing feedback: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(tag, "📥 Received ${feedbackList.size} feedback items")
                trySend(feedbackList)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mark feedback as read
     */
    suspend fun markFeedbackAsRead(feedbackId: String): Result<Unit> {
        return try {
            firestore.collection(FEEDBACK_COLLECTION)
                .document(feedbackId)
                .update("isRead", true)
                .await()

            Log.d(tag, "✅ Feedback marked as read: $feedbackId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error marking feedback as read", e)
            Result.failure(e)
        }
    }

    /**
     * ONE-TIME MIGRATION: Mirror all existing ACCEPTED connections from Firestore to RTDB
     * This ensures that old connections (accepted before mirroring code was added) work with security rules
     * 
     * Call this once from your app to migrate all existing connections.
     * Safe to call multiple times - will only update existing ACCEPTED connections.
     */
    suspend fun migrateExistingConnectionsToRTDB(): Result<MigrationResult> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(tag, "🔄 Starting connection migration to RTDB...")

            // Get ALL ACCEPTED connections from Firestore (not just for current user)
            val acceptedConnections = firestore.collection(CONNECTIONS_COLLECTION)
                .whereEqualTo("status", ConnectionStatus.ACCEPTED.name)
                .get()
                .await()

            Log.d(tag, "📊 Found ${acceptedConnections.size()} ACCEPTED connections in Firestore")

            var migratedCount = 0
            var skippedCount = 0
            var errorCount = 0

            acceptedConnections.documents.forEach { doc ->
                try {
                    val doctorId = doc.getString("doctorId")
                    val patientId = doc.getString("patientId")
                    val respondedAt = doc.getLong("respondedAt") ?: System.currentTimeMillis()

                    if (doctorId != null && patientId != null) {
                        // Check if already exists in RTDB
                        val existingRef = realtimeDatabase.reference
                            .child("connections")
                            .child(doctorId)
                            .child(patientId)
                            .get()
                            .await()

                        if (!existingRef.exists()) {
                            // Mirror to RTDB
                            realtimeDatabase.reference
                                .child("connections")
                                .child(doctorId)
                                .child(patientId)
                                .setValue(
                                    mapOf(
                                        "status" to "ACCEPTED",
                                        "acceptedAt" to respondedAt,
                                        "patientId" to patientId,
                                        "doctorId" to doctorId,
                                        "migratedAt" to com.google.firebase.database.ServerValue.TIMESTAMP
                                    )
                                )
                                .await()

                            migratedCount++
                            Log.d(tag, "✅ Migrated: Doctor=$doctorId, Patient=$patientId")
                        } else {
                            skippedCount++
                            Log.d(tag, "⏭️ Skipped (already exists): Doctor=$doctorId, Patient=$patientId")
                        }
                    } else {
                        errorCount++
                        Log.w(tag, "⚠️ Invalid connection document: ${doc.id} - missing doctorId or patientId")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(tag, "❌ Error migrating connection ${doc.id}", e)
                }
            }

            val result = MigrationResult(
                totalConnections = acceptedConnections.size(),
                migratedCount = migratedCount,
                skippedCount = skippedCount,
                errorCount = errorCount
            )

            Log.d(tag, "🎉 Migration complete!")
            Log.d(tag, "   📊 Total connections: ${result.totalConnections}")
            Log.d(tag, "   ✅ Migrated: ${result.migratedCount}")
            Log.d(tag, "   ⏭️ Skipped (already existed): ${result.skippedCount}")
            Log.d(tag, "   ❌ Errors: ${result.errorCount}")

            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "❌ Fatal error during migration", e)
            Result.failure(e)
        }
    }

    /**
     * Data class for migration results
     */
    data class MigrationResult(
        val totalConnections: Int,
        val migratedCount: Int,
        val skippedCount: Int,
        val errorCount: Int
    ) {
        val isSuccessful: Boolean
            get() = migratedCount > 0 && errorCount == 0

        val summary: String
            get() = "Migrated $migratedCount/$totalConnections connections (Skipped: $skippedCount, Errors: $errorCount)"
    }
}
