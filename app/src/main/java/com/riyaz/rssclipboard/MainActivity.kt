package com.riyaz.rssclipboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riyaz.rssclipboard.data.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);renderApp()}
    override fun onResume(){super.onResume();if(UserPrefs.isRegistered(this)&&FloatingPrefs.enabled(this))startClipboardService()}
    private fun renderApp(){setContent{RssClipboardTheme(ThemePrefs.get(this)){if(UserPrefs.isRegistered(this))RssApp(::showFloatingSettings,::showThemeSettings,::openBatteryOptimization)else WelcomeScreen{n,e->UserPrefs.register(this,n,e);startClipboardService();renderApp()}}}}
    private fun startClipboardService(){if(Build.VERSION.SDK_INT>=26)startForegroundService(Intent(this,FloatingClipboardService::class.java))else startService(Intent(this,FloatingClipboardService::class.java))}
    private fun showFloatingSettings(){setContent{RssClipboardTheme(ThemePrefs.get(this)){RssApp(::showFloatingSettings,::showThemeSettings,::openBatteryOptimization);FloatingSettingsDialog(::renderApp)}}}
    private fun showThemeSettings(){setContent{RssClipboardTheme(ThemePrefs.get(this)){RssApp(::showFloatingSettings,::showThemeSettings,::openBatteryOptimization);ThemeSelectorDialog(::renderApp)}}}
    private fun openBatteryOptimization(){if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M)return;val pm=getSystemService(PowerManager::class.java);if(pm.isIgnoringBatteryOptimizations(packageName))return;startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})}
}
private enum class RssScreen { CLIPBOARD, SAVED, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RssApp(onOpenFloating:()->Unit,onOpenTheme:()->Unit,onBatteryOptimization:()->Unit){
    var screen by remember{mutableStateOf(RssScreen.CLIPBOARD)}
    val drawerState=rememberDrawerState(DrawerValue.Closed);val scope=rememberCoroutineScope()
    ModalNavigationDrawer(drawerState=drawerState,drawerContent={ModalDrawerSheet{Column(Modifier.fillMaxHeight().padding(18.dp)){
        Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=.82f)),elevation=CardDefaults.cardElevation(defaultElevation=1.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){ComposeImage(painterResource(com.riyaz.rssclipboard.R.drawable.rss_original_logo),"Razeen Secure Solution",Modifier.size(58.dp));Column{Text("Welcome",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(UserPrefs.name(LocalContext.current),style=MaterialTheme.typography.titleMedium);Text(UserPrefs.email(LocalContext.current),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        Spacer(Modifier.height(26.dp))
        NavigationDrawerItem(label={Text("Clipboard")},selected=screen==RssScreen.CLIPBOARD,onClick={screen=RssScreen.CLIPBOARD;scope.launch{drawerState.close()}},icon={ColorfulIcon(Icons.Default.ContentPaste, "Clipboard")})
        NavigationDrawerItem(label={Text("Saved List")},selected=screen==RssScreen.SAVED,onClick={screen=RssScreen.SAVED;scope.launch{drawerState.close()}},icon={ColorfulIcon(Icons.Default.Bookmark, "Saved")})
        NavigationDrawerItem(label={Text("Settings")},selected=screen==RssScreen.SETTINGS,onClick={screen=RssScreen.SETTINGS;scope.launch{drawerState.close()}},icon={ColorfulIcon(Icons.Default.Settings, "Settings")})
        Spacer(Modifier.weight(1f));ComposeImage(painterResource(com.riyaz.rssclipboard.R.drawable.rss_original_logo),"Razeen Secure Solution",Modifier.size(44.dp));Spacer(Modifier.height(6.dp));Text("Razeen Secure Solution",style=MaterialTheme.typography.labelSmall);Text("RSS Clipboard • v1.0.0",style=MaterialTheme.typography.labelSmall)
    }}}){Scaffold(topBar={CenterAlignedTopAppBar(title={Text(when(screen){RssScreen.CLIPBOARD->"Clipboard";RssScreen.SAVED->"Saved List";RssScreen.SETTINGS->"Settings"})},navigationIcon={IconButton({scope.launch{drawerState.open()}}){Icon(Icons.Default.Menu,"Menu")}})},bottomBar={NavigationBar{
        NavigationBarItem(selected=screen==RssScreen.CLIPBOARD,onClick={screen=RssScreen.CLIPBOARD},icon={ColorfulIcon(Icons.Default.ContentPaste, "Clipboard")},label={Text("Clipboard")})
        NavigationBarItem(selected=screen==RssScreen.SAVED,onClick={screen=RssScreen.SAVED},icon={ColorfulIcon(Icons.Default.Bookmark, "Saved")},label={Text("Saved")})
        NavigationBarItem(selected=screen==RssScreen.SETTINGS,onClick={screen=RssScreen.SETTINGS},icon={ColorfulIcon(Icons.Default.Settings, "Settings")},label={Text("Settings")})
    }}){pad->Box(Modifier.fillMaxSize().padding(pad)){AnimatedContent(targetState=screen,label="screen"){target->when(target){RssScreen.CLIPBOARD->ClipboardScreen();RssScreen.SAVED->SavedListScreen();RssScreen.SETTINGS->SettingsScreen(onOpenFloating,onOpenTheme,onBatteryOptimization)}}}}}
}
@Composable private fun WelcomeScreen(onRegister:(String,String)->Unit){
    var name by remember{mutableStateOf("")};var email by remember{mutableStateOf("")}
    val valid=name.trim().length>=2&&android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(.10f)
    val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(.08f)
    val backgroundBrush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.12f),MaterialTheme.colorScheme.surface,MaterialTheme.colorScheme.secondary.copy(.10f)))
    Box(Modifier.fillMaxSize().background(backgroundBrush)){

        Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            ComposeImage(painterResource(com.riyaz.rssclipboard.R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(92.dp))
            Spacer(Modifier.height(18.dp));Text("Welcome to RSS Clipboard",style=MaterialTheme.typography.headlineMedium);Text("Fast, private and organized.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp));Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=1.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("Create your profile",style=MaterialTheme.typography.titleLarge);OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Your name")},leadingIcon={Icon(Icons.Default.Person,null)});OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Email address")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),leadingIcon={Icon(Icons.Default.Email,null)});Button({onRegister(name,email)},enabled=valid,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)){Text("Get started")}}}
            Spacer(Modifier.height(14.dp));Text("History stays on this device and expires after 24 hours.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ClipboardScreen(vm: MainViewModel = viewModel()) {
    val items by vm.visibleItems.collectAsState(); val query by vm.query.collectAsState(); var showClear by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<ClipboardItem?>(null) }; var saving by remember { mutableStateOf<ClipboardItem?>(null) }; val clipboard=LocalClipboardManager.current
    Scaffold(topBar={TopAppBar(title={Text("RSS Clipboard")},actions={IconButton({showClear=true}){Icon(Icons.Default.DeleteSweep,"Clear")}})}) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)) {
            OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search clipboard")}); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilledTonalButton(onClick={clipboard.getText()?.text?.let{if(it.isNotBlank())vm.add(it)}},modifier=Modifier.weight(1f)){Icon(Icons.Default.ContentPaste,null);Spacer(Modifier.width(6.dp));Text("Capture current")};FilterRow(vm)}; Spacer(Modifier.height(8.dp))
            if(items.isEmpty()) Text("No clipboard items yet",Modifier.padding(16.dp)) else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){itemsIndexed(items,key={_,it->it.id}){index,item->ClipboardCard(index+1,item,{clipboard.setText(AnnotatedString(item.content))},{vm.togglePin(item)},{vm.delete(item)},{editing=item},{saving=item})}}
        }
    }
    if(showClear) AlertDialog(onDismissRequest={showClear=false},title={Text("Clear clipboard history?")},text={Text("This removes clipboard history only. Your Saved List is kept permanently.")},confirmButton={TextButton({vm.clearAll();showClear=false}){Text("Clear")}},dismissButton={TextButton({showClear=false}){Text("Cancel")}})
    editing?.let{item->var text by remember(item.id){mutableStateOf(item.content)};AlertDialog(onDismissRequest={editing=null},title={Text("Edit item")},text={OutlinedTextField(text,{text=it},minLines=3)},confirmButton={TextButton({vm.update(item,text);editing=null}){Text("Save")}},dismissButton={TextButton({editing=null}){Text("Cancel")}})}
    saving?.let{item->SaveToListDialog(item.content){saving=null}}
}

@Composable private fun ClipboardCard(
    index: Int, item: ClipboardItem, onCopy: () -> Unit, onPin: () -> Unit,
    onDelete: () -> Unit, onEdit: () -> Unit, onSave: () -> Unit
) {
    Card(shape=RoundedCornerShape(18.dp), colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface), elevation=CardDefaults.cardElevation(defaultElevation=1.dp), modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment=Alignment.Top, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(shape=RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.surfaceVariant) {
                ColorfulIcon(if(item.type==ClipboardType.URL) Icons.Default.Link else if(item.type==ClipboardType.EMAIL) Icons.Default.Email else if(item.type==ClipboardType.PHONE) Icons.Default.Phone else Icons.Default.ContentPaste, item.type.name, modifier=Modifier.padding(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Text("#$index • ${item.type.name}", style=MaterialTheme.typography.labelMedium)
                    Row { IconButton(onClick=onPin){Icon(Icons.Default.PushPin,if(item.pinned)"Unpin" else "Pin")}; IconButton(onClick=onDelete){Icon(Icons.Default.Delete,"Delete")} }
                }
                Text(item.content, Modifier.fillMaxWidth().padding(vertical=6.dp), maxLines=8, style=MaterialTheme.typography.bodyLarge)
                Text("Expires in 24 hours${if(item.pinned) " • Pinned" else ""}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { TextButton(onClick=onCopy){Text("Copy")}; TextButton(onClick=onEdit){Text("Edit")}; TextButton(onClick=onSave){Text("Save")} }
            }
        }
    }
}
@Composable private fun SaveToListDialog(initialData:String,onDismiss:()->Unit){val vm:SavedListViewModel=viewModel();var category by remember{mutableStateOf("General")};var fileName by remember{mutableStateOf("")};var data by remember(initialData){mutableStateOf(initialData)};var description by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Save to List")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Permanent Saved List item. It will not expire with clipboard history.",style=MaterialTheme.typography.bodySmall);OutlinedTextField(category,{category=it},label={Text("Category")},singleLine=true);OutlinedTextField(fileName,{fileName=it},label={Text("File name")},singleLine=true);OutlinedTextField(data,{data=it},label={Text("Data")},minLines=3);OutlinedTextField(description,{description=it},label={Text("Description")},minLines=2)}},confirmButton={TextButton({vm.add(category,fileName,data,description);onDismiss()}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SavedListScreen(vm:SavedListViewModel=viewModel()){val items by vm.visibleItems.collectAsState();val query by vm.query.collectAsState();val categories by vm.categories.collectAsState();val selectedCategory by vm.category.collectAsState();val clipboard=LocalClipboardManager.current;var editing by remember{mutableStateOf<SavedItem?>(null)};var showClear by remember{mutableStateOf(false)};Scaffold(topBar={TopAppBar(title={Text("Saved List")},actions={IconButton({showClear=true}){Icon(Icons.Default.DeleteSweep,"Clear")}})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)){OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search saved list")});Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){FilterChip(selected=selectedCategory==null,onClick={vm.setCategory(null)},label={Text("All")});categories.take(5).forEach{cat->FilterChip(selected=selectedCategory==cat,onClick={vm.setCategory(cat)},label={Text(cat)})}};Spacer(Modifier.height(8.dp));if(items.isEmpty())Text("No saved items yet",Modifier.padding(16.dp))else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items,key={it.id}){item->SavedCard(item,{clipboard.setText(AnnotatedString(item.data))},{editing=item},{vm.delete(item)})}}}};if(showClear)AlertDialog(onDismissRequest={showClear=false},title={Text("Clear Saved List?")},text={Text("This permanently deletes all saved items. Clipboard history is not affected.")},confirmButton={TextButton({vm.clearAll();showClear=false}){Text("Delete all")}},dismissButton={TextButton({showClear=false}){Text("Cancel")}});editing?.let{item->EditSavedDialog(item,{editing=null}){c,n,d,desc->vm.update(item,c,n,d,desc);editing=null}}}

@Composable private fun SavedCard(item:SavedItem,onCopy:()->Unit,onEdit:()->Unit,onDelete:()->Unit){
    Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=1.dp),modifier=Modifier.fillMaxWidth()){
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){ColorfulIcon(Icons.Default.Bookmark, "Saved", modifier=Modifier.padding(10.dp))}
            Column(Modifier.weight(1f)){
                Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(item.category,style=MaterialTheme.typography.labelMedium);Text("PERMANENT",style=MaterialTheme.typography.labelSmall)}
                Text(item.fileName,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(4.dp));Text(item.data,maxLines=5)
                if(item.description.isNotBlank())Text(item.description,style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick=onCopy){Text("Copy")};TextButton(onClick=onEdit){Text("Edit")};TextButton(onClick=onDelete){Text("Delete")}}
            }
        }
    }
}
@Composable private fun EditSavedDialog(item:SavedItem,onDismiss:()->Unit,onSave:(String,String,String,String)->Unit){var c by remember(item.id){mutableStateOf(item.category)};var n by remember(item.id){mutableStateOf(item.fileName)};var d by remember(item.id){mutableStateOf(item.data)};var desc by remember(item.id){mutableStateOf(item.description)};AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Saved Item")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(c,{c=it},label={Text("Category")});OutlinedTextField(n,{n=it},label={Text("File name")});OutlinedTextField(d,{d=it},label={Text("Data")},minLines=3);OutlinedTextField(desc,{desc=it},label={Text("Description")})}},confirmButton={TextButton({onSave(c,n,d,desc)}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(onOpenFloating:()->Unit,onOpenTheme:()->Unit,onBatteryOptimization:()->Unit){
    val context=LocalContext.current
    val batteryOptimized=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) !(context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: false) else false
    Scaffold(topBar={TopAppBar(title={Text("Settings")})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=1.dp),modifier=Modifier.fillMaxWidth()){
            Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
                ComposeImage(painterResource(com.riyaz.rssclipboard.R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(62.dp))
                Column{Text("RSS Clipboard",style=MaterialTheme.typography.titleLarge);Text(UserPrefs.name(context),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
        SettingsRow(Icons.Default.BubbleChart,"Floating Clipboard","Overlay, auto-hide and dialog options",onOpenFloating)
        SettingsRow(Icons.Default.BatteryChargingFull,"Battery Optimization",if(batteryOptimized)"Allow RSS Clipboard to stay active with less background restriction" else "Optimized for always-on background operation",onBatteryOptimization)
        SettingsRow(Icons.Default.Notifications,"Notifications",if(FloatingPrefs.notifications(context))"On — foreground notification preference" else "Off — clipboard capture continues",{FloatingPrefs.setNotifications(context,!FloatingPrefs.notifications(context))})
        SettingsRow(Icons.Default.Palette,"Theme","Windows, Ubuntu, Android, macOS, iOS and more",onOpenTheme)
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(bottom=18.dp),horizontalAlignment=Alignment.CenterHorizontally){
            ComposeImage(painterResource(com.riyaz.rssclipboard.R.drawable.rss_original_logo),"Razeen Secure Solution",Modifier.size(58.dp))
            Spacer(Modifier.height(6.dp));Text("Razeen Secure Solution",style=MaterialTheme.typography.titleSmall)
            Text("RSS • Mobile & PC Software • CCTV • Networking • System Administration",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("www.rsscctvsolution.eu.cc",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }}
}
@Composable private fun SettingsRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){ColorfulIcon(icon,title,modifier=Modifier.size(28.dp));Column{Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}

@Composable private fun ThemeSelectorDialog(onDismiss:()->Unit){val context=LocalContext.current;var selected by remember{mutableStateOf(ThemePrefs.get(context))};AlertDialog(onDismissRequest=onDismiss,title={Text("Theme")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Choose the visual style for RSS Clipboard.",style=MaterialTheme.typography.bodySmall);AppTheme.entries.forEach{theme->FilterChip(selected=selected==theme,onClick={selected=theme;ThemePrefs.set(context,theme);(context as? Activity)?.recreate();onDismiss()},label={Text(theme.label)},modifier=Modifier.fillMaxWidth())}}},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})}

@Composable private fun FloatingSettingsDialog(onDismiss:()->Unit){
    val context=LocalContext.current
    var enabled by remember{mutableStateOf(FloatingPrefs.enabled(context))}
    var bubble by remember{mutableStateOf(FloatingPrefs.showBubble(context))}
    var autoHide by remember{mutableStateOf(FloatingPrefs.autoHide(context))}
    var timer by remember{mutableStateOf(FloatingPrefs.hideTimerSeconds(context))}
    fun restart(){context.stopService(Intent(context,FloatingClipboardService::class.java));if(enabled){if(Build.VERSION.SDK_INT>=26)context.startForegroundService(Intent(context,FloatingClipboardService::class.java))else context.startService(Intent(context,FloatingClipboardService::class.java))}}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Clipboard monitoring")},text={Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        SettingSwitch("Capture new copies",enabled){enabled=it;FloatingPrefs.setEnabled(context,it);restart()}
        SettingSwitch("Show floating shortcut",bubble){bubble=it;FloatingPrefs.setShowBubble(context,it);restart()
            if(it && !Settings.canDrawOverlays(context)){
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+context.packageName)))
            }
        }
        SettingSwitch("Auto-hide shortcut",autoHide){autoHide=it;FloatingPrefs.setAutoHide(context,it);restart()}
        Text("Hide after",style=MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(3,5,10,15).forEach{seconds->FilterChip(timer==seconds,{timer=seconds;FloatingPrefs.setHideTimerSeconds(context,seconds);restart()},label={Text(seconds.toString()+"s")})}}
        Text("The clipboard monitor works without the floating shortcut.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})
}

@Composable private fun ColorfulIcon(icon: ImageVector, key: String, modifier: Modifier = Modifier) {
    val tint = when {
        key.contains("URL", true) || key.contains("LINK", true) -> Color(0xFF1976D2)
        key.contains("EMAIL", true) -> Color(0xFFE53935)
        key.contains("PHONE", true) -> Color(0xFF43A047)
        key.contains("CLIP", true) -> Color(0xFF7E57C2)
        key.contains("SAVE", true) || key.contains("BOOKMARK", true) -> Color(0xFFFFA000)
        key.contains("SETTING", true) -> Color(0xFF00897B)
        key.contains("BATTERY", true) -> Color(0xFF2E7D32)
        key.contains("NOTIF", true) -> Color(0xFFFF7043)
        key.contains("THEME", true) || key.contains("PALETTE", true) -> Color(0xFF8E24AA)
        key.contains("FLOAT", true) || key.contains("BUBBLE", true) -> Color(0xFF039BE5)
        key.contains("HOME", true) -> Color(0xFF3949AB)
        else -> MaterialTheme.colorScheme.primary
    }
    Icon(icon, contentDescription = key, modifier = modifier, tint = tint)
}
@Composable private fun SettingSwitch(label:String,checked:Boolean,onCheckedChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Switch(checked,onCheckedChange)}}
@Composable private fun FilterRow(vm:MainViewModel){val filter by vm.filter.collectAsState();Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){FilterChip(selected=filter==null,onClick={vm.setFilter(null)},label={Text("All")});FilterChip(selected=filter==ClipboardType.TEXT,onClick={vm.setFilter(ClipboardType.TEXT)},label={Text("Text")});FilterChip(selected=filter==ClipboardType.URL,onClick={vm.setFilter(ClipboardType.URL)},label={Text("URLs")});FilterChip(selected=filter==ClipboardType.EMAIL,onClick={vm.setFilter(ClipboardType.EMAIL)},label={Text("Email")});FilterChip(selected=filter==ClipboardType.PHONE,onClick={vm.setFilter(ClipboardType.PHONE)},label={Text("Phone")})}}
