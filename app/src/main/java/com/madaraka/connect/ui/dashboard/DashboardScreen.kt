package com.madaraka.connect.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madaraka.connect.ui.auth.AuthViewModel

// Brand colors shared across all screens
private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)

@Composable
fun DashboardScreen(
    onSignOut: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    // Observe the currently signed-in user
    val user by viewModel.currentUser.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Two-tone wordmark matching the auth screens
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = BrandCharcoal, fontWeight = FontWeight.Bold)) {
                        append("Madaraka ")
                    }
                    withStyle(SpanStyle(color = BrandBlue, fontWeight = FontWeight.Bold)) {
                        append("Connect")
                    }
                },
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // White card holding the welcome message and sign-out button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome 👋",
                        color = BrandCharcoal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shows the signed-in email, or a fallback if null
                    Text(
                        text = user?.email ?: "(not signed in)",
                        color = BrandBlue,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Temporary note until the real dashboard arrives
                    Text(
                        text = "Dashboard coming in Phase 4B",
                        color = BrandCharcoal.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Sign-out button clears the session and returns to Login
                    Button(
                        onClick = {
                            viewModel.signOut()
                            onSignOut()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Preview rendered in Android Studio without running the app
@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun DashboardScreenPreview() {
    DashboardScreen()
}