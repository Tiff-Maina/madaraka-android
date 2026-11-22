package com.madaraka.connect.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.madaraka.connect.data.models.User
import com.madaraka.connect.data.models.UserRole
import com.madaraka.connect.data.repositories.UserRepository
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

    // Full user profile from Firestore, including role and department
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    // Where we fetch the user's profile document after sign-in
    private val userRepository = UserRepository()

    // True while a Firebase call is running
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Human-readable error to show in red on the screen
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // On startup, if Firebase Auth already has a user, fetch their profile
    init {
        val existing = auth.currentUser
        if (existing != null) {
            viewModelScope.launch {
                _currentUserProfile.value = userRepository.getUserById(existing.uid)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Sign in an existing user and load their Firestore profile
    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please enter both email and password."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Step 1: sign in with Firebase Auth
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                _currentUser.value = result.user

                // Step 2: fetch the user's Firestore profile
                val uid = result.user?.uid ?: ""
                val profile = userRepository.getUserById(uid)

                // Step 3: refuse to proceed if the profile is missing
                if (profile == null) {
                    auth.signOut()
                    _currentUser.value = null
                    _isLoading.value = false
                    _errorMessage.value = "Your account profile could not be found. Contact the administrator."
                    return@launch
                }

                // Step 3b: refuse to proceed if the account is suspended
                if (!profile.isActive) {
                    auth.signOut()
                    _currentUser.value = null
                    _isLoading.value = false
                    _errorMessage.value = "Your account is suspended. Contact the administrator."
                    return@launch
                }

                // Step 4: store the profile and let the UI navigate
                _currentUserProfile.value = profile
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = friendlyError(e.message ?: "")
            }
        }
    }

    // Create a new account and load (or default) the profile
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
                // Step 1: create the Firebase Auth account
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                _currentUser.value = result.user

                // Step 2: fetch the profile; if none exists, build a default Resident profile
                val uid = result.user?.uid ?: ""
                val fetched = userRepository.getUserById(uid)
                val profile = fetched ?: User(
                    uid = uid,
                    email = email.trim(),
                    role = UserRole.RESIDENT,
                    status = "Active"
                )

                // Step 3: store the profile and let the UI navigate
                _currentUserProfile.value = profile
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = friendlyError(e.message ?: "")
            }
        }
    }

    // Sign out and clear the cached user + profile
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _currentUserProfile.value = null
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