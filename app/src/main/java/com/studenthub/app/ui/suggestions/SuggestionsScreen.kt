package com.studenthub.app.ui.suggestions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.Suggestion
import com.studenthub.app.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(vm: SuggestionsViewModel, currentUser: AppUser, onBack: () -> Unit) {
    val context = LocalContext.current
    val suggestions by vm.suggestions.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val submitted by vm.submitted.collectAsState()
    var text by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photo = ImageUtils.uriToCompressedBase64(context, uri) }

    LaunchedEffect(submitted) {
        if (submitted) { text = ""; photo = null; vm.resetSubmitted() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggestions") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Got an idea to improve StudentHub? Tell us.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
                placeholder = { Text("Your suggestion…") }
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (photo == null) "📷 Attach photo" else "📷 Change photo") }
                photo?.let {
                    AsyncImage(
                        model = it, contentDescription = "Attached photo",
                        modifier = Modifier.padding(start = 8.dp).size(40.dp).clip(RoundedCornerShape(6.dp))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.submit(text, photo) },
                enabled = !submitting && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (submitting) "Sending…" else "Submit suggestion") }

            if (currentUser.isAdminOrOwner) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Text(
                    "All suggestions (${suggestions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(suggestions, key = { it.id }) { s -> SuggestionRow(s, onDelete = { vm.delete(s.id) }) }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(s: Suggestion, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.username, style = MaterialTheme.typography.labelMedium)
                Text(s.text, style = MaterialTheme.typography.bodyMedium)
                s.photo?.let {
                    AsyncImage(
                        model = it, contentDescription = "Suggestion photo",
                        modifier = Modifier.padding(top = 6.dp).size(80.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
