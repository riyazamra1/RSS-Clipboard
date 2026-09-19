package com.riyaz.rssclipboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun RssWelcomeExperience(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Image(painterResource(R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(104.dp))
        Spacer(Modifier.height(20.dp))
        Text("Welcome to RSS Clipboard",style=MaterialTheme.typography.headlineMedium)
        Text("Your clipboard experience is ready.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick=onContinue,modifier=Modifier.fillMaxWidth().height(52.dp)){Text("Explore App Features")}
    }
}

@Composable
fun RssFeaturesScreen(){
    val features=listOf(
        Triple(Icons.Default.ContentPaste,"24-hour Clipboard","Automatically capture recent clipboard history for quick reuse."),
        Triple(Icons.Default.Bookmark,"Permanent Saved List","Save important content separately from expiring history."),
        Triple(Icons.Default.Search,"Smart Search & Filters","Find text, URLs, emails and phone numbers quickly."),
        Triple(Icons.Default.BubbleChart,"Floating Shortcut","Access clipboard controls above other apps when overlay access is enabled."),
        Triple(Icons.Default.Palette,"Light / Dark / System","Keep the visual appearance consistent across the application."),
        Triple(Icons.Default.Security,"Privacy First","Clipboard history is stored locally and expires after 24 hours.")
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("RSS Clipboard Features",style=MaterialTheme.typography.headlineSmall);Text("Everything included in this lightweight clipboard manager.",style=MaterialTheme.typography.bodyMedium)}
        items(features){(icon,title,description)->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){ColorfulIcon(icon,title,Modifier.size(30.dp));Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
    }
}

@Composable fun RssAboutScreen()=RssInfoScreen("About RSS Clipboard",Icons.Default.Info,listOf("RSS Clipboard","A lightweight clipboard manager by Razeen Secure Solution.","Version 1.0.0","Package: com.riyaz.rssclipboard"))
@Composable fun RssContactScreen()=RssInfoScreen("Contact",Icons.Default.ContactMail,listOf("Razeen Secure Solution","Mobile & PC software development","CCTV camera installation","Networking & System Administration","Email: rsscctvsolution@gmail.com","Mobile: 077 115 5504 | 070 155 5504","Website: www.rsscctvsolution.eu.cc"))
@Composable fun RssPrivacyScreen()=RssInfoScreen("Privacy Policy",Icons.Default.PrivacyTip,listOf("RSS Clipboard is designed with local-first privacy in mind.","Clipboard history is stored on the device and expires after 24 hours.","Items explicitly saved to the Saved List are retained until the user deletes them.","Core clipboard functions do not require clipboard content to be sent to a remote server."))
@Composable fun RssTermsScreen()=RssInfoScreen("Terms & Conditions",Icons.Default.Description,listOf("RSS Clipboard is provided for personal clipboard organization and productivity.","You are responsible for information placed in clipboard history or the Saved List.","Features that require Android permissions depend on the permissions granted by the user."))

@Composable private fun RssInfoScreen(title:String,icon:ImageVector,body:List<String>){
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){ColorfulIcon(icon,title,Modifier.size(34.dp));Text(title,style=MaterialTheme.typography.headlineSmall)}}
        items(body){paragraph->Card(Modifier.fillMaxWidth()){Text(paragraph,Modifier.padding(18.dp),style=MaterialTheme.typography.bodyLarge)}}
    }
}

@Composable private fun ColorfulIcon(icon:ImageVector,key:String,modifier:Modifier=Modifier){
    val tint=when{
        key.contains("URL",true)||key.contains("LINK",true)->Color(0xFF1976D2)
        key.contains("EMAIL",true)->Color(0xFFE53935)
        key.contains("PHONE",true)->Color(0xFF43A047)
        key.contains("CLIP",true)||key.contains("PASTE",true)->Color(0xFF7E57C2)
        key.contains("SAVE",true)||key.contains("BOOKMARK",true)->Color(0xFFFFA000)
        key.contains("SETTING",true)->Color(0xFF00897B)
        key.contains("THEME",true)||key.contains("PALETTE",true)->Color(0xFF8E24AA)
        else->MaterialTheme.colorScheme.primary
    }
    Icon(icon,key,modifier,tint)
}
