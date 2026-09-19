package com.riyaz.rssclipboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.ui.res.painterResource

@Composable
fun RssWelcomeExperience(onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ComposeImage(painterResource(R.drawable.rss_clipboard_logo), "RSS Clipboard", Modifier.size(104.dp))
        Spacer(Modifier.height(20.dp))
        Text("Congratulations!", style = MaterialTheme.typography.headlineMedium)
        Text("Welcome to RSS Clipboard", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text("Your RSS Clipboard experience is ready.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Explore App Features")
        }
    }
}

@Composable
fun RssFeaturesScreen() {
    val features = listOf(
        Triple(Icons.Default.ContentPaste, "24-hour Clipboard", "Keep recent clipboard history available for quick reuse."),
        Triple(Icons.Default.Bookmark, "Permanent Saved List", "Save important content separately from temporary history."),
        Triple(Icons.Default.Search, "Smart Search & Filters", "Find text, URLs, email addresses and phone numbers quickly."),
        Triple(Icons.Default.BubbleChart, "Floating Shortcut", "Access clipboard controls from other apps when enabled."),
        Triple(Icons.Default.Palette, "Light / Dark / System", "Use a consistent appearance across the application."),
        Triple(Icons.Default.Security, "Privacy First", "Clipboard history is designed to stay on this device.")
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("RSS Clipboard Features", style = MaterialTheme.typography.headlineSmall)
            Text("Everything included in this lightweight clipboard manager.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        items(features) { (icon, title, description) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ColorfulIcon(icon, title, Modifier.size(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun RssAboutScreen() {
    RssInfoScreen(
        title = "About RSS Clipboard",
        icon = Icons.Default.Info,
        body = listOf(
            "RSS Clipboard",
            "A lightweight clipboard manager by Razeen Secure Solution.",
            "Version 1.0.0",
            "Package: com.riyaz.rssclipboard",
            "Developed by Razeen Secure Solution since 2015."
        )
    )
}

@Composable
fun RssContactScreen() {
    RssInfoScreen(
        title = "Contact",
        icon = Icons.Default.ContactMail,
        body = listOf(
            "Razeen Secure Solution",
            "Mobile & PC software development",
            "CCTV camera installation",
            "Networking & System Administration",
            "Email: rsscctvsolution@gmail.com",
            "Mobile: 077 115 5504 | 070 155 5504",
            "Website: www.rsscctvsolution.eu.cc"
        )
    )
}

@Composable
fun RssPrivacyScreen() {
    RssInfoScreen(
        title = "Privacy Policy",
        icon = Icons.Default.PrivacyTip,
        body = listOf(
            "RSS Clipboard is designed with local-first privacy in mind.",
            "Clipboard history is stored on the device and expires after 24 hours.",
            "Items explicitly saved to the Saved List are retained until the user deletes them.",
            "The app does not require clipboard content to be sent to a remote server for its core clipboard functions.",
            "Users should avoid storing sensitive information in clipboard history or the Saved List unless they understand the associated device-security risks."
        )
    )
}

@Composable
fun RssTermsScreen() {
    RssInfoScreen(
        title = "Terms & Conditions",
        icon = Icons.Default.Description,
        body = listOf(
            "RSS Clipboard is provided for personal clipboard organization and productivity.",
            "Use the application responsibly and do not intentionally store or distribute unlawful or harmful content.",
            "You are responsible for the information you place in clipboard history or the Saved List.",
            "Features that require Android permissions, such as floating controls, depend on the permissions granted by the user.",
            "Razeen Secure Solution may update application features and these terms as the project evolves."
        )
    )
}

@Composable
private fun RssInfoScreen(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, body: List<String>) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ColorfulIcon(icon, title, Modifier.size(34.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
        }
        items(body) { paragraph ->
            Card(Modifier.fillMaxWidth()) {
                Text(paragraph, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
