package com.madaraka.connect.ui.resident

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madaraka.connect.data.repositories.NotificationRepository
import kotlinx.coroutines.launch

// Brand colors shared across the app
private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val SuccessGreen = Color(0xFF10B981)
private val ErrorRed = Color(0xFFDC2626)

// Full Help & Support screen: contact admin, FAQs, agency phone numbers
@Composable
fun HelpSupportScreen(
    residentId: String,
    residentEmail: String,
    onBack: () -> Unit
) {
    // Local state for the contact form
    var messageText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val notificationRepo = remember { NotificationRepository() }

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
                        text = "Help & Support",
                        color = BrandCharcoal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
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
                .padding(16.dp)
        ) {
            // Contact admin card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Contact the Administrator",
                        color = BrandCharcoal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Describe your issue and the admin will respond as soon as possible.",
                        color = BrandCharcoal.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (sent) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Message sent to admin. We'll respond soon.",
                                color = SuccessGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it; errorText = null },
                            label = { Text("Your message") },
                            placeholder = { Text("E.g., My ticket hasn't been resolved in 3 days.") },
                            minLines = 4,
                            enabled = !sending,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        if (errorText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                errorText ?: "",
                                color = ErrorRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (messageText.isBlank()) {
                                    errorText = "Please type a message before sending."
                                    return@Button
                                }
                                sending = true
                                scope.launch {
                                    val ok = notificationRepo.createNotification(
                                        recipientId = "admin",
                                        title = "Support Request from $residentEmail",
                                        message = messageText.trim()
                                    )
                                    sending = false
                                    if (ok) {
                                        sent = true
                                        messageText = ""
                                    } else {
                                        errorText = "Failed to send. Please try again."
                                    }
                                }
                            },
                            enabled = !sending,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (sending) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send Message", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FAQ card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Common Questions",
                        color = BrandCharcoal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FaqItem(
                        question = "How long does a fix take?",
                        answer = "Most issues are resolved within 24 hours. If yours takes longer, an Escalate button appears on the ticket in My Reports."
                    )
                    FaqItem(
                        question = "How does the AI grouping work?",
                        answer = "When multiple residents report similar issues nearby, the system groups them into one incident. This helps dispatchers send one crew instead of many."
                    )
                    FaqItem(
                        question = "Can I cancel a report?",
                        answer = "Yes — while a ticket is still Pending, tap Cancel this report in My Reports. Once a technician has been assigned, cancellation isn't available."
                    )
                    FaqItem(
                        question = "How do I rate a technician?",
                        answer = "After a fix is confirmed, a 5-star rating widget appears on the ticket in My Reports. Your rating helps us improve service."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency contacts card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Direct Agency Contacts",
                            color = Color(0xFF92400E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "For urgent emergencies, contact the agencies directly:",
                        color = Color(0xFF78350F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ContactRow("Water (NCWSC)", "0709 596 000")
                    ContactRow("Electricity (KPLC)", "95551")
                    ContactRow("Waste (Nairobi County)", "0709 026 000")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// One FAQ row
@Composable
private fun FaqItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            question,
            color = BrandCharcoal,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            answer,
            color = BrandCharcoal.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// One contact row
@Composable
private fun ContactRow(label: String, phone: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color(0xFF78350F),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            phone,
            color = Color(0xFF92400E),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}