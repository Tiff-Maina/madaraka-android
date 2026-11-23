package com.madaraka.connect.data.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Writes audit log entries to the Firestore "audit_logs" collection
class AuditLogsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Write one audit log entry. Returns true on success, false on failure.
    suspend fun writeAuditLog(
        ticketId: String,
        action: String,
        details: String,
        performedBy: String
    ): Boolean {
        if (ticketId.isBlank() || action.isBlank()) return false
        return try {
            val data = mapOf(
                "ticketId" to ticketId,
                "action" to action,
                "details" to details,
                "performedBy" to performedBy,
                "timestamp" to Timestamp.now()
            )
            firestore.collection("audit_logs").add(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Fetches all audit logs, newest first (admin audit screen)
    suspend fun getAllAuditLogs(): List<Map<String, Any?>> {
        return try {
            val snapshot = firestore.collection("audit_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.plus("id" to doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}