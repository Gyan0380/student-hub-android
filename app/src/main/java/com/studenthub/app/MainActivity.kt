package com.studenthub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.repo.AuthRepository
import com.studenthub.app.data.repo.UserRepository
import com.studenthub.app.ui.admin.AdminPanelScreen
import com.studenthub.app.ui.admin.AdminViewModel
import com.studenthub.app.ui.auth.ForgotPasswordScreen
import com.studenthub.app.ui.auth.LoginScreen
import com.studenthub.app.ui.auth.RegisterScreen
import com.studenthub.app.ui.bugreport.BugReportScreen
import com.studenthub.app.ui.bugreport.BugReportViewModel
import com.studenthub.app.ui.chat.ChatScreen
import com.studenthub.app.ui.chat.ChatViewModel
import com.studenthub.app.ui.home.HomeScreen
import com.studenthub.app.ui.home.HomeViewModel
import com.studenthub.app.ui.notifications.NotificationsScreen
import com.studenthub.app.ui.notifications.NotificationsViewModel
import com.studenthub.app.ui.profile.ProfileScreen
import com.studenthub.app.ui.profile.ProfileViewModel
import com.studenthub.app.ui.rules.CommunityRulesScreen
import com.studenthub.app.ui.rules.CommunityRulesViewModel
import com.studenthub.app.ui.settings.NotificationSettingsScreen
import com.studenthub.app.ui.suggestions.SuggestionsScreen
import com.studenthub.app.ui.suggestions.SuggestionsViewModel
import com.studenthub.app.ui.theme.StudentHubTheme
import com.studenthub.app.util.AppThemeOption
import com.studenthub.app.util.ThemeStore
import com.studenthub.app.util.UnreadStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Full nav graph: login/register/forgot -> home (room list) -> chat/{roomId} | profile ->
 * notification_settings | notifications. Community rules / suggestions / bug report
 * (step 6) and the admin panel (step 7) are still TODO — see NATIVE_BUILD_PROMPT.md.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeStore = remember { ThemeStore(applicationContext) }
            val theme by themeStore.theme.collectAsState(initial = AppThemeOption.LIGHT)
            StudentHubTheme(option = theme) {
                Surface(modifier = Modifier) {
                    AppNavHost(themeStore = themeStore)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(themeStore: ThemeStore) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val authRepo = remember { AuthRepository() }
    val userRepo = remember { UserRepository() }
    val unreadStore = remember { UnreadStore(context) }
    var currentUser by remember { mutableStateOf<AppUser?>(null) }
    val scope = rememberCoroutineScope()

    // If Firebase Auth already has a persisted session (native default — no WebView
    // logout bug to work around), skip straight past login.
    val startDestination = if (authRepo.currentUid != null) "loading" else "login"

    // Step 8 (NATIVE_BUILD_PROMPT.md): whenever a user session becomes active (fresh
    // login, register, or an already-persisted session picked up via "loading"), fetch
    // the device's current FCM token and store it on Users/{uid}.fcmToken. onNewToken
    // in StudentHubMessagingService covers later token rotations for an already-signed-
    // in user; this covers the initial "token existed before/without a signed-in user"
    // case. Best-effort — failures (offline, etc.) are swallowed in updateFcmToken.
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepo.updateFcmToken(uid, token)
        } catch (e: Exception) {
            // best-effort; ignore (e.g. no network, Play Services unavailable)
        }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("loading") {
            LaunchedEffect(Unit) {
                val uid = authRepo.currentUid
                if (uid == null) {
                    nav.navigate("login") { popUpTo("loading") { inclusive = true } }
                } else {
                    currentUser = userRepo.getUser(uid) ?: AppUser(uid = uid, username = "Student")
                    nav.navigate("home") { popUpTo("loading") { inclusive = true } }
                }
            }
        }

        composable("login") {
            LoginScreen(
                onLoggedIn = {
                    val uid = authRepo.currentUid ?: return@LoginScreen
                    scope.launch {
                        currentUser = userRepo.getUser(uid) ?: AppUser(uid = uid, username = "Student")
                        nav.navigate("home") { popUpTo("login") { inclusive = true } }
                    }
                },
                onGoRegister = { nav.navigate("register") },
                onGoForgotPassword = { nav.navigate("forgot") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistered = {
                    val uid = authRepo.currentUid ?: return@RegisterScreen
                    scope.launch {
                        currentUser = userRepo.getUser(uid) ?: AppUser(uid = uid, username = "Student")
                        nav.navigate("home") { popUpTo("login") { inclusive = true } }
                    }
                },
                onGoLogin = { nav.popBackStack() }
            )
        }

        composable("forgot") {
            ForgotPasswordScreen(onGoLogin = { nav.popBackStack() })
        }

        composable("home") {
            val user = currentUser
            if (user != null) {
                val vm = remember(user.uid) { HomeViewModel(currentUser = user, unreadStore = unreadStore) }
                HomeScreen(
                    vm = vm,
                    onOpenRoom = { roomId -> nav.navigate("chat/$roomId") },
                    onOpenProfile = { nav.navigate("profile") },
                    onOpenNotifications = { nav.navigate("notifications") }
                )
            }
        }

        composable("chat/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "global"
            val user = currentUser
            if (user != null) {
                val vm = remember(roomId) { ChatViewModel(roomId = roomId, currentUser = user) }
                ChatScreen(vm)
            }
        }

        composable("notifications") {
            val user = currentUser
            if (user != null) {
                val vm = remember(user.uid) { NotificationsViewModel(uid = user.uid) }
                NotificationsScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }

        composable("profile") {
            val user = currentUser
            if (user != null) {
                val vm = remember(user.uid) { ProfileViewModel(currentUser = user, themeStore = themeStore) }
                ProfileScreen(
                    vm = vm,
                    currentUser = user,
                    onBack = { nav.popBackStack() },
                    onOpenNotificationSettings = { nav.navigate("notification_settings") },
                    onOpenCommunityRules = { nav.navigate("rules") },
                    onOpenSuggestions = { nav.navigate("suggestions") },
                    onOpenBugReports = { nav.navigate("bugreport") },
                    onOpenAdminPanel = { nav.navigate("admin") },
                    onSignedOut = {
                        currentUser = null
                        nav.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }

        composable("notification_settings") {
            val user = currentUser
            if (user != null) {
                NotificationSettingsScreen(currentUser = user, onBack = { nav.popBackStack() })
            }
        }

        composable("rules") {
            val user = currentUser
            if (user != null) {
                val vm = remember { CommunityRulesViewModel() }
                CommunityRulesScreen(vm = vm, currentUser = user, onBack = { nav.popBackStack() })
            }
        }

        composable("suggestions") {
            val user = currentUser
            if (user != null) {
                val vm = remember(user.uid) { SuggestionsViewModel(currentUser = user) }
                SuggestionsScreen(vm = vm, currentUser = user, onBack = { nav.popBackStack() })
            }
        }

        composable("bugreport") {
            val user = currentUser
            if (user != null) {
                val vm = remember(user.uid) { BugReportViewModel(currentUser = user) }
                BugReportScreen(vm = vm, currentUser = user, onBack = { nav.popBackStack() })
            }
        }

        composable("admin") {
            val user = currentUser
            if (user != null && user.isAdminOrOwner) {
                val vm = remember(user.uid) { AdminViewModel(currentUser = user) }
                AdminPanelScreen(
                    vm = vm,
                    currentUser = user,
                    onBack = { nav.popBackStack() },
                    onOpenCommunityRules = { nav.navigate("rules") },
                    onOpenSuggestions = { nav.navigate("suggestions") },
                    onOpenBugReports = { nav.navigate("bugreport") }
                )
            }
        }
    }
}
