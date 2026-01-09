package com.example.chronicdiseaseapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chronicdiseaseapp.datamodels.PatientVitalsHistory
import com.example.chronicdiseaseapp.datamodels.PatientVitalsInfo
import com.example.chronicdiseaseapp.repository.PatientVitalsRepository
import com.example.chronicdiseaseapp.repository.DoctorConnectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing patient vitals view for doctors
 */
class PatientVitalsViewModel : ViewModel() {

    private val repository = PatientVitalsRepository()
    private val connectionRepository = DoctorConnectionRepository()

    // All patients with their latest vitals
    private val _patientsVitals = MutableStateFlow<List<PatientVitalsInfo>>(emptyList())
    val patientsVitals: StateFlow<List<PatientVitalsInfo>> = _patientsVitals.asStateFlow()

    // Selected patient's detailed vitals history
    private val _selectedPatientHistory = MutableStateFlow<PatientVitalsHistory?>(null)
    val selectedPatientHistory: StateFlow<PatientVitalsHistory?> =
        _selectedPatientHistory.asStateFlow()

    // Search query for filtering patients
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered patients based on search query
    private val _filteredPatients = MutableStateFlow<List<PatientVitalsInfo>>(emptyList())
    val filteredPatients: StateFlow<List<PatientVitalsInfo>> = _filteredPatients.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Sort options
    private val _sortBy = MutableStateFlow(SortOption.NAME)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    // Filter options
    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    init {
        loadAllPatientsVitals()
    }

    /**
     * Load all patients and their vitals
     */
    fun loadAllPatientsVitals() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getAllPatientsWithVitals()

                result.onSuccess { patients ->
                    _patientsVitals.value = patients
                    applyFiltersAndSort()
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to load patients: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading patients: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load detailed vitals history for a specific patient
     */
    fun loadPatientVitalsHistory(patientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getPatientVitalsHistory(patientId)

                result.onSuccess { history ->
                    _selectedPatientHistory.value = history
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to load patient history: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading patient history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update search query and filter patients
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFiltersAndSort()
    }

    /**
     * Update sort option
     */
    fun updateSortOption(sortOption: SortOption) {
        _sortBy.value = sortOption
        applyFiltersAndSort()
    }

    /**
     * Update filter option
     */
    fun updateFilterOption(filterOption: FilterOption) {
        _filterOption.value = filterOption
        applyFiltersAndSort()
    }

    /**
     * Apply filters and sorting to patients list
     */
    private fun applyFiltersAndSort() {
        var patients = _patientsVitals.value

        // Apply search filter
        if (_searchQuery.value.isNotBlank()) {
            patients = patients.filter { patient ->
                patient.patientName.contains(_searchQuery.value, ignoreCase = true) ||
                        patient.patientEmail.contains(_searchQuery.value, ignoreCase = true)
            }
        }

        // Apply data filter
        patients = when (_filterOption.value) {
            FilterOption.ALL -> patients
            FilterOption.WITH_DATA -> patients.filter { it.hasVitalsData }
            FilterOption.WITHOUT_DATA -> patients.filter { !it.hasVitalsData }
        }

        // Apply sorting
        patients = when (_sortBy.value) {
            SortOption.NAME -> patients.sortedBy { it.patientName }
            SortOption.RECENT_UPDATE -> patients.sortedByDescending { it.lastVitalsUpdate }
            SortOption.AGE -> patients.sortedBy { it.age }
            SortOption.HEART_RATE -> patients.sortedByDescending { it.latestHeartRate ?: 0 }
        }

        _filteredPatients.value = patients
    }

    /**
     * Clear selected patient history
     */
    fun clearSelectedPatient() {
        _selectedPatientHistory.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadAllPatientsVitals()
    }

    /**
     * Remove connection with a patient (for doctors to disconnect from patients)
     */
    fun removeConnection(connectionRequestId: String, patientName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val result = connectionRepository.removeConnection(connectionRequestId)
                result.onSuccess {
                    _successMessage.value = "Disconnected from $patientName"
                    loadAllPatientsVitals()
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to remove connection: ${exception.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error removing connection: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    enum class SortOption(val displayName: String) {
        NAME("Name"),
        RECENT_UPDATE("Recent Update"),
        AGE("Age"),
        HEART_RATE("Heart Rate")
    }

    enum class FilterOption(val displayName: String) {
        ALL("All Patients"),
        WITH_DATA("With Vitals"),
        WITHOUT_DATA("No Vitals")
    }
}
