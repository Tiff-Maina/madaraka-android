package com.madaraka.connect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madaraka.connect.data.models.TicketStatus

// Color pair: background + text/border for each status
private data class StatusColors(
    val background: Color,
    val content: Color
)

// Maps a TicketStatus to the colors used on the website
private fun colorsFor(status: TicketStatus): StatusColors = when (status) {
    TicketStatus.PENDING        -> StatusColors(Color(0xFFFEF3C7), Color(0xFFB45309)) // yellow-100 / yellow-700
    TicketStatus.DISPATCHED     -> StatusColors(Color(0xFFDBEAFE), Color(0xFF1D4ED8)) // blue-100 / blue-700
    TicketStatus.IN_PROGRESS    -> StatusColors(Color(0xFFDBEAFE), Color(0xFF1D4ED8)) // reuse blue
    TicketStatus.RESOLVED       -> StatusColors(Color(0xFFD1FAE5), Color(0xFF047857)) // emerald-100 / emerald-700
    TicketStatus.CLOSED         -> StatusColors(Color(0xFFE2E8F0), Color(0xFF334155)) // slate-200 / slate-700
    TicketStatus.ESCALATED      -> StatusColors(Color(0xFFFEE2E2), Color(0xFFB91C1C)) // red-100 / red-700
    TicketStatus.ADMIN_REVIEWED -> StatusColors(Color(0xFFEDE9FE), Color(0xFF6D28D9)) // purple-100 / purple-700
    TicketStatus.CANCELLED      -> StatusColors(Color(0xFFFEE2E2), Color(0xFFEF4444)) // red-50 / red-500
    TicketStatus.UNKNOWN        -> StatusColors(Color(0xFFF1F5F9), Color(0xFF334155)) // slate-100 / slate-700
}

// Small pill-shaped badge showing the ticket's status
@Composable
fun StatusBadge(
    status: TicketStatus,
    modifier: Modifier = Modifier
) {
    val colors = colorsFor(status)

    Text(
        text = status.displayName.uppercase(),
        color = colors.content,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .background(colors.background, RoundedCornerShape(999.dp))
            .border(1.dp, colors.content.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}