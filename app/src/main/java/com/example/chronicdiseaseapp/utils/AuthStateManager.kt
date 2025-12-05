package com.example.chronicdiseaseapp.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized auth state manager to prevent Health Connect data mix-ups
 *
 * Problem: Health Connect data is device-scoped, not user-scoped.
 * If we read Health Connect before/during auth changes, device readings
 * might be saved under the wrong user account.
 *
 * Solution: This manager ensures Health Connect data is only read AFTER
 * successful sign-in and stops reading on sign-out.
 */
class AuthStateManager private constructor() {

    private val tag = "AuthStateManager"
    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private val callbacks = mutableListOf<AuthStateCallback>()

    sealed class AuthState {
        object Unauthenticated : AuthState()
        data class Authenticated(val userId: String) : AuthState()
    }

    interface AuthStateCallback {
        fun onSignedIn(userId: String)
        fun onSignedOut()
    }

    companion object {
        @Volatile
        private var instance: AuthStateManager? = null

        fun getInstance(): AuthStateManager {
            return instance ?: synchronized(this) {
                instance ?: AuthStateManager().also { instance = it }
            }
        }
    }

    init {
        setupAuthListener()
    }

    private fun setupAuthListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            if (user != null) {
                Log.d(tag, "🟢 User signed in: ${user.uid}")
                _authState.value = AuthState.Authenticated(user.uid)

                // Notify all callbacks
                callbacks.forEach { callback ->
                    try {
                        callback.onSignedIn(user.uid)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in onSignedIn callback", e)
                    }
                }
            } else {
                Log.d(tag, "🔴 User signed out")
                _authState.value = AuthState.Unauthenticated

                // Notify all callbacks
                callbacks.forEach { callback ->
                    try {
                        callback.onSignedOut()
                    } catch (e: Exception) {
                        Log.e(tag, "Error in onSignedOut callback", e)
                    }
                }
            }
        }

        authStateListener?.let { auth.addAuthStateListener(it) }
    }

    /**
     * Register a callback for auth state changes
     */
    fun registerCallback(callback: AuthStateCallback) {
        callbacks.add(callback)
        Log.d(tag, "Registered auth callback (total: ${callbacks.size})")

        // Immediately notify if already authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            callback.onSignedIn(currentUser.uid)
        }
    }

    /**
     * Unregister a callback
     */
    fun unregisterCallback(callback: AuthStateCallback) {
        callbacks.remove(callback)
        Log.d(tag, "Unregistered auth callback (remaining: ${callbacks.size})")
    }

    /**
     * Get current user ID (null if not authenticated)
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Check if user is currently authenticated
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Cleanup when app is destroyed
     */
    fun cleanup() {
        authStateListener?.let { auth.removeAuthStateListener(it) }
        callbacks.clear()
        Log.d(tag, "AuthStateManager cleaned up")
    }
}
