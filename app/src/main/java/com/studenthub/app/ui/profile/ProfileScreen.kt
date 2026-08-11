package com.studenthub.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.util.AppThemeOption
import com.studenthub.app.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    currentUser: AppUser,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenCommunityRules: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenBugReports: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val bio by vm.bio.collectAsState()
    val pendingPhoto by vm.pendingPhotoBase64.collectAsState()
    val saving by vm.saving.collectAsState()
    val saved by vm.saved.collectAsState()
    val theme by vm.theme.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ImageUtils.uriToCompressedBase64(context, uri, maxDimension = 512)?.let {
                vm.onPhotoPicked(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                val shown = pendingPhoto ?: currentUser.profilePhoto
                if (shown != null) {
                    AsyncImage(model = shown, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                }
            }
            Text("Tap to change photo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(20.dp))
            Text(currentUser.username, style = MaterialTheme.typography.titleLarge)
            Text(currentUser.role, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = vm::onBioChange,
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(24.dp))
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ThemeRow(current = theme, onSelect = vm::setTheme)

            Spacer(Modifier.height(24.dp))
            Button(onClick = vm::save, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (saving) "Saving…" else "Save changes")
            }
            if (saved) {
                Text(
                    "Saved ✓",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Notification settings")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenCommunityRules, modifier = Modifier.fillMaxWidth()) {
                Text("Community rules")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenSuggestions, modifier = Modifier.fillMaxWidth()) {
                Text("Suggestions")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenBugReports, modifier = Modifier.fillMaxWidth()) {
                Text("Report a bug")
            }
            if (currentUser.isAdminOrOwner) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenAdminPanel, modifier = Modifier.fillMaxWidth()) {
                    Text("Admin panel")
                }
            }

            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.signOut(); onSignedOut() }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun ThemeRow(current: AppThemeOption, onSelect: (AppThemeOption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeSwatch("Light", AppThemeOption.LIGHT, current, onSelect)
        ThemeSwatch("Dark", AppThemeOption.DARK, current, onSelect)
        ThemeSwatch("Sepia", AppThemeOption.SEPIA, current, onSelect)
        ThemeSwatch("Ocean", AppThemeOption.OCEAN, current, onSelect)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSwatch(
    label: String,
    option: AppThemeOption,
    current: AppThemeOption,
    onSelect: (AppThemeOption) -> Unit
) {
    val selected = option == current
    FilterChip(
        selected = selected,
        onClick = { onSelect(option) },
        label = { Text(label) }
    )
}
