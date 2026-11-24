package com.madaraka.connect.data.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.GeoPoint
import com.madaraka.connect.data.models.MasterIncident
import com.madaraka.connect.data.models.TicketStatus
import kotlinx.coroutines.tasks.await

class MasterIncidentsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getAllMasterIncidents(): List<MasterIncident> {
        return try {
            val snapshot = firestore.collection("master_incidents")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { MasterIncident.fromMap(id = doc.id, map = it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllMasterIncidents failed", e)
            emptyList()
        }
    }

    suspend fun getMasterIncidentById(incidentId: String): MasterIncident? {
        if (incidentId.isBlank()) return null
        return try {
            val snapshot = firestore.collection("master_incidents")
                .document(incidentId)
                .get()
                .await()
            if (!snapshot.exists()) return null
            val map = snapshot.data ?: return null
            MasterIncident.fromMap(id = snapshot.id, map = map)
        } catch (e: Exception) {
            Log.e(TAG, "getMasterIncidentById failed for $incidentId", e)
            null
        }
    }

    // Creates a new MasterIncident or appends a ticketId to an existing one
    suspend fun attachTicketToIncident(
        existingIncidentId: String?,
        ticketId: String,
        category: FaultCategory,
        location: GeoPoint,
        timestamp: Timestamp,
        areaName: String = "Madaraka Estate, Nairobi"
    ): String? {
        return try {
            if (!existingIncidentId.isNullOrBlank()) {
                val docRef = firestore.collection("master_incidents").document(existingIncidentId)
                val snap = docRef.get().await()
                if (snap.exists()) {
                    docRef.update(
                        mapOf(
                            "ticketIds" to FieldValue.arrayUnion(ticketId),
                            "reportCount" to FieldValue.increment(1),
                            "lastUpdatedAt" to timestamp
                        )
                    ).await()
                    return existingIncidentId
                }
            }

            // Create a new MasterIncident document
            val newDoc = firestore.collection("master_incidents").document()
            val payload = mapOf(
                "category" to category.displayName,
                "location" to mapOf(
                    "lat" to location.lat,
                    "lng" to location.lng
                ),
                "ticketIds" to listOf(ticketId),
                "reportCount" to 1,
                "status" to TicketStatus.PENDING.displayName,
                "createdAt" to timestamp,
                "lastUpdatedAt" to timestamp,
                "areaName" to areaName
            )
            newDoc.set(payload).await()
            newDoc.id
        } catch (e: Exception) {
            Log.e(TAG, "attachTicketToIncident failed", e)
            null
        }
    }

    private companion object {
        const val TAG = "MasterIncidentsRepo"
    }
}