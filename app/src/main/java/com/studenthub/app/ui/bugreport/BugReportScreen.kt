package com.studenthub.app.ui.bugreport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studenthub.app.data.model.AppUser
import com.studenthub.app.data.model.BugReport
import com.studenthub.app.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(vm: BugReportViewModel, currentUser: AppUser, onBack: () -> Unit) {
    val context = LocalContext.current
    val reports by vm.reports.collectAsState()
    val pendingPhotos by vm.pendingPhotos.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val submitted by vm.submitted.collectAsState()
    var text by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = vm.maxPhotos)
    ) { uris -> uris.forEach { uri -> ImageUtils.uriToCompressedBase64(context, uri)?.let(vm::addPhoto) } }

    LaunchedEffect(submitted) { if (submitted) { text = ""; vm.resetSubmitted() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Bug") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Found something broken? Describe it below.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
                placeholder = { Text("What went wrong?") }
            )
            Spacer(Modifier.height(8.dp))

            if (pendingPhotos.isNotEmpty()) {
                LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    itemsIndexed(pendingPhotos) { index, photo ->
                        Box(Modifier.padding(end = 8.dp)) {
                            AsyncImage(
                                model = photo, contentDescription = "Attached photo",
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                            )
                            TextButton(
                                onClick = { vm.removePhoto(index) },
                                modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                            ) { Text("✖", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = pendingPhotos.size < vm.maxPhotos
            ) { Text("📷 Attach photos (${pendingPhotos.size}/${vm.maxPhotos})") }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.submit(text) },
                enabled = !submitting && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (submitting) "Sending…" else "Submit report") }

            if (currentUser.isAdminOrOwner) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Text(
                    "All bug reports (${reports.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(reports, key = { it.id }) { r -> BugReportRow(r, onDelete = { vm.delete(r.id) }) }
                }
            }
        }
    }
}

@Composable
private fun BugReportRow(r: BugReport, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.username, style = MaterialTheme.typography.labelMedium)
                Text(r.text, style = MaterialTheme.typography.bodyMedium)
                if (r.photos.isNotEmpty()) {
                    LazyRow(Modifier.padding(top = 6.dp)) {
                        items(r.photos) { photo ->
                            AsyncImage(
                                model = photo, contentDescription = "Bug photo",
                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
