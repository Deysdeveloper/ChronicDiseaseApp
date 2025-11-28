package com.example.chronicdiseaseapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.PermissionController

class HealthConnectPermissionHandler(private val context: Context) {

    private val tag = "HealthPermissions"

    // Define the health permissions we need for Galaxy Watch 4 data
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    /**
     * Check if Health Connect is available on this device
     */
    fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.e(tag, "Error checking Health Connect availability", e)
            false
        }
    }

    /**
     * Check if all required permissions are granted
     */
    suspend fun arePermissionsGranted(): Boolean {
        return try {
            if (!isHealthConnectAvailable()) {
                Log.w(tag, "Health Connect not available")
                return false
            }

            val healthConnectClient = HealthConnectClient.getOrCreate(context)
            val grantedPermissions =
                healthConnectClient.permissionController.getGrantedPermissions()

            val allGranted = requiredPermissions.all { permission ->
                grantedPermissions.contains(permission)
            }

            Log.d(tag, "Permissions check: $allGranted")
            Log.d(tag, "Required: ${requiredPermissions.size}, Granted: ${grantedPermissions.size}")

            allGranted
        } catch (e: Exception) {
            Log.e(tag, "Error checking permissions", e)
            false
        }
    }

    /**
     * Get the ActivityResultContract for requesting permissions
     * Returns the correct Health Connect contract that expects permission strings
     */
    fun getPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    /**
     * Get human-readable permission descriptions
     */
    fun getPermissionDescriptions(): List<String> {
        return listOf(
            "Heart Rate - Monitor your heart rate data from Samsung Galaxy Watch",
            "Blood Pressure - Track blood pressure measurements",
            "SpO2 (Oxygen Saturation) - Monitor blood oxygen levels",
            "Steps - Count daily steps and activity",
            "Calories - Track active calories burned"
        )
    }

    /**
     * Get permission status summary for UI display
     */
    suspend fun getPermissionStatusSummary(): PermissionStatus {
        return try {
            when {
                !isHealthConnectAvailable() -> PermissionStatus.NotAvailable
                arePermissionsGranted() -> PermissionStatus.Granted
                else -> PermissionStatus.NotGranted
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting permission status", e)
            PermissionStatus.Error
        }
    }
}

/**
 * Enum representing the status of Health Connect permissions
 */
enum class PermissionStatus {
    NotAvailable,   // Health Connect not installed/available
    Granted,        // All required permissions granted
    NotGranted,     // Some or all permissions not granted
    Error          // Error checking permissions
}