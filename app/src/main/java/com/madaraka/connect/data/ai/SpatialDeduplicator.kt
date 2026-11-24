package com.madaraka.connect.data.ai

import com.google.firebase.Timestamp
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.GeoPoint
import com.madaraka.connect.data.models.MasterIncident
import com.madaraka.connect.data.models.Ticket
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class DeduplicationDecision(
    val masterIncidentId: String,
    val isNewIncident: Boolean,
    val distanceMeters: Double?,
    val matchedTicketCount: Int,
    val explanation: String
)

object SpatialDeduplicator {
    // Realistic local radius inside Madaraka Estate (~1.2 km² total area)
    const val DEFAULT_RADIUS_METERS: Double = 150.0

    // Matches lecturer's suggestion for hourly time-window grouping (1 hour = 3,600,000 ms)
    const val DEFAULT_WINDOW_MILLIS: Long = 60L * 60L * 1000L

    fun isValidCoordinate(location: GeoPoint?): Boolean {
        if (location == null) return false
        if (location.lat.isNaN() || location.lng.isNaN()) return false
        if (location.lat == 0.0 && location.lng == 0.0) return false
        return location.lat in -90.0..90.0 && location.lng in -180.0..180.0
    }

    // Distance between two GPS points in metres (Haversine formula)
    fun distanceInMeters(a: GeoPoint, b: GeoPoint): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)

        val haversine = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        val centralAngle = 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
        return earthRadiusMeters * centralAngle
    }

    /**
     * Finds the nearest recent incident of the same category within radiusMeters and windowMillis.
     * Prefers existing MasterIncidents first, then recent open/matching tickets.
     */
    fun decideIncident(
        category: FaultCategory,
        location: GeoPoint?,
        timestamp: Timestamp = Timestamp.now(),
        existingIncidents: List<MasterIncident>,
        recentTickets: List<Ticket>,
        radiusMeters: Double = DEFAULT_RADIUS_METERS,
        windowMillis: Long = DEFAULT_WINDOW_MILLIS
    ): DeduplicationDecision {
        if (!isValidCoordinate(location)) {
            return DeduplicationDecision(
                masterIncidentId = "",
                isNewIncident = false,
                distanceMeters = null,
                matchedTicketCount = 0,
                explanation = "Invalid or missing GPS coordinates; report cannot be spatially clustered."
            )
        }

        val targetLoc = location!!
        val targetMillis = timestamp.toDate().time

        // 1. Check active MasterIncidents in the same category and time window
        val matchingIncident = existingIncidents
            .filter { inc ->
                inc.isOpen &&
                        inc.category == category &&
                        inc.location != null &&
                        isValidCoordinate(inc.location) &&
                        inc.id.isNotBlank()
            }
            .mapNotNull { inc ->
                val refTime = (inc.lastUpdatedAt ?: inc.createdAt)?.toDate()?.time ?: targetMillis
                val withinWindow = kotlin.math.abs(targetMillis - refTime) <= windowMillis
                val dist = distanceInMeters(targetLoc, inc.location!!)
                if (withinWindow && dist <= radiusMeters) inc to dist else null
            }
            .minByOrNull { it.second }

        if (matchingIncident != null) {
            val (inc, dist) = matchingIncident
            val roundedDist = dist.toInt()
            val newCount = maxOf(inc.reportCount, inc.ticketIds.size) + 1
            return DeduplicationDecision(
                masterIncidentId = inc.id,
                isNewIncident = false,
                distanceMeters = dist,
                matchedTicketCount = newCount,
                explanation = "Grouped into Master Incident ${inc.id}: ${category.displayName} report is ~${roundedDist}m away (within ${radiusMeters.toInt()}m) and within the 1-hour window."
            )
        }

        // 2. Fallback: check recent ungrouped/matching tickets if no MasterIncident matched
        val matchingTicket = recentTickets
            .filter { t ->
                t.category == category &&
                        t.location != null &&
                        isValidCoordinate(t.location)
            }
            .mapNotNull { t ->
                val tTime = t.createdAt?.toDate()?.time ?: targetMillis
                val withinWindow = kotlin.math.abs(targetMillis - tTime) <= windowMillis
                val dist = distanceInMeters(targetLoc, t.location!!)
                if (withinWindow && dist <= radiusMeters) t to dist else null
            }
            .minByOrNull { it.second }

        if (matchingTicket != null) {
            val (t, dist) = matchingTicket
            val roundedDist = dist.toInt()
            val sharedId = t.masterIncidentId?.takeIf { it.isNotBlank() }
                ?: "MI-${t.id.take(6).uppercase()}"
            return DeduplicationDecision(
                masterIncidentId = sharedId,
                isNewIncident = false,
                distanceMeters = dist,
                matchedTicketCount = 2,
                explanation = "Grouped under $sharedId: ${category.displayName} report is ~${roundedDist}m from a nearby report within the 1-hour window."
            )
        }

        // 3. Otherwise, start a new Master Incident
        val nearestSameCategoryDist = existingIncidents
            .filter { it.category == category && it.location != null && isValidCoordinate(it.location) }
            .minOfOrNull { distanceInMeters(targetLoc, it.location!!) }

        val reasonSuffix = if (nearestSameCategoryDist != null) {
            "Nearest ${category.displayName} incident is ~${nearestSameCategoryDist.toInt()}m away (outside ${radiusMeters.toInt()}m or 1-hour window)."
        } else {
            "No other ${category.displayName} reports found within ${radiusMeters.toInt()}m in the past 1 hour."
        }

        return DeduplicationDecision(
            masterIncidentId = "", // Filled with new MasterIncident ID once created
            isNewIncident = true,
            distanceMeters = nearestSameCategoryDist,
            matchedTicketCount = 1,
            explanation = "Created a new Master Incident: $reasonSuffix"
        )
    }
}