package com.madaraka.connect.ui.dispatcher

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madaraka.connect.data.models.Ticket
import com.madaraka.connect.data.models.TicketStatus
import com.madaraka.connect.ui.common.CategoryIcon
import com.madaraka.connect.ui.common.StatusBadge

private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)
private val AiPurple = Color(0xFF7C3AED)

private enum class DispatcherTab(val label: String) {
    ACTIVE("Active"),
    GROUPED("AI Grouped"),
    CLOSED("Closed"),
    CANCELLED("Cancelled")
}

private sealed class DispatcherItem {
    data class Group(val masterIncidentId: String, val tickets: List<Ticket>) : DispatcherItem()
    data class Single(val ticket: Ticket) : DispatcherItem()
}

@Composable
fun DispatcherDashboardScreen(
    department: String,
    agencyName: String,
    onSignOut: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    viewModel: DispatcherViewModel = viewModel()
) {
    LaunchedEffect(department, agencyName) {
        if (department.isNotBlank()) viewModel.load(department, agencyName)
    }

    val allLoadedTickets by viewModel.tickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    var selectedTab by remember { mutableStateOf(DispatcherTab.ACTIVE) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val ticketsRaw: List<Ticket> = remember(allLoadedTickets, selectedTab) {
        when (selectedTab) {
            DispatcherTab.ACTIVE -> allLoadedTickets.filter {
                it.status != TicketStatus.CLOSED && it.status != TicketStatus.CANCELLED
            }
            DispatcherTab.GROUPED -> allLoadedTickets.filter {
                !it.masterIncidentId.isNullOrBlank()
            }
            DispatcherTab.CLOSED -> allLoadedTickets.filter {
                it.status == TicketStatus.CLOSED
            }
            DispatcherTab.CANCELLED -> allLoadedTickets.filter {
                it.status == TicketStatus.CANCELLED
            }
        }
    }
    val itemsGrouped = remember(ticketsRaw) { groupTickets(ticketsRaw) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold) },
            text = { Text("You will be signed out of the dispatcher dashboard.", fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOut()
                }) {
                    Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = BrandCharcoal, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        )
    }

    Scaffold(
        containerColor = PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dispatcher Dashboard",
                                color = BrandCharcoal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (department.isNotBlank()) {
                                Text(
                                    text = department,
                                    color = BrandBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { showSignOutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = ErrorRed
                            )
                        }
                    }
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.White,
                        contentColor = BrandBlue,
                        edgePadding = 8.dp
                    ) {
                        DispatcherTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
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
                    label = { Text("Dashboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAnalytics,
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        if (isLoading && itemsGrouped.isEmpty()) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                TabSummaryBanner(
                    tab = selectedTab,
                    count = ticketsRaw.size,
                    groupedCount = ticketsRaw.mapNotNull { it.masterIncidentId }.distinct().size
                )
            }

            if (selectedTab == DispatcherTab.ACTIVE) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DispatcherStatCard("Total", viewModel.totalCount.toString(), BrandCharcoal, Modifier.weight(1f))
                        DispatcherStatCard("Pending", viewModel.pendingCount.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                        DispatcherStatCard("Dispatched", viewModel.dispatchedCount.toString(), BrandBlue, Modifier.weight(1f))
                    }
                }
            }

            if (itemsGrouped.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = emptyMessageFor(selectedTab),
                                color = BrandCharcoal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.load(department, agencyName) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Refresh", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(itemsGrouped) { item ->
                    when (item) {
                        is DispatcherItem.Group -> GroupedIncidentCard(
                            item = item,
                            onAssignFirst = { name, phone ->
                                item.tickets.firstOrNull()?.let { firstTicket ->
                                    viewModel.assignTechnician(firstTicket, name, phone)
                                }
                            },
                            onResolveGroup = {
                                viewModel.resolveGroup(item.tickets)
                            }
                        )
                        is DispatcherItem.Single -> DispatcherTicketCard(
                            ticket = item.ticket,
                            onPending = { viewModel.changeStatus(item.ticket, "Pending") },
                            onResolve = { viewModel.changeStatus(item.ticket, "Resolved") },
                            onAssign = { name, phone ->
                                viewModel.assignTechnician(item.ticket, name, phone)
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TabSummaryBanner(
    tab: DispatcherTab,
    count: Int,
    groupedCount: Int
) {
    val (bg, fg, title, subtitle) = when (tab) {
        DispatcherTab.ACTIVE -> TabSummary(
            Color(0xFFEFF6FF), BrandBlue,
            "Active Tickets",
            "$count ticket${if (count == 1) "" else "s"} waiting for action"
        )
        DispatcherTab.GROUPED -> TabSummary(
            Color(0xFFF5F3FF), AiPurple,
            "AI-Grouped Incidents",
            "$groupedCount master incident${if (groupedCount == 1) "" else "s"} · $count report${if (count == 1) "" else "s"} grouped (K-Means / 150m & 1h)"
        )
        DispatcherTab.CLOSED -> TabSummary(
            Color(0xFFF1F5F9), BrandCharcoal,
            "Closed Tickets",
            "$count resolved ticket${if (count == 1) "" else "s"}"
        )
        DispatcherTab.CANCELLED -> TabSummary(
            Color(0xFFFEF2F2), ErrorRed,
            "Cancelled Tickets",
            "$count ticket${if (count == 1) "" else "s"} cancelled by residents"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = fg.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private data class TabSummary(
    val bg: Color,
    val fg: Color,
    val title: String,
    val subtitle: String
)

private fun emptyMessageFor(tab: DispatcherTab): String = when (tab) {
    DispatcherTab.ACTIVE -> "No active tickets."
    DispatcherTab.GROUPED -> "No AI-grouped incidents yet. Nearby reports in the same category and time window will appear here."
    DispatcherTab.CLOSED -> "No closed tickets yet."
    DispatcherTab.CANCELLED -> "No cancelled tickets."
}

private fun groupTickets(tickets: List<Ticket>): List<DispatcherItem> {
    val groups = LinkedHashMap<String, MutableList<Ticket>>()
    val singles = mutableListOf<DispatcherItem>()

    tickets.forEach { t ->
        val id = t.masterIncidentId
        if (id.isNullOrBlank()) {
            singles.add(DispatcherItem.Single(t))
        } else {
            groups.getOrPut(id) { mutableListOf() }.add(t)
        }
    }

    val groupItems: List<DispatcherItem> = groups.map { (id, list) ->
        DispatcherItem.Group(id, list) as DispatcherItem
    }.sortedByDescending { g ->
        val grouped = g as DispatcherItem.Group
        val hasUrgent = grouped.tickets.any {
            it.status == TicketStatus.ESCALATED || it.status == TicketStatus.ADMIN_REVIEWED
        }
        if (hasUrgent) 1 else 0
    }

    return groupItems + singles
}

@Composable
private fun GroupedIncidentCard(
    item: DispatcherItem.Group,
    onAssignFirst: (String, String) -> Unit,
    onResolveGroup: () -> Unit
) {
    val context = LocalContext.current
    val first = item.tickets.firstOrNull() ?: return
    val count = item.tickets.size
    val hasUrgent = item.tickets.any {
        it.status == TicketStatus.ESCALATED || it.status == TicketStatus.ADMIN_REVIEWED
    }

    var assigningOpen by remember { mutableStateOf(false) }
    var techName by remember { mutableStateOf(first.technicianName ?: "") }
    var techPhone by remember { mutableStateOf(first.technicianContact ?: "") }
    var showTicketsDialog by remember { mutableStateOf(false) }

    if (showTicketsDialog) {
        AlertDialog(
            onDismissRequest = { showTicketsDialog = false },
            title = {
                Text("${item.masterIncidentId} · $count report${if (count == 1) "" else "s"}", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.tickets) { t ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    t.residentEmail.ifBlank { "Resident (${t.residentId.take(6)})" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandCharcoal
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(t.description, fontSize = 13.sp, color = BrandCharcoal.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTicketsDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AiPurple)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (count > 1) "AI CLUSTER ($count REPORTS)" else "MASTER INCIDENT (1 REPORT)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(item.masterIncidentId.take(10), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (hasUrgent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorRed)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONTAINS ESCALATED TICKET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(category = first.category, size = 42)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = first.category.displayName,
                            color = BrandCharcoal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (count > 1)
                                "$count nearby reports grouped within 150m & 1h"
                            else
                                "Single report tracking this area",
                            color = AiPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = first.description,
                    color = BrandCharcoal.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (first.location != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val lat = first.location.lat
                    val lng = first.location.lng
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Grouped+Incident)")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Incident Location", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (first.hasTechnician) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(first.technicianName ?: "", color = Color(0xFF1E3A8A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(first.technicianContact ?: "", color = Color(0xFF1E3A8A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showTicketsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("View all $count report${if (count == 1) "" else "s"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (assigningOpen) {
                    Column {
                        OutlinedTextField(
                            value = techName,
                            onValueChange = { techName = it },
                            label = { Text("Technician name (first + last)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = techPhone,
                            onValueChange = { techPhone = it },
                            label = { Text("Phone (0712345678 or +254712345678)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onAssignFirst(techName, techPhone)
                                    assigningOpen = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = {
                                    assigningOpen = false
                                    techName = first.technicianName ?: ""
                                    techPhone = first.technicianContact ?: ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { assigningOpen = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Assign", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onResolveGroup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Resolve all", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatcherTicketCard(
    ticket: Ticket,
    onPending: () -> Unit,
    onResolve: () -> Unit,
    onAssign: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    val isUrgent = ticket.status == TicketStatus.ESCALATED || ticket.status == TicketStatus.ADMIN_REVIEWED

    var assigningOpen by remember { mutableStateOf(false) }
    var techName by remember { mutableStateOf(ticket.technicianName ?: "") }
    var techPhone by remember { mutableStateOf(ticket.technicianContact ?: "") }

    val isPendingDisabled = ticket.status == TicketStatus.DISPATCHED ||
            ticket.status == TicketStatus.RESOLVED ||
            ticket.status == TicketStatus.CLOSED ||
            ticket.status == TicketStatus.CANCELLED ||
            ticket.status == TicketStatus.ADMIN_REVIEWED ||
            !ticket.technicianName.isNullOrBlank()

    val isAssignDisabled = ticket.status == TicketStatus.RESOLVED ||
            ticket.status == TicketStatus.CLOSED ||
            ticket.status == TicketStatus.CANCELLED

    val isResolveDisabled = ticket.status == TicketStatus.RESOLVED ||
            ticket.status == TicketStatus.CLOSED ||
            ticket.status == TicketStatus.CANCELLED ||
            ticket.technicianName.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            if (isUrgent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorRed)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (ticket.status == TicketStatus.ADMIN_REVIEWED)
                            "ADMIN PINGED: RESOLVE IMMEDIATELY"
                        else
                            "PRIORITY ESCALATION",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(category = ticket.category, size = 42)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ticket.category.displayName,
                            color = BrandCharcoal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Reported by: ${ticket.residentEmail}",
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
                    color = BrandCharcoal.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (ticket.location != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val lat = ticket.location.lat
                    val lng = ticket.location.lng
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Reported+Fault)")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Exact Location in Maps", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (ticket.hasTechnician) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                if (assigningOpen) {
                    Column {
                        OutlinedTextField(
                            value = techName,
                            onValueChange = { techName = it },
                            label = { Text("Technician name (first + last)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = techPhone,
                            onValueChange = { techPhone = it },
                            label = { Text("Phone (0712345678 or +254712345678)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onAssign(techName, techPhone)
                                    assigningOpen = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = {
                                    assigningOpen = false
                                    techName = ticket.technicianName ?: ""
                                    techPhone = ticket.technicianContact ?: ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onPending,
                            enabled = !isPendingDisabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Pending", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { assigningOpen = true },
                            enabled = !isAssignDisabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (ticket.hasTechnician) "Reassign" else "Assign", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onResolve,
                            enabled = !isResolveDisabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Resolve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatcherStatCard(
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
                .padding(14.dp)
        ) {
            Text(label, color = BrandCharcoal.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}