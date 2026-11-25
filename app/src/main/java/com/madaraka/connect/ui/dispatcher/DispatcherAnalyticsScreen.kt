package com.madaraka.connect.ui.dispatcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madaraka.connect.data.models.Ticket

private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)
private val Amber = Color(0xFFF59E0B)
private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AiPurple = Color(0xFF7C3AED)
private val AiPurpleBg = Color(0xFFF5F3FF)

private data class DeduplicationImpact(
    val totalReports: Int,
    val effectiveIncidents: Int,
    val savedPercent: Int
)

private fun computeDeduplicationImpact(tickets: List<Ticket>): DeduplicationImpact {
    val totalReports = tickets.size
    if (totalReports == 0) {
        return DeduplicationImpact(totalReports = 0, effectiveIncidents = 0, savedPercent = 0)
    }

    val countsByIncidentId = tickets
        .mapNotNull { it.masterIncidentId?.takeIf { id -> id.isNotBlank() } }
        .groupingBy { it }
        .eachCount()

    val multiReportClusters = countsByIncidentId.count { it.value >= 2 }
    val clusteredTicketCount = countsByIncidentId
        .filter { it.value >= 2 }
        .values
        .sum()
    val standaloneReports = totalReports - clusteredTicketCount

    val effectiveIncidents = multiReportClusters + standaloneReports
    val savedReports = (totalReports - effectiveIncidents).coerceAtLeast(0)
    val deduplicationPercent = ((savedReports.toDouble() / totalReports) * 100).toInt()

    return DeduplicationImpact(
        totalReports = totalReports,
        effectiveIncidents = effectiveIncidents,
        savedPercent = deduplicationPercent
    )
}

@Composable
fun DispatcherAnalyticsScreen(
    department: String = "",
    agencyName: String = "",
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
    viewModel: DispatcherViewModel = viewModel()
) {
    LaunchedEffect(department, agencyName) {
        if (department.isNotBlank()) {
            viewModel.load(department, agencyName)
        }
    }

    val tickets by viewModel.tickets.collectAsState()

    val average = viewModel.averageRating
    val ratedCount = viewModel.ratedCount
    val breakdown = viewModel.ratingBreakdown()
    val totalRatedForPercent = if (ratedCount > 0) ratedCount else 1

    val impact = remember(tickets) { computeDeduplicationImpact(tickets) }

    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold) },
            text = { Text("You will be signed out of the dispatcher dashboard.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOut()
                }) {
                    Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = BrandCharcoal)
                }
            }
        )
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandCharcoal)
                    }
                    Text(
                        text = "Performance Analytics",
                        color = BrandCharcoal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = ErrorRed
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = false,
                    onClick = onBack,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AiPurpleBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = AiPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Deduplication Impact",
                            color = AiPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AiStatCell(
                            label = "Reports",
                            value = impact.totalReports.toString(),
                            color = BrandCharcoal,
                            modifier = Modifier.weight(1f)
                        )
                        AiStatCell(
                            label = "Incidents",
                            value = impact.effectiveIncidents.toString(),
                            color = AiPurple,
                            modifier = Modifier.weight(1f)
                        )
                        AiStatCell(
                            label = "Saved",
                            value = "${impact.savedPercent}%",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI groups similar resident reports into single incidents so your team dispatches one crew instead of many.",
                        color = AiPurple.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AmberBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = average,
                        color = Amber,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        for (star in 1..5) {
                            val avgNum = average.toDoubleOrNull() ?: 0.0
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= avgNum) Amber else AmberBorder,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AVERAGE RATING",
                        color = Color(0xFF92400E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Based on $ratedCount review${if (ratedCount == 1) "" else "s"}",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rating Breakdown",
                        color = BrandCharcoal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    for (star in 5 downTo 1) {
                        val count = breakdown[star] ?: 0
                        val percent = (count.toFloat() / totalRatedForPercent).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = star.toString(),
                                color = BrandCharcoal.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(12.dp)
                            )
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(5.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percent)
                                        .background(Amber, RoundedCornerShape(5.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = count.toString(),
                                color = BrandCharcoal.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ratings are collected anonymously when a resident confirms a ticket has been successfully resolved. High ratings ensure prompt and effective community management.",
                        color = Color(0xFF1E3A8A),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AiStatCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = BrandCharcoal.copy(alpha = 0.65f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}