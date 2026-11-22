package com.madaraka.connect.ui.resident

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
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
import com.madaraka.connect.data.models.MasterIncident
import com.madaraka.connect.data.repositories.MasterIncidentsRepository
import com.madaraka.connect.ui.common.CategoryIcon
import com.madaraka.connect.ui.common.StatusBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)

// ViewModel holding master incident clusters for display
class MasterIncidentsViewModel(
    private val repository: MasterIncidentsRepository = MasterIncidentsRepository()
) : ViewModel() {

    // List of master incidents from Firestore
    private val _incidents = MutableStateFlow<List<MasterIncident>>(emptyList())
    val incidents: StateFlow<List<MasterIncident>> = _incidents.asStateFlow()

    // Loading state while fetching clusters
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Load master incidents from Firestore
    fun loadIncidents() {
        _isLoading.value = true
        viewModelScope.launch {
            _incidents.value = repository.getAllMasterIncidents()
            _isLoading.value = false
        }
    }
}

@Composable
fun MasterIncidentsScreen(
    onBack: () -> Unit = {},
    viewModel: MasterIncidentsViewModel = viewModel()
) {
    val incidents by viewModel.incidents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Fetch clusters when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadIncidents()
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = BrandCharcoal
                            )
                        }
                        Text(
                            text = "Grouped Incidents",
                            color = BrandCharcoal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = { viewModel.loadIncidents() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = BrandCharcoal
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && incidents.isEmpty()) {
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

        if (incidents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = BrandCharcoal.copy(alpha = 0.35f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No grouped incident clusters yet.",
                        color = BrandCharcoal.copy(alpha = 0.65f),
                        fontSize = 14.sp
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(incidents, key = { it.id }) { incident ->
                MasterIncidentCard(incident = incident)
            }
        }
    }
}

// Card showing one clustered incident and report count
@Composable
private fun MasterIncidentCard(incident: MasterIncident) {
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
                CategoryIcon(category = incident.category, size = 44)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incident.areaName?.takeIf { it.isNotBlank() }
                            ?: incident.category.displayName,
                        color = BrandCharcoal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = incident.category.displayName,
                        color = BrandCharcoal.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
                StatusBadge(status = incident.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            val countLabel = if (incident.isCluster) {
                "${incident.reportCount} reports grouped"
            } else {
                "${incident.reportCount} report linked"
            }
            Text(
                text = countLabel,
                color = BrandBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}