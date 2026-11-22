package com.madaraka.connect.data.repositories

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madaraka.connect.data.models.MasterIncident
import kotlinx.coroutines.tasks.await

private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)

// Repository reading clustered master incidents from Firestore
class MasterIncidentsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Fetch all master incidents ordered newest first
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

    // Fetch a single master incident by document ID
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

    private companion object {
        const val TAG = "MasterIncidentsRepo"
    }
}