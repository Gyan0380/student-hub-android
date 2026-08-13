package com.studenthub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.studenthub.app.data.model.ChatMessage
import com.studenthub.app.data.model.ReplyTo
import com.studenthub.app.ui.viewmodel.ChatViewModel
import com.studenthub.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class ChatViewModelFactory(
    private val roomId: String,
    private val isAnonymous: Boolean
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(roomId, isAnonymous) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    roomTitle: String,
    isAnonymous: Boolean,
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(roomId, isAnonymous)
    )
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val sendError by chatViewModel.sendError.collectAsStateWithLifecycle()
    val currentUser by homeViewModel.currentUser.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ReplyTo?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val isMuted = currentUser?.timeoutExpiry?.let {
        it.toDate().time > System.currentTimeMillis()
    } ?: false

    val isBanned = currentUser?.isBanned ?: false

    val timeLeftLabel = remember(currentUser?.timeoutExpiry) {
        currentUser?.timeoutExpiry?.let { formatTimeLeft(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = roomTitle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },

                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },

        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp)
            ) {

                if (isBanned) {

                    Text(
                        text = "You are banned from chatting.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )

                } else if (isMuted) {

                    Text(
                        text = "You are muted${timeLeftLabel?.let { " for $it" } ?: ""}.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )

                } else {

                    replyTarget?.let { reply ->

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = "Replying to ${reply.senderName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    Text(
                                        text = reply.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        replyTarget = null
                                        chatViewModel.replyTarget = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Cancel reply",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    sendError?.let { error ->

                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OutlinedTextField(
                            value = input,

                            onValueChange = {
                                input = it
                                chatViewModel.clearError()
                            },

                            modifier = Modifier.weight(1f),

                            placeholder = {
                                Text("Type a message...")
                            },

                            shape = RoundedCornerShape(24.dp),

                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),

                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant,

                                focusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        FilledIconButton(
                            onClick = {

                                val name =
                                    if (isAnonymous) {
                                        "Anonymous Ninja"
                                    } else {
                                        currentUser
                                            ?.fullName
                                            ?.ifBlank {
                                                currentUser.username
                                            }
                                            ?: "Student"
                                    }

                                chatViewModel.sendMessage(
                                    input.trim(),
                                    name,
                                    currentUser?.profilePhoto ?: ""
                                )

                                input = ""
                                replyTarget = null
                            },

                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary
                            ),

                            enabled = input.isNotBlank()
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        LazyColumn(
            state = listState,

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),

            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(
                items = messages,
                key = { it.id }
            ) { message ->

                MessageBubble(
                    message = message,

                    isMine = message.senderId ==
                        (currentUser?.uid ?: ""),

                    onReply = {

                        val target = ReplyTo(
                            senderName = message.senderName,
                            text = message.text
                        )

                        replyTarget = target
                        chatViewModel.replyTarget = target
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onReply: () -> Unit
) {

    val bubbleColor =
        if (isMine) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            if (isMine) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {

            if (!isMine) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (message.senderPhoto.isNotBlank()) {

                        AsyncImage(
                            model = message.senderPhoto,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,

                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )
                    }

                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(
                    modifier = Modifier.height(2.dp)
                )
            }

            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    message.replyTo?.let { reply ->

                        Surface(
                            color = MaterialTheme.colorScheme.background
                                .copy(alpha = 0.4f),

                            shape = RoundedCornerShape(8.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(6.dp)
                            ) {

                                Text(
                                    text = reply.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Text(
                                    text = reply.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )
                    }

                    if (message.photoUrl.isNotBlank()) {

                        AsyncImage(
                            model = message.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )
                    }

                    if (message.text.isNotBlank()) {

                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = SimpleDateFormat(
                                "HH:mm",
                                Locale.getDefault()
                            ).format(
                                Date(message.createdAtMillis)
                            ),

                            style = MaterialTheme.typography.labelSmall,

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = onReply,
                            contentPadding = PaddingValues(0.dp)
                        ) {

                            Text(
                                text = "Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeLeft(
    expiry: Timestamp
): String {

    val millisLeft =
        expiry.toDate().time - System.currentTimeMillis()

    if (millisLeft <= 0) {
        return ""
    }

    val minutes =
        millisLeft / 60000

    return if (minutes >= 60) {
        "${minutes / 60}h"
    } else {
        "${minutes.coerceAtLeast(1)}m"
    }
}
