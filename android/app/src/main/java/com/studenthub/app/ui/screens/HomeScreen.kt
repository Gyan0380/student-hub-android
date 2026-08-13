package com.studenthub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studenthub.app.data.model.User
import com.studenthub.app.ui.viewmodel.HomeViewModel
import com.studenthub.app.util.Slug

private data class RoomEntry(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

private fun roomsFor(user: User): List<RoomEntry> {
    val rooms = mutableListOf(
        RoomEntry("global", "Global Chat", "Everyone in StudentHub", Icons.Filled.Public),
        RoomEntry("anonymous", "Anonymous Chat", "Chat without revealing your identity", Icons.Filled.VisibilityOff)
    )
    val classNames = LinkedHashSet<String>()
    if (user.classLevel.isNotBlank()) classNames.add(user.classLevel)
    classNames.addAll(user.classAccess)
    classNames.forEach { className ->
        rooms.add(
            RoomEntry(
                Slug.classRoomId(className),
                className,
                "Class room",
                Icons.Filled.Groups
            )
        )
    }
    if (user.role == "Admin" || user.role == "Owner") {
        rooms.add(RoomEntry("admin-room", "Admin Room", "Admins & Owners only", Icons.Filled.Groups))
    }
    return rooms
}

@Composable
fun HomeScreen(
    onOpenRoom: (roomId: String, isAnonymous: Boolean, title: String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Text("Hi, ${user?.fullName ?: user?.username ?: "there"} 👋", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Choose a room to start chatting", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))

        val currentUser = user
        if (currentUser == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(roomsFor(currentUser)) { room ->
                    RoomCard(room = room, onClick = {
                        onOpenRoom(room.id, room.id == "anonymous", room.title)
                    })
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: RoomEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(room.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(room.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(room.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
