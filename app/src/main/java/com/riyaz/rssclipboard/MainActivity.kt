package com.riyaz.rssclipboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riyaz.rssclipboard.data.*

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme { ClipboardScreen(onOpenSettings = ::showFloatingSettings) } } }
    override fun onResume() { super.onResume(); if (FloatingPrefs.enabled(this) && Settings.canDrawOverlays(this)) startFloatingService() }
    private fun startFloatingService() { if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS); startForegroundService(Intent(this, FloatingClipboardService::class.java)) }
    private fun showFloatingSettings() {
        setContent { MaterialTheme {
            ClipboardScreen(onOpenSettings = ::showFloatingSettings)
            FloatingSettingsDialog(
                onDismiss = { setContent { MaterialTheme { ClipboardScreen(onOpenSettings = ::showFloatingSettings) } } },
                onEnable = { FloatingPrefs.setEnabled(this, true); if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) else startFloatingService() },
                onDisable = { FloatingPrefs.setEnabled(this, false); stopService(Intent(this, FloatingClipboardService::class.java)) }
            )
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(vm: MainViewModel = viewModel(), onOpenSettings: () -> Unit) {
    val items by vm.visibleItems.collectAsState(); val query by vm.query.collectAsState(); var showClear by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<ClipboardItem?>(null) }; val clipboard = LocalClipboardManager.current
    Scaffold(topBar = { TopAppBar(title = { Text("RSS Clipboard") }, actions = { TextButton(onClick = onOpenSettings) { Text("Floating") }; TextButton(onClick = { showClear = true }) { Text("Clear") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp)) {
            OutlinedTextField(value = query, onValueChange = vm::setQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search clipboard") }); Spacer(Modifier.height(8.dp)); FilterRow(vm); Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) Text("No clipboard items yet", modifier = Modifier.padding(16.dp)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsIndexed(items, key = { _, it -> it.id }) { index, item -> ClipboardCard(index + 1, item, onCopy = { clipboard.setText(AnnotatedString(item.content)) }, onPin = { vm.togglePin(item) }, onDelete = { vm.delete(item) }, onEdit = { editing = item }) } }
        }
    }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, title = { Text("Clear clipboard history?") }, text = { Text("This removes all saved entries, including pinned items.") }, confirmButton = { TextButton(onClick = { vm.clearAll(); showClear = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } })
    editing?.let { item -> var text by remember(item.id) { mutableStateOf(item.content) }; AlertDialog(onDismissRequest = { editing = null }, title = { Text("Edit item") }, text = { OutlinedTextField(text, { text = it }, minLines = 3) }, confirmButton = { TextButton(onClick = { vm.update(item, text); editing = null }) { Text("Save") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }) }
}

@Composable
private fun FloatingSettingsDialog(onDismiss: () -> Unit, onEnable: () -> Unit, onDisable: () -> Unit) {
    val context = LocalContext.current; var enabled by remember { mutableStateOf(FloatingPrefs.enabled(context)) }; var bubble by remember { mutableStateOf(FloatingPrefs.showBubble(context)) }; var openOnCopy by remember { mutableStateOf(FloatingPrefs.openOnCopy(context)) }; var closeAfterCopy by remember { mutableStateOf(FloatingPrefs.closeAfterCopy(context)) }; var autoHide by remember { mutableStateOf(FloatingPrefs.autoHide(context)) }; var hideTimer by remember { mutableStateOf(FloatingPrefs.hideTimerSeconds(context)) }; var size by remember { mutableStateOf(FloatingPrefs.size(context)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Floating clipboard") }, text = { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Runs as a foreground service while enabled.", style = MaterialTheme.typography.bodySmall)
        SettingSwitch("Floating clipboard", enabled) { enabled = it; if (it) onEnable() else onDisable() }
        SettingSwitch("Show floating button", bubble) { bubble = it; FloatingPrefs.setShowBubble(context, it) }
        SettingSwitch("Open list when something is copied", openOnCopy) { openOnCopy = it; FloatingPrefs.setOpenOnCopy(context, it) }
        SettingSwitch("Close after copying an item", closeAfterCopy) { closeAfterCopy = it; FloatingPrefs.setCloseAfterCopy(context, it) }
        SettingSwitch("Auto-hide floating button", autoHide) { autoHide = it; FloatingPrefs.setAutoHide(context, it) }
        Text("Hide timer", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) { listOf(5, 15, 30, 60).forEach { seconds -> FilterChip(selected = hideTimer == seconds, onClick = { hideTimer = seconds; FloatingPrefs.setHideTimerSeconds(context, seconds) }, label = { Text("${seconds}s") }) } }
        Text("Timer applies when Auto-hide is ON. Tap the floating button to reset the timer.", style = MaterialTheme.typography.bodySmall)
        Text("Dialog size", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("small", "medium", "large").forEach { value -> FilterChip(selected = size == value, onClick = { size = value; FloatingPrefs.setSize(context, value) }, label = { Text(value.replaceFirstChar { it.uppercase() }) }) } }
    } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}

@Composable private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onCheckedChange) } }
@Composable private fun FilterRow(vm: MainViewModel) { val filter by vm.filter.collectAsState(); Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { FilterChip(selected = filter == null, onClick = { vm.setFilter(null) }, label = { Text("All") }); FilterChip(selected = filter == ClipboardType.TEXT, onClick = { vm.setFilter(ClipboardType.TEXT) }, label = { Text("Text") }); FilterChip(selected = filter == ClipboardType.URL, onClick = { vm.setFilter(ClipboardType.URL) }, label = { Text("URLs") }); FilterChip(selected = filter == ClipboardType.EMAIL, onClick = { vm.setFilter(ClipboardType.EMAIL) }, label = { Text("Email") }); FilterChip(selected = filter == ClipboardType.PHONE, onClick = { vm.setFilter(ClipboardType.PHONE) }, label = { Text("Phone") }) } }
@Composable private fun ClipboardCard(number: Int, item: ClipboardItem, onCopy: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) { Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("#$number  ${item.type.name}", style = MaterialTheme.typography.labelMedium); Text(if (item.pinned) "PINNED" else "24H", style = MaterialTheme.typography.labelSmall) }; Spacer(Modifier.height(6.dp)); Text(item.content, maxLines = 5, style = MaterialTheme.typography.bodyLarge); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { TextButton(onClick = onCopy) { Text("Copy") }; TextButton(onClick = onPin) { Text(if (item.pinned) "Unpin" else "Pin") }; TextButton(onClick = onEdit) { Text("Edit") }; TextButton(onClick = onDelete) { Text("Delete") } } } } }
