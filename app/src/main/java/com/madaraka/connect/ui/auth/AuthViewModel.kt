package com.madaraka.connect.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Holds Firebase auth state for Login and Register screens
class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    // Signed-in user, or null when signed out
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // True while a Firebase call is running
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Human-readable error to show in red on the screen
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    // Sign in an existing user
    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please enter both email and password."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                _currentUser.value = result.user
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = friendlyError(e.message ?: "")
            }
        }
    }

    // Create a new account; Firebase signs the user in automatically
    fun register(email: String, password: String, confirmPassword: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields."
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Passwords do not match."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                _currentUser.value = result.user
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = friendlyError(e.message ?: "")
            }
        }
    }

    // Sign out and clear the cached user
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    // Map ugly Firebase messages to something a user can read
    private fun friendlyError(raw: String): String = when {
        raw.contains("password is invalid", true) ||
                raw.contains("INVALID_LOGIN_CREDENTIALS", true) -> "Incorrect email or password."
        raw.contains("no user record", true) -> "No account found with that email."
        raw.contains("email address is already in use", true) -> "That email is already registered. Try signing in."
        raw.contains("network", true) -> "Network error. Check your connection."
        raw.contains("badly formatted", true) -> "That doesn't look like a valid email."
        else -> raw.ifBlank { "Something went wrong. Please try again." }
    }
}