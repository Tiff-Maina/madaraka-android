package com.madaraka.connect.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.madaraka.connect.ui.admin.AdminDashboardScreen
import com.madaraka.connect.ui.auth.AuthViewModel
import com.madaraka.connect.ui.auth.LoginScreen
import com.madaraka.connect.ui.auth.RegisterScreen
import com.madaraka.connect.ui.dispatcher.DispatcherAnalyticsScreen
import com.madaraka.connect.ui.dispatcher.DispatcherDashboardScreen
import com.madaraka.connect.ui.resident.HelpSupportScreen
import com.madaraka.connect.ui.resident.MasterIncidentsScreen
import com.madaraka.connect.ui.resident.MyReportsScreen
import com.madaraka.connect.ui.resident.ReportFaultScreen
import com.madaraka.connect.ui.resident.ResidentDashboardScreen

// Route names used across navigation
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESIDENT_DASHBOARD = "resident_dashboard"
    const val MY_REPORTS = "my_reports"
    const val REPORT_FAULT = "report_fault"
    const val MASTER_INCIDENTS = "master_incidents"
    const val HELP = "help"
    const val DISPATCHER_DASHBOARD = "dispatcher_dashboard"
    const val DISPATCHER_ANALYTICS = "dispatcher_analytics"
    const val ADMIN_DASHBOARD = "admin_dashboard"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AuthViewModel = viewModel()
) {
    val currentProfile by viewModel.currentUserProfile.collectAsState()
    val residentId = currentProfile?.uid.orEmpty()
    val role = currentProfile?.role
    val department = currentProfile?.department.orEmpty()
    val agencyName = currentProfile?.agencyName
        ?: currentProfile?.email
        ?: "Dispatcher"
    val adminEmail = currentProfile?.email.orEmpty()

    val startDestination = when {
        currentProfile == null -> Routes.LOGIN
        role?.name == "RESIDENT" -> Routes.RESIDENT_DASHBOARD
        role?.name == "DISPATCHER" -> Routes.DISPATCHER_DASHBOARD
        role?.name == "ADMIN" -> Routes.ADMIN_DASHBOARD
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    val target = when (viewModel.currentUserProfile.value?.role?.name) {
                        "RESIDENT" -> Routes.RESIDENT_DASHBOARD
                        "DISPATCHER" -> Routes.DISPATCHER_DASHBOARD
                        "ADMIN" -> Routes.ADMIN_DASHBOARD
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

        // ----- RESIDENT ROUTES -----

        composable(Routes.RESIDENT_DASHBOARD) {
            ResidentDashboardScreen(
                residentId = residentId,
                onReportNewFault = { navController.navigate(Routes.REPORT_FAULT) },
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
                onBackToDashboard = {
                    navController.navigate(Routes.RESIDENT_DASHBOARD) {
                        popUpTo(Routes.RESIDENT_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToClusters = { navController.navigate(Routes.MASTER_INCIDENTS) },
                onNavigateToSupport = { navController.navigate(Routes.HELP) }
            )
        }

        composable(Routes.REPORT_FAULT) {
            ReportFaultScreen(
                residentId = residentId,
                residentEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
                onBack = { navController.popBackStack() },
                onReportSubmitted = { navController.popBackStack() }
            )
        }

        composable(Routes.MASTER_INCIDENTS) {
            MasterIncidentsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HELP) {
            HelpSupportScreen(
                residentId = residentId,
                residentEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
                onBack = {
                    navController.navigate(Routes.RESIDENT_DASHBOARD) {
                        popUpTo(Routes.RESIDENT_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        // ----- DISPATCHER ROUTES -----

        composable(Routes.DISPATCHER_DASHBOARD) {
            DispatcherDashboardScreen(
                department = department,
                agencyName = agencyName,
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DISPATCHER_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToAnalytics = { navController.navigate(Routes.DISPATCHER_ANALYTICS) }
            )
        }

        composable(Routes.DISPATCHER_ANALYTICS) {
            DispatcherAnalyticsScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.DISPATCHER_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        // ----- ADMIN ROUTES -----

        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                adminEmail = adminEmail,
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ADMIN_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}