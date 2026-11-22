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
import com.madaraka.connect.ui.auth.AuthViewModel
import com.madaraka.connect.ui.auth.LoginScreen
import com.madaraka.connect.ui.auth.RegisterScreen
import com.madaraka.connect.ui.resident.MyReportsScreen
import com.madaraka.connect.ui.resident.ResidentDashboardScreen

// All route names used by the app
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESIDENT_DASHBOARD = "resident_dashboard"
    const val MY_REPORTS = "my_reports"
    const val HELP = "help"
    const val DISPATCHER_PLACEHOLDER = "dispatcher_placeholder"
    const val ADMIN_PLACEHOLDER = "admin_placeholder"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = viewModel()
) {
    // Read the profile to know who is signed in and what role
    val currentProfile by viewModel.currentUserProfile.collectAsState()
    val residentId = currentProfile?.uid.orEmpty()
    val role = currentProfile?.role

    // Choose the start destination based on role
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
                onReportNewFault = { /* Phase 4C: navigate to ReportFault screen */ },
                onViewAllReports = { navController.navigate(Routes.MY_REPORTS) },
                onNavigateToSupport = { navController.navigate(Routes.HELP) },
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

        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DISPATCHER_PLACEHOLDER) {
            RolePlaceholderScreen(
                title = "Dispatcher Dashboard",
                subtitle = "Coming in Phase 4E",
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
                subtitle = "Coming in Phase 4F",
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

@Composable
private fun RolePlaceholderScreen(
    title: String,
    subtitle: String,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = Color(0xFF2563EB))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSignOut) { Text("Sign Out") }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Help & Support", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Support form coming soon.", fontSize = 14.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}