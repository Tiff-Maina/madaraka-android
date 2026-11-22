package com.madaraka.connect.data.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.GeoPoint
import com.madaraka.connect.data.models.Ticket
import kotlinx.coroutines.tasks.await

// Reads and updates ticket documents in the Firestore "tickets" collection
class TicketRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // All tickets submitted by one resident, newest first
    suspend fun getTicketsByResident(residentId: String): List<Ticket> {
        if (residentId.isBlank()) {
            Log.w(TAG, "getTicketsByResident called with blank residentId")
            return emptyList()
        }
        return try {
            val snapshot = firestore.collection("tickets")
                .whereEqualTo("residentId", residentId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Log.d(TAG, "getTicketsByResident returned ${snapshot.size()} docs for $residentId")
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Ticket.fromMap(id = doc.id, map = it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTicketsByResident failed for $residentId", e)
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
            Log.e(TAG, "getTicketsByCategory failed for ${category.displayName}", e)
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
            Log.e(TAG, "getAllTickets failed", e)
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
            Log.e(TAG, "getTicketById failed for $ticketId", e)
            null
        }
    }

    // ----- WRITE ACTIONS -----

    // Create a new fault report in Firestore
    suspend fun createTicket(
        residentId: String,
        residentEmail: String,
        category: FaultCategory,
        description: String,
        location: GeoPoint? = null,
        imageUrl: String? = null
    ): String? {
        if (
            residentId.isBlank() ||
            description.isBlank() ||
            category == FaultCategory.UNKNOWN
        ) {
            Log.w(
                TAG,
                "createTicket rejected invalid input: residentIdBlank=${residentId.isBlank()}, " +
                        "descBlank=${description.isBlank()}, category=$category"
            )
            return null
        }

        return try {
            val ticketRef = firestore.collection("tickets").document()

            val ticketData = mutableMapOf<String, Any>(
                "residentId" to residentId,
                "residentEmail" to residentEmail,
                "category" to category.displayName,
                "description" to description.trim(),
                "status" to "Pending",
                "createdAt" to Timestamp.now()
            )

            // Add location only when one is provided
            location?.let {
                ticketData["location"] = mapOf(
                    "lat" to it.lat,
                    "lng" to it.lng
                )
            }

            // Add image URL only when one is provided
            if (!imageUrl.isNullOrBlank()) {
                ticketData["imageUrl"] = imageUrl
            }

            ticketRef.set(ticketData).await()
            Log.d(TAG, "createTicket succeeded with id=${ticketRef.id}")

            ticketRef.id
        } catch (e: Exception) {
            Log.e(TAG, "createTicket failed", e)
            null
        }
    }

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
            Log.e(TAG, "updateFields failed for $ticketId", e)
            false
        }
    }

    private companion object {
        const val TAG = "TicketRepository"
    }
}