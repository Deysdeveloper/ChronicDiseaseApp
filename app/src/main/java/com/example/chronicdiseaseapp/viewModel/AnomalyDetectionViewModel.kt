package com.example.chronicdiseaseapp.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronicdiseaseapp.datamodels.HealthReading
import com.example.chronicdiseaseapp.ml.AnomalyResult
import com.example.chronicdiseaseapp.ml.TFLiteModelHelper
import com.example.chronicdiseaseapp.ml.VitalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for ML-powered anomaly detection in health vitals
 */
class AnomalyDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "AnomalyDetectionVM"

    // TFLite model helper
    private val modelHelper = TFLiteModelHelper(application.applicationContext)

    // LiveData for anomaly results
    private val _anomalies = MutableLiveData<List<AnomalyResult>>(emptyList())
    val anomalies: LiveData<List<AnomalyResult>> = _anomalies

    // LiveData for loading state
    private val _isAnalyzing = MutableLiveData(false)
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    // LiveData for model initialization status
    private val _modelInitialized = MutableLiveData(false)
    val modelInitialized: LiveData<Boolean> = _modelInitialized

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Track if analysis has been performed
    private val _hasAnalyzed = MutableLiveData(false)
    val hasAnalyzed: LiveData<Boolean> = _hasAnalyzed

    init {
        initializeModel()
    }

    /**
     * Initialize the TFLite model
     */
    private fun initializeModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = modelHelper.initialize()
                withContext(Dispatchers.Main) {
                    _modelInitialized.value = success
                    if (success) {
                        Log.d(tag, "✅ ML model initialized successfully")
                    } else {
                        _errorMessage.value = "Failed to initialize ML model"
                        Log.e(tag, "❌ Failed to initialize ML model")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _modelInitialized.value = false
                    _errorMessage.value = "Error initializing model: ${e.message}"
                    Log.e(tag, "Error initializing model", e)
                }
            }
        }
    }

    /**
     * Analyze heart rate data for anomalies
     */
    fun analyzeHeartRate(readings: List<HealthReading>) {
        if (readings.isEmpty()) {
            _errorMessage.value = "No heart rate data available"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { _isAnalyzing.value = true }

                // Filter valid heart rate values (20-250 bpm) - exclude corrupted data
                val validReadings = readings.filter { reading ->
                    val hr = reading.heartRate?.toFloat()
                    hr != null && hr >= 20f && hr <= 250f
                }

                val invalidCount = readings.size - validReadings.size
                if (invalidCount > 0) {
                    Log.w(
                        tag,
                        "⚠️ Filtered out $invalidCount invalid heart rate readings (values outside 20-250 bpm)"
                    )
                }

                val values = validReadings.mapNotNull { it.heartRate?.toFloat() }
                val timestamps = validReadings.mapNotNull {
                    if (it.heartRate != null) it.timestamp else null
                }

                if (values.size < 10) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Need at least 10 heart rate readings"
                        _isAnalyzing.value = false
                    }
                    return@launch
                }

                val detectedAnomalies = modelHelper.detectAnomaliesInSeries(
                    values = values,
                    timestamps = timestamps,
                    vitalType = VitalType.HEART_RATE
                )

                withContext(Dispatchers.Main) {
                    _anomalies.value = detectedAnomalies
                    _isAnalyzing.value = false
                    _hasAnalyzed.value = true
                    Log.d(tag, "Heart Rate Analysis: ${detectedAnomalies.size} anomalies found")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error analyzing heart rate: ${e.message}"
                    _isAnalyzing.value = false
                    Log.e(tag, "Error analyzing heart rate", e)
                }
            }
        }
    }

    /**
     * Analyze SpO2 data for anomalies
     */
    fun analyzeSpO2(readings: List<HealthReading>) {
        if (readings.isEmpty()) {
            _errorMessage.value = "No SpO2 data available"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { _isAnalyzing.value = true }

                // Filter valid SpO2 values (0-100%) - exclude corrupted data
                val validReadings = readings.filter { reading ->
                    val spo2 = reading.oxygenSaturation?.toFloat()
                    spo2 != null && spo2 >= 0f && spo2 <= 100f
                }

                // Log invalid data for debugging
                val invalidCount = readings.size - validReadings.size
                if (invalidCount > 0) {
                    Log.w(
                        tag,
                        "⚠️ Filtered out $invalidCount invalid SpO2 readings (values outside 0-100%)"
                    )
                    readings.filter {
                        val spo2 = it.oxygenSaturation?.toFloat()
                        spo2 != null && (spo2 < 0f || spo2 > 100f)
                    }.forEach { reading ->
                        Log.w(
                            tag,
                            "  Invalid: ${reading.oxygenSaturation}% at ${
                                java.text.SimpleDateFormat("HH:mm:ss").format(reading.timestamp)
                            }"
                        )
                    }
                }

                val values = validReadings.mapNotNull { it.oxygenSaturation?.toFloat() }
                val timestamps = validReadings.mapNotNull {
                    if (it.oxygenSaturation != null) it.timestamp else null
                }

                if (values.size < 10) {
                    withContext(Dispatchers.Main) {
                        val totalReadings = readings.size
                        val validReadings = values.size
                        val filteredCount = totalReadings - validReadings

                        _errorMessage.value = if (filteredCount > 0) {
                            "Need at least 10 valid SpO2 readings. Found $validReadings valid out of $totalReadings total ($filteredCount corrupted values filtered out)"
                        } else {
                            "Need at least 10 SpO2 readings (currently have $validReadings)"
                        }
                        _isAnalyzing.value = false
                    }
                    return@launch
                }

                val detectedAnomalies = modelHelper.detectAnomaliesInSeries(
                    values = values,
                    timestamps = timestamps,
                    vitalType = VitalType.SPO2
                )

                withContext(Dispatchers.Main) {
                    _anomalies.value = detectedAnomalies
                    _isAnalyzing.value = false
                    _hasAnalyzed.value = true
                    Log.d(tag, "SpO2 Analysis: ${detectedAnomalies.size} anomalies found")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error analyzing SpO2: ${e.message}"
                    _isAnalyzing.value = false
                    Log.e(tag, "Error analyzing SpO2", e)
                }
            }
        }
    }

    /**
     * Analyze blood pressure data for anomalies
     */
    fun analyzeBloodPressure(readings: List<HealthReading>, useSystolic: Boolean = true) {
        if (readings.isEmpty()) {
            _errorMessage.value = "No blood pressure data available"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { _isAnalyzing.value = true }

                val values = if (useSystolic) {
                    readings.mapNotNull { it.bloodPressureSystolic?.toFloat() }
                } else {
                    readings.mapNotNull { it.bloodPressureDiastolic?.toFloat() }
                }

                val timestamps = readings.mapNotNull { reading ->
                    if (useSystolic) {
                        if (reading.bloodPressureSystolic != null) reading.timestamp else null
                    } else {
                        if (reading.bloodPressureDiastolic != null) reading.timestamp else null
                    }
                }

                if (values.size < 10) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Need at least 10 blood pressure readings"
                        _isAnalyzing.value = false
                    }
                    return@launch
                }

                val vitalType = if (useSystolic) VitalType.SYSTOLIC else VitalType.DIASTOLIC
                val detectedAnomalies = modelHelper.detectAnomaliesInSeries(
                    values = values,
                    timestamps = timestamps,
                    vitalType = vitalType
                )

                withContext(Dispatchers.Main) {
                    _anomalies.value = detectedAnomalies
                    _isAnalyzing.value = false
                    _hasAnalyzed.value = true
                    val bpType = if (useSystolic) "Systolic" else "Diastolic"
                    Log.d(tag, "$bpType BP Analysis: ${detectedAnomalies.size} anomalies found")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error analyzing blood pressure: ${e.message}"
                    _isAnalyzing.value = false
                    Log.e(tag, "Error analyzing blood pressure", e)
                }
            }
        }
    }

    /**
     * Analyze all vitals at once
     */
    fun analyzeAllVitals(
        heartRateData: List<HealthReading>,
        spO2Data: List<HealthReading>,
        bloodPressureData: List<HealthReading>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { _isAnalyzing.value = true }

                val allAnomalies = mutableListOf<AnomalyResult>()

                // Analyze Heart Rate (with validation)
                if (heartRateData.size >= 10) {
                    // Filter valid heart rate values (20-250 bpm)
                    val validHR = heartRateData.filter { reading ->
                        val hr = reading.heartRate?.toFloat()
                        hr != null && hr >= 20f && hr <= 250f
                    }

                    if (validHR.size >= 10) {
                        val hrValues = validHR.mapNotNull { it.heartRate?.toFloat() }
                        val hrTimestamps = validHR.mapNotNull {
                            if (it.heartRate != null) it.timestamp else null
                        }
                        val hrAnomalies = modelHelper.detectAnomaliesInSeries(
                            hrValues, hrTimestamps, VitalType.HEART_RATE
                        )
                        allAnomalies.addAll(hrAnomalies)
                    }
                }

                // Analyze SpO2 (with validation)
                if (spO2Data.size >= 10) {
                    // Filter valid SpO2 values (0-100%)
                    val validSpO2 = spO2Data.filter { reading ->
                        val spo2 = reading.oxygenSaturation?.toFloat()
                        spo2 != null && spo2 >= 0f && spo2 <= 100f
                    }

                    val invalidSpO2Count = spO2Data.size - validSpO2.size
                    if (invalidSpO2Count > 0) {
                        Log.w(
                            tag,
                            "⚠️ Filtered $invalidSpO2Count invalid SpO2 readings in batch analysis"
                        )
                    }

                    if (validSpO2.size >= 10) {
                        val spo2Values = validSpO2.mapNotNull { it.oxygenSaturation?.toFloat() }
                        val spo2Timestamps = validSpO2.mapNotNull {
                            if (it.oxygenSaturation != null) it.timestamp else null
                        }
                        val spo2Anomalies = modelHelper.detectAnomaliesInSeries(
                            spo2Values, spo2Timestamps, VitalType.SPO2
                        )
                        allAnomalies.addAll(spo2Anomalies)
                    }
                }

                // Analyze Systolic BP (with validation)
                if (bloodPressureData.size >= 10) {
                    // Filter valid systolic values (60-250 mmHg)
                    val validSystolic = bloodPressureData.filter { reading ->
                        val sys = reading.bloodPressureSystolic?.toFloat()
                        sys != null && sys >= 60f && sys <= 250f
                    }

                    if (validSystolic.size >= 10) {
                        val sysValues =
                            validSystolic.mapNotNull { it.bloodPressureSystolic?.toFloat() }
                        val sysTimestamps = validSystolic.mapNotNull {
                            if (it.bloodPressureSystolic != null) it.timestamp else null
                        }
                        val sysAnomalies = modelHelper.detectAnomaliesInSeries(
                            sysValues, sysTimestamps, VitalType.SYSTOLIC
                        )
                        allAnomalies.addAll(sysAnomalies)
                    }

                    // Analyze Diastolic BP (with validation)
                    val validDiastolic = bloodPressureData.filter { reading ->
                        val dia = reading.bloodPressureDiastolic?.toFloat()
                        dia != null && dia >= 30f && dia <= 150f
                    }

                    if (validDiastolic.size >= 10) {
                        val diaValues =
                            validDiastolic.mapNotNull { it.bloodPressureDiastolic?.toFloat() }
                        val diaTimestamps = validDiastolic.mapNotNull {
                            if (it.bloodPressureDiastolic != null) it.timestamp else null
                        }
                        val diaAnomalies = modelHelper.detectAnomaliesInSeries(
                            diaValues, diaTimestamps, VitalType.DIASTOLIC
                        )
                        allAnomalies.addAll(diaAnomalies)
                    }
                }

                // Sort by timestamp (most recent first)
                val sortedAnomalies = allAnomalies.sortedByDescending { it.timestamp }

                withContext(Dispatchers.Main) {
                    _anomalies.value = sortedAnomalies
                    _isAnalyzing.value = false
                    _hasAnalyzed.value = true
                    Log.d(tag, "Complete Analysis: ${sortedAnomalies.size} total anomalies found")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error analyzing vitals: ${e.message}"
                    _isAnalyzing.value = false
                    Log.e(tag, "Error analyzing all vitals", e)
                }
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
     * Clear detected anomalies
     */
    fun clearAnomalies() {
        _anomalies.value = emptyList()
        _hasAnalyzed.value = false
    }

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        modelHelper.close()
        Log.d(tag, "ViewModel cleared, model resources released")
    }
}
