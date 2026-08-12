package com.studenthub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studenthub.app.data.model.User
import com.studenthub.app.ui.viewmodel.AdminViewModel

private val timeoutPresets = listOf(1, 4, 10, 30, 60, 1440)
private fun formatTimeout(mins: Int) = when {
    mins >= 1440 -> "${mins / 1440}d"
    mins >= 60 -> "${mins / 60}h"
    else -> "${mins}m"
}
private val roleOptions = listOf("Student", "Admin", "Owner")

@Composable
fun AdminPanelScreen(viewModel: AdminViewModel = viewModel()) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }
    var deleteRoomId by remember { mutableStateOf("global") }
    var deleteMessageId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Text("Admin Panel", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Send Global Notification", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notifTitle, onValueChange = { notifTitle = it },
                    placeholder = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notifBody, onValueChange = { notifBody = it },
                    placeholder = { Text("Message") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (notifTitle.isNotBlank() && notifBody.isNotBlank()) {
                            viewModel.sendGlobalNotification(notifTitle.trim(), notifBody.trim())
                            notifTitle = ""; notifBody = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("SEND") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Delete a Message", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deleteRoomId, onValueChange = { deleteRoomId = it },
                    placeholder = { Text("Room id (e.g. global)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deleteMessageId, onValueChange = { deleteMessageId = it },
                    placeholder = { Text("Message id") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (deleteMessageId.isNotBlank()) {
                            viewModel.deleteMessage(deleteRoomId.trim(), deleteMessageId.trim())
                            deleteMessageId = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("DELETE MESSAGE", color = MaterialTheme.colorScheme.error) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Users (${users.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users, key = { it.uid }) { user ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("@${user.username} · ${user.role}${if (user.isBanned) " · BANNED" else ""}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                        Text(user.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { selectedUser = user }) {
                            Text("Manage", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    val user = selectedUser
    if (user != null) {
        AlertDialog(
            onDismissRequest = { selectedUser = null },
            confirmButton = {
                TextButton(onClick = { selectedUser = null }) { Text("Close") }
            },
            title = { Text("@${user.username}") },
            text = {
                Column {
                    Text("Change role", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        roleOptions.forEach { role ->
                            FilterChip(
                                selected = user.role == role,
                                onClick = { viewModel.setUserRole(user.uid, role) },
                                label = { Text(role) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Timeout", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        timeoutPresets.forEach { preset ->
                            AssistChip(
                                onClick = { viewModel.timeoutUser(user.uid, preset) },
                                label = { Text(formatTimeout(preset)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.setBanned(user.uid, !user.isBanned)
                            selectedUser = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (user.isBanned) "Unban user" else "Ban user")
                    }
                }
            }
        )
    }
}
