package com.example.chronicdiseaseapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.chronicdiseaseapp.datamodels.UserProfile
import com.example.chronicdiseaseapp.datamodels.UserType
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // LiveData for authentication state
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    // LiveData for current user
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData for user profile (from Firestore)
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    init {
        // Initialize current user state
        _currentUser.value = auth.currentUser
        _authState.value =
            if (auth.currentUser != null) AuthState.AUTHENTICATED else AuthState.UNAUTHENTICATED

        // Load user profile if user is already signed in
        if (auth.currentUser != null) {
            loadCurrentUserProfile()
        }
    }

    /**
     * Sign up a new user with email and password
     */
    fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String?,
        age: Int?,
        userType: UserType = UserType.PATIENT,
        termsAccepted: Boolean,
        // Doctor-specific fields
        medicalExpertise: String? = null,
        currentHospital: String? = null,
        licenseNumber: String? = null,
        specialization: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!isValidEmail(email)) {
            onResult(false, "Please enter a valid email address")
            return
        }

        if (!isValidPassword(password)) {
            onResult(false, "Password must be at least 6 characters long")
            return
        }

        if (fullName.isBlank()) {
            onResult(false, "Please enter your full name")
            return
        }

        if (!phoneNumber.isNullOrBlank() && phoneNumber.length != 10) {
            onResult(false, "Phone number must be exactly 10 digits")
            return
        }

        // Validate doctor-specific fields if user is a doctor
        if (userType == UserType.DOCTOR) {
            if (medicalExpertise.isNullOrBlank()) {
                onResult(false, "Please enter your medical expertise")
                return
            }
            if (currentHospital.isNullOrBlank()) {
                onResult(false, "Please enter your current hospital")
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                // Update user profile with display name
                user?.let {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    it.updateProfile(profileUpdates).await()

                    // Prepare and persist user profile to Firestore
                    val userProfile = UserProfile(
                        uid = it.uid,
                        fullName = fullName,
                        email = email,
                        phoneNumber = phoneNumber?.takeIf { pn -> pn.isNotBlank() },
                        age = age,
                        userType = userType,
                        termsAccepted = termsAccepted,
                        photoUrl = it.photoUrl?.toString(),
                        // Doctor-specific fields
                        medicalExpertise = medicalExpertise?.takeIf { exp -> exp.isNotBlank() },
                        currentHospital = currentHospital?.takeIf { hosp -> hosp.isNotBlank() },
                        licenseNumber = licenseNumber?.takeIf { lic -> lic.isNotBlank() },
                        specialization = specialization?.takeIf { spec -> spec.isNotBlank() }
                    )

                    // Store in appropriate collection based on user type
                    val collection = if (userType == UserType.DOCTOR) "doctors" else "patients"

                    // Store in both users collection (for general auth) and specific collection
                    firestore.collection("users").document(it.uid).set(userProfile).await()
                    firestore.collection(collection).document(it.uid).set(userProfile).await()

                    _currentUser.value = it
                    _authState.value = AuthState.AUTHENTICATED
                    // Load user profile after successful sign up
                    _userProfile.value = userProfile
                    onResult(true, null)
                }
            } catch (e: Exception) {
                val errorMsg = getFirebaseErrorMessage(e)
                _errorMessage.value = errorMsg
                _authState.value = AuthState.ERROR
                onResult(false, errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign in existing user with email and password
     */
    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (!isValidEmail(email)) {
            onResult(false, "Please enter a valid email address")
            return
        }

        if (password.isBlank()) {
            onResult(false, "Please enter your password")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    _currentUser.value = it
                    _authState.value = AuthState.AUTHENTICATED
                    // Load user profile after successful sign in
                    loadCurrentUserProfile()
                    onResult(true, null)
                }
            } catch (e: Exception) {
                val errorMsg = getFirebaseErrorMessage(e)
                _errorMessage.value = errorMsg
                _authState.value = AuthState.ERROR
                onResult(false, errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.UNAUTHENTICATED
        _errorMessage.value = null
    }

    /**
     * Send password reset email
     */
    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        if (!isValidEmail(email)) {
            onResult(false, "Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                auth.sendPasswordResetEmail(email).await()
                onResult(true, "Password reset email sent successfully")
            } catch (e: Exception) {
                val errorMsg = getFirebaseErrorMessage(e)
                _errorMessage.value = errorMsg
                onResult(false, errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if user is currently signed in
     */
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current user's display name
     */
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    /**
     * Get current user's email
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Load the current user's profile from Firestore and expose it via LiveData
     */
    fun loadCurrentUserProfile() {
        val userId = auth.currentUser?.uid ?: run {
            _userProfile.value = null
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)
                _userProfile.value = profile
            } catch (e: Exception) {
                // Keep previous value, but set an error for UI if needed
                _errorMessage.value = getFirebaseErrorMessage(e)
            }
        }
    }

    /**
     * Update the user's profile photo by uploading to Firebase Storage and saving URL in Firestore
     */
    fun updateProfilePhoto(photoUri: android.net.Uri, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false, "No user signed in")
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val storageRef = storage.reference.child("profile_photos/${user.uid}/profile.jpg")
                storageRef.putFile(photoUri).await()
                val url = storageRef.downloadUrl.await().toString()
                // Update Firestore record
                firestore.collection("users").document(user.uid).update("photoUrl", url).await()
                // Update user profile in LiveData
                loadCurrentUserProfile()
                onResult(true, null)
            } catch (e: Exception) {
                _errorMessage.value = getFirebaseErrorMessage(e)
                onResult(false, getFirebaseErrorMessage(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get current user's type from Firestore
     */
    fun getCurrentUserType(onResult: (UserType?) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(null)
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)
                onResult(profile?.userType)
            } catch (e: Exception) {
                _errorMessage.value = getFirebaseErrorMessage(e)
                onResult(null)
            }
        }
    }

    // Private helper functions
    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    private fun getFirebaseErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The email address is badly formatted." -> "Please enter a valid email address"
            "The password is invalid or the user does not have a password." -> "Invalid password"
            "There is no user record corresponding to this identifier. The user may have been deleted." -> "No account found with this email"
            "The email address is already in use by another account." -> "An account with this email already exists"
            "Password should be at least 6 characters" -> "Password must be at least 6 characters long"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Network error. Please check your connection"
            else -> exception.message ?: "An unexpected error occurred"
        }
    }
}

/**
 * Enum class representing different authentication states
 */
enum class AuthState {
    AUTHENTICATED,
    UNAUTHENTICATED,
    LOADING,
    ERROR
}