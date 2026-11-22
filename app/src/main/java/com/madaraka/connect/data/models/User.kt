package com.madaraka.connect.data.models

import com.google.firebase.Timestamp

// What role a user plays in the system
enum class UserRole {
    RESIDENT,
    DISPATCHER,
    ADMIN,
    UNKNOWN;

    companion object {
        // Converts Firestore strings like "Dispatcher" into the enum
        fun fromString(value: String?): UserRole = when (value?.trim()?.lowercase()) {
            "resident" -> RESIDENT
            "dispatcher" -> DISPATCHER
            "system admin", "admin" -> ADMIN
            else -> UNKNOWN
        }
    }
}

// Matches one document in the Firestore "users" collection
data class User(
    // The Firestore document ID (same as the Firebase Auth UID)
    val uid: String = "",
    // Fields from the Firestore document
    val email: String = "",
    val role: UserRole = UserRole.UNKNOWN,
    val department: String? = null,
    val agencyName: String? = null,
    val status: String = "Active",
    val createdAt: Timestamp? = null
) {
    // True only if the account is active
    val isActive: Boolean get() = status.equals("Active", ignoreCase = true)

    // Convenience: true if this user is a Resident
    val isResident: Boolean get() = role == UserRole.RESIDENT

    // Convenience: true if this user is a Dispatcher
    val isDispatcher: Boolean get() = role == UserRole.DISPATCHER

    // Convenience: true if this user is a System Admin
    val isAdmin: Boolean get() = role == UserRole.ADMIN

    companion object {
        // Builds a User from a Firestore document's raw fields
        fun fromMap(uid: String, map: Map<String, Any?>): User {
            return User(
                uid = uid,
                email = (map["email"] as? String).orEmpty(),
                role = UserRole.fromString(map["role"] as? String),
                department = map["department"] as? String,
                agencyName = map["agencyName"] as? String,
                status = (map["status"] as? String) ?: "Active",
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}