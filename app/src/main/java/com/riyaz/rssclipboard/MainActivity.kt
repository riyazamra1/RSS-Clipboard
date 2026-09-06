package com.riyaz.rssclipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riyaz.rssclipboard.data.*

class MainActivity : ComponentActivity() {
    private lateinit var clipboard: ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        setContent { MaterialTheme { ClipboardScreen() } }
    }

    override fun onResume() {
        super.onResume()
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboard.primaryClip ?: return@OnPrimaryClipChangedListener
            if (clip.description.hasMimeType("text/plain") || clip.description.hasMimeType("text/html")) {
                clip.getItemAt(0).coerceToText(this).toString().takeIf { it.isNotBlank() }?.let {
                    (application as? ClipboardApplication)?.lastCaptured = it
                }
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
    }

    override fun onPause() {
        listener?.let { clipboard.removePrimaryClipChangedListener(it) }
        listener = null
        super.onPause()
    }
}

class ClipboardApplication : android.app.Application() { var lastCaptured: String? = null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(vm: MainViewModel = viewModel()) {
    val items by vm.visibleItems.collectAsState()
    val query by vm.query.collectAsState()
    var showClear by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ClipboardItem?>(null) }
    val clipboard = LocalClipboardManager.current

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("RSS Clipboard") },
            actions = { TextButton(onClick = { showClear = true }) { Text("Clear") } }
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Search clipboard") }
            )
            Spacer(Modifier.height(8.dp))
            FilterRow(vm)
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text("No clipboard items yet", modifier = Modifier.padding(16.dp))
            } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                    ClipboardCard(
                        number = index + 1, item = item,
                        onCopy = { clipboard.setText(AnnotatedString(item.content)) },
                        onPin = { vm.togglePin(item) },
                        onDelete = { vm.delete(item) },
                        onEdit = { editing = item }
                    )
                }
            }
        }
    }
    if (showClear) AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("Clear clipboard history?") },
        text = { Text("This removes all saved entries, including pinned items.") },
        confirmButton = { TextButton(onClick = { vm.clearAll(); showClear = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } }
    )
    editing?.let { item ->
        var text by remember(item.id) { mutableStateOf(item.content) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit item") },
            text = { OutlinedTextField(text, { text = it }, minLines = 3) },
            confirmButton = { TextButton(onClick = { vm.update(item, text); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FilterRow(vm: MainViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(selected = true, onClick = { vm.setFilter(null) }, label = { Text("All") })
        FilterChip(selected = false, onClick = { vm.setFilter(ClipboardType.URL) }, label = { Text("URLs") })
        FilterChip(selected = false, onClick = { vm.setFilter(ClipboardType.EMAIL) }, label = { Text("Email") })
        FilterChip(selected = false, onClick = { vm.setFilter(ClipboardType.PHONE) }, label = { Text("Phone") })
    }
}

@Composable
private fun ClipboardCard(number: Int, item: ClipboardItem, onCopy: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("#$number  ${item.type.name}", style = MaterialTheme.typography.labelMedium)
                Text(if (item.pinned) "PINNED" else "24H", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(item.content, maxLines = 5, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onCopy) { Text("Copy") }
                TextButton(onClick = onPin) { Text(if (item.pinned) "Unpin" else "Pin") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
