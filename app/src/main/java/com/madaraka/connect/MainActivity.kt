package com.madaraka.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.madaraka.connect.navigation.AppNavHost
import com.madaraka.connect.ui.theme.MadarakaConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge rendering so the app uses the full screen
        enableEdgeToEdge()
        setContent {
            // Wrap the entire app in the Madaraka Connect theme
            MadarakaConnectTheme {
                // Surface fills the screen and paints the background color
                Surface(modifier = Modifier.fillMaxSize()) {
                    // The NavHost decides which screen to show based on auth state
                    AppNavHost()
                }
            }
        }
    }
}