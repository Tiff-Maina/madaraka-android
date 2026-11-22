package com.madaraka.connect.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.madaraka.connect.data.models.User
import kotlinx.coroutines.tasks.await

// Fetches user documents from the Firestore "users" collection
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Fetches a single user by their Firebase Auth UID
    // Returns null if the user document does not exist
    suspend fun getUserById(uid: String): User? {
        if (uid.isBlank()) return null
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (!snapshot.exists()) return null
            val map = snapshot.data ?: return null
            User.fromMap(uid = snapshot.id, map = map)
        } catch (e: Exception) {
            // Network or permission error — caller decides what to show
            null
        }
    }

    // Fetches all users with a given role (useful for admin screens later)
    suspend fun getUsersByRole(role: String): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", role)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { User.fromMap(uid = doc.id, map = it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}