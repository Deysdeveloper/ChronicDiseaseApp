package com.example.chronicdiseaseapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chronicdiseaseapp.datamodels.DoctorInfo
import com.example.chronicdiseaseapp.datamodels.DoctorFeedback
import com.example.chronicdiseaseapp.repository.DoctorConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing doctor connections from patient side
 */
class DoctorConnectionViewModel : ViewModel() {

    private val repository = DoctorConnectionRepository()

    // List of all available doctors
    private val _doctors = MutableStateFlow<List<DoctorInfo>>(emptyList())
    val doctors: StateFlow<List<DoctorInfo>> = _doctors.asStateFlow()

    // Feedback from doctors
    private val _feedback = MutableStateFlow<List<DoctorFeedback>>(emptyList())
    val feedback: StateFlow<List<DoctorFeedback>> = _feedback.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered doctors
    private val _filteredDoctors = MutableStateFlow<List<DoctorInfo>>(emptyList())
    val filteredDoctors: StateFlow<List<DoctorInfo>> = _filteredDoctors.asStateFlow()

    init {
        try {
            loadDoctors()
            observeFeedback()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to initialize: ${e.message}"
        }
    }

    /**
     * Load all available doctors
     */
    fun loadDoctors() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getAllDoctors()
                result.onSuccess { doctorsList ->
                    _doctors.value = doctorsList
                    applySearch()
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to load doctors: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading doctors: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Send connection request to a doctor
     */
    fun sendConnectionRequest(doctorId: String, doctorName: String, message: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val result = repository.sendConnectionRequest(doctorId, doctorName, message)
                result.onSuccess {
                    _successMessage.value = "Connection request sent to $doctorName"
                    // Reload doctors to update connection status
                    loadDoctors()
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to send request: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error sending request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cancel connection request
     */
    fun cancelConnectionRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.cancelConnectionRequest(requestId)
                result.onSuccess {
                    _successMessage.value = "Connection request cancelled"
                    loadDoctors()
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to cancel request: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error cancelling request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Observe feedback from doctors
     */
    private fun observeFeedback() {
        viewModelScope.launch {
            try {
                repository.getPatientFeedback().collect { feedbackList ->
                    _feedback.value = feedbackList
                }
            } catch (e: Exception) {
                // Silently fail - feedback is optional
                _feedback.value = emptyList()
            }
        }
    }

    /**
     * Mark feedback as read
     */
    fun markFeedbackAsRead(feedbackId: String) {
        viewModelScope.launch {
            repository.markFeedbackAsRead(feedbackId)
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applySearch()
    }

    /**
     * Apply search filter
     */
    private fun applySearch() {
        val query = _searchQuery.value.trim()
        _filteredDoctors.value = if (query.isEmpty()) {
            _doctors.value
        } else {
            _doctors.value.filter { doctor ->
                doctor.fullName.contains(query, ignoreCase = true) ||
                        doctor.specialization.contains(query, ignoreCase = true) ||
                        doctor.currentHospital.contains(query, ignoreCase = true) ||
                        doctor.medicalExpertise.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadDoctors()
    }

    /**
     * Get unread feedback count
     */
    fun getUnreadFeedbackCount(): Int {
        return _feedback.value.count { !it.isRead }
    }
}
