package com.example.chronicdiseaseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chronicdiseaseapp.navigation.Navigation
import com.example.chronicdiseaseapp.ui.theme.ChronicDiseaseAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChronicDiseaseAppTheme {
                Navigation()
            }
        }
    }
}

