package com.madaraka.connect.data.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Writes notification documents to the Firestore "notifications" collection
class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Create one notification for a specific recipient. Returns true on success.
    suspend fun createNotification(
        recipientId: String,
        title: String,
        message: String
    ): Boolean {
        if (recipientId.isBlank() || title.isBlank() || message.isBlank()) return false
        return try {
            val data = mapOf(
                "recipientId" to recipientId,
                "title" to title,
                "message" to message,
                "read" to false,
                "createdAt" to Timestamp.now()
            )
            firestore.collection("notifications").add(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Fetches all notifications addressed to "admin", newest first
    suspend fun getAdminMessages(): List<Map<String, Any?>> {
        return try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("recipientId", "admin")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.plus("id" to doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Marks one notification as read
    suspend fun markAsRead(notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}