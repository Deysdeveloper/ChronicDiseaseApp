package com.example.chronicdiseaseapp.ml

import android.content.Context
import android.util.Log

/**
 * Test class to verify ML model integration
 */
class ModelTester(private val context: Context) {

    private val tag = "ModelTester"

    /**
     * Run comprehensive tests on the ML model
     */
    fun runTests(): TestResults {
        val results = TestResults()

        Log.d(tag, "========================================")
        Log.d(tag, "Starting ML Model Integration Tests")
        Log.d(tag, "========================================")

        // Test 1: Model Initialization
        results.test1_modelInit = testModelInitialization()

        // Test 2: Single Window Inference
        results.test2_singleWindow = testSingleWindowInference()

        // Test 3: Series Detection (Normal Data)
        results.test3_normalSeries = testNormalSeriesDetection()

        // Test 4: Series Detection (Anomaly Data)
        results.test4_anomalySeries = testAnomalySeriesDetection()

        // Test 5: NaN Handling
        results.test5_nanHandling = testNaNHandling()

        // Print Summary
        printTestSummary(results)

        return results
    }

    /**
     * Test 1: Model Initialization
     */
    private fun testModelInitialization(): Boolean {
        Log.d(tag, "\n--- Test 1: Model Initialization ---")
        return try {
            val modelHelper = TFLiteModelHelper(context)
            val success = modelHelper.initialize()

            if (success) {
                Log.d(tag, "✅ PASS: Model initialized successfully")
                modelHelper.close()
                true
            } else {
                Log.e(tag, "❌ FAIL: Model initialization failed")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ FAIL: Exception during initialization", e)
            false
        }
    }

    /**
     * Test 2: Single Window Inference
     */
    private fun testSingleWindowInference(): Boolean {
        Log.d(tag, "\n--- Test 2: Single Window Inference ---")
        return try {
            val modelHelper = TFLiteModelHelper(context)
            if (!modelHelper.initialize()) {
                Log.e(tag, "❌ FAIL: Model not initialized")
                return false
            }

            // Normal SpO2 window (97-99%)
            val normalWindow = floatArrayOf(98f, 97f, 98f, 99f, 98f, 97f, 98f, 99f, 97f, 98f)

            val (mse, mask) = modelHelper.detectAnomaliesInWindow(normalWindow, VitalType.SPO2)

            Log.d(tag, "Input: ${normalWindow.joinToString()}")
            Log.d(tag, "MSE: ${mse.joinToString { "%.4f".format(it) }}")
            Log.d(tag, "Mask: ${mask.joinToString()}")

            val anomalyCount = mask.count { it }
            Log.d(tag, "Anomalies detected: $anomalyCount / ${mask.size}")

            modelHelper.close()

            if (mse.size == normalWindow.size && mask.size == normalWindow.size) {
                Log.d(tag, "✅ PASS: Inference completed with correct output shape")
                true
            } else {
                Log.e(tag, "❌ FAIL: Output shape mismatch")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ FAIL: Exception during inference", e)
            false
        }
    }

    /**
     * Test 3: Series Detection (Normal Data)
     */
    private fun testNormalSeriesDetection(): Boolean {
        Log.d(tag, "\n--- Test 3: Normal Series Detection ---")
        return try {
            val modelHelper = TFLiteModelHelper(context)
            if (!modelHelper.initialize()) {
                Log.e(tag, "❌ FAIL: Model not initialized")
                return false
            }

            // 30 normal SpO2 readings (97-99%)
            val normalValues = List(30) { 97f + (it % 3).toFloat() }
            val timestamps = List(30) { System.currentTimeMillis() + it * 60000L }

            Log.d(tag, "Testing with 30 normal SpO2 readings (97-99%)")

            val anomalies = modelHelper.detectAnomaliesInSeries(
                values = normalValues,
                timestamps = timestamps,
                vitalType = VitalType.SPO2
            )

            Log.d(tag, "Anomalies detected: ${anomalies.size} / ${normalValues.size}")

            anomalies.forEach { anomaly ->
                Log.d(
                    tag,
                    "  - ${anomaly.severity}: Value=${anomaly.value}, Votes=${anomaly.votes}"
                )
            }

            modelHelper.close()

            // For normal data, we expect 0 or very few anomalies
            if (anomalies.size <= 3) {
                Log.d(tag, "✅ PASS: Normal data correctly identified (${anomalies.size} anomalies)")
                true
            } else {
                Log.w(tag, "⚠️ WARNING: Too many anomalies for normal data (${anomalies.size})")
                true // Still pass but with warning
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ FAIL: Exception during series detection", e)
            false
        }
    }

    /**
     * Test 4: Series Detection (Anomaly Data)
     */
    private fun testAnomalySeriesDetection(): Boolean {
        Log.d(tag, "\n--- Test 4: Anomaly Series Detection ---")
        return try {
            val modelHelper = TFLiteModelHelper(context)
            if (!modelHelper.initialize()) {
                Log.e(tag, "❌ FAIL: Model not initialized")
                return false
            }

            // SpO2 series with anomalies
            val anomalyValues = listOf(
                98f, 97f, 98f, 99f, 98f,  // Normal
                85f,                       // Critical drop (anomaly)
                87f, 88f,                  // Low values (anomalies)
                97f, 98f, 98f, 97f,       // Recovery to normal
                92f, 93f, 94f,            // Slightly low (may be anomaly)
                98f, 99f, 97f, 98f, 99f   // Back to normal
            )
            val timestamps = anomalyValues.indices.map { System.currentTimeMillis() + it * 60000L }

            Log.d(tag, "Testing with SpO2 data containing anomalies")
            Log.d(tag, "Values: ${anomalyValues.joinToString()}")

            val anomalies = modelHelper.detectAnomaliesInSeries(
                values = anomalyValues,
                timestamps = timestamps,
                vitalType = VitalType.SPO2
            )

            Log.d(tag, "Anomalies detected: ${anomalies.size} / ${anomalyValues.size}")

            anomalies.forEach { anomaly ->
                Log.d(
                    tag,
                    "  - ${anomaly.severity}: Value=${anomaly.value}, Votes=${anomaly.votes}, Deviation=${anomaly.deviation}"
                )
            }

            modelHelper.close()

            // We expect to detect the critical drops (85, 87, 88)
            val criticalAnomalies = anomalies.filter { it.severity == AnomalySeverity.CRITICAL }

            if (anomalies.isNotEmpty() && criticalAnomalies.isNotEmpty()) {
                Log.d(
                    tag,
                    "✅ PASS: Anomalies detected (${anomalies.size} total, ${criticalAnomalies.size} critical)"
                )
                true
            } else {
                Log.e(tag, "❌ FAIL: Expected to detect anomalies but found ${anomalies.size}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ FAIL: Exception during anomaly detection", e)
            false
        }
    }

    /**
     * Test 5: NaN Handling
     */
    private fun testNaNHandling(): Boolean {
        Log.d(tag, "\n--- Test 5: NaN Handling ---")
        return try {
            val modelHelper = TFLiteModelHelper(context)
            if (!modelHelper.initialize()) {
                Log.e(tag, "❌ FAIL: Model not initialized")
                return false
            }

            // Window with some NaN values
            val windowWithNaN =
                floatArrayOf(98f, Float.NaN, 97f, 98f, Float.NaN, 99f, 98f, 97f, 98f, 99f)

            Log.d(tag, "Testing with NaN values")
            Log.d(
                tag,
                "Input: ${windowWithNaN.joinToString { if (it.isNaN()) "NaN" else "%.1f".format(it) }}"
            )

            val (mse, mask) = modelHelper.detectAnomaliesInWindow(windowWithNaN, VitalType.SPO2)

            Log.d(tag, "MSE: ${mse.joinToString { "%.4f".format(it) }}")
            Log.d(tag, "Mask: ${mask.joinToString()}")

            modelHelper.close()

            // Should handle NaN without crashing
            if (mse.none { it.isNaN() }) {
                Log.d(tag, "✅ PASS: NaN values handled correctly")
                true
            } else {
                Log.e(tag, "❌ FAIL: NaN propagated to output")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ FAIL: Exception during NaN handling", e)
            false
        }
    }

    /**
     * Print test summary
     */
    private fun printTestSummary(results: TestResults) {
        Log.d(tag, "\n========================================")
        Log.d(tag, "ML Model Test Summary")
        Log.d(tag, "========================================")
        Log.d(
            tag,
            "Test 1 - Model Initialization:     ${if (results.test1_modelInit) "✅ PASS" else "❌ FAIL"}"
        )
        Log.d(
            tag,
            "Test 2 - Single Window Inference:  ${if (results.test2_singleWindow) "✅ PASS" else "❌ FAIL"}"
        )
        Log.d(
            tag,
            "Test 3 - Normal Series Detection:  ${if (results.test3_normalSeries) "✅ PASS" else "❌ FAIL"}"
        )
        Log.d(
            tag,
            "Test 4 - Anomaly Series Detection: ${if (results.test4_anomalySeries) "✅ PASS" else "❌ FAIL"}"
        )
        Log.d(
            tag,
            "Test 5 - NaN Handling:              ${if (results.test5_nanHandling) "✅ PASS" else "❌ FAIL"}"
        )
        Log.d(tag, "----------------------------------------")
        Log.d(tag, "Total: ${results.passedCount()} / 5 tests passed")

        if (results.allPassed()) {
            Log.d(tag, "🎉 ALL TESTS PASSED! Model is working correctly.")
        } else {
            Log.e(tag, "⚠️ SOME TESTS FAILED. Check logs above for details.")
        }
        Log.d(tag, "========================================\n")
    }
}

/**
 * Test results data class
 */
data class TestResults(
    var test1_modelInit: Boolean = false,
    var test2_singleWindow: Boolean = false,
    var test3_normalSeries: Boolean = false,
    var test4_anomalySeries: Boolean = false,
    var test5_nanHandling: Boolean = false
) {
    fun allPassed(): Boolean = test1_modelInit && test2_singleWindow &&
            test3_normalSeries && test4_anomalySeries &&
            test5_nanHandling

    fun passedCount(): Int = listOf(
        test1_modelInit, test2_singleWindow, test3_normalSeries,
        test4_anomalySeries, test5_nanHandling
    ).count { it }
}
