package com.studenthub.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studenthub.app.NotificationSettings
import com.studenthub.app.data.model.AppUser

/**
 * Per-category push-notification toggles. This is the native replacement for the old
 * WebView build's JS `StudentHubAndroid` bridge (build prompt Step 6): there's no WebView
 * anymore, so this screen reads/writes `NotificationSettings.java` (unchanged, already
 * correct) directly instead of going through a JS<->Java bridge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(currentUser: AppUser, onBack: () -> Unit) {
    val context = LocalContext.current

    var appEnabled by remember { mutableStateOf(NotificationSettings.isAppEnabled(context)) }
    var globalEnabled by remember { mutableStateOf(NotificationSettings.isGlobalEnabled(context)) }
    var announcementsEnabled by remember { mutableStateOf(NotificationSettings.isAnnouncementEnabled(context)) }
    var messageMode by remember { mutableStateOf(NotificationSettings.getMessageMode(context)) }
    val classAccess = currentUser.classAccess ?: emptyList()
    val classToggles = remember {
        mutableStateMapOf<String, Boolean>().apply {
            classAccess.forEach { put(it, NotificationSettings.isClassEnabled(context, it)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SwitchRow("Enable notifications", appEnabled) {
                appEnabled = it
                NotificationSettings.setAppEnabled(context, it)
            }
            HorizontalDivider()
            SwitchRow("Global chat", globalEnabled, enabled = appEnabled) {
                globalEnabled = it
                NotificationSettings.setGlobalEnabled(context, it)
            }
            HorizontalDivider()
            SwitchRow("Announcements", announcementsEnabled, enabled = appEnabled) {
                announcementsEnabled = it
                NotificationSettings.setAnnouncementEnabled(context, it)
            }
            HorizontalDivider()

            Text(
                "Class rooms",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            classAccess.forEach { classId ->
                SwitchRow(classId, classToggles[classId] ?: true, enabled = appEnabled) { value ->
                    classToggles[classId] = value
                    NotificationSettings.setClassEnabled(context, classId, value)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Messages", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.padding(top = 4.dp)) {
                MessageModeOption("All messages", "all", messageMode, appEnabled) {
                    messageMode = it
                    NotificationSettings.setMessageMode(context, it)
                }
                MessageModeOption("Mentions & replies only", "mentions", messageMode, appEnabled) {
                    messageMode = it
                    NotificationSettings.setMessageMode(context, it)
                }
                MessageModeOption("Off", "off", messageMode, appEnabled) {
                    messageMode = it
                    NotificationSettings.setMessageMode(context, it)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun MessageModeOption(
    label: String,
    value: String,
    current: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = current == value, onClick = { onSelect(value) }, enabled = enabled)
        Text(label)
    }
}
