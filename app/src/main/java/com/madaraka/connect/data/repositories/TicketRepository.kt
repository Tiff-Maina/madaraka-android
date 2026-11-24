package com.madaraka.connect.data.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madaraka.connect.data.ai.DeduplicationDecision
import com.madaraka.connect.data.ai.SpatialDeduplicator
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.GeoPoint
import com.madaraka.connect.data.models.Ticket
import kotlinx.coroutines.tasks.await

data class TicketSubmissionResult(
    val ticketId: String?,
    val decision: DeduplicationDecision
)

class TicketRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val incidentsRepository: MasterIncidentsRepository = MasterIncidentsRepository(firestore)
) {

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

    // Standard entry point (kept for backward compatibility)
    suspend fun createTicket(
        residentId: String,
        residentEmail: String,
        category: FaultCategory,
        description: String,
        location: GeoPoint? = null,
        imageUrl: String? = null
    ): String? {
        return createTicketWithDeduplication(
            residentId = residentId,
            residentEmail = residentEmail,
            category = category,
            description = description,
            location = location,
            imageUrl = imageUrl
        ).ticketId
    }

    // Creates a ticket AND automatically groups/links a MasterIncident within radius & 1-hour window
    suspend fun createTicketWithDeduplication(
        residentId: String,
        residentEmail: String,
        category: FaultCategory,
        description: String,
        location: GeoPoint? = null,
        imageUrl: String? = null
    ): TicketSubmissionResult {
        if (residentId.isBlank() || description.isBlank() || category == FaultCategory.UNKNOWN) {
            Log.w(
                TAG,
                "createTicket rejected invalid input: residentIdBlank=${residentId.isBlank()}, " +
                        "descBlank=${description.isBlank()}, category=$category"
            )
            return TicketSubmissionResult(
                ticketId = null,
                decision = DeduplicationDecision(
                    masterIncidentId = "",
                    isNewIncident = false,
                    distanceMeters = null,
                    matchedTicketCount = 0,
                    explanation = "Submission blocked: missing required resident, category, or description fields."
                )
            )
        }

        if (!SpatialDeduplicator.isValidCoordinate(location)) {
            return TicketSubmissionResult(
                ticketId = null,
                decision = DeduplicationDecision(
                    masterIncidentId = "",
                    isNewIncident = false,
                    distanceMeters = null,
                    matchedTicketCount = 0,
                    explanation = "Submission blocked: please provide valid non-zero GPS coordinates within Madaraka Estate."
                )
            )
        }

        val validLoc = location!!
        val now = Timestamp.now()

        return try {
            val existingIncidents = incidentsRepository.getAllMasterIncidents()
            val recentTickets = getTicketsByCategory(category)

            val decision = SpatialDeduplicator.decideIncident(
                category = category,
                location = validLoc,
                timestamp = now,
                existingIncidents = existingIncidents,
                recentTickets = recentTickets
            )

            val ticketRef = firestore.collection("tickets").document()
            val resolvedIncidentId = incidentsRepository.attachTicketToIncident(
                existingIncidentId = decision.masterIncidentId.ifBlank { null },
                ticketId = ticketRef.id,
                category = category,
                location = validLoc,
                timestamp = now
            ) ?: decision.masterIncidentId

            val ticketData = mutableMapOf<String, Any>(
                "residentId" to residentId,
                "residentEmail" to residentEmail,
                "category" to category.displayName,
                "description" to description.trim(),
                "status" to "Pending",
                "createdAt" to now,
                "location" to mapOf(
                    "lat" to validLoc.lat,
                    "lng" to validLoc.lng
                )
            )

            if (resolvedIncidentId.isNotBlank()) {
                ticketData["masterIncidentId"] = resolvedIncidentId
            }

            if (!imageUrl.isNullOrBlank()) {
                ticketData["imageUrl"] = imageUrl
            }

            ticketRef.set(ticketData).await()
            Log.d(TAG, "createTicket succeeded id=${ticketRef.id}, incident=$resolvedIncidentId")

            TicketSubmissionResult(
                ticketId = ticketRef.id,
                decision = decision.copy(masterIncidentId = resolvedIncidentId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "createTicket failed", e)
            TicketSubmissionResult(
                ticketId = null,
                decision = DeduplicationDecision(
                    masterIncidentId = "",
                    isNewIncident = false,
                    distanceMeters = null,
                    matchedTicketCount = 0,
                    explanation = "Could not save report due to a network or database error."
                )
            )
        }
    }

    suspend fun escalateTicket(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf(
            "status" to "Escalated",
            "escalatedAt" to Timestamp.now()
        )
    )

    suspend fun confirmResolution(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf("status" to "Closed")
    )

    suspend fun rejectResolution(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf("status" to "Pending")
    )

    suspend fun cancelTicket(ticketId: String): Boolean = updateFields(
        ticketId,
        mapOf("status" to "Cancelled")
    )

    suspend fun rateTicket(ticketId: String, rating: Int): Boolean {
        if (rating !in 1..5) return false
        return updateFields(ticketId, mapOf("rating" to rating))
    }

    suspend fun assignTechnician(
        ticketId: String,
        technicianName: String,
        technicianContact: String
    ): Boolean {
        if (technicianName.isBlank() || technicianContact.isBlank()) return false
        return updateFields(
            ticketId,
            mapOf(
                "status" to "Dispatched",
                "technicianName" to technicianName.trim(),
                "technicianContact" to technicianContact.trim()
            )
        )
    }

    suspend fun updateTicketStatus(ticketId: String, newStatus: String): Boolean {
        if (ticketId.isBlank() || newStatus.isBlank()) return false
        return updateFields(ticketId, mapOf("status" to newStatus))
    }

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

    private companion object {
        const val TAG = "TicketRepository"
    }
}