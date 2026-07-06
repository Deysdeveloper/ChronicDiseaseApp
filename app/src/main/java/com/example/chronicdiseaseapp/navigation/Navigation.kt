package com.example.chronicdiseaseapp.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.chronicdiseaseapp.screens.AuthenticationScreen
import com.example.chronicdiseaseapp.screens.patientScreen.HomeScreen
import com.example.chronicdiseaseapp.screens.doctorScreen.DocHomeScreen
import com.example.chronicdiseaseapp.screens.doctorScreen.PatientsVitalsScreen
import com.example.chronicdiseaseapp.screens.LoginScreen
import com.example.chronicdiseaseapp.screens.SignUpScreen
import com.example.chronicdiseaseapp.viewModel.AuthState
import com.example.chronicdiseaseapp.viewModel.FirebaseAuthViewModel
import com.example.chronicdiseaseapp.datamodels.UserType
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy defining all navigation keys (destinations) in the app.
 *
 * Each key implements [NavKey] and is [Serializable] so that the back stack can be
 * saved and restored across configuration changes and process death via
 * [rememberNavBackStack].
 */
sealed interface Screen : NavKey {
    @Serializable data object Authentication : Screen
    @Serializable data object Login : Screen
    @Serializable data object SignUp : Screen
    @Serializable data object PatientHome : Screen
    @Serializable data object DoctorHome : Screen
    @Serializable data object PatientsVitals : Screen
    @Serializable data object ForgotPassword : Screen

    // Legacy home route for backward compatibility
    @Serializable data object Home : Screen
}

/** Replaces the entire back stack with a single [key], clearing any existing history. */
private fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    clear()
    add(key)
}

@Composable
fun Navigation(
    authViewModel: FirebaseAuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.observeAsState()

    // Determine the start destination based on authentication state. When the user is
    // signed in we default to PatientHome and let the LaunchedEffect below correct the
    // destination once the user type is known.
    val startKey: NavKey = if (authViewModel.isUserSignedIn()) {
        Screen.PatientHome
    } else {
        Screen.Authentication
    }

    val backStack = rememberNavBackStack(startKey)

    // Track if initial navigation is done
    var initialNavigationDone by remember { mutableStateOf(false) }

    // Check user type on startup for authenticated users
    LaunchedEffect(authState) {
        if (authState == AuthState.AUTHENTICATED && !initialNavigationDone) {
            authViewModel.getCurrentUserType { userType ->
                val destination: NavKey = when (userType) {
                    UserType.DOCTOR -> Screen.DoctorHome
                    UserType.PATIENT -> Screen.PatientHome
                    else -> Screen.PatientHome
                }

                val currentKey = backStack.lastOrNull()
                if (currentKey != destination && currentKey in listOf(
                        Screen.Authentication,
                        Screen.Login,
                        Screen.SignUp,
                        Screen.PatientHome, // In case we defaulted to patient
                        Screen.Home
                    )
                ) {
                    backStack.resetTo(destination)
                }
                initialNavigationDone = true
            }
        } else if (authState == AuthState.UNAUTHENTICATED) {
            initialNavigationDone = false // Reset for next login
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            // Authentication Landing Screen
            entry<Screen.Authentication> {
                AuthenticationScreen(
                    onLoginClick = {
                        backStack.add(Screen.Login)
                    },
                    onSignUpClick = {
                        backStack.add(Screen.SignUp)
                    }
                )
            }

            // Login Screen
            entry<Screen.Login> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToSignUp = {
                        // Replace Login with SignUp, keeping Authentication underneath
                        backStack.removeLastOrNull()
                        backStack.add(Screen.SignUp)
                    },
                    onNavigateToForgotPassword = {
                        backStack.add(Screen.ForgotPassword)
                    },
                    onLoginSuccess = { userType ->
                        val destination: NavKey = if (userType == "DOCTOR") {
                            Screen.DoctorHome
                        } else {
                            Screen.PatientHome
                        }
                        backStack.resetTo(destination)
                    }
                )
            }

            // Sign Up Screen
            entry<Screen.SignUp> {
                SignUpScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToLogin = {
                        // Replace SignUp with Login, keeping Authentication underneath
                        backStack.removeLastOrNull()
                        backStack.add(Screen.Login)
                    },
                    onSignUpSuccess = { userType ->
                        val destination: NavKey = if (userType == "DOCTOR") {
                            Screen.DoctorHome
                        } else {
                            Screen.PatientHome
                        }
                        backStack.resetTo(destination)
                    }
                )
            }

            // Patient Home Screen
            entry<Screen.PatientHome> {
                HomeScreen(
                    authViewModel = authViewModel,
                    onSignOut = {
                        authViewModel.signOut()
                        backStack.resetTo(Screen.Authentication)
                    }
                )
            }

            // Doctor Home Screen
            entry<Screen.DoctorHome> {
                DocHomeScreen(
                    onNavigateToPatientDetails = { patientId ->
                        // TODO: Navigate to patient details screen
                    },
                    onNavigateToAppointments = {
                        // TODO: Navigate to appointments screen
                    },
                    onNavigateToPatientList = {
                        backStack.add(Screen.PatientsVitals)
                    },
                    onNavigateToProfile = {
                        // TODO: Navigate to doctor profile screen
                    },
                    onSignOut = {
                        authViewModel.signOut()
                        backStack.resetTo(Screen.Authentication)
                    }
                )
            }

            // Patients Vitals Screen
            entry<Screen.PatientsVitals> {
                PatientsVitalsScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    },
                    onNavigateToPatientDetail = { patientId ->
                        // TODO: Navigate to patient detail screen
                    }
                )
            }

            // Legacy Home Screen (redirect to patient home for backward compatibility)
            entry<Screen.Home> {
                HomeScreen(
                    authViewModel = authViewModel,
                    onSignOut = {
                        authViewModel.signOut()
                        backStack.resetTo(Screen.Authentication)
                    }
                )
            }

            // Forgot Password Screen (placeholder for future implementation)
            entry<Screen.ForgotPassword> {
                ForgotPasswordScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.UNAUTHENTICATED -> {
                // If user becomes unauthenticated while on a protected screen
                val currentKey = backStack.lastOrNull()
                if (currentKey in listOf(
                        Screen.PatientHome,
                        Screen.DoctorHome,
                        Screen.Home
                    )
                ) {
                    backStack.resetTo(Screen.Authentication)
                }
            }

            AuthState.AUTHENTICATED -> {
                // If user becomes authenticated while on auth screens
                val currentKey = backStack.lastOrNull()
                if (currentKey in listOf(
                        Screen.Authentication,
                        Screen.Login,
                        Screen.SignUp
                    )
                ) {
                    authViewModel.getCurrentUserType { userType ->
                        val destination: NavKey = when (userType) {
                            UserType.DOCTOR -> Screen.DoctorHome
                            UserType.PATIENT -> Screen.PatientHome
                            else -> Screen.PatientHome
                        }
                        backStack.resetTo(destination)
                    }
                }
            }

            else -> { /* Do nothing for LOADING and ERROR states */
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    authViewModel: FirebaseAuthViewModel,
    onNavigateBack: () -> Unit
) {
    // Placeholder for forgot password screen
    // You can implement this later with email input and reset functionality
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text(
            text = "Forgot Password Screen",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
        androidx.compose.material3.Button(
            onClick = onNavigateBack
        ) {
            androidx.compose.material3.Text("Back")
        }
    }
}
