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

class DispatcherViewModel(
    private val ticketRepository: TicketRepository = TicketRepository(),
    private val auditLogsRepository: AuditLogsRepository = AuditLogsRepository(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private var department: String = ""
    private var performedBy: String = ""

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(departmentName: String, agencyOrEmail: String) {
        if (departmentName.isBlank()) return
        department = departmentName
        performedBy = agencyOrEmail.ifBlank { "Dispatcher" }
        _isLoading.value = true
        viewModelScope.launch {
            val category = FaultCategory.fromString(departmentName)
            val fetched = if (category == FaultCategory.UNKNOWN) {
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

    fun activeTickets(): List<Ticket> = _tickets.value.filter {
        it.status != TicketStatus.CLOSED && it.status != TicketStatus.CANCELLED
    }

    fun allTickets(): List<Ticket> = _tickets.value

    fun assignTechnician(
        ticket: Ticket,
        technicianName: String,
        technicianContact: String
    ) {
        val nameParts = technicianName.trim().split(Regex("\\s+"))
        if (nameParts.size < 2) {
            _message.value = "Please provide both first and last name of the technician."
            return
        }

        val phone = technicianContact.trim()
        val phoneRegex = Regex("^(\\+254|0)\\d{9}$")
        if (!phoneRegex.matches(phone)) {
            _message.value = "Phone must be in Kenyan format, e.g. 0712345678 or +254712345678."
            return
        }

        val cleanName = technicianName.trim()
        viewModelScope.launch {
            _isLoading.value = true
            val ok = ticketRepository.assignTechnician(ticket.id, cleanName, phone)
            if (ok) {
                _tickets.value = sortUrgentFirst(
                    _tickets.value.map { current ->
                        if (current.id == ticket.id) {
                            current.copy(
                                status = TicketStatus.DISPATCHED,
                                technicianName = cleanName,
                                technicianContact = phone
                            )
                        } else {
                            current
                        }
                    }
                )
                auditLogsRepository.writeAuditLog(
                    ticketId = ticket.id,
                    action = "Technician Assignment",
                    details = "Assigned $cleanName ($phone)",
                    performedBy = performedBy
                )
                if (ticket.residentId.isNotBlank()) {
                    notificationRepository.createNotification(
                        recipientId = ticket.residentId,
                        title = "Technician Dispatched",
                        message = "$cleanName has been assigned to your ${ticket.category.displayName} issue."
                    )
                }
                _message.value = "Technician dispatched successfully."
            } else {
                _message.value = "Failed to assign technician. Please try again."
            }
            _isLoading.value = false
        }
    }

    fun changeStatus(ticket: Ticket, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = ticketRepository.updateTicketStatus(ticket.id, newStatus)
            if (ok) {
                val parsedStatus = TicketStatus.fromString(newStatus)
                _tickets.value = sortUrgentFirst(
                    _tickets.value.map { current ->
                        if (current.id == ticket.id) current.copy(status = parsedStatus) else current
                    }
                )
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
            } else {
                _message.value = "Failed to update status."
            }
            _isLoading.value = false
        }
    }

    // Resolves every open/unresolved ticket in a group cleanly in one pass
    fun resolveGroup(groupTickets: List<Ticket>) {
        val targets = groupTickets.filter {
            it.status != TicketStatus.RESOLVED &&
                    it.status != TicketStatus.CLOSED &&
                    it.status != TicketStatus.CANCELLED
        }
        if (targets.isEmpty()) {
            _message.value = "All reports in this group are already resolved or closed."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val updatedIds = mutableSetOf<String>()
            for (t in targets) {
                val ok = ticketRepository.updateTicketStatus(t.id, "Resolved")
                if (ok) {
                    updatedIds.add(t.id)
                    auditLogsRepository.writeAuditLog(
                        ticketId = t.id,
                        action = "Status Update",
                        details = "Changed status to Resolved (group action)",
                        performedBy = performedBy
                    )
                    if (t.residentId.isNotBlank()) {
                        notificationRepository.createNotification(
                            recipientId = t.residentId,
                            title = "Issue Resolved",
                            message = "Your ${t.category.displayName} ticket has been marked as resolved. Please confirm in your dashboard."
                        )
                    }
                }
            }

            if (updatedIds.isNotEmpty()) {
                _tickets.value = sortUrgentFirst(
                    _tickets.value.map { current ->
                        if (current.id in updatedIds) current.copy(status = TicketStatus.RESOLVED) else current
                    }
                )
                _message.value = "Resolved ${updatedIds.size} report${if (updatedIds.size == 1) "" else "s"} in group."
            } else {
                _message.value = "Failed to resolve group tickets."
            }
            _isLoading.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

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

    fun ratingBreakdown(): Map<Int, Int> {
        val counts = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0)
        _tickets.value.forEach { ticket ->
            val r = ticket.rating ?: 0
            if (r in 1..5) counts[r] = (counts[r] ?: 0) + 1
        }
        return counts
    }

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