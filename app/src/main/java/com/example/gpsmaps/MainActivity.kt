package com.example.gpsmaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 (targetSdk 35) dibuja detrás de las barras del sistema: lo hacemos explícito
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MapScreen()
            }
        }
    }
}
