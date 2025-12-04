package com.example.chronicdiseaseapp.ml

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

/**
 * Helper class to load and run TensorFlow Lite model for health vital anomaly detection
 */
class TFLiteModelHelper(private val context: Context) {

    private val tag = "TFLiteModelHelper"

    // Model interpreter
    private var interpreter: Interpreter? = null

    // Model metadata
    private var trainMin: Float = 60f
    private var trainMax: Float = 220f
    private var threshold: Float = 0.015f
    private var windowLen: Int = 10
    private var target: String = "systolic"

    // Detection parameters
    private var kVotes: Int = 2
    private var delta: Float = 8.0f
    private var absCriticalLowSpO2: Float = 90.0f
    private var absCriticalHighSystolic: Float = 180.0f
    private var absCriticalLowSystolic: Float = 90.0f
    private var absCriticalHighDiastolic: Float = 85.0f
    private var absCriticalLowDiastolic: Float = 40.0f
    private var absCriticalHighHeartRate: Float = 85.0f
    private var absCriticalLowHeartRate: Float = 40.0f

    /**
     * Initialize the model by loading TFLite file and metadata
     */
    fun initialize(modelFileName: String = "conv_model_float32.tflite"): Boolean {
        return try {
            // Load metadata
            loadMetadata()

            // Load TFLite model
            val modelBuffer = loadModelFile(modelFileName)

            // Create interpreter with options
            val options = Interpreter.Options().apply {
                setNumThreads(4) // Use 4 threads for better performance
                setUseNNAPI(false) // Disable NNAPI for compatibility
            }

            interpreter = Interpreter(modelBuffer, options)

            Log.d(tag, "✅ TFLite model initialized successfully")
            Log.d(tag, "Model: $modelFileName, Target: $target, Window: $windowLen")
            true
        } catch (e: Exception) {
            Log.e(tag, "❌ Failed to initialize TFLite model", e)
            false
        }
    }

    /**
     * Load model metadata from JSON file
     */
    private fun loadMetadata() {
        try {
            val jsonString =
                context.assets.open("model_meta.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            trainMin = jsonObject.getDouble("min").toFloat()
            trainMax = jsonObject.getDouble("max").toFloat()
            threshold = jsonObject.getDouble("threshold").toFloat()
            windowLen = jsonObject.getInt("window_len")
            target = jsonObject.getString("target")
            kVotes = jsonObject.optInt("k_votes", 2)
            delta = jsonObject.optDouble("delta", 8.0).toFloat()
            absCriticalLowSpO2 = jsonObject.optDouble("abs_critical_low_spo2", 90.0).toFloat()
            absCriticalHighSystolic =
                jsonObject.optDouble("abs_critical_high_systolic", 180.0).toFloat()
            absCriticalLowSystolic =
                jsonObject.optDouble("abs_critical_low_systolic", 90.0).toFloat()
            absCriticalHighDiastolic =
                jsonObject.optDouble("abs_critical_high_diastolic", 85.0).toFloat()
            absCriticalLowDiastolic =
                jsonObject.optDouble("abs_critical_low_diastolic", 40.0).toFloat()
            absCriticalHighHeartRate =
                jsonObject.optDouble("abs_critical_high_heart_rate", 85.0).toFloat()
            absCriticalLowHeartRate =
                jsonObject.optDouble("abs_critical_low_heart_rate", 40.0).toFloat()

            Log.d(tag, "Metadata loaded - Min: $trainMin, Max: $trainMax, Threshold: $threshold")
            Log.d(
                tag,
                "Heart Rate thresholds: Low=$absCriticalLowHeartRate, High=$absCriticalHighHeartRate"
            )
            Log.d(tag, "Diastolic BP thresholds: Low=$absCriticalLowDiastolic, High=$absCriticalHighDiastolic")
        } catch (e: Exception) {
            Log.e(tag, "Error loading metadata, using defaults", e)
        }
    }

    /**
     * Load TFLite model file from assets
     */
    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Normalize input values using min-max scaling
     * Uses dynamic range based on vital type
     */
    private fun normalize(value: Float, vitalType: VitalType): Float {
        // Use appropriate range based on vital type
        val (min, max) = when (vitalType) {
            VitalType.SPO2 -> Pair(trainMin, trainMax) // Use trained range for SpO2
            VitalType.HEART_RATE -> Pair(40f, 200f) // Dynamic range for HR
            VitalType.SYSTOLIC -> Pair(60f, 220f) // Dynamic range for systolic
            VitalType.DIASTOLIC -> Pair(40f, 140f) // Dynamic range for diastolic
        }
        return (value - min) / (max - min + 1e-9f)
    }

    /**
     * Run inference on a single window of data
     * Returns pair of (MSE values, anomaly mask)
     */
    fun detectAnomaliesInWindow(window: FloatArray,
                                vitalType: VitalType
    ): Pair<FloatArray, BooleanArray> {
        if (interpreter == null) {
            Log.e(tag, "Model not initialized")
            return Pair(FloatArray(window.size), BooleanArray(window.size))
        }

        if (window.size != windowLen) {
            Log.e(tag, "Window size mismatch: expected $windowLen, got ${window.size}")
            return Pair(FloatArray(window.size), BooleanArray(window.size))
        }

        try {
            // Handle NaN values - replace with median of non-NaN values
            val cleanWindow =
                window.map { if (it.isNaN()) getMedian(window) else it }.toFloatArray()

            // Normalize input
            val normalizedWindow = cleanWindow.map { normalize(it, vitalType) }.toFloatArray()

            // Prepare input buffer [1, window_len, 1]
            val inputBuffer = ByteBuffer.allocateDirect(4 * windowLen).apply {
                order(ByteOrder.nativeOrder())
                normalizedWindow.forEach { putFloat(it) }
                rewind()
            }

            // Prepare output buffer [1, window_len, 1]
            val outputBuffer = ByteBuffer.allocateDirect(4 * windowLen).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Extract reconstruction
            outputBuffer.rewind()
            val reconstruction = FloatArray(windowLen) { outputBuffer.float }

            // Calculate MSE for each point
            val mseValues = FloatArray(windowLen) { i ->
                val error = reconstruction[i] - normalizedWindow[i]
                error * error
            }

            // Create anomaly mask based on threshold
            val anomalyMask = BooleanArray(windowLen) { i ->
                mseValues[i] > threshold
            }

            return Pair(mseValues, anomalyMask)

        } catch (e: Exception) {
            Log.e(tag, "Error during inference", e)
            return Pair(FloatArray(window.size), BooleanArray(window.size))
        }
    }

    /**
     * Get median of array, ignoring NaN values
     */
    private fun getMedian(values: FloatArray): Float {
        val validValues = values.filter { !it.isNaN() }.sorted()
        return if (validValues.isEmpty()) {
            0f
        } else {
            validValues[validValues.size / 2]
        }
    }

    /**
     * Calculate local median for deviation detection
     */
    private fun calculateLocalMedian(values: List<Float>, index: Int, radius: Int = 5): Float {
        val start = maxOf(0, index - radius)
        val end = minOf(values.size, index + radius + 1)
        val neighborhood = values.subList(start, end).filter { !it.isNaN() }
        return if (neighborhood.isEmpty()) {
            getMedian(values.toFloatArray())
        } else {
            neighborhood.sorted()[neighborhood.size / 2]
        }
    }

    /**
     * Detect anomalies in a time series using sliding window approach
     * Returns list of AnomalyResult objects
     */
    fun detectAnomaliesInSeries(
        values: List<Float>,
        timestamps: List<Long>,
        vitalType: VitalType
    ): List<AnomalyResult> {
        if (interpreter == null) {
            Log.e(tag, "Model not initialized")
            return emptyList()
        }

        val n = values.size
        if (n < windowLen) {
            Log.w(tag, "Not enough data points: $n < $windowLen")
            return emptyList()
        }

        // Voting array - counts how many windows flagged each point
        val votes = IntArray(n)

        // Slide window across the series
        for (start in 0..(n - windowLen)) {
            val window = values.subList(start, start + windowLen).toFloatArray()

            // Skip if too many NaNs
            if (window.count { it.isNaN() } >= windowLen) {
                continue
            }

            val (_, mask) = detectAnomaliesInWindow(window, vitalType)

            // Increment votes for flagged points
            mask.forEachIndexed { idx, isFlagged ->
                if (isFlagged) {
                    votes[start + idx]++
                }
            }
        }

        // Calculate local medians for deviation check
        val localMedians = values.indices.map { i ->
            calculateLocalMedian(values, i)
        }

        // Get critical thresholds based on vital type
        val (criticalLow, criticalHigh) = when (vitalType) {
            VitalType.SPO2 -> Pair(absCriticalLowSpO2, Float.MAX_VALUE)
            VitalType.SYSTOLIC -> Pair(absCriticalLowSystolic, absCriticalHighSystolic)
            VitalType.DIASTOLIC -> Pair(absCriticalLowDiastolic, absCriticalHighDiastolic)
            VitalType.HEART_RATE -> Pair(absCriticalLowHeartRate, absCriticalHighHeartRate)
        }

        // Identify final anomalies
        val anomalies = mutableListOf<AnomalyResult>()

        for (i in values.indices) {
            val value = values[i]

            // Skip NaN values
            if (value.isNaN()) continue

            // Check model votes
            val hasModelVotes = votes[i] >= kVotes

            // Check deviation from local median or critical thresholds
            val hasDeviation = abs(value - localMedians[i]) >= delta ||
                    value <= criticalLow ||
                    value >= criticalHigh

            // Check if value exceeds critical thresholds (absolute check)
            val exceedsCriticalThreshold = value <= criticalLow || value >= criticalHigh

            // Final anomaly: either exceeds critical threshold OR (has votes AND deviation)
            if (exceedsCriticalThreshold || (hasModelVotes && hasDeviation)) {
                val severity = when {
                    value <= criticalLow || value >= criticalHigh -> AnomalySeverity.CRITICAL
                    abs(value - localMedians[i]) >= delta * 2 -> AnomalySeverity.HIGH
                    votes[i] >= kVotes * 2 -> AnomalySeverity.HIGH
                    else -> AnomalySeverity.MEDIUM
                }

                anomalies.add(
                    AnomalyResult(
                        timestamp = timestamps[i],
                        value = value,
                        vitalType = vitalType,
                        severity = severity,
                        votes = votes[i],
                        localMedian = localMedians[i],
                        deviation = abs(value - localMedians[i])
                    )
                )
            }
        }

        Log.d(tag, "Detected ${anomalies.size} anomalies in ${values.size} data points")
        return anomalies
    }

    /**
     * Clean up resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(tag, "Model resources released")
    }
}

/**
 * Vital type enum
 */
enum class VitalType {
    SPO2,
    SYSTOLIC,
    DIASTOLIC,
    HEART_RATE
}

/**
 * Anomaly severity levels
 */
enum class AnomalySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Result of anomaly detection
 */
data class AnomalyResult(
    val timestamp: Long,
    val value: Float,
    val vitalType: VitalType,
    val severity: AnomalySeverity,
    val votes: Int,
    val localMedian: Float,
    val deviation: Float
) {
    fun getMessage(): String {
        val vitalName = when (vitalType) {
            VitalType.SPO2 -> "SpO2"
            VitalType.SYSTOLIC -> "Systolic BP"
            VitalType.DIASTOLIC -> "Diastolic BP"
            VitalType.HEART_RATE -> "Heart Rate"
        }

        return when (severity) {
            AnomalySeverity.CRITICAL -> "⚠️ CRITICAL: $vitalName at $value is critically abnormal"
            AnomalySeverity.HIGH -> "🔴 HIGH: $vitalName at $value shows significant deviation"
            AnomalySeverity.MEDIUM -> "🟡 MEDIUM: $vitalName at $value may be abnormal"
            AnomalySeverity.LOW -> "🟢 LOW: $vitalName at $value shows minor irregularity"
        }
    }
}
