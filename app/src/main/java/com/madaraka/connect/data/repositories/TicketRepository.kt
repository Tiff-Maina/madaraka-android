package com.madaraka.connect.data.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.Ticket
import kotlinx.coroutines.tasks.await

// Reads and updates ticket documents in the Firestore "tickets" collection
class TicketRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // All tickets submitted by one resident, newest first
    suspend fun getTicketsByResident(residentId: String): List<Ticket> {
        if (residentId.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("tickets")
                .whereEqualTo("residentId", residentId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Ticket.fromMap(id = doc.id, map = it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // All tickets in one department, newest first (dispatchers use this)
    suspend fun getTicketsByCategory(category: FaultCategory): List<Ticket> {
        if (category == FaultCategory.UNKNOWN) return emptyList()
        return try {
            val snapshot = firestore.collection("tickets")
                .whereEqualTo("category", category.displayName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Ticket.fromMap(id = doc.id, map = it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Every ticket in the system, newest first (admins use this)
    suspend fun getAllTickets(): List<Ticket> {
        return try {
            val snapshot = firestore.collection("tickets")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Ticket.fromMap(id = doc.id, map = it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // One ticket by its Firestore document ID (detail screens use this)
    suspend fun getTicketById(ticketId: String): Ticket? {
        if (ticketId.isBlank()) return null
        return try {
            val snapshot = firestore.collection("tickets").document(ticketId).get().await()
            if (!snapshot.exists()) return null
            val map = snapshot.data ?: return null
            Ticket.fromMap(id = snapshot.id, map = map)
        } catch (e: Exception) {
            null
        }
    }

    // ----- WRITE ACTIONS -----

    // Escalate a ticket: status = "Escalated", record when
    suspend fun escalateTicket(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf(
            "status" to "Escalated",
            "escalatedAt" to Timestamp.now()
        )
    )

    // Resident confirms the fix worked: status = "Closed"
    suspend fun confirmResolution(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf("status" to "Closed")
    )

    // Resident says the fix didn't work: reopen as "Pending"
    suspend fun rejectResolution(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf("status" to "Pending")
    )

    // Resident cancels a pending ticket: status = "Cancelled"
    suspend fun cancelTicket(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf("status" to "Cancelled")
    )

    // Resident rates a closed ticket
    suspend fun rateTicket(ticketId: String, rating: Int): Boolean {
        if (rating !in 1..5) return false
        return updateFields(ticketId, mapOf("rating" to rating))
    }

    // Shared helper: apply a partial update to one ticket, return success
    private suspend fun updateFields(ticketId: String, fields: Map<String, Any?>): Boolean {
        if (ticketId.isBlank()) return false
        return try {
            firestore.collection("tickets")
                .document(ticketId)
                .update(fields)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}