package com.riyaz.rssclipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ClipItem(val text: String, val type: String, val time: String)

class ClipboardActivity : ComponentActivity() {
    private lateinit var clipboard: ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var onCaptured: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        startClipboardCapture()
        setContent { App() }
    }

    private fun startClipboardCapture() {
        val intent = Intent(this, ClipboardCaptureService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }

    override fun onResume() {
        super.onResume()
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
            if (!text.isNullOrEmpty()) onCaptured?.invoke(text)
        }
        clipboard.addPrimaryClipChangedListener(listener)
    }

    override fun onPause() {
        listener?.let { clipboard.removePrimaryClipChangedListener(it) }
        listener = null
        super.onPause()
    }

    private fun saveCaptured(text: String) {
        val prefs = getSharedPreferences("rss_clipboard", MODE_PRIVATE)
        val current = prefs.getStringSet("clips", emptySet())?.toMutableSet() ?: mutableSetOf()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val type = when {
            android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches() -> "Email"
            android.util.Patterns.WEB_URL.matcher(text).matches() -> "URL"
            else -> "Text"
        }
        current.removeAll { it.substringBefore("|") == text }
        current.add("$text|$type|$stamp")
        prefs.edit().putStringSet("clips", current).apply()
    }

    @Composable
    private fun App() {
        var splash by remember { mutableStateOf(true) }
        var registered by remember {
            mutableStateOf(getSharedPreferences("rss_clipboard", MODE_PRIVATE).getBoolean("registered", false))
        }
        var stage by remember { mutableStateOf(if (registered) "welcome" else "register") }
        var clips by remember { mutableStateOf(loadClips()) }
        var drawer by remember { mutableStateOf(false) }

        onCaptured = { text ->
            saveCaptured(text)
            clips = loadClips()
        }

        LaunchedEffect(Unit) { delay(900); splash = false }

        MaterialTheme(colorScheme = lightColorScheme(
            primary = Color(0xFFB4862E),
            onPrimary = Color.White,
            surface = Color(0xFFF8F7F3),
            background = Color(0xFFF8F7F3)
        )) {
            if (splash) SplashScreen() else when (stage) {
                "register" -> RegisterScreen {
                    getSharedPreferences("rss_clipboard", MODE_PRIVATE).edit()
                        .putBoolean("registered", true).putString("name", it).apply()
                    registered = true
                    stage = "welcome"
                }
                "welcome" -> WelcomeScreen { stage = "features" }
                "features" -> FeaturesScreen { stage = "main" }
                "main" -> MainScreen(clips, { clips = loadClips() }, { drawer = true })
                "settings" -> SettingsScreen { stage = "main" }
                "about" -> AboutScreen { stage = "main" }
            }
            if (drawer) Drawer(
                onClose = { drawer = false },
                onHome = { drawer = false; stage = "main" },
                onSettings = { drawer = false; stage = "settings" },
                onAbout = { drawer = false; stage = "about" }
            )
        }
    }

    private fun loadClips(): List<ClipItem> {
        return getSharedPreferences("rss_clipboard", MODE_PRIVATE)
            .getStringSet("clips", emptySet()).orEmpty()
            .mapNotNull {
                val p = it.split("|", limit = 3)
                if (p.size == 3) ClipItem(p[0], p[1], p[2]) else null
            }.sortedByDescending { it.time }
    }

    @Composable private fun Logo(modifier: Modifier = Modifier) {
        Image(
            painter = painterResource(id = R.drawable.rss_clipboard_logo),
            contentDescription = "RSS Clipboard logo",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }

    @Composable private fun SplashScreen() {
        Box(Modifier.fillMaxSize().background(Color(0xFFF8F7F3)), contentAlignment = Alignment.Center) {
            Logo(Modifier.size(180.dp))
        }
    }

    @Composable private fun RegisterScreen(onDone: (String) -> Unit) {
        var name by remember { mutableStateOf("") }
        Box(Modifier.fillMaxSize().background(Color(0xFFF8F7F3)).padding(28.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Logo(Modifier.size(120.dp))
                Spacer(Modifier.height(24.dp))
                Text("Create your account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Set up RSS Clipboard on this device.", color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                Button(onClick = { if (name.isNotBlank()) onDone(name.trim()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }
        }
    }

    @Composable private fun WelcomeScreen(onNext: () -> Unit) {
        Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(Modifier.size(140.dp))
                Spacer(Modifier.height(20.dp))
                Text("Welcome to RSS Clipboard", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Keep useful copied text organized in one lightweight clipboard.", color = Color.Gray)
                Spacer(Modifier.height(28.dp))
                Button(onClick = onNext) { Text("See features") }
            }
        }
    }

    @Composable private fun FeaturesScreen(onNext: () -> Unit) {
        val features = listOf(
            Triple(Icons.Default.ContentCopy, "Clipboard history", "Capture clipboard changes even when RSS Clipboard is closed."),
            Triple(Icons.Default.Link, "Smart types", "Separate text, URLs and email addresses."),
            Triple(Icons.Default.Security, "Private by design", "Data stays on this device unless a future backup feature is explicitly enabled.")
        )
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("What you get", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            features.forEach { (icon, title, body) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 7.dp), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = Color(0xFFB4862E), modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(16.dp))
                        Column { Text(title, fontWeight = FontWeight.Bold); Text(body, color = Color.Gray) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onNext, Modifier.fillMaxWidth()) { Text("Open RSS Clipboard") }
        }
    }

    @Composable private fun MainScreen(clips: List<ClipItem>, refresh: () -> Unit, openDrawer: () -> Unit) {
        var query by remember { mutableStateOf("") }
        var listDialog by remember { mutableStateOf(false) }
        var selectedText by remember { mutableStateOf("") }
        fun showSaveToList(text: String) { selectedText = text; listDialog = true }
        val filtered = clips.filter { it.text.contains(query, true) }
        if (listDialog) {
            val lists = loadLists()
            AlertDialog(
                onDismissRequest = { listDialog = false },
                title = { Text("Save to list") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (lists.isEmpty()) Text("No lists yet. Create one in Settings.")
                        lists.forEach { name ->
                            TextButton(onClick = { saveToList(name, selectedText); listDialog = false }) { Text(name) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { listDialog = false }) { Text("Close") } }
            )
        }
        Scaffold(topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Logo(Modifier.size(38.dp)); Spacer(Modifier.width(10.dp)); Text("RSS Clipboard") } },
                navigationIcon = { IconButton(openDrawer) { Icon(Icons.Default.Menu, "Menu") } }
            )
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search clipboard") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                Spacer(Modifier.height(12.dp))
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(48.dp), tint = Color.Gray)
                            Text("No clipboard items yet", fontWeight = FontWeight.Bold)
                            Text("Copy text anywhere. RSS Clipboard saves it automatically.", color = Color.Gray)
                        }
                    }
                } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered) { item ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (item.type == "URL") Icons.Default.Link else if (item.type == "Email") Icons.Default.Email else Icons.Default.TextFields, null, tint = Color(0xFFB4862E))
                                    Spacer(Modifier.width(8.dp)); Text(item.type, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(item.time, fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(item.text, maxLines = 5)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        clipboard.setPrimaryClip(ClipData.newPlainText("RSS Clipboard", item.text))
                                        refresh()
                                    }) { Text("Copy again") }
                                    TextButton(onClick = { showSaveToList(item.text) }) { Text("Save to list") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun Drawer(onClose: () -> Unit, onHome: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit) {
        ModalDrawerSheet {
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(24.dp)) { Logo(Modifier.size(72.dp)); Text("RSS Clipboard", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            NavigationDrawerItem(label = { Text("Clipboard") }, selected = false, onClick = onHome, icon = { Icon(Icons.Default.ContentCopy, null) })
            NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = onSettings, icon = { Icon(Icons.Default.Settings, null) })
            NavigationDrawerItem(label = { Text("About") }, selected = false, onClick = onAbout, icon = { Icon(Icons.Default.Info, null) })
            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            Text("Razeen Secure Solution", Modifier.padding(24.dp), fontWeight = FontWeight.SemiBold)
        }
    }

    private fun loadLists(): List<String> =
        getSharedPreferences("rss_clipboard", MODE_PRIVATE).getStringSet("clip_lists", emptySet()).orEmpty().sorted()

    private fun saveToList(name: String, text: String) {
        val prefs = getSharedPreferences("rss_clipboard", MODE_PRIVATE)
        val key = "list_" + name
        val values = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        values.add(text)
        prefs.edit().putStringSet(key, values).apply()
    }

    @Composable private fun SettingsScreen(back: () -> Unit) {
        Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { pad ->
            Column(Modifier.padding(pad).padding(20.dp)) {
                Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("The current build uses a clean light RSS interface.", color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                Text("Clipboard capture", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Background capture is enabled. RSS Clipboard monitors supported clipboard changes even when the app screen is closed. Android requires an ongoing foreground-service notification for continuous monitoring.", color = Color.Gray)
            }
        }
    }

    @Composable private fun AboutScreen(back: () -> Unit) {
        Scaffold(topBar = { TopAppBar(title = { Text("About RSS Clipboard") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { pad ->
            Column(Modifier.padding(pad).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(Modifier.size(110.dp))
                Spacer(Modifier.height(18.dp))
                Text("RSS Clipboard", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Lightweight clipboard organizer", color = Color.Gray)
                Spacer(Modifier.height(18.dp))
                Text("Razeen Secure Solution", fontWeight = FontWeight.SemiBold)
                Text("rsscctvsolution@gmail.com", color = Color.Gray)
            }
        }
    }
}
