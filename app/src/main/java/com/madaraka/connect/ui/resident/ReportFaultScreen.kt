
package com.madaraka.connect.ui.resident

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madaraka.connect.data.models.FaultCategory
import com.madaraka.connect.data.repositories.TicketRepository
import kotlinx.coroutines.launch

private val ReportBlue = Color(0xFF2563EB)
private val ReportBackground = Color(0xFFF8FAFC)

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
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = ReportBackground,
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !isSubmitting) {
                        Icon(
                            Icons.Default.ArrowBack,
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Tell us what needs fixing",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Text(
                text = "Select the service affected and describe the problem. Your report will be sent to the relevant dispatch team.",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )

            Text(
                text = "1. Select fault category",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FaultCategory.entries
                    .filter { it != FaultCategory.UNKNOWN }
                    .forEach { category ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedCategory = category },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCategory == category)
                                    Color(0xFFEFF6FF)
                                else Color.White
                            ),
                            border = if (selectedCategory == category)
                                androidx.compose.foundation.BorderStroke(
                                    1.dp, ReportBlue
                                )
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category }
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

            Text(
                text = "2. Describe the problem",
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
                    .heightIn(min = 140.dp),
                placeholder = {
                    Text("Explain what happened and where you noticed it...")
                },
                minLines = 4,
                maxLines = 7,
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "Please provide enough detail to help the dispatch team understand the issue.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = {
                    when {
                        residentId.isBlank() ->
                            errorMessage = "Your session could not be identified. Please sign in again."

                        selectedCategory == FaultCategory.UNKNOWN ->
                            errorMessage = "Please select a fault category."

                        description.isBlank() ->
                            errorMessage = "Please describe the fault."

                        description.trim().length < 10 ->
                            errorMessage = "Please provide a little more detail (at least 10 characters)."

                        else -> {
                            isSubmitting = true
                            errorMessage = null

                            scope.launch {
                                val ticketId = ticketRepository.createTicket(
                                    residentId = residentId,
                                    residentEmail = residentEmail,
                                    category = selectedCategory,
                                    description = description.trim()
                                )

                                isSubmitting = false

                                if (ticketId != null) {
                                    onReportSubmitted(ticketId)
                                } else {
                                    errorMessage =
                                        "We couldn't submit your report. Please check your connection and try again."
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReportBlue
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Submitting...")
                } else {
                    Text(
                        "Submit Report",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Your report will initially be marked as Pending.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
