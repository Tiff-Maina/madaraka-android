package com.madaraka.connect.ui.resident

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
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

// Brand colors shared across the app
private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)
private val SuccessGreen = Color(0xFF10B981)
private val Amber = Color(0xFFF59E0B)

// ViewModel that holds the resident's tickets and exposes ticket actions
class MyReportsViewModel(
    private val ticketRepository: TicketRepository = TicketRepository()
) : ViewModel() {

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _tempRatings = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tempRatings: StateFlow<Map<String, Int>> = _tempRatings.asStateFlow()

    private var residentId: String = ""

    fun load(residentId: String) {
        if (residentId.isBlank()) return
        this.residentId = residentId
        _isLoading.value = true
        viewModelScope.launch {
            val fetched = ticketRepository.getTicketsByResident(residentId)
            _tickets.value = fetched
            _isLoading.value = false
        }
    }

    fun reload() {
        load(residentId)
    }

    fun escalate(ticket: Ticket) {
        viewModelScope.launch {
            val ok = ticketRepository.escalateTicket(ticket.id)
            _message.value = if (ok) "Ticket escalated. Admin notified." else "Failed to escalate."
            if (ok) reload()
        }
    }

    fun confirmResolution(ticket: Ticket) {
        viewModelScope.launch {
            val ok = ticketRepository.confirmResolution(ticket.id)
            _message.value = if (ok) "Resolution confirmed. Thank you!" else "Failed to confirm."
            if (ok) reload()
        }
    }

    fun rejectResolution(ticket: Ticket) {
        viewModelScope.launch {
            val ok = ticketRepository.rejectResolution(ticket.id)
            _message.value = if (ok) "Ticket reopened and sent back to dispatchers." else "Failed to reopen."
            if (ok) reload()
        }
    }

    fun cancelTicket(ticket: Ticket) {
        viewModelScope.launch {
            val ok = ticketRepository.cancelTicket(ticket.id)
            _message.value = if (ok) "Report cancelled." else "Failed to cancel."
            if (ok) reload()
        }
    }

    fun setTempRating(ticketId: String, stars: Int) {
        _tempRatings.value = _tempRatings.value + (ticketId to stars)
    }

    fun submitRating(ticket: Ticket) {
        val stars = _tempRatings.value[ticket.id] ?: return
        viewModelScope.launch {
            val ok = ticketRepository.rateTicket(ticket.id, stars)
            _message.value = if (ok) "Thank you for your feedback!" else "Failed to submit rating."
            if (ok) reload()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

@Composable
fun MyReportsScreen(
    residentId: String,
    onBackToDashboard: () -> Unit = {},
    onNavigateToClusters: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    viewModel: MyReportsViewModel = viewModel()
) {
    val tickets by viewModel.tickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val tempRatings by viewModel.tempRatings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(residentId) {
        if (residentId.isNotBlank()) viewModel.load(residentId)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandCharcoal)
                    }
                    Text(
                        text = "My Reports",
                        color = BrandCharcoal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = false,
                    onClick = onBackToDashboard,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Description, contentDescription = "My Reports") },
                    label = { Text("My Reports", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToClusters,
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Clusters") },
                    label = { Text("Clusters", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSupport,
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Help") },
                    label = { Text("Help", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
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

        if (tickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandCharcoal.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You haven't reported any issues yet.",
                        color = BrandCharcoal.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(tickets) { ticket ->
                ReportCard(
                    ticket = ticket,
                    selectedStars = tempRatings[ticket.id] ?: 0,
                    onEscalate = { viewModel.escalate(ticket) },
                    onConfirm = { viewModel.confirmResolution(ticket) },
                    onReject = { viewModel.rejectResolution(ticket) },
                    onCancel = { viewModel.cancelTicket(ticket) },
                    onSelectStars = { n -> viewModel.setTempRating(ticket.id, n) },
                    onSubmitRating = { viewModel.submitRating(ticket) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ReportCard(
    ticket: Ticket,
    selectedStars: Int,
    onEscalate: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onSelectStars: (Int) -> Unit,
    onSubmitRating: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(category = ticket.category, size = 48)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.category.displayName,
                        color = BrandCharcoal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = formatDate(ticket.createdAt?.toDate()),
                        color = BrandCharcoal.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                StatusBadge(status = ticket.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = ticket.description,
                color = BrandCharcoal.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (ticket.hasTechnician) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ticket.technicianName ?: "", color = Color(0xFF1E3A8A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ticket.technicianContact ?: "", color = Color(0xFF1E3A8A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (ticket.canEscalate) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Taking longer than usual.", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onEscalate) {
                        Text("Escalate", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            if (ticket.status == TicketStatus.RESOLVED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("It's fixed", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Still broken", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (ticket.canRate) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFFFFFBEB), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("Rate the Technician", color = Color(0xFF92400E), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (star in 1..5) {
                            IconButton(onClick = { onSelectStars(star) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (star <= selectedStars) Amber else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (selectedStars > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onSubmitRating,
                                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Submit", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            if (ticket.status == TicketStatus.CLOSED && ticket.rating != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("You rated this:", color = BrandCharcoal.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    for (star in 1..5) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (star <= (ticket.rating ?: 0)) Amber else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (ticket.canCancel) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel this report", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID: ${ticket.id.take(6)}...",
                color = BrandCharcoal.copy(alpha = 0.35f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDate(date: Date?): String {
    if (date == null) return "Just now"
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
}