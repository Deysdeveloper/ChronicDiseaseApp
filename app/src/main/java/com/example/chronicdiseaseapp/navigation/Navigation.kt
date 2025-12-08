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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chronicdiseaseapp.screens.AuthenticationScreen
import com.example.chronicdiseaseapp.screens.patientScreen.HomeScreen
import com.example.chronicdiseaseapp.screens.doctorScreen.DocHomeScreen
import com.example.chronicdiseaseapp.screens.doctorScreen.PatientsVitalsScreen
import com.example.chronicdiseaseapp.screens.LoginScreen
import com.example.chronicdiseaseapp.screens.SignUpScreen
import com.example.chronicdiseaseapp.viewModel.AuthState
import com.example.chronicdiseaseapp.viewModel.FirebaseAuthViewModel
import com.example.chronicdiseaseapp.datamodels.UserType

/**
 * Sealed class defining all navigation routes in the app
 */
sealed class Screen(val route: String) {
    object Authentication : Screen("authentication")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object PatientHome : Screen("patient_home")
    object DoctorHome : Screen("doctor_home")
    object PatientsVitals : Screen("patients_vitals")
    object ForgotPassword : Screen("forgot_password")

    // Legacy home route for backward compatibility
    object Home : Screen("home")
}

@Composable
fun Navigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: FirebaseAuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.observeAsState()
    val currentUser by authViewModel.currentUser.observeAsState()

    // Determine the start destination based on authentication state
    val startDestination = if (authViewModel.isUserSignedIn()) {
        // We'll let the LaunchedEffect handle navigation based on user type
        Screen.PatientHome.route // Default fallback
    } else {
        Screen.Authentication.route
    }

    // Add state to track if initial navigation is done
    var initialNavigationDone by remember { mutableStateOf(false) }

    // Check user type on startup for authenticated users
    LaunchedEffect(authState) {
        if (authState == AuthState.AUTHENTICATED && !initialNavigationDone) {
            authViewModel.getCurrentUserType { userType ->
                val destination = when (userType) {
                    UserType.DOCTOR -> Screen.DoctorHome.route
                    UserType.PATIENT -> Screen.PatientHome.route
                    else -> Screen.PatientHome.route
                }

                val currentRoute = navController.currentDestination?.route
                if (currentRoute != destination && currentRoute in listOf(
                        Screen.Authentication.route,
                        Screen.Login.route,
                        Screen.SignUp.route,
                        Screen.PatientHome.route, // In case we defaulted to patient
                        Screen.Home.route
                    )
                ) {
                    navController.navigate(destination) {
                        popUpTo(0) { // Clear entire back stack
                            inclusive = true
                        }
                    }
                }
                initialNavigationDone = true
            }
        } else if (authState == AuthState.UNAUTHENTICATED) {
            initialNavigationDone = false // Reset for next login
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication Landing Screen
        composable(Screen.Authentication.route) {
            AuthenticationScreen(
                onLoginClick = {
                    navController.navigate(Screen.Login.route)
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Authentication.route)
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = { userType ->
                    val destination = if (userType == "DOCTOR") {
                        Screen.DoctorHome.route
                    } else {
                        Screen.PatientHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Authentication.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Sign Up Screen
        composable(Screen.SignUp.route) {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Authentication.route)
                    }
                },
                onSignUpSuccess = { userType ->
                    val destination = if (userType == "DOCTOR") {
                        Screen.DoctorHome.route
                    } else {
                        Screen.PatientHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Authentication.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Patient Home Screen
        composable(Screen.PatientHome.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(Screen.PatientHome.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Doctor Home Screen  
        composable(Screen.DoctorHome.route) {
            DocHomeScreen(
                onNavigateToPatientDetails = { patientId ->
                    // TODO: Navigate to patient details screen
                },
                onNavigateToAppointments = {
                    // TODO: Navigate to appointments screen
                },
                onNavigateToPatientList = {
                    navController.navigate(Screen.PatientsVitals.route)
                },
                onNavigateToProfile = {
                    // TODO: Navigate to doctor profile screen
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(Screen.DoctorHome.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Patients Vitals Screen
        composable(Screen.PatientsVitals.route) {
            PatientsVitalsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPatientDetail = { patientId ->
                    // TODO: Navigate to patient detail screen
                }
            )
        }

        // Legacy Home Screen (redirect to patient home for backward compatibility)
        composable(Screen.Home.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Forgot Password Screen (placeholder for future implementation)
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.UNAUTHENTICATED -> {
                // If user becomes unauthenticated while on a protected screen
                val currentRoute = navController.currentDestination?.route
                if (currentRoute in listOf(
                        Screen.PatientHome.route,
                        Screen.DoctorHome.route,
                        Screen.Home.route
                    )
                ) {
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(currentRoute ?: Screen.PatientHome.route) {
                            inclusive = true
                        }
                    }
                }
            }

            AuthState.AUTHENTICATED -> {
                // If user becomes authenticated while on auth screens
                val currentRoute = navController.currentDestination?.route
                if (currentRoute in listOf(
                        Screen.Authentication.route,
                        Screen.Login.route,
                        Screen.SignUp.route
                    )
                ) {
                    authViewModel.getCurrentUserType { userType ->
                        val route = when (userType) {
                            UserType.DOCTOR -> Screen.DoctorHome.route
                            UserType.PATIENT -> Screen.PatientHome.route
                            else -> Screen.PatientHome.route
                        }
                        navController.navigate(route) {
                            popUpTo(Screen.Authentication.route) {
                                inclusive = true
                            }
                        }
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