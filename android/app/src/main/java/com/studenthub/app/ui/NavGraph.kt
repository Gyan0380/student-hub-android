package com.studenthub.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studenthub.app.ui.screens.AdminPanelScreen
import com.studenthub.app.ui.screens.BugReportScreen
import com.studenthub.app.ui.screens.ChatScreen
import com.studenthub.app.ui.screens.HomeScreen
import com.studenthub.app.ui.screens.LoginScreen
import com.studenthub.app.ui.screens.NotificationsScreen
import com.studenthub.app.ui.screens.ProfileScreen
import com.studenthub.app.ui.screens.RegisterScreen
import com.studenthub.app.ui.screens.RulesScreen
import com.studenthub.app.ui.screens.SplashScreen
import com.studenthub.app.ui.screens.SuggestionsScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val NOTIFICATIONS = "notifications"
    const val RULES = "rules"
    const val PROFILE = "profile"
    const val SUGGESTIONS = "suggestions"
    const val BUG_REPORT = "bug_report"
    const val ADMIN = "admin"
    const val CHAT = "chat/{roomId}/{title}/{anon}"

    fun chat(roomId: String, title: String, isAnonymous: Boolean): String =
        "chat/$roomId/${URLEncoder.encode(title, "UTF-8")}/$isAnonymous"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications),
    Tab(Routes.RULES, "Rules", Icons.Filled.Info),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

@Composable
fun StudentHubNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = startDestination) {
                appRoutes(navController)
            }
        }
    }
}

private fun NavGraphBuilder.appRoutes(navController: NavHostController) {
    composable(Routes.SPLASH) {
        SplashScreen(onFinished = {
            val signedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
            navController.navigate(if (signedIn) Routes.HOME else Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        })
    }

    composable(Routes.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            },
            onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
        )
    }

    composable(Routes.REGISTER) {
        RegisterScreen(
            onRegisterSuccess = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            },
            onNavigateToLogin = { navController.popBackStack() },
        )
    }

    composable(Routes.HOME) {
        HomeScreen(onOpenRoom = { roomId, isAnonymous, title ->
            navController.navigate(Routes.chat(roomId, title, isAnonymous))
        })
    }

    composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
    composable(Routes.RULES) { RulesScreen() }
    composable(Routes.SUGGESTIONS) { SuggestionsScreen() }
    composable(Routes.BUG_REPORT) { BugReportScreen() }
    composable(Routes.ADMIN) { AdminPanelScreen() }

    composable(Routes.PROFILE) {
        ProfileScreen(onLoggedOut = {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        })
    }

    composable(Routes.CHAT) { entry ->
        val roomId = entry.arguments?.getString("roomId").orEmpty()
        val title = URLDecoder.decode(entry.arguments?.getString("title").orEmpty(), "UTF-8")
        val anon = entry.arguments?.getString("anon") == "true"
        ChatScreen(
            roomId = roomId,
            roomTitle = title,
            isAnonymous = anon,
            onBack = { navController.popBackStack() },
        )
    }
}
