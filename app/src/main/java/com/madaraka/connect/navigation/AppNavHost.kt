package com.madaraka.connect.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.madaraka.connect.ui.auth.AuthViewModel
import com.madaraka.connect.ui.auth.LoginScreen
import com.madaraka.connect.ui.auth.RegisterScreen
import com.madaraka.connect.ui.resident.MasterIncidentsScreen
import com.madaraka.connect.ui.resident.MyReportsScreen
import com.madaraka.connect.ui.resident.ReportFaultScreen
import com.madaraka.connect.ui.resident.ResidentDashboardScreen

private val BrandBlue = Color(0xFF2563EB)
private val BrandCharcoal = Color(0xFF111827)
private val PageBackground = Color(0xFFF8FAFC)
private val ErrorRed = Color(0xFFDC2626)

// Route names used across navigation
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESIDENT_DASHBOARD = "resident_dashboard"
    const val MY_REPORTS = "my_reports"
    const val REPORT_FAULT = "report_fault"
    const val MASTER_INCIDENTS = "master_incidents"
    const val HELP = "help"
    const val DISPATCHER_PLACEHOLDER = "dispatcher_placeholder"
    const val ADMIN_PLACEHOLDER = "admin_placeholder"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = viewModel()
) {
    val currentProfile by viewModel.currentUserProfile.collectAsState()
    val residentId = currentProfile?.uid.orEmpty()
    val role = currentProfile?.role

    // Select start destination from signed-in user's role
    val startDestination = when {
        currentProfile == null -> Routes.LOGIN
        role?.name == "RESIDENT" -> Routes.RESIDENT_DASHBOARD
        role?.name == "DISPATCHER" -> Routes.DISPATCHER_PLACEHOLDER
        role?.name == "ADMIN" -> Routes.ADMIN_PLACEHOLDER
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    val target = when (viewModel.currentUserProfile.value?.role?.name) {
                        "RESIDENT" -> Routes.RESIDENT_DASHBOARD
                        "DISPATCHER" -> Routes.DISPATCHER_PLACEHOLDER
                        "ADMIN" -> Routes.ADMIN_PLACEHOLDER
                        else -> Routes.LOGIN
                    }
                    navController.navigate(target) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateBackToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.RESIDENT_DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        composable(Routes.RESIDENT_DASHBOARD) {
            ResidentDashboardScreen(
                residentId = residentId,
                onReportNewFault = {
                    navController.navigate(Routes.REPORT_FAULT)
                },
                onViewAllReports = { navController.navigate(Routes.MY_REPORTS) },
                onNavigateToSupport = { navController.navigate(Routes.HELP) },
                onNavigateToClusters = { navController.navigate(Routes.MASTER_INCIDENTS) },
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.RESIDENT_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MY_REPORTS) {
            MyReportsScreen(
                residentId = residentId,
                onBackToDashboard = { navController.popBackStack() },
                onNavigateToSupport = { navController.navigate(Routes.HELP) }
            )
        }

        composable(Routes.REPORT_FAULT) {
            ReportFaultScreen(
                residentId = residentId,
                residentEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
                onBack = {
                    navController.popBackStack()
                },
                onReportSubmitted = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MASTER_INCIDENTS) {
            MasterIncidentsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DISPATCHER_PLACEHOLDER) {
            RolePlaceholderScreen(
                title = "Dispatcher Dashboard",
                subtitle = "Handled primarily on the web dashboard",
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DISPATCHER_PLACEHOLDER) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADMIN_PLACEHOLDER) {
            RolePlaceholderScreen(
                title = "Admin Dashboard",
                subtitle = "Handled primarily on the web dashboard",
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ADMIN_PLACEHOLDER) { inclusive = true }
                    }
                }
            )
        }
    }
}

// Temporary fallback screen for roles still in progress
@Composable
private fun RolePlaceholderScreen(
    title: String,
    subtitle: String,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandCharcoal)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = BrandBlue)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSignOut) { Text("Sign Out") }
    }
}

// Placeholder Help and Support screen
@Composable
private fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Help & Support", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandCharcoal)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Support form coming soon.", fontSize = 14.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}