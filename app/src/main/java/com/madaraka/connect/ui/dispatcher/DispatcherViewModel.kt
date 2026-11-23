package com.madaraka.connect.ui.dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.models.Ticket
import com.madaraka.connect.data.models.TicketStatus
import com.madaraka.connect.data.repositories.AuditLogsRepository
import com.madaraka.connect.data.repositories.NotificationRepository
import com.madaraka.connect.data.repositories.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// Holds dispatcher state, ticket list, and all actions the dispatcher can take
class DispatcherViewModel(
    private val ticketRepository: TicketRepository = TicketRepository(),
    private val auditLogsRepository: AuditLogsRepository = AuditLogsRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    // The dispatcher's department, e.g. "Water Services"
    private var department: String = ""

    // Display name used on audit logs (agencyName or fallback email)
    private var performedBy: String = ""

    // All tickets for the dispatcher's department
    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    // True while a fetch or action is running
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Snackbar message after an action
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Load tickets for the dispatcher's department
    fun load(departmentName: String, agencyOrEmail: String) {
        if (departmentName.isBlank()) return
        department = departmentName
        performedBy = agencyOrEmail.ifBlank { "Dispatcher" }
        _isLoading.value = true
        viewModelScope.launch {
            val category = FaultCategory.fromString(departmentName)
            val fetched = if (category == FaultCategory.UNKNOWN) {
                // Fallback if department string doesn't match a known category
                emptyList()
            } else {
                ticketRepository.getTicketsByCategory(category)
            }
            _tickets.value = sortUrgentFirst(fetched)
            _isLoading.value = false
        }
    }

    fun reload() {
        if (department.isNotBlank()) load(department, performedBy)
    }

    // Active tickets: exclude Closed and Cancelled, urgent first
    fun activeTickets(): List<Ticket> = _tickets.value.filter {
        it.status != TicketStatus.CLOSED && it.status != TicketStatus.CANCELLED
    }

    // All tickets, urgent first
    fun allTickets(): List<Ticket> = _tickets.value

    // Assign a technician to a ticket. Returns validation error message, or null on success.
    fun assignTechnician(
        ticket: Ticket,
        technicianName: String,
        technicianContact: String
    ) {
        // Validate name (first + last)
        val nameParts = technicianName.trim().split(Regex("\\s+"))
        if (nameParts.size < 2) {
            _message.value = "Please provide both first and last name of the technician."
            return
        }

        // Validate phone (Kenyan format)
        val phone = technicianContact.trim()
        val phoneRegex = Regex("^(\\+254|0)\\d{9}$")
        if (!phoneRegex.matches(phone)) {
            _message.value = "Phone must be in Kenyan format, e.g. 0712345678 or +254712345678."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val ok = ticketRepository.assignTechnician(ticket.id, technicianName.trim(), phone)
            if (ok) {
                // Audit log — non-fatal if it fails
                auditLogsRepository.writeAuditLog(
                    ticketId = ticket.id,
                    action = "Technician Assignment",
                    details = "Assigned ${technicianName.trim()} ($phone)",
                    performedBy = performedBy
                )
                // Notify the resident — non-fatal if it fails
                if (ticket.residentId.isNotBlank()) {
                    notificationRepository.createNotification(
                        recipientId = ticket.residentId,
                        title = "Technician Dispatched",
                        message = "${technicianName.trim()} has been assigned to your ${ticket.category.displayName} issue."
                    )
                }
                _message.value = "Technician dispatched successfully."
                reload()
            } else {
                _message.value = "Failed to assign technician. Please try again."
            }
            _isLoading.value = false
        }
    }

    // Change ticket status (Pending / Resolved / etc.). Notifies resident when Resolved.
    fun changeStatus(ticket: Ticket, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = ticketRepository.updateTicketStatus(ticket.id, newStatus)
            if (ok) {
                auditLogsRepository.writeAuditLog(
                    ticketId = ticket.id,
                    action = "Status Update",
                    details = "Changed status to $newStatus",
                    performedBy = performedBy
                )
                if (newStatus == "Resolved" && ticket.residentId.isNotBlank()) {
                    notificationRepository.createNotification(
                        recipientId = ticket.residentId,
                        title = "Issue Resolved",
                        message = "Your ${ticket.category.displayName} ticket has been marked as resolved. Please confirm in your dashboard."
                    )
                }
                _message.value = "Status updated to $newStatus."
                reload()
            } else {
                _message.value = "Failed to update status."
            }
            _isLoading.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    // ----- ANALYTICS -----

    val totalCount: Int get() = _tickets.value.size

    val pendingCount: Int get() = _tickets.value.count {
        it.status == TicketStatus.PENDING ||
                it.status == TicketStatus.ESCALATED ||
                it.status == TicketStatus.ADMIN_REVIEWED
    }

    val dispatchedCount: Int get() = _tickets.value.count {
        it.status == TicketStatus.DISPATCHED
    }

    val averageRating: String
        get() {
            val rated = _tickets.value.filter { (it.rating ?: 0) > 0 }
            if (rated.isEmpty()) return "0.0"
            val total = rated.sumOf { it.rating ?: 0 }
            return String.format(Locale.US, "%.1f", total.toDouble() / rated.size)
        }

    val ratedCount: Int get() = _tickets.value.count { (it.rating ?: 0) > 0 }

    // Map: star (5..1) -> count of tickets with that rating
    fun ratingBreakdown(): Map<Int, Int> {
        val counts = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0)
        _tickets.value.forEach { ticket ->
            val r = ticket.rating ?: 0
            if (r in 1..5) counts[r] = (counts[r] ?: 0) + 1
        }
        return counts
    }

    // Sort Escalated / Admin_Reviewed to the top, keep the rest in original order
    private fun sortUrgentFirst(list: List<Ticket>): List<Ticket> {
        val urgent = list.filter {
            it.status == TicketStatus.ESCALATED || it.status == TicketStatus.ADMIN_REVIEWED
        }
        val rest = list.filter {
            it.status != TicketStatus.ESCALATED && it.status != TicketStatus.ADMIN_REVIEWED
        }
        return urgent + rest
    }
}