package com.example.chronicdiseaseapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chronicdiseaseapp.viewModel.FirebaseAuthViewModel
import com.example.chronicdiseaseapp.datamodels.UserType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: FirebaseAuthViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: (String) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLoading by authViewModel.isLoading.observeAsState(false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF333333)
                    )
                }
                Text(
                    text = "Welcome Back",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Health Icon
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(60.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD).copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💊",
                            fontSize = 50.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Welcome Text
                Text(
                    text = "Sign In to Your Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Continue your health management journey",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
                )

                // Error Message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6A5ACD),
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedLabelColor = Color(0xFF6A5ACD),
                        unfocusedLabelColor = Color(0xFF999999),
                        focusedTextColor = Color(0xFF333333),
                        unfocusedTextColor = Color(0xFF333333),
                        focusedLeadingIconColor = Color(0xFF6A5ACD),
                        unfocusedLeadingIconColor = Color(0xFF999999),
                        cursorColor = Color(0xFF6A5ACD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = Color(0xFF6A5ACD),
                                fontSize = 14.sp
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6A5ACD),
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedLabelColor = Color(0xFF6A5ACD),
                        unfocusedLabelColor = Color(0xFF999999),
                        focusedTextColor = Color(0xFF333333),
                        unfocusedTextColor = Color(0xFF333333),
                        focusedLeadingIconColor = Color(0xFF6A5ACD),
                        unfocusedLeadingIconColor = Color(0xFF999999),
                        cursorColor = Color(0xFF6A5ACD)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                // Remember Me and Forgot Password Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remember Me Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF6A5ACD),
                                uncheckedColor = Color(0xFFCCCCCC),
                                checkmarkColor = Color.White
                            ),
                            enabled = !isLoading
                        )
                        Text(
                            text = "Remember me",
                            color = Color(0xFF666666),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Forgot Password
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFF6A5ACD),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Sign In Button
                Button(
                    onClick = {
                        authViewModel.signIn(email, password) { success, error ->
                            if (success) {
                                // Get the actual user type from Firestore
                                authViewModel.getCurrentUserType { userType ->
                                    onLoginSuccess(userType?.name ?: UserType.PATIENT.name)
                                }
                            } else {
                                errorMessage = error
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A5ACD),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Don't have account text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Don't have an account?",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    TextButton(
                        onClick = onNavigateToSignUp,
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Sign Up",
                            color = Color(0xFF6A5ACD),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Footer text
                Text(
                    text = "Secure access to your health data",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}