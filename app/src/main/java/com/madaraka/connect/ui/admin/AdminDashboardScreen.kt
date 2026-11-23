package com.madaraka.connect.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.Ticket
import com.madaraka.connect.data.models.TicketStatus
import com.madaraka.connect.data.models.User
import com.madaraka.connect.ui.common.CategoryIcon
import com.madaraka.connect.ui.common.StatusBadge
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)
private val SuccessGreen = Color(0xFF10B981)
private val Amber = Color(0xFFF59E0B)
private val AdminDark = Color(0xFF1E293B)

// Which admin tab is showing
private enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    USERS("Users"),
    INBOX("Inbox"),
    AUDIT("Audit")
}

@Composable
fun AdminDashboardScreen(
    adminEmail: String,
    onSignOut: () -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    LaunchedEffect(adminEmail) {
        viewModel.load(adminEmail)
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    var selectedTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold) },
            text = { Text("You will be signed out of the admin dashboard.", fontSize = 15.sp) },
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
            Surface(color = AdminDark, shadowElevation = 1.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Admin",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = adminEmail,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { showSignOutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = Color(0xFFFCA5A5)
                            )
                        }
                    }
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = AdminDark,
                        contentColor = SuccessGreen,
                        edgePadding = 8.dp
                    ) {
                        AdminTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedTab == tab) SuccessGreen else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && viewModel.allTickets.value.isEmpty() && viewModel.allUsers.value.isEmpty()) {
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

        when (selectedTab) {
            AdminTab.OVERVIEW -> OverviewTab(viewModel = viewModel, padding = padding)
            AdminTab.USERS -> UsersTab(viewModel = viewModel, padding = padding)
            AdminTab.INBOX -> InboxTab(viewModel = viewModel, padding = padding)
            AdminTab.AUDIT -> AuditTab(viewModel = viewModel, padding = padding)
        }
    }
}

// ---------- OVERVIEW TAB ----------
@Composable
private fun OverviewTab(viewModel: AdminViewModel, padding: PaddingValues) {
    val tickets = viewModel.allTickets.collectAsState().value
    val escalated = viewModel.escalatedTickets()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard("Tickets", tickets.size.toString(), BrandBlue, Modifier.weight(1f))
                AdminStatCard("Residents", viewModel.activeResidentsCount.toString(), SuccessGreen, Modifier.weight(1f))
                AdminStatCard("Dispatchers", viewModel.activeDispatchersCount.toString(), Amber, Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Ticket Breakdown",
                color = BrandCharcoal,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryCountCard("Water", viewModel.ticketCountFor(FaultCategory.WATER), Color(0xFF0EA5E9), Modifier.weight(1f))
                CategoryCountCard("Electricity", viewModel.ticketCountFor(FaultCategory.ELECTRICITY), Amber, Modifier.weight(1f))
                CategoryCountCard("Waste", viewModel.ticketCountFor(FaultCategory.WASTE), SuccessGreen, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Requires Admin Attention",
                    color = BrandCharcoal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (escalated.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "No escalated tickets. System is healthy.",
                            color = BrandCharcoal.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(escalated, key = { it.id }) { ticket ->
                EscalatedTicketCard(
                    ticket = ticket,
                    onAcknowledge = { viewModel.acknowledgeEscalation(ticket) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun EscalatedTicketCard(
    ticket: Ticket,
    onAcknowledge: () -> Unit
) {
    val isAlreadyReviewed = ticket.status == TicketStatus.ADMIN_REVIEWED
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
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
                    text = if (isAlreadyReviewed) "ALREADY PINGED · PING AGAIN" else "ESCALATED BY RESIDENT",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(category = ticket.category, size = 42)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ticket.category.displayName,
                            color = BrandCharcoal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Resident: ${ticket.residentEmail}",
                            color = BrandCharcoal.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    StatusBadge(status = ticket.status)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    ticket.description,
                    color = BrandCharcoal.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadyReviewed) ErrorRed else BrandCharcoal
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (isAlreadyReviewed) "Ping Again" else "Acknowledge & Ping Dispatcher",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
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
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = BrandCharcoal.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun CategoryCountCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(count.toString(), color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ---------- USERS TAB ----------
@Composable
private fun UsersTab(viewModel: AdminViewModel, padding: PaddingValues) {
    val users = viewModel.allUsers.collectAsState().value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Text(
                "${users.size} users in system",
                color = BrandCharcoal.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (users.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "No users found. Check Firestore security rules — admins may need read access to all users.",
                        color = BrandCharcoal.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(users, key = { it.uid }) { user ->
                UserCard(user = user, onToggle = { viewModel.toggleUserStatus(user) })
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun UserCard(user: User, onToggle: () -> Unit) {
    val isSuspended = user.status == "Suspended"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = when (user.role.name) {
                        "RESIDENT" -> BrandBlue
                        "DISPATCHER" -> Amber
                        "ADMIN" -> BrandCharcoal
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.agencyName ?: user.email,
                        color = BrandCharcoal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        user.email,
                        color = BrandCharcoal.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Role chip
                val roleColor = when (user.role.name) {
                    "RESIDENT" -> BrandBlue
                    "DISPATCHER" -> Amber
                    "ADMIN" -> BrandCharcoal
                    else -> Color.Gray
                }
                Text(
                    user.role.name,
                    color = roleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(roleColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Status: ",
                    color = BrandCharcoal.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    user.status,
                    color = if (isSuspended) ErrorRed else SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (user.role.name == "RESIDENT") {
                    OutlinedButton(
                        onClick = onToggle,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (isSuspended) "Reactivate" else "Suspend",
                            color = if (isSuspended) SuccessGreen else ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------- INBOX TAB ----------
@Composable
private fun InboxTab(viewModel: AdminViewModel, padding: PaddingValues) {
    val messages = viewModel.adminMessages.collectAsState().value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        if (messages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = BrandCharcoal.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No support messages yet.",
                            color = BrandCharcoal.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(messages, key = { (it["id"] as? String) ?: "" }) { msg ->
                MessageCard(msg = msg, onMarkRead = {
                    (msg["id"] as? String)?.let { viewModel.markMessageRead(it) }
                })
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MessageCard(msg: Map<String, Any?>, onMarkRead: () -> Unit) {
    val isRead = (msg["read"] as? Boolean) == true
    val title = msg["title"] as? String ?: "(no title)"
    val body = msg["message"] as? String ?: ""
    val createdAt = msg["createdAt"] as? Timestamp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isRead) Color.White else Color(0xFFEFF6FF))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BrandBlue, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    title,
                    color = BrandCharcoal,
                    fontSize = 14.sp,
                    fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                body,
                color = BrandCharcoal.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTimestamp(createdAt),
                    color = BrandCharcoal.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!isRead) {
                    TextButton(onClick = onMarkRead) {
                        Text("Mark Read", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------- AUDIT LOGS TAB ----------
@Composable
private fun AuditTab(viewModel: AdminViewModel, padding: PaddingValues) {
    val logs = viewModel.auditLogs.collectAsState().value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "No audit logs yet.",
                        color = BrandCharcoal.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(logs, key = { (it["id"] as? String) ?: "" }) { log ->
                AuditLogCard(log)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AuditLogCard(log: Map<String, Any?>) {
    val action = log["action"] as? String ?: "-"
    val details = log["details"] as? String ?: ""
    val performedBy = log["performedBy"] as? String ?: "-"
    val timestamp = log["timestamp"] as? Timestamp
    val ticketId = log["ticketId"] as? String ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    action,
                    color = BrandBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .background(BrandBlue.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    formatTimestamp(timestamp),
                    color = BrandCharcoal.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                details,
                color = BrandCharcoal.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "By: $performedBy  ·  Ticket: ${ticketId.take(8)}...",
                color = BrandCharcoal.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Timestamp formatter
private fun formatTimestamp(ts: Timestamp?): String {
    if (ts == null) return "—"
    val date: Date = ts.toDate()
    return SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(date)
}