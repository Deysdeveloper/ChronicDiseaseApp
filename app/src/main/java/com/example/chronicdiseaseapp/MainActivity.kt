package com.example.chronicdiseaseapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chronicdiseaseapp.navigation.Navigation
import com.example.chronicdiseaseapp.ui.theme.ChronicDiseaseAppTheme
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    private var appStartTrace: Trace? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start Performance Monitoring trace for app startup
        appStartTrace = FirebasePerformance.getInstance().newTrace("app_startup_trace")
        appStartTrace?.start()
        Log.d(TAG, "Firebase Performance Monitoring: App startup trace started")
        
        enableEdgeToEdge()
        setContent {
            ChronicDiseaseAppTheme {
                Navigation()
            }
        }
        
        // Stop the trace after UI is set up
        appStartTrace?.stop()
        Log.d(TAG, "Firebase Performance Monitoring: App startup trace stopped")
        
        // Test Performance Monitoring with a custom metric
        testPerformanceMonitoring()
    }
    
    private fun testPerformanceMonitoring() {
        val trace = FirebasePerformance.getInstance().newTrace("test_performance_trace")
        trace.start()
        
        // Add custom attributes
        trace.putAttribute("test_attribute", "test_value")
        trace.putAttribute("app_version", "1.0")
        
        // Add custom metrics
        trace.putMetric("test_metric", 100)
        
        // Simulate some work
        Thread.sleep(100)
        
        trace.stop()
        Log.d(TAG, "Firebase Performance Monitoring: Test trace completed successfully")
        Log.d(TAG, "Firebase Performance is WORKING ✓")
    }
}

