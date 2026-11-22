package com.madaraka.connect.ui.resident

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madaraka.connect.data.models.Ticket
import com.madaraka.connect.data.models.TicketStatus
import com.madaraka.connect.data.repositories.TicketRepository
import com.madaraka.connect.ui.common.CategoryIcon
import com.madaraka.connect.ui.common.StatusBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2563EB)
private val BrandBlueDark = Color(0xFF1E3A8A)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)

// ViewModel holding resident tickets for the dashboard
class ResidentViewModel(
    private val ticketRepository: TicketRepository = TicketRepository()
) : ViewModel() {

    // Resident tickets sorted newest first
    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    // Loading state for initial fetch
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message if loading fails
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Fetch tickets for the signed-in resident
    fun loadTickets(residentId: String) {
        if (residentId.isBlank()) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val fetched = ticketRepository.getTicketsByResident(residentId)
            _tickets.value = fetched
            _isLoading.value = false
        }
    }
}

@Composable
fun ResidentDashboardScreen(
    residentId: String,
    onReportNewFault: () -> Unit = {},
    onViewAllReports: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToClusters: () -> Unit = {},
    onSignOut: () -> Unit = {},
    viewModel: ResidentViewModel = viewModel()
) {
    val tickets by viewModel.tickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Reload tickets on initial load and when returning to screen
    LaunchedEffect(residentId) {
        if (residentId.isNotBlank()) {
            viewModel.loadTickets(residentId)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh tickets whenever the dashboard resumes
    DisposableEffect(lifecycleOwner, residentId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && residentId.isNotBlank()) {
                viewModel.loadTickets(residentId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val totalReports = tickets.size
    val dispatchedReports = tickets.count {
        it.status == TicketStatus.DISPATCHED || it.status == TicketStatus.IN_PROGRESS
    }
    val resolvedReports = tickets.count {
        it.status == TicketStatus.RESOLVED || it.status == TicketStatus.CLOSED
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Resident Dashboard",
                        color = BrandCharcoal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = BrandCharcoal
                            )
                        }
                        IconButton(onClick = onSignOut) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = ErrorRed
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewAllReports,
                    icon = { Icon(Icons.Default.Description, contentDescription = "My Reports") },
                    label = { Text("My Reports", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToClusters,
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Clusters") },
                    label = { Text("Clusters", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSupport,
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Help") },
                    label = { Text("Help", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        if (isLoading && tickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Scaffold
        }

        if (errorMessage != null && tickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorRed,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadTickets(residentId) }) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Report fault banner card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BrandBlue, BrandBlueDark)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Experiencing an Issue?",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Report water leaks, power outages, or waste issues directly to the Madaraka dispatch team.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onReportNewFault,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = BrandBlue
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Report New Fault", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Summary stat cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Total",
                        value = totalReports.toString(),
                        valueColor = BrandCharcoal,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Dispatched",
                        value = dispatchedReports.toString(),
                        valueColor = BrandBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Resolved",
                        value = resolvedReports.toString(),
                        valueColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent activity section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Activity",
                        color = BrandCharcoal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewAllReports) {
                        Text("View all", color = BrandBlue, fontSize = 13.sp)
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (tickets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent activity.",
                                color = BrandCharcoal.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(tickets.take(3)) { ticket ->
                    RecentActivityRow(ticket = ticket)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// Single metric card on the dashboard
@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                color = BrandCharcoal.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// Single ticket summary row under Recent Activity
@Composable
private fun RecentActivityRow(ticket: Ticket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(category = ticket.category, size = 40)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ticket.category.displayName,
                    color = BrandCharcoal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ticket.description,
                    color = BrandCharcoal.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status = ticket.status)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(ticket.createdAt?.toDate()),
                    color = BrandCharcoal.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Format ticket date for display
private fun formatDate(date: Date?): String {
    if (date == null) return "Just now"
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
}