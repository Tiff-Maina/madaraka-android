package com.madaraka.connect.data.models

import com.google.firebase.Timestamp

// The exact category strings stored in Firestore
enum class FaultCategory(val displayName: String) {
    WATER("Water Services"),
    ELECTRICITY("Electricity Services"),
    WASTE("Waste Management"),
    UNKNOWN("Unknown");

    companion object {
        // Converts a Firestore string back into the enum
        fun fromString(value: String?): FaultCategory = when (value?.trim()) {
            "Water Services" -> WATER
            "Electricity Services" -> ELECTRICITY
            "Waste Management" -> WASTE
            else -> UNKNOWN
        }
    }
}

// The exact status strings stored in Firestore
enum class TicketStatus(val displayName: String) {
    PENDING("Pending"),
    DISPATCHED("Dispatched"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    ESCALATED("Escalated"),
    ADMIN_REVIEWED("Admin_Reviewed"),
    CANCELLED("Cancelled"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String?): TicketStatus = when (value?.trim()) {
            "Pending" -> PENDING
            "Dispatched" -> DISPATCHED
            "Assigned" -> DISPATCHED          // backend sometimes says "Assigned"
            "In Progress" -> IN_PROGRESS
            "Resolved" -> RESOLVED
            "Closed" -> CLOSED
            "Escalated" -> ESCALATED
            "Admin_Reviewed" -> ADMIN_REVIEWED
            "Cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

// Holds the nested { lat, lng } map stored in Firestore
data class GeoPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0
) {
    companion object {
        // Builds a GeoPoint from Firestore's nested map
        @Suppress("UNCHECKED_CAST")
        fun fromMap(value: Any?): GeoPoint? {
            val map = value as? Map<String, Any?> ?: return null
            val lat = (map["lat"] as? Number)?.toDouble() ?: return null
            val lng = (map["lng"] as? Number)?.toDouble() ?: return null
            return GeoPoint(lat, lng)
        }
    }
}

// Matches one document in the Firestore "tickets" collection
data class Ticket(
    // The Firestore document ID (auto-generated)
    val id: String = "",
    // Fields from the Firestore document
    val category: FaultCategory = FaultCategory.UNKNOWN,
    val description: String = "",
    val imageUrl: String? = null,
    val location: GeoPoint? = null,
    val residentEmail: String = "",
    val residentId: String = "",
    val status: TicketStatus = TicketStatus.PENDING,
    val technicianContact: String? = null,
    val technicianName: String? = null,
    val createdAt: Timestamp? = null,
    // Populated by escalation logic
    val escalatedAt: Timestamp? = null,
    val adminReviewedAt: Timestamp? = null,
    // Populated after a resident rates a resolved ticket
    val rating: Int? = null,
    // Populated later by the AI backend once clusters are created
    val masterIncidentId: String? = null
) {
    // True while the ticket still needs work
    val isOpen: Boolean
        get() = status in listOf(
            TicketStatus.PENDING,
            TicketStatus.DISPATCHED,
            TicketStatus.IN_PROGRESS,
            TicketStatus.ESCALATED
        )

    // True if a technician has been assigned
    val hasTechnician: Boolean
        get() = !technicianName.isNullOrBlank() && !technicianContact.isNullOrBlank()

    // True if the resident can escalate (pending/dispatched for over 24h)
    val canEscalate: Boolean
        get() = status in listOf(TicketStatus.PENDING, TicketStatus.DISPATCHED) && isOlderThan24Hours

    // True if the resident can cancel (only while still pending)
    val canCancel: Boolean
        get() = status == TicketStatus.PENDING

    // True if the resident can rate (closed and not yet rated)
    val canRate: Boolean
        get() = status == TicketStatus.CLOSED && rating == null

    // Helper: true if createdAt is more than 24 hours ago
    private val isOlderThan24Hours: Boolean
        get() {
            val created = createdAt ?: return false
            val ageMillis = System.currentTimeMillis() - created.toDate().time
            return ageMillis >= 24L * 60 * 60 * 1000
        }

    companion object {
        // Builds a Ticket from a Firestore document's raw fields
        fun fromMap(id: String, map: Map<String, Any?>): Ticket {
            return Ticket(
                id = id,
                category = FaultCategory.fromString(map["category"] as? String),
                description = (map["description"] as? String).orEmpty(),
                imageUrl = map["imageUrl"] as? String,
                location = GeoPoint.fromMap(map["location"]),
                residentEmail = (map["residentEmail"] as? String).orEmpty(),
                residentId = (map["residentId"] as? String).orEmpty(),
                status = TicketStatus.fromString(map["status"] as? String),
                technicianContact = map["technicianContact"] as? String,
                technicianName = map["technicianName"] as? String,
                createdAt = map["createdAt"] as? Timestamp,
                escalatedAt = map["escalatedAt"] as? Timestamp,
                adminReviewedAt = map["adminReviewedAt"] as? Timestamp,
                rating = (map["rating"] as? Number)?.toInt(),
                masterIncidentId = map["masterIncidentId"] as? String
            )
        }
    }
}