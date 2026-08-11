package com.studenthub.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studenthub.app.data.model.Message
import com.studenthub.app.util.ImageUtils
import com.studenthub.app.ui.theme.glassMorphism // <-- Glassmorphism Theme Import Kiya Gaya Hai

private val MENTION_REGEX = Regex("(?<![\\w.])@\\w+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val context = LocalContext.current
    val messages by vm.messages.collectAsState()
    val replyTo by vm.replyTo.collectAsState()
    val pendingPhotos by vm.pendingPhotos.collectAsState()
    var input by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<Message?>(null) }
    var editing by remember { mutableStateOf<Message?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = vm.maxPhotos)
    ) { uris ->
        uris.forEach { uri ->
            ImageUtils.uriToCompressedBase64(context, uri)?.let { vm.addPhoto(it) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(8.dp)) {
            items(messages, key = { it.id }) { m ->
                MessageBubble(
                    message = m,
                    mentioned = vm.isMentioned(m),
                    onTap = { if (vm.canOpenMenu(m)) menuFor = m }
                )
            }
        }

        replyTo?.let { r ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("↩️ Replying to ${r.senderName}", Modifier.weight(1f))
                TextButton(onClick = { vm.clearReply() }) { Text("✖") }
            }
        }

        if (pendingPhotos.isNotEmpty()) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                itemsIndexed(pendingPhotos) { index, photo ->
                    Box(Modifier.padding(end = 8.dp)) {
                        AsyncImage(
                            model = photo,
                            contentDescription = "Attached photo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        IconButton(
                            onClick = { vm.removePhoto(index) },
                            modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                        ) {
                            Text("✖", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = pendingPhotos.size < vm.maxPhotos
            ) {
                Text("📷")
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message… (@username to mention)") }
            )
            IconButton(onClick = {
                if (editing != null) {
                    vm.edit(editing!!.id, input); editing = null
                } else {
                    vm.send(input)
                }
                input = ""
            }) { Text("➤") }
        }
    }

    // Tap-to-open action sheet: only Reply is always available; Edit only on your own
    // messages; Delete on your own messages or (as "(mod)") any message for Admin/Owner.
    menuFor?.let { m ->
        ModalBottomSheet(onDismissRequest = { menuFor = null }) {
            Column(Modifier.padding(16.dp)) {
                TextButton(onClick = {
                    vm.setReply(m); menuFor = null
                }) { Text("↩️ Reply") }

                if (vm.canEdit(m)) {
                    TextButton(onClick = {
                        editing = m; input = m.text; menuFor = null
                    }) { Text("✏️ Edit") }
                }

                if (vm.canDelete(m)) {
                    TextButton(onClick = {
                        vm.delete(m.id); menuFor = null
                    }) { Text(if (vm.canEdit(m)) "🗑️ Delete" else "🗑️ Delete (mod)") }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, mentioned: Boolean, onTap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            // Glassmorphism modifier apply kiya gaya hai messages ke upar
            .glassMorphism(
                backgroundAlpha = if (mentioned) 0.3f else 0.15f,
                borderAlpha = if (mentioned) 0.6f else 0.2f
            ),
        // Card ka background transparent set kiya gaya hai taaki Glass theme dikh sake
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onTap
    ) {
        // Padding adjust ki gayi hai kyunki glassMorphism mein already 16dp padding hai
        Column(Modifier.padding(0.dp)) {
            Text(message.senderName, style = MaterialTheme.typography.labelMedium)
            message.replyTo?.let {
                Text("↩️ ${it.senderName}: ${it.text.take(40)}", style = MaterialTheme.typography.bodySmall)
            }
            if (message.text.isNotBlank()) {
                Text(buildAnnotatedString {
                    append(highlightMentions(message.text))
                    if (message.edited) append("  (edited)")
                })
            }
            message.photos?.let { photos ->
                if (photos.isNotEmpty()) {
                    LazyRow(Modifier.padding(top = 6.dp)) {
                        items(photos) { photo ->
                            AsyncImage(
                                model = photo,
                                contentDescription = "Message photo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Bolds/colors `@username` tokens — same visual rule the web build uses for mentions. */
private fun highlightMentions(text: String) = buildAnnotatedString {
    var last = 0
    for (match in MENTION_REGEX.findAll(text)) {
        append(text.substring(last, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))) {
            append(match.value)
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}
