package com.madaraka.connect.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Brand colors shared across all screens
private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun LoginScreen(
    // Called when the user taps the register link at the bottom
    onNavigateToRegister: () -> Unit = {},
    // Called when sign-in succeeds so the NavHost can move on
    onLoginSuccess: () -> Unit = {},
    // Shared auth ViewModel provided by the NavHost
    viewModel: AuthViewModel = viewModel()
) {
    // Holds the current text typed in the email field
    var email by remember { mutableStateOf("") }
    // Holds the current text typed in the password field
    var password by remember { mutableStateOf("") }
    // Tracks whether the password is shown as plain text or hidden
    var passwordVisible by remember { mutableStateOf(false) }

    // Observe ViewModel state
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Full-screen container that centers the login card both horizontally and vertically
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground),
        contentAlignment = Alignment.Center
    ) {
        // Column arranges the logo, heading, register link and card vertically
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Two-tone wordmark: "Madaraka" in charcoal, "Connect" in brand blue
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
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Main heading of the screen
            Text(
                text = "Sign in to your account",
                color = BrandCharcoal,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Link that switches to the registration screen
            TextButton(onClick = {
                viewModel.clearError()
                onNavigateToRegister()
            }) {
                Text(
                    text = "Or register for a new account",
                    color = BrandBlue,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // White card that holds the login form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Email input field with envelope icon
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = { Text("Email address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // Password input field with lock icon and show/hide toggle
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Forgot password link aligned to the right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { /* Phase 4C will wire this up */ }) {
                            Text("Forgot Password?", color = BrandBlue, fontSize = 13.sp)
                        }
                    }

                    // Show the error from the ViewModel, if any
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Primary sign-in button with inline spinner while loading
                    Button(
                        onClick = {
                            viewModel.signIn(email, password, onSuccess = onLoginSuccess)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sign In →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// Preview function that renders this screen in Android Studio without running the app
@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}