package com.madaraka.connect.data.models

import com.google.firebase.Timestamp

// Matches one document in the Firestore "master_incidents" collection
data class MasterIncident(
    // The Firestore document ID (e.g. "MT001" or auto-ID)
    val id: String = "",
    // Fields from the Firestore document
    val category: FaultCategory = FaultCategory.UNKNOWN,
    // Cluster centroid — the average location of the grouped tickets
    val location: GeoPoint? = null,
    // IDs of the individual tickets grouped into this incident
    val ticketIds: List<String> = emptyList(),
    // How many tickets are grouped (kept as a field for quick display)
    val reportCount: Int = 0,
    // Overall status of the master incident
    val status: TicketStatus = TicketStatus.PENDING,
    // When the cluster was first created
    val createdAt: Timestamp? = null,
    // When the cluster last received a new ticket
    val lastUpdatedAt: Timestamp? = null,
    // Free-text area name for display (e.g. "Madaraka Estate, Block C")
    val areaName: String? = null
) {
    // True while the incident still needs work
    val isOpen: Boolean
        get() = status in listOf(
            TicketStatus.PENDING,
            TicketStatus.DISPATCHED,
            TicketStatus.IN_PROGRESS,
            TicketStatus.ESCALATED
        )

    // True if this incident groups more than one report
    val isCluster: Boolean
        get() = reportCount > 1

    companion object {
        // Builds a MasterIncident from a Firestore document's raw fields
        fun fromMap(id: String, map: Map<String, Any?>): MasterIncident {
            return MasterIncident(
                id = id,
                category = FaultCategory.fromString(map["category"] as? String),
                location = GeoPoint.fromMap(map["location"]),
                ticketIds = (map["ticketIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                reportCount = (map["reportCount"] as? Number)?.toInt() ?: 0,
                status = TicketStatus.fromString(map["status"] as? String),
                createdAt = map["createdAt"] as? Timestamp,
                lastUpdatedAt = map["lastUpdatedAt"] as? Timestamp,
                areaName = map["areaName"] as? String
            )
        }
    }
}