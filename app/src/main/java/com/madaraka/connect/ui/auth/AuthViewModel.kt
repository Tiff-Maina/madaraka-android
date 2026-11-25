package com.madaraka.connect.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.madaraka.connect.data.local.EmailHistoryStore
import com.madaraka.connect.data.models.User
import com.madaraka.connect.data.models.UserRole
import com.madaraka.connect.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel managing authentication state and user profile loading
class AuthViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {

    // Secondary constructor so Android's ViewModel factory can instantiate us
    constructor(application: Application) : this(
        application = application,
        auth = FirebaseAuth.getInstance(),
        firestore = FirebaseFirestore.getInstance()
    )

    // Active Firebase Auth user or null when signed out
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Firestore profile for the signed-in user
    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    // Repository for reading user documents
    private val userRepository = UserRepository(firestore)

    // Local store for remembered emails
    private val emailHistoryStore = EmailHistoryStore(application.applicationContext)

    // Loading indicator flag for auth actions
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message displayed on auth screens
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Load profile automatically on startup if already signed in
    init {
        val existing = auth.currentUser
        if (existing != null) {
            viewModelScope.launch {
                _currentUserProfile.value = userRepository.getUserById(existing.uid)
            }
        }
    }

    // Clear current error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Returns emails previously used to sign in, most recent first
    fun getSavedEmails(): List<String> = emailHistoryStore.getEmails()

    // Remembers a successfully used email for next time
    fun saveEmail(email: String) = emailHistoryStore.addEmail(email)

    // Sign in with email and password and verify Firestore profile
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

                val uid = result.user?.uid.orEmpty()
                val profile = userRepository.getUserById(uid)

                if (profile == null) {
                    auth.signOut()
                    _currentUser.value = null
                    _isLoading.value = false
                    _errorMessage.value = "Your account profile could not be found. Contact the administrator."
                    return@launch
                }

                if (!profile.isActive) {
                    auth.signOut()
                    _currentUser.value = null
                    _isLoading.value = false
                    _errorMessage.value = "Your account is suspended. Contact the administrator."
                    return@launch
                }

                // Remember this email for the dropdown on the login screen
                saveEmail(email.trim())

                _currentUserProfile.value = profile
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = friendlyError(e.message.orEmpty())
            }
        }
    }

    // Register a new resident account and save its Firestore profile document
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

                val uid = result.user?.uid.orEmpty()
                val cleanEmail = email.trim()
                val existing = userRepository.getUserById(uid)

                val profile = if (existing != null) {
                    existing
                } else {
                    val now = Timestamp.now()
                    val userDoc = mapOf(
                        "email" to cleanEmail,
                        "role" to "Resident",
                        "status" to "Active",
                        "createdAt" to now
                    )
                    if (uid.isNotBlank()) {
                        firestore.collection("users").document(uid).set(userDoc).await()
                    }
                    User(
                        uid = uid,
                        email = cleanEmail,
                        role = UserRole.RESIDENT,
                        status = "Active",
                        createdAt = now
                    )
                }

                // Remember this email for the dropdown on the login screen
                saveEmail(cleanEmail)

                _currentUserProfile.value = profile
                _isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = friendlyError(e.message.orEmpty())
            }
        }
    }

    // Sign out and clear active session state
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _currentUserProfile.value = null
    }

    // Convert Firebase error messages into clear user-facing text
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