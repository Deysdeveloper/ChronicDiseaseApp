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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Info
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
fun SignUpScreen(
    authViewModel: FirebaseAuthViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onSignUpSuccess: (String) -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedUserType by remember { mutableStateOf(UserType.PATIENT) } // Default to Patient

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
                    text = "Create Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Welcome Text
                Text(
                    text = "Join Our Health Community",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Create your account to start your health journey",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                // User Type Selection Section
                Text(
                    text = "Sign up as:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Patient Option
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedUserType == UserType.PATIENT)
                                Color(0xFF6A5ACD).copy(alpha = 0.1f)
                            else
                                Color.Transparent
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (selectedUserType == UserType.PATIENT)
                                Color(0xFF6A5ACD)
                            else
                                Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { selectedUserType = UserType.PATIENT }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Patient",
                                tint = if (selectedUserType == UserType.PATIENT)
                                    Color(0xFF6A5ACD)
                                else
                                    Color(0xFF666666),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Patient",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedUserType == UserType.PATIENT)
                                    Color(0xFF6A5ACD)
                                else
                                    Color(0xFF666666),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Doctor Option
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedUserType == UserType.DOCTOR)
                                Color(0xFF6A5ACD).copy(alpha = 0.1f)
                            else
                                Color.Transparent
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (selectedUserType == UserType.DOCTOR)
                                Color(0xFF6A5ACD)
                            else
                                Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { selectedUserType = UserType.DOCTOR }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Doctor",
                                tint = if (selectedUserType == UserType.DOCTOR)
                                    Color(0xFF6A5ACD)
                                else
                                    Color(0xFF666666),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Doctor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedUserType == UserType.DOCTOR)
                                    Color(0xFF6A5ACD)
                                else
                                    Color(0xFF666666),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

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

                // Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
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
                    enabled = !isLoading
                )

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
                    enabled = !isLoading
                )

                // Phone Number Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        val digitsOnly = it.filter { ch -> ch.isDigit() }
                        if (digitsOnly.length <= 10) {
                            phoneNumber = digitsOnly
                            errorMessage = null
                        }
                    },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    enabled = !isLoading
                )

                // Age Field
                OutlinedTextField(
                    value = age,
                    onValueChange = {
                        val digitsOnly = it.filter { ch -> ch.isDigit() }
                        age = digitsOnly
                        errorMessage = null
                    },
                    label = { Text("Age") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    enabled = !isLoading
                )

                // Additional Fields for Doctor
                var expertise by remember { mutableStateOf("") }
                var hospital by remember { mutableStateOf("") }
                if (selectedUserType == UserType.DOCTOR) {
                    OutlinedTextField(
                        value = expertise,
                        onValueChange = {
                            expertise = it
                            errorMessage = null
                        },
                        label = { Text("Medical Expertise Field") },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
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
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = hospital,
                        onValueChange = {
                            hospital = it
                            errorMessage = null
                        },
                        label = { Text("Current Hospital") },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
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
                        enabled = !isLoading
                    )
                }

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
                    enabled = !isLoading
                )

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Text(
                                text = if (confirmPasswordVisible) "Hide" else "Show",
                                color = Color(0xFF6A5ACD),
                                fontSize = 14.sp
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
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
                    enabled = !isLoading
                )

                // Terms and Conditions Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreeToTerms,
                        onCheckedChange = { agreeToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6A5ACD),
                            uncheckedColor = Color(0xFFCCCCCC),
                            checkmarkColor = Color.White
                        ),
                        enabled = !isLoading
                    )
                    Text(
                        text = "I agree to the Terms of Service and Privacy Policy",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Sign Up Button
                Button(
                    onClick = {
                        // Validate inputs
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }

                        if (phoneNumber.length != 10) {
                            errorMessage = "Phone number must be exactly 10 digits"
                            return@Button
                        }

                        if (age.isBlank()) {
                            errorMessage = "Please enter your age"
                            return@Button
                        }

                        if (selectedUserType == UserType.DOCTOR) {
                            if (expertise.isBlank()) {
                                errorMessage = "Please enter your medical expertise field"
                                return@Button
                            }
                            if (hospital.isBlank()) {
                                errorMessage = "Please enter your current hospital"
                                return@Button
                            }
                        }

                        authViewModel.signUp(
                            email = email,
                            password = password,
                            fullName = fullName,
                            phoneNumber = phoneNumber,
                            age = age.toIntOrNull(),
                            termsAccepted = agreeToTerms,
                            userType = selectedUserType,
                            medicalExpertise = if (selectedUserType == UserType.DOCTOR) expertise else null,
                            currentHospital = if (selectedUserType == UserType.DOCTOR) hospital else null
                        ) { success, error ->
                            if (success) {
                                onSignUpSuccess(selectedUserType.name)
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
                    enabled = agreeToTerms && fullName.isNotBlank() && email.isNotBlank() &&
                            password.isNotBlank() && confirmPassword.isNotBlank() && age.isNotBlank() &&
                            phoneNumber.length == 10 &&
                            (selectedUserType != UserType.DOCTOR || (expertise.isNotBlank() && hospital.isNotBlank())) &&
                            !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Already have account text
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Sign In",
                            color = Color(0xFF6A5ACD),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPagePreview() {
    SignUpScreen()
}