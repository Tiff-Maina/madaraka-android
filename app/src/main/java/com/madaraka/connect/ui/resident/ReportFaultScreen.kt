package com.madaraka.connect.ui.resident

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madaraka.connect.data.ai.DeduplicationDecision
import com.madaraka.connect.data.ai.SpatialDeduplicator
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.GeoPoint
import com.madaraka.connect.data.repositories.TicketRepository
import kotlinx.coroutines.launch
import java.util.Locale

private val ReportBlue = Color(0xFF2563EB)
private val ReportBackground = Color(0xFFF8FAFC)
private val AiPurple = Color(0xFF7C3AED)
private val SuccessGreen = Color(0xFF15803D)

private data class DemoLocationPreset(
    val label: String,
    val lat: String,
    val lng: String,
    val hint: String
)

// Realistic coordinates around Madaraka Estate, Nairobi (~1.2 km² area)
private val MadarakaPresets = listOf(
    DemoLocationPreset(
        label = "Spot A (Near Strathmore)",
        lat = "-1.3043",
        lng = "36.8092",
        hint = "Base location for nearby grouping"
    ),
    DemoLocationPreset(
        label = "Spot A2 (~45m away)",
        lat = "-1.3046",
        lng = "36.8095",
        hint = "Within 150m -> groups into Spot A's Master Incident"
    ),
    DemoLocationPreset(
        label = "Spot B (~650m away)",
        lat = "-1.3095",
        lng = "36.8125",
        hint = "Outside 150m -> creates a separate Master Incident"
    )
)

@Composable
fun ReportFaultScreen(
    residentId: String,
    residentEmail: String,
    onBack: () -> Unit,
    onReportSubmitted: (String) -> Unit,
    ticketRepository: TicketRepository = TicketRepository()
) {
    var selectedCategory by remember { mutableStateOf(FaultCategory.UNKNOWN) }
    var description by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("-1.3043") }
    var longitudeText by remember { mutableStateOf("36.8092") }
    var activePresetHint by remember { mutableStateOf(MadarakaPresets.first().hint) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submittedTicketId by remember { mutableStateOf<String?>(null) }
    var lastDecision by remember { mutableStateOf<DeduplicationDecision?>(null) }

    val scope = rememberCoroutineScope()

    fun parseCoordinateInputs(): Pair<GeoPoint?, String?> {
        val latStr = latitudeText.trim()
        val lngStr = longitudeText.trim()
        if (latStr.isEmpty() || lngStr.isEmpty()) {
            return null to "Please enter both latitude and longitude (or tap a Madaraka preset)."
        }
        val lat = latStr.toDoubleOrNull()
        val lng = lngStr.toDoubleOrNull()
        if (lat == null || lng == null) {
            return null to "Latitude and longitude must be valid decimal numbers (e.g. -1.3043 and 36.8092)."
        }
        val point = GeoPoint(lat = lat, lng = lng)
        if (!SpatialDeduplicator.isValidCoordinate(point)) {
            return null to "Invalid GPS coordinates: latitude must be -90..90, longitude -180..180, and not (0.0, 0.0)."
        }
        return point to null
    }

    Scaffold(
        containerColor = ReportBackground,
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !isSubmitting) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                    Text(
                        text = "Report a Fault",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tell us what needs fixing",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Text(
                text = "Nearby reports in the same category within 150m and 1 hour are automatically grouped into a Master Incident for dispatchers.",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            // 1. Category selection
            Text(
                text = "1. Select fault category",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FaultCategory.entries
                    .filter { it != FaultCategory.UNKNOWN }
                    .forEach { category ->
                        val isSelected = selectedCategory == category
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedCategory = category
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                            ),
                            border = if (isSelected) BorderStroke(1.dp, ReportBlue) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = category
                                        errorMessage = null
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.displayName,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
            }

            // 2. GPS coordinates + quick demo presets
            Text(
                text = "2. Location inside Madaraka Estate (GPS)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MadarakaPresets.forEach { preset ->
                    FilterChip(
                        selected = latitudeText.trim() == preset.lat && longitudeText.trim() == preset.lng,
                        onClick = {
                            latitudeText = preset.lat
                            longitudeText = preset.lng
                            activePresetHint = preset.hint
                            errorMessage = null
                        },
                        label = { Text(preset.label, fontSize = 11.sp, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Text(
                text = "Demo hint: $activePresetHint",
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = {
                        latitudeText = it
                        errorMessage = null
                    },
                    label = { Text("Latitude") },
                    placeholder = { Text("-1.3043") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = {
                        longitudeText = it
                        errorMessage = null
                    },
                    label = { Text("Longitude") },
                    placeholder = { Text("36.8092") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 3. Description
            Text(
                text = "3. Describe the problem",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = {
                    Text("Explain what happened and where in Madaraka you noticed it...")
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val (parsedGeo, coordError) = parseCoordinateInputs()
                    when {
                        residentId.isBlank() ->
                            errorMessage = "Your session could not be identified. Please sign in again."

                        selectedCategory == FaultCategory.UNKNOWN ->
                            errorMessage = "Please select a fault category."

                        coordError != null ->
                            errorMessage = coordError

                        description.isBlank() ->
                            errorMessage = "Please describe the fault."

                        description.trim().length < 10 ->
                            errorMessage = "Please provide a little more detail (at least 10 characters)."

                        else -> {
                            isSubmitting = true
                            errorMessage = null
                            submittedTicketId = null
                            lastDecision = null

                            scope.launch {
                                val result = ticketRepository.createTicketWithDeduplication(
                                    residentId = residentId,
                                    residentEmail = residentEmail,
                                    category = selectedCategory,
                                    description = description.trim(),
                                    location = parsedGeo
                                )

                                isSubmitting = false

                                if (result.ticketId != null) {
                                    submittedTicketId = result.ticketId
                                    lastDecision = result.decision
                                    description = ""
                                } else {
                                    errorMessage = result.decision.explanation.ifBlank {
                                        "We couldn't submit your report. Please check your connection and try again."
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ReportBlue)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Checking & Submitting...")
                } else {
                    Text(
                        text = "Submit Report",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Explainable AI Decision Card
            if (submittedTicketId != null && lastDecision != null) {
                val decision = lastDecision!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    border = BorderStroke(1.dp, AiPurple.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = AiPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (decision.isNewIncident)
                                    "New Master Incident Created"
                                else
                                    "Grouped into Existing Master Incident",
                                color = AiPurple,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ticket #${submittedTicketId!!.take(8)} · Incident ${decision.masterIncidentId.take(8)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                        }

                        Text(
                            text = decision.explanation,
                            fontSize = 13.sp,
                            color = Color(0xFF1F2937)
                        )

                        decision.distanceMeters?.let { dist ->
                            Text(
                                text = "Measured distance: ${String.format(Locale.US, "%.1f", dist)} m (Threshold: ${SpatialDeduplicator.DEFAULT_RADIUS_METERS.toInt()} m · Window: 1 hr)",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563)
                            )
                        }

                        OutlinedButton(
                            onClick = { onReportSubmitted(submittedTicketId!!) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}