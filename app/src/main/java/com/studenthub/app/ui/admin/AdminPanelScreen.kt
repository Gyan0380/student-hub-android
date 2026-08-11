package com.studenthub.app.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studenthub.app.data.model.AppTag
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.CLASS_ROOM_OPTIONS
import com.studenthub.app.ui.theme.glassMorphism // <-- Glassmorphism Theme Import

private val ROLE_OPTIONS = listOf("Student", "Admin", "Owner")
private val TIMEOUT_HOUR_OPTIONS = listOf(1, 6, 24, 72)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    vm: AdminViewModel,
    currentUser: AppUser,
    onBack: () -> Unit,
    onOpenCommunityRules: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenBugReports: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val errorMessage by vm.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                TextButton(onClick = onOpenCommunityRules) { Text("Rules") }
                TextButton(onClick = onOpenSuggestions) { Text("Suggestions") }
                TextButton(onClick = onOpenBugReports) { Text("Bug reports") }
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Users") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Tags") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Anti-Abuse") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Notify") })
            }

            errorMessage?.let { msg ->
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::clearError) { Text("✖") }
                }
            }

            when (tab) {
                0 -> UsersTab(vm, currentUser)
                1 -> TagsTab(vm)
                2 -> AntiAbuseTab(vm)
                3 -> NotifyTab(vm)
            }
        }
    }
}

// ---------- Users ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersTab(vm: AdminViewModel, currentUser: AppUser) {
    val query by vm.userQuery.collectAsState()
    // Recompute on every users/query emission.
    vm.users.collectAsState()
    val users = vm.filteredUsers()
    var expandedUid by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = vm::onUserQueryChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            placeholder = { Text("Search by username…") },
            singleLine = true
        )
        LazyColumn(Modifier.weight(1f)) {
            items(users, key = { it.uid }) { u ->
                UserRow(
                    user = u,
                    currentUser = currentUser,
                    expanded = expandedUid == u.uid,
                    onToggleExpand = { expandedUid = if (expandedUid == u.uid) null else u.uid },
                    vm = vm
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserRow(
    user: AppUser,
    currentUser: AppUser,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    vm: AdminViewModel
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .glassMorphism() // <-- Glassmorphism applied to User Row
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.titleSmall)
                val badges = buildList {
                    add(user.role)
                    if (user.isBanned) add("Banned")
                    if (user.isTimedOut) add("Timed out")
                }
                Text(badges.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Text(if (expanded) "▲" else "▼")
        }

        if (expanded) {
            Column(Modifier.padding(top = 12.dp)) {
                // Role
                Text("Role", style = MaterialTheme.typography.labelMedium)
                Row {
                    ROLE_OPTIONS.forEach { role ->
                        val isOwnerChange = role == "Owner" || user.role == "Owner"
                        FilterChip(
                            selected = user.role == role,
                            onClick = { vm.setRole(user, role) },
                            label = { Text(role) },
                            enabled = !isOwnerChange || currentUser.isOwner,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ban user", modifier = Modifier.weight(1f))
                    Switch(checked = user.isBanned, onCheckedChange = { vm.setBanned(user, it) })
                }

                Spacer(Modifier.height(6.dp))
                Text("Timeout", style = MaterialTheme.typography.labelMedium)
                Row {
                    TIMEOUT_HOUR_OPTIONS.forEach { hrs ->
                        TextButton(onClick = { vm.setTimeout(user, hrs) }) { Text("${hrs}h") }
                    }
                    TextButton(onClick = { vm.setTimeout(user, null) }) { Text("Clear") }
                }

                Spacer(Modifier.height(6.dp))
                Text("Class access", style = MaterialTheme.typography.labelMedium)
                val access = user.classAccess ?: emptyList()
                LazyRow {
                    items(CLASS_ROOM_OPTIONS) { room ->
                        FilterChip(
                            selected = access.contains(room),
                            onClick = {
                                val updated = if (access.contains(room)) access - room else access + room
                                vm.setClassAccess(user, updated)
                            },
                            label = { Text(room, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Tags", style = MaterialTheme.typography.labelMedium)
                val tags by vm.tags.collectAsState()
                LazyRow {
                    items(tags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = (user.tags ?: emptyList()).contains(tag.id),
                            onClick = { vm.toggleTagOnUser(user, tag.id) },
                            label = { Text(tag.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------- Tags ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsTab(vm: AdminViewModel) {
    val tags by vm.tags.collectAsState()
    var label by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().glassMorphism(), // <-- Glassmorphism applied to Tag Input
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = label, onValueChange = { label = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("New tag label (e.g. Verified)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { vm.createTag(label, "#2563EB"); label = "" }) { Text("Add") }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(tags, key = { it.id }) { tag -> 
                TagRow(tag, onDelete = { vm.deleteTag(tag.id) }) 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagRow(tag: AppTag, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .glassMorphism(), // <-- Glassmorphism applied to each Tag item
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tag.label, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
    }
}

// ---------- Anti-Abuse ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AntiAbuseTab(vm: AdminViewModel) {
    val words by vm.antiAbuseWords.collectAsState()
    var newWord by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Blocked words are filtered/flagged in chat.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().glassMorphism(), // <-- Glassmorphism applied to Input
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newWord, onValueChange = { newWord = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add word…") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { vm.addAntiAbuseWord(newWord); newWord = "" }) { Text("Add") }
        }
        
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(words) { word ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .glassMorphism(), // <-- Glassmorphism applied to each Word item
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(word, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { vm.removeAntiAbuseWord(word) }) { 
                        Text("Remove", color = MaterialTheme.colorScheme.error) 
                    }
                }
            }
        }
    }
}

// ---------- Notify ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyTab(vm: AdminViewModel) {
    val sending by vm.sending.collectAsState()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Broadcast a push notification to all users.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism() // <-- Glassmorphism applied to Notification Form
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = body, onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Message") }, minLines = 3
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.sendNotification(title, body, "all"); title = ""; body = "" },
                enabled = !sending && (title.isNotBlank() || body.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (sending) "Sending…" else "Send to all users") }
        }
    }
}
