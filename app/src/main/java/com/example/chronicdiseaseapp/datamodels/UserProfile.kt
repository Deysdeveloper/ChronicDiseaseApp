package com.example.chronicdiseaseapp.datamodels

/**
 * Represents the profile information we persist to Firestore for each user.
 */
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val age: Int? = null,
    val userType: UserType = UserType.PATIENT,
    val termsAccepted: Boolean = false,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Doctor-specific fields (only populated if userType is DOCTOR)
    val medicalExpertise: String? = null,
    val currentHospital: String? = null,
    val licenseNumber: String? = null,
    val specialization: String? = null
)

/**
 * Enum representing the type of user
 */
enum class UserType {
    PATIENT,
    DOCTOR
}