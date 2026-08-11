package com.studenthub.app.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studenthub.app.data.model.AppUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityRulesScreen(vm: CommunityRulesViewModel, currentUser: AppUser, onBack: () -> Unit) {
    val rules by vm.rules.collectAsState()
    val saving by vm.saving.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var draft by remember(rules, editing) { mutableStateOf(rules) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Rules") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } },
                actions = {
                    if (currentUser.isAdminOrOwner) {
                        TextButton(onClick = { editing = !editing }) {
                            Text(if (editing) "Cancel" else "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = { Text("Rules") }
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.save(draft); editing = false },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (saving) "Saving…" else "Save rules") }
            } else if (rules.isBlank()) {
                Text("No community rules have been posted yet.")
            } else {
                Text(rules)
            }
        }
    }
}
