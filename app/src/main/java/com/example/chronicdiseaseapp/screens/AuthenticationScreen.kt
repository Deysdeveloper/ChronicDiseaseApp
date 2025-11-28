package com.example.chronicdiseaseapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthenticationScreen(
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Welcome Message
            Text(
                text = "Welcome to",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Chronic Disease App",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Your health companion for better living",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            // Image placeholder (using a medical/health icon)
            Card(
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 40.dp),
                shape = RoundedCornerShape(90.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6A5ACD).copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Using a medical cross icon as placeholder
                    Text(
                        text = "🏥",
                        fontSize = 80.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Login Button
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A5ACD),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signup Button
            OutlinedButton(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6A5ACD)
                ),
                border = BorderStroke(2.dp, Color(0xFF6A5ACD)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Footer text
            Text(
                text = "Take control of your health journey",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthenticationScreenPreview() {
    AuthenticationScreen()
}