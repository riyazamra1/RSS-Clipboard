package com.riyaz.rssclipboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riyaz.rssclipboard.data.*

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startClipboardService() else FloatingPrefs.setEnabled(this, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); renderApp() }

    override fun onResume() {
        super.onResume()
        if (UserPrefs.isRegistered(this) && FloatingPrefs.enabled(this)) startClipboardService()
    }

    private fun renderApp() {
        setContent {
            RssClipboardTheme(ThemePrefs.get(this)) {
                if (UserPrefs.isRegistered(this)) {
                    RssApp(::showFloatingSettings, ::showThemeSettings, ::openBatteryOptimization)
                } else {
                    WelcomeScreen { name, email ->
                        UserPrefs.register(this, name, email)
                        startClipboardService()
                        renderApp()
                    }
                }
            }
        }
    }

    private fun startClipboardService() {
        if (!FloatingPrefs.notifications(this)) return
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, FloatingClipboardService::class.java))
        else startService(Intent(this, FloatingClipboardService::class.java))
    }

    private fun showFloatingSettings() {
        setContent { RssClipboardTheme(ThemePrefs.get(this)) {
            RssApp(::showFloatingSettings, ::showThemeSettings, ::openBatteryOptimization)
            FloatingSettingsDialog(::renderApp)
        }}
    }

    private fun showThemeSettings() {
        setContent { RssClipboardTheme(ThemePrefs.get(this)) {
            RssApp(::showFloatingSettings, ::showThemeSettings, ::openBatteryOptimization)
            ThemeSelectorDialog(::renderApp)
        }}
    }

    private fun openBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        })
    }
}
private enum class RssScreen { CLIPBOARD, SAVED, SETTINGS }

@Composable private fun RssApp(onOpenFloating: () -> Unit, onOpenTheme: () -> Unit, onBatteryOptimization: () -> Unit) {
    var screen by remember { mutableStateOf(RssScreen.CLIPBOARD) }
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected=screen==RssScreen.CLIPBOARD,onClick={screen=RssScreen.CLIPBOARD},icon={Icon(Icons.Default.ContentPaste,"Clipboard")},label={Text("Clipboard")})
            NavigationBarItem(selected=screen==RssScreen.SAVED,onClick={screen=RssScreen.SAVED},icon={Icon(Icons.Default.Bookmark,"Saved")},label={Text("Saved")})
            NavigationBarItem(selected=screen==RssScreen.SETTINGS,onClick={screen=RssScreen.SETTINGS},icon={Icon(Icons.Default.Settings,"Settings")},label={Text("Settings")})
        }
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when(screen) {
                RssScreen.CLIPBOARD -> ClipboardScreen()
                RssScreen.SAVED -> SavedListScreen()
                RssScreen.SETTINGS -> SettingsScreen(onOpenFloating,onOpenTheme,onBatteryOptimization)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ClipboardScreen(vm: MainViewModel = viewModel()) {
    val items by vm.visibleItems.collectAsState(); val query by vm.query.collectAsState(); var showClear by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<ClipboardItem?>(null) }; var saving by remember { mutableStateOf<ClipboardItem?>(null) }; val clipboard=LocalClipboardManager.current
    Scaffold(topBar={TopAppBar(title={Text("RSS Clipboard")},actions={IconButton({showClear=true}){Icon(Icons.Default.DeleteSweep,"Clear")}})}) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)) {
            OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search clipboard")}); Spacer(Modifier.height(8.dp)); FilterRow(vm); Spacer(Modifier.height(8.dp))
            if(items.isEmpty()) Text("No clipboard items yet",Modifier.padding(16.dp)) else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){itemsIndexed(items,key={_,it->it.id}){index,item->ClipboardCard(index+1,item,{clipboard.setText(AnnotatedString(item.content))},{vm.togglePin(item)},{vm.delete(item)},{editing=item},{saving=item})}}
        }
    }
    if(showClear) AlertDialog(onDismissRequest={showClear=false},title={Text("Clear clipboard history?")},text={Text("This removes clipboard history only. Your Saved List is kept permanently.")},confirmButton={TextButton({vm.clearAll();showClear=false}){Text("Clear")}},dismissButton={TextButton({showClear=false}){Text("Cancel")}})
    editing?.let{item->var text by remember(item.id){mutableStateOf(item.content)};AlertDialog(onDismissRequest={editing=null},title={Text("Edit item")},text={OutlinedTextField(text,{text=it},minLines=3)},confirmButton={TextButton({vm.update(item,text);editing=null}){Text("Save")}},dismissButton={TextButton({editing=null}){Text("Cancel")}})}
    saving?.let{item->SaveToListDialog(item.content){saving=null}}
}

@Composable private fun SaveToListDialog(initialData:String,onDismiss:()->Unit){val vm:SavedListViewModel=viewModel();var category by remember{mutableStateOf("General")};var fileName by remember{mutableStateOf("")};var data by remember(initialData){mutableStateOf(initialData)};var description by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Save to List")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Permanent Saved List item. It will not expire with clipboard history.",style=MaterialTheme.typography.bodySmall);OutlinedTextField(category,{category=it},label={Text("Category")},singleLine=true);OutlinedTextField(fileName,{fileName=it},label={Text("File name")},singleLine=true);OutlinedTextField(data,{data=it},label={Text("Data")},minLines=3);OutlinedTextField(description,{description=it},label={Text("Description")},minLines=2)}},confirmButton={TextButton({vm.add(category,fileName,data,description);onDismiss()}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SavedListScreen(vm:SavedListViewModel=viewModel()){val items by vm.visibleItems.collectAsState();val query by vm.query.collectAsState();val categories by vm.categories.collectAsState();val selectedCategory by vm.category.collectAsState();val clipboard=LocalClipboardManager.current;var editing by remember{mutableStateOf<SavedItem?>(null)};var showClear by remember{mutableStateOf(false)};Scaffold(topBar={TopAppBar(title={Text("Saved List")},actions={IconButton({showClear=true}){Icon(Icons.Default.DeleteSweep,"Clear")}})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)){OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search saved list")});Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){FilterChip(selected=selectedCategory==null,onClick={vm.setCategory(null)},label={Text("All")});categories.take(5).forEach{cat->FilterChip(selected=selectedCategory==cat,onClick={vm.setCategory(cat)},label={Text(cat)})}};Spacer(Modifier.height(8.dp));if(items.isEmpty())Text("No saved items yet",Modifier.padding(16.dp))else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items,key={it.id}){item->SavedCard(item,{clipboard.setText(AnnotatedString(item.data))},{editing=item},{vm.delete(item)})}}}};if(showClear)AlertDialog(onDismissRequest={showClear=false},title={Text("Clear Saved List?")},text={Text("This permanently deletes all saved items. Clipboard history is not affected.")},confirmButton={TextButton({vm.clearAll();showClear=false}){Text("Delete all")}},dismissButton={TextButton({showClear=false}){Text("Cancel")}});editing?.let{item->EditSavedDialog(item,{editing=null}){c,n,d,desc->vm.update(item,c,n,d,desc);editing=null}}}

@Composable private fun SavedCard(item:SavedItem,onCopy:()->Unit,onEdit:()->Unit,onDelete:()->Unit){Card(shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(item.category,style=MaterialTheme.typography.labelMedium);Text("PERMANENT",style=MaterialTheme.typography.labelSmall)};Text(item.fileName,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(4.dp));Text(item.data,maxLines=5);if(item.description.isNotBlank())Text(item.description,style=MaterialTheme.typography.bodySmall);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick=onCopy){Text("Copy")};TextButton(onClick=onEdit){Text("Edit")};TextButton(onClick=onDelete){Text("Delete")}}}}}

@Composable private fun EditSavedDialog(item:SavedItem,onDismiss:()->Unit,onSave:(String,String,String,String)->Unit){var c by remember(item.id){mutableStateOf(item.category)};var n by remember(item.id){mutableStateOf(item.fileName)};var d by remember(item.id){mutableStateOf(item.data)};var desc by remember(item.id){mutableStateOf(item.description)};AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Saved Item")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(c,{c=it},label={Text("Category")});OutlinedTextField(n,{n=it},label={Text("File name")});OutlinedTextField(d,{d=it},label={Text("Data")},minLines=3);OutlinedTextField(desc,{desc=it},label={Text("Description")})}},confirmButton={TextButton({onSave(c,n,d,desc)}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable private fun SettingsScreen(onOpenFloating:()->Unit,onOpenTheme:()->Unit,onBatteryOptimization:()->Unit){
    val context=LocalContext.current
    val batteryOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) !(context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: false) else false
    Scaffold(topBar={TopAppBar(title={Text("Settings")})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        SettingsRow(Icons.Default.BubbleChart,"Floating Clipboard", "Overlay, auto-hide and dialog options",onOpenFloating)
        SettingsRow(Icons.Default.BatteryChargingFull,"Battery Optimization", if (batteryOptimized) "Allow RSS Clipboard to stay active with less background restriction" else "Optimized for always-on background operation",onBatteryOptimization)
        SettingsRow(Icons.Default.Notifications,"Notifications", if (FloatingPrefs.notifications(context)) "On — required while Floating Clipboard is active" else "Off — Floating Clipboard will remain stopped",{
            val next=!FloatingPrefs.notifications(context); FloatingPrefs.setNotifications(context,next)
            if (!next) { FloatingPrefs.setEnabled(context,false); context.stopService(Intent(context,FloatingClipboardService::class.java)) }
        })
        SettingsRow(Icons.Default.Palette,"Theme", "Windows, Ubuntu, Android, macOS, iOS and more",onOpenTheme)
    }}
}
@Composable private fun SettingsRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){Icon(icon,title);Column{Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall)}}}}

@Composable private fun ThemeSelectorDialog(onDismiss:()->Unit){val context=LocalContext.current;var selected by remember{mutableStateOf(ThemePrefs.get(context))};AlertDialog(onDismissRequest=onDismiss,title={Text("Theme")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Choose the visual style for RSS Clipboard.",style=MaterialTheme.typography.bodySmall);AppTheme.entries.forEach{theme->FilterChip(selected=selected==theme,onClick={selected=theme;ThemePrefs.set(context,theme);(context as? Activity)?.recreate();onDismiss()},label={Text(theme.label)},modifier=Modifier.fillMaxWidth())}}},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})}

@Composable private fun FloatingSettingsDialog(onDismiss:()->Unit,onEnable:()->Unit,onDisable:()->Unit){val context=LocalContext.current;var enabled by remember{mutableStateOf(FloatingPrefs.enabled(context))};var bubble by remember{mutableStateOf(FloatingPrefs.showBubble(context))};var openOnCopy by remember{mutableStateOf(FloatingPrefs.openOnCopy(context))};var closeAfterCopy by remember{mutableStateOf(FloatingPrefs.closeAfterCopy(context))};var autoHide by remember{mutableStateOf(FloatingPrefs.autoHide(context))};var hideTimer by remember{mutableStateOf(FloatingPrefs.hideTimerSeconds(context))};var size by remember{mutableStateOf(FloatingPrefs.size(context))};AlertDialog(onDismissRequest=onDismiss,title={Text("Floating clipboard")},text={Column(verticalArrangement=Arrangement.spacedBy(2.dp)){Text("Runs as a foreground service while enabled. A visible service notification is required.",style=MaterialTheme.typography.bodySmall);SettingSwitch("Floating clipboard",enabled){enabled=it;if(it)onEnable()else onDisable()};SettingSwitch("Show floating button",bubble){bubble=it;FloatingPrefs.setShowBubble(context,it)};SettingSwitch("Open list when something is copied",openOnCopy){openOnCopy=it;FloatingPrefs.setOpenOnCopy(context,it)};SettingSwitch("Close after copying an item",closeAfterCopy){closeAfterCopy=it;FloatingPrefs.setCloseAfterCopy(context,it)};SettingSwitch("Auto-hide floating button",autoHide){autoHide=it;FloatingPrefs.setAutoHide(context,it)};SettingSwitch("Notifications",FloatingPrefs.notifications(context)){value->FloatingPrefs.setNotifications(context,value);if(!value){FloatingPrefs.setEnabled(context,false);context.stopService(Intent(context,FloatingClipboardService::class.java))}};Text("Hide timer",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf(5,15,30,60).forEach{seconds->FilterChip(selected=hideTimer==seconds,onClick={hideTimer=seconds;FloatingPrefs.setHideTimerSeconds(context,seconds)},label={Text("${seconds}s")})}};Text("Dialog size",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("small","medium","large").forEach{value->FilterChip(selected=size==value,onClick={size=value;FloatingPrefs.setSize(context,value)},label={Text(value.replaceFirstChar{it.uppercase()})})}}}},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})}
@Composable private fun SettingSwitch(label:String,checked:Boolean,onCheckedChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Switch(checked,onCheckedChange)}}
@Composable private fun FilterRow(vm:MainViewModel){val filter by vm.filter.collectAsState();Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){FilterChip(selected=filter==null,onClick={vm.setFilter(null)},label={Text("All")});FilterChip(selected=filter==ClipboardType.TEXT,onClick={vm.setFilter(ClipboardType.TEXT)},label={Text("Text")});FilterChip(selected=filter==ClipboardType.URL,onClick={vm.setFilter(ClipboardType.URL)},label={Text("URLs")});FilterChip(selected=filter==ClipboardType.EMAIL,onClick={vm.setFilter(ClipboardType.EMAIL)},label={Text("Email")});FilterChip(selected=filter==ClipboardType.PHONE,onClick={vm.setFilter(ClipboardType.PHONE)},label={Text("Phone")})}}@Composable
private fun RssApp(onOpenFloating: () -> Unit, onOpenTheme: () -> Unit, onBatteryOptimization: () -> Unit) {
    var screen by remember { mutableStateOf(RssScreen.CLIPBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxHeight().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("RSS Clipboard", style = MaterialTheme.typography.titleLarge)
                            Text(UserPrefs.name(LocalContext.current), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    DrawerItem("Clipboard", Icons.Default.ContentPaste, screen == RssScreen.CLIPBOARD) { screen = RssScreen.CLIPBOARD; scope.launch { drawerState.close() } }
                    DrawerItem("Saved List", Icons.Default.Bookmark, screen == RssScreen.SAVED) { screen = RssScreen.SAVED; scope.launch { drawerState.close() } }
                    DrawerItem("Settings", Icons.Default.Settings, screen == RssScreen.SETTINGS) { screen = RssScreen.SETTINGS; scope.launch { drawerState.close() } }
                    Spacer(Modifier.weight(1f))
                    Text("RSS Clipboard  •  v1.0.0", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(when(screen) { RssScreen.CLIPBOARD -> "Clipboard"; RssScreen.SAVED -> "Saved List"; RssScreen.SETTINGS -> "Settings" }) },
                    navigationIcon = { IconButton({ scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") } }
                )
            },
            bottomBar = {
                NavigationBar {
                    BottomItem("Clipboard", Icons.Default.ContentPaste, screen == RssScreen.CLIPBOARD) { screen = RssScreen.CLIPBOARD }
                    BottomItem("Saved", Icons.Default.Bookmark, screen == RssScreen.SAVED) { screen = RssScreen.SAVED }
                    BottomItem("Settings", Icons.Default.Settings, screen == RssScreen.SETTINGS) { screen = RssScreen.SETTINGS }
                }
            }
        ) { pad ->
            AnimatedContent(targetState = screen, label = "screenTransition") { target ->
                Box(Modifier.fillMaxSize().padding(pad)) {
                    when(target) {
                        RssScreen.CLIPBOARD -> ClipboardScreen()
                        RssScreen.SAVED -> SavedListScreen()
                        RssScreen.SETTINGS -> SettingsScreen(onOpenFloating, onOpenTheme, onBatteryOptimization)
                    }
                }
            }
        }
    }
}

@Composable private fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(label = { Text(label) }, selected = selected, onClick = onClick, icon = { Icon(icon, null) }, modifier = Modifier.padding(vertical = 3.dp))
}

@Composable private fun BottomItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, null) }, label = { Text(label) })
}

@Composable
private fun WelcomeScreen(onRegister: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val transition = rememberInfiniteTransition(label = "welcomeBackground")
    val offset by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6500), RepeatMode.Reverse), label = "offset")
    val valid = name.trim().length >= 2 && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.12f), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondary.copy(.10f))))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(MaterialTheme.colorScheme.primary.copy(.10f), 260f, androidx.compose.ui.geometry.Offset(size.width * (.15f + .15f * offset), size.height * .16f))
            drawCircle(MaterialTheme.colorScheme.secondary.copy(.08f), 320f, androidx.compose.ui.geometry.Offset(size.width * (.88f - .12f * offset), size.height * .82f))
        }
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Welcome to RSS Clipboard", style = MaterialTheme.typography.headlineMedium)
            Text("Your fast, private clipboard companion.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Create your profile", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Your name") }, leadingIcon = { Icon(Icons.Default.Person, null) })
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), leadingIcon = { Icon(Icons.Default.Email, null) })
                    Button(onClick = { onRegister(name, email) }, enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Get started") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Clipboard history stays local and expires after 24 hours.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun ClipboardScreen(vm: MainViewModel = viewModel()) {
    val items by vm.visibleItems.collectAsState(); val query by vm.query.collectAsState(); var showClear by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<ClipboardItem?>(null) }; var saving by remember { mutableStateOf<ClipboardItem?>(null) }; val clipboard=LocalClipboardManager.current
    Scaffold(topBar={TopAppBar(title={Text("RSS Clipboard")},actions={IconButton({showClear=true}){Icon(Icons.Default.DeleteSweep,"Clear")}})}) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)) {
            OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search clipboard")}); Spacer(Modifier.height(8.dp)); FilterRow(vm); Spacer(Modifier.height(8.dp))
            if(items.isEmpty()) Text("No clipboard items yet",Modifier.padding(16.dp)) else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){itemsIndexed(items,key={_,it->it.id}){index,item->ClipboardCard(index+1,item,{clipboard.setText(AnnotatedString(item.content))},{vm.togglePin(item)},{vm.delete(item)},{editing=item},{saving=item})}}
        }
    }
    if(showClear) AlertDialog(onDismissRequest={showClear=false},title={Text("Clear clipboard history?")},text={Text("This removes clipboard history only. Your Saved List is kept permanently.")},confirmButton={TextButton({vm.clearAll();showClear=false}){Text("Clear")}},dismissButton={TextButton({showClear=false}){Text("Cancel")}})
    editing?.let{item->var text by remember(item.id){mutableStateOf(item.content)};AlertDialog(onDismissRequest={editing=null},title={Text("Edit item")},text={OutlinedTextField(text,{text=it},minLines=3)},confirmButton={TextButton({vm.update(item,text);editing=null}){Text("Save")}},dismissButton={TextButton({editing=null}){Text("Cancel")}})}
    saving?.let{item->SaveToListDialog(item.content){saving=null}}
}

@Composable private fun SaveToListDialog(initialData:String,onDismiss:()->Unit){val vm:SavedListViewModel=viewModel();var category by remember{mutableStateOf("General")};var fileName by remember{mutableStateOf("")};var data by remember(initialData){mutableStateOf(initialData)};var description by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Save to List")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Permanent Saved List item. It will not expire with clipboard history.",style=MaterialTheme.typography.bodySmall);OutlinedTextField(category,{category=it},label={Text("Category")},singleLine=true);OutlinedTextField(fileName,{fileName=it},label={Text("File name")},singleLine=true);OutlinedTextField(data,{data=it},label={Text("Data")},minLines=3);OutlinedTextField(description,{description=it},label={Text("Description")},minLines=2)}},confirmButton={TextButton({vm.add(category,fileName,data,description);onDismiss()}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SavedListScreen(vm:SavedListViewModel=viewModel()){val items by vm.visibleItems.collectAsState();val query by vm.query.collectAsState();val categories by vm.categories.collectAsState();val selectedCategory by vm.category.collectAsState();val clipboard=LocalClipboardManager.current;var editing by remember{mutableStateOf<SavedItem?>(null)};var showClear by remember{mutableStateOf(false)};Scaffold(topBar={TopAppBar(title={Text("Saved List")},actions={IconButton({showClear=true}){Icon(Icons.Default.DeleteSweep,"Clear")}})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=12.dp)){OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search saved list")});Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){FilterChip(selected=selectedCategory==null,onClick={vm.setCategory(null)},label={Text("All")});categories.take(5).forEach{cat->FilterChip(selected=selectedCategory==cat,onClick={vm.setCategory(cat)},label={Text(cat)})}};Spacer(Modifier.height(8.dp));if(items.isEmpty())Text("No saved items yet",Modifier.padding(16.dp))else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items,key={it.id}){item->SavedCard(item,{clipboard.setText(AnnotatedString(item.data))},{editing=item},{vm.delete(item)})}}}};if(showClear)AlertDialog(onDismissRequest={showClear=false},title={Text("Clear Saved List?")},text={Text("This permanently deletes all saved items. Clipboard history is not affected.")},confirmButton={TextButton({vm.clearAll();showClear=false}){Text("Delete all")}},dismissButton={TextButton({showClear=false}){Text("Cancel")}});editing?.let{item->EditSavedDialog(item,{editing=null}){c,n,d,desc->vm.update(item,c,n,d,desc);editing=null}}}

@Composable private fun SavedCard(item:SavedItem,onCopy:()->Unit,onEdit:()->Unit,onDelete:()->Unit){Card(shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(item.category,style=MaterialTheme.typography.labelMedium);Text("PERMANENT",style=MaterialTheme.typography.labelSmall)};Text(item.fileName,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(4.dp));Text(item.data,maxLines=5);if(item.description.isNotBlank())Text(item.description,style=MaterialTheme.typography.bodySmall);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick=onCopy){Text("Copy")};TextButton(onClick=onEdit){Text("Edit")};TextButton(onClick=onDelete){Text("Delete")}}}}}

@Composable private fun EditSavedDialog(item:SavedItem,onDismiss:()->Unit,onSave:(String,String,String,String)->Unit){var c by remember(item.id){mutableStateOf(item.category)};var n by remember(item.id){mutableStateOf(item.fileName)};var d by remember(item.id){mutableStateOf(item.data)};var desc by remember(item.id){mutableStateOf(item.description)};AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Saved Item")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(c,{c=it},label={Text("Category")});OutlinedTextField(n,{n=it},label={Text("File name")});OutlinedTextField(d,{d=it},label={Text("Data")},minLines=3);OutlinedTextField(desc,{desc=it},label={Text("Description")})}},confirmButton={TextButton({onSave(c,n,d,desc)}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable private fun SettingsScreen(onOpenFloating:()->Unit,onOpenTheme:()->Unit,onBatteryOptimization:()->Unit){
    val context=LocalContext.current
    val batteryOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) !(context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: false) else false
    Scaffold(topBar={TopAppBar(title={Text("Settings")})}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        SettingsRow(Icons.Default.BubbleChart,"Floating Clipboard", "Overlay, auto-hide and dialog options",onOpenFloating)
        SettingsRow(Icons.Default.BatteryChargingFull,"Battery Optimization", if (batteryOptimized) "Allow RSS Clipboard to stay active with less background restriction" else "Optimized for always-on background operation",onBatteryOptimization)
        SettingsRow(Icons.Default.Notifications,"Notifications", if (FloatingPrefs.notifications(context)) "On — required while Floating Clipboard is active" else "Off — Floating Clipboard will remain stopped",{
            val next=!FloatingPrefs.notifications(context); FloatingPrefs.setNotifications(context,next)
            if (!next) { FloatingPrefs.setEnabled(context,false); context.stopService(Intent(context,FloatingClipboardService::class.java)) }
        })
        SettingsRow(Icons.Default.Palette,"Theme", "Windows, Ubuntu, Android, macOS, iOS and more",onOpenTheme)
    }}
}
@Composable private fun SettingsRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){Icon(icon,title);Column{Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall)}}}}

@Composable private fun ThemeSelectorDialog(onDismiss:()->Unit){val context=LocalContext.current;var selected by remember{mutableStateOf(ThemePrefs.get(context))};AlertDialog(onDismissRequest=onDismiss,title={Text("Theme")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Choose the visual style for RSS Clipboard.",style=MaterialTheme.typography.bodySmall);AppTheme.entries.forEach{theme->FilterChip(selected=selected==theme,onClick={selected=theme;ThemePrefs.set(context,theme);(context as? Activity)?.recreate();onDismiss()},label={Text(theme.label)},modifier=Modifier.fillMaxWidth())}}},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})}

@Composable private fun FloatingSettingsDialog(onDismiss:()->Unit,onEnable:()->Unit,onDisable:()->Unit){val context=LocalContext.current;var enabled by remember{mutableStateOf(FloatingPrefs.enabled(context))};var bubble by remember{mutableStateOf(FloatingPrefs.showBubble(context))};var openOnCopy by remember{mutableStateOf(FloatingPrefs.openOnCopy(context))};var closeAfterCopy by remember{mutableStateOf(FloatingPrefs.closeAfterCopy(context))};var autoHide by remember{mutableStateOf(FloatingPrefs.autoHide(context))};var hideTimer by remember{mutableStateOf(FloatingPrefs.hideTimerSeconds(context))};var size by remember{mutableStateOf(FloatingPrefs.size(context))};AlertDialog(onDismissRequest=onDismiss,title={Text("Floating clipboard")},text={Column(verticalArrangement=Arrangement.spacedBy(2.dp)){Text("Runs as a foreground service while enabled. A visible service notification is required.",style=MaterialTheme.typography.bodySmall);SettingSwitch("Floating clipboard",enabled){enabled=it;if(it)onEnable()else onDisable()};SettingSwitch("Show floating button",bubble){bubble=it;FloatingPrefs.setShowBubble(context,it)};SettingSwitch("Open list when something is copied",openOnCopy){openOnCopy=it;FloatingPrefs.setOpenOnCopy(context,it)};SettingSwitch("Close after copying an item",closeAfterCopy){closeAfterCopy=it;FloatingPrefs.setCloseAfterCopy(context,it)};SettingSwitch("Auto-hide floating button",autoHide){autoHide=it;FloatingPrefs.setAutoHide(context,it)};SettingSwitch("Notifications",FloatingPrefs.notifications(context)){value->FloatingPrefs.setNotifications(context,value);if(!value){FloatingPrefs.setEnabled(context,false);context.stopService(Intent(context,FloatingClipboardService::class.java))}};Text("Hide timer",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf(5,15,30,60).forEach{seconds->FilterChip(selected=hideTimer==seconds,onClick={hideTimer=seconds;FloatingPrefs.setHideTimerSeconds(context,seconds)},label={Text("${seconds}s")})}};Text("Dialog size",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("small","medium","large").forEach{value->FilterChip(selected=size==value,onClick={size=value;FloatingPrefs.setSize(context,value)},label={Text(value.replaceFirstChar{it.uppercase()})})}}}},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})}
@Composable private fun SettingSwitch(label:String,checked:Boolean,onCheckedChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(label,Modifier.weight(1f));Switch(checked,onCheckedChange)}}
@Composable private fun FilterRow(vm:MainViewModel){val filter by vm.filter.collectAsState();Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()){FilterChip(selected=filter==null,onClick={vm.setFilter(null)},label={Text("All")});FilterChip(selected=filter==ClipboardType.TEXT,onClick={vm.setFilter(ClipboardType.TEXT)},label={Text("Text")});FilterChip(selected=filter==ClipboardType.URL,onClick={vm.setFilter(ClipboardType.URL)},label={Text("URLs")});FilterChip(selected=filter==ClipboardType.EMAIL,onClick={vm.setFilter(ClipboardType.EMAIL)},label={Text("Email")});FilterChip(selected=filter==ClipboardType.PHONE,onClick={vm.setFilter(ClipboardType.PHONE)},label={Text("Phone")})}}
