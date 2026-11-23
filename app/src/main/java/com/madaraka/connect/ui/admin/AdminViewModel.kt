package com.madaraka.connect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.Ticket
import com.madaraka.connect.data.models.TicketStatus
import com.madaraka.connect.data.models.User
import com.madaraka.connect.data.repositories.AuditLogsRepository
import com.madaraka.connect.data.repositories.NotificationRepository
import com.madaraka.connect.data.repositories.TicketRepository
import com.madaraka.connect.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Holds all admin-visible state and actions
class AdminViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val ticketRepository: TicketRepository = TicketRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository(),
    private val auditLogsRepository: AuditLogsRepository = AuditLogsRepository()
) : ViewModel() {

    // Display name of the signed-in admin (for audit logs)
    private var performedBy: String = "System Admin"

    // All users in the system
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    // All tickets in the system
    private val _allTickets = MutableStateFlow<List<Ticket>>(emptyList())
    val allTickets: StateFlow<List<Ticket>> = _allTickets.asStateFlow()

    // Support inbox messages (recipientId == "admin")
    private val _adminMessages = MutableStateFlow<List<Map<String, Any?>>>(emptyList())
    val adminMessages: StateFlow<List<Map<String, Any?>>> = _adminMessages.asStateFlow()

    // Audit log entries
    private val _auditLogs = MutableStateFlow<List<Map<String, Any?>>>(emptyList())
    val auditLogs: StateFlow<List<Map<String, Any?>>> = _auditLogs.asStateFlow()

    // True while loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Snackbar message
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Loads everything in parallel
    fun load(adminEmail: String) {
        performedBy = adminEmail.ifBlank { "System Admin" }
        _isLoading.value = true
        viewModelScope.launch {
            // Fetch all in parallel
            val users = userRepository.getAllUsers()
            val tickets = ticketRepository.getAllTickets()
            val messages = notificationRepository.getAdminMessages()
            val logs = auditLogsRepository.getAllAuditLogs()

            _allUsers.value = users
            _allTickets.value = tickets
            _adminMessages.value = messages
            _auditLogs.value = logs
            _isLoading.value = false
        }
    }

    fun reload() {
        load(performedBy)
    }

    // Acknowledge an escalated ticket and ping the dispatcher
    fun acknowledgeEscalation(ticket: Ticket) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = ticketRepository.updateTicketStatus(ticket.id, "Admin_Reviewed")
            if (ok) {
                // Audit log
                auditLogsRepository.writeAuditLog(
                    ticketId = ticket.id,
                    action = if (ticket.status == TicketStatus.ADMIN_REVIEWED) "Admin Re-Ping" else "Admin Review",
                    details = "Administrator flagged the escalation to the dispatcher.",
                    performedBy = performedBy
                )
                // Notify the dispatcher
                val recipientId = "dispatcher_${ticket.category.displayName}"
                notificationRepository.createNotification(
                    recipientId = recipientId,
                    title = "Admin Ping: Urgent Ticket",
                    message = "Admin has reviewed an escalated ticket for ${ticket.category.displayName}. Please resolve immediately."
                )
                _message.value = "Dispatcher pinged successfully."
                reload()
            } else {
                _message.value = "Failed to acknowledge escalation."
            }
            _isLoading.value = false
        }
    }

    // Toggle user Active <-> Suspended
    fun toggleUserStatus(user: User) {
        val newStatus = if (user.status == "Suspended") "Active" else "Suspended"
        viewModelScope.launch {
            val ok = userRepository.updateUserStatus(user.uid, newStatus)
            if (ok) {
                _message.value = "User ${user.email} set to $newStatus."
                reload()
            } else {
                _message.value = "Failed to update user status."
            }
        }
    }

    // Mark a support message as read
    fun markMessageRead(id: String) {
        viewModelScope.launch {
            val ok = notificationRepository.markAsRead(id)
            if (ok) {
                reload()
            } else {
                _message.value = "Failed to mark message as read."
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    // ----- COMPUTED GETTERS -----

    val activeResidentsCount: Int
        get() = _allUsers.value.count {
            it.isResident && it.status != "Suspended"
        }

    val activeDispatchersCount: Int
        get() = _allUsers.value.count {
            it.isDispatcher && it.status != "Suspended"
        }

    fun ticketCountFor(category: FaultCategory): Int =
        _allTickets.value.count { it.category == category }

    // Tickets that need admin attention
    fun escalatedTickets(): List<Ticket> =
        _allTickets.value.filter {
            it.status == TicketStatus.ESCALATED || it.status == TicketStatus.ADMIN_REVIEWED
        }

    // Count of unread admin messages
    val unreadMessageCount: Int
        get() = _adminMessages.value.count { (it["read"] as? Boolean) != true }
}