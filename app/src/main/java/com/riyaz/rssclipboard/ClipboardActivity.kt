package com.riyaz.rssclipboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riyaz.rssclipboard.data.*
import kotlinx.coroutines.launch

class ClipboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RssClipboardTheme(ThemePrefs.get(this)) { ClipboardRoot() } }
    }
    override fun onResume() {
        super.onResume()
        if (UserPrefs.isRegistered(this) && FloatingPrefs.enabled(this)) startMonitor(this)
    }
}

private fun startMonitor(context: Context) {
    runCatching {
        val i = Intent(context, FloatingClipboardService::class.java)
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
    }
}

@Composable private fun ClipboardRoot() {
    val context = LocalContext.current
    var registered by remember { mutableStateOf(UserPrefs.isRegistered(context)) }
    var welcome by remember { mutableStateOf(UserPrefs.welcomeSeen(context)) }
    when {
        !registered -> RegistrationV2 { name, email ->
            UserPrefs.register(context, name, email); registered = true; welcome = false; startMonitor(context)
        }
        !welcome -> WelcomeV2 { UserPrefs.setWelcomeSeen(context); welcome = true }
        else -> MainShellV2()
    }
}

@Composable private fun RegistrationV2(onRegister: (String,String)->Unit) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    val valid = name.trim().length >= 2 && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    // Plain app background: no custom gradient or animation.\n    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.height(42.dp))
            Image(painterResource(R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(112.dp))
            Spacer(Modifier.height(16.dp))
            Text("RSS Clipboard",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            Text("Your clipboard, finally organized.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Card(shape=RoundedCornerShape(28.dp),elevation=CardDefaults.cardElevation(8.dp),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text("Create your account",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text("Set up your local profile. Clipboard data stays on this device.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Your name")},leadingIcon={Icon(Icons.Default.Person,null)})
                    OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Email address")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),leadingIcon={Icon(Icons.Default.Email,null)})
                    Button({onRegister(name.trim(),email.trim())},enabled=valid,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Icon(Icons.Default.ArrowForward,null);Spacer(Modifier.width(8.dp));Text("Continue")}
                }
            }
            Spacer(Modifier.weight(1f))
            Text("24-hour history • Permanent Saved List • Privacy-first",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun WelcomeV2(onContinue:()->Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages=listOf(
        Triple(Icons.Default.ContentPaste,"Capture every copy","Keep recent text, links, emails and phone numbers for 24 hours."),
        Triple(Icons.Default.Bookmark,"Save what matters","Move important clipboard items into the permanent Saved List."),
        Triple(Icons.Default.Security,"Built for privacy","History stays local. Background capture runs through the RSS Clipboard monitoring service.")
    )
    Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(44.dp));Image(painterResource(R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(112.dp));Spacer(Modifier.height(24.dp))
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Surface(Modifier.size(78.dp),shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(pages[page].first,pages[page].second,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.primary)}}
            Spacer(Modifier.height(20.dp));Text(pages[page].second,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp));Text(pages[page].third,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){pages.indices.forEach{Box(Modifier.size(if(it==page)26.dp else 8.dp,8.dp).clip(CircleShape).background(if(it==page)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))}}
        Spacer(Modifier.height(22.dp))
        Button({if(page<pages.lastIndex)page++ else onContinue()},Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(17.dp)){Text(if(page<pages.lastIndex)"Next" else "Start using RSS Clipboard");Spacer(Modifier.width(8.dp));Icon(Icons.Default.ArrowForward,null)}
        if(page<pages.lastIndex)TextButton(onContinue){Text("Skip")}
    }
}

private enum class ScreenV2 { CLIPBOARD,SAVED,FEATURES,SETTINGS,ABOUT,CONTACT,PRIVACY,TERMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MainShellV2() {
    val context=LocalContext.current; var screen by remember{mutableStateOf(ScreenV2.CLIPBOARD)}
    val drawer=rememberDrawerState(DrawerValue.Closed); val scope=rememberCoroutineScope()
    BackHandler(drawer.isOpen){scope.launch{drawer.close()}}
    ModalNavigationDrawer(drawerState=drawer,drawerContent={
        ModalDrawerSheet(Modifier.width(318.dp)) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                Image(painterResource(R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(12.dp));Column{Text("RSS Clipboard",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(UserPrefs.name(context),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            Spacer(Modifier.height(18.dp))
            DrawerItem("Clipboard",Icons.Default.ContentPaste,screen==ScreenV2.CLIPBOARD){screen=ScreenV2.CLIPBOARD;scope.launch{drawer.close()}}
            DrawerItem("Saved List",Icons.Default.Bookmark,screen==ScreenV2.SAVED){screen=ScreenV2.SAVED;scope.launch{drawer.close()}}
            HorizontalDivider(Modifier.padding(vertical=8.dp))
            DrawerItem("App Features",Icons.Default.AutoAwesome,screen==ScreenV2.FEATURES){screen=ScreenV2.FEATURES;scope.launch{drawer.close()}}
            DrawerItem("Settings",Icons.Default.Settings,screen==ScreenV2.SETTINGS){screen=ScreenV2.SETTINGS;scope.launch{drawer.close()}}
            DrawerItem("About",Icons.Default.Info,screen==ScreenV2.ABOUT){screen=ScreenV2.ABOUT;scope.launch{drawer.close()}}
            DrawerItem("Contact",Icons.Default.ContactMail,screen==ScreenV2.CONTACT){screen=ScreenV2.CONTACT;scope.launch{drawer.close()}}
            DrawerItem("Privacy Policy",Icons.Default.PrivacyTip,screen==ScreenV2.PRIVACY){screen=ScreenV2.PRIVACY;scope.launch{drawer.close()}}
            DrawerItem("Terms & Conditions",Icons.Default.Description,screen==ScreenV2.TERMS){screen=ScreenV2.TERMS;scope.launch{drawer.close()}}
            Spacer(Modifier.weight(1f));HorizontalDivider()
            Column(Modifier.padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Image(painterResource(R.drawable.rss_logo_only),"Razeen Secure Solution",Modifier.size(48.dp))
                Text("Razeen Secure Solution",style=MaterialTheme.typography.titleSmall);Text("RSS Clipboard • v1.0.0",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }){
        Scaffold(topBar={
            TopAppBar(title={Text(when(screen){ScreenV2.CLIPBOARD->"Clipboard";ScreenV2.SAVED->"Saved List";ScreenV2.FEATURES->"App Features";ScreenV2.SETTINGS->"Settings";ScreenV2.ABOUT->"About";ScreenV2.CONTACT->"Contact";ScreenV2.PRIVACY->"Privacy Policy";ScreenV2.TERMS->"Terms & Conditions"})},
                navigationIcon={IconButton({scope.launch{drawer.open()}}){Image(painterResource(R.drawable.rss_clipboard_logo),"Menu",Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)))}},
                actions={if(screen==ScreenV2.CLIPBOARD)IconButton({screen=ScreenV2.SETTINGS}){Icon(Icons.Default.Settings,"Settings")}})},
            bottomBar={NavigationBar{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){BottomItem("Clipboard",Icons.Default.ContentPaste,screen==ScreenV2.CLIPBOARD){screen=ScreenV2.CLIPBOARD};BottomItem("Saved",Icons.Default.Bookmark,screen==ScreenV2.SAVED){screen=ScreenV2.SAVED};BottomItem("Settings",Icons.Default.Settings,screen==ScreenV2.SETTINGS){screen=ScreenV2.SETTINGS}}}}
        ){pad->Box(Modifier.fillMaxSize().padding(pad)){when(screen){
            ScreenV2.CLIPBOARD->ClipboardV2();ScreenV2.SAVED->SavedV2();ScreenV2.FEATURES->RssFeaturesScreen();ScreenV2.SETTINGS->SettingsV2()
            ScreenV2.ABOUT->RssAboutScreen();ScreenV2.CONTACT->RssContactScreen();ScreenV2.PRIVACY->RssPrivacyScreen();ScreenV2.TERMS->RssTermsScreen()
        }}}
    }
}

@Composable private fun DrawerItem(label:String,icon:ImageVector,selected:Boolean,onClick:()->Unit){NavigationDrawerItem(label={Text(label)},icon={Icon(icon,label,tint=MaterialTheme.colorScheme.primary)},selected=selected,onClick=onClick,modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp))}
@Composable private fun BottomItem(label:String,icon:ImageVector,selected:Boolean,onClick:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally){IconButton(onClick){Icon(icon,label,tint=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)};Text(label,style=MaterialTheme.typography.labelSmall,color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}}

@Composable private fun ClipboardV2(vm:MainViewModel=viewModel()) {
    val items by vm.visibleItems.collectAsState();val query by vm.query.collectAsState();val clipboard=LocalClipboardManager.current
    var clear by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=10.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("RSS Clipboard",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Recent copies • 24 hours",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton({clear=true}){Icon(Icons.Default.DeleteSweep,"Clear",tint=MaterialTheme.colorScheme.error)}}
        OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search clipboard")},leadingIcon={Icon(Icons.Default.Search,null)})
        Spacer(Modifier.height(9.dp));Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){FilterRowV2(vm)};Spacer(Modifier.height(10.dp))
        if(items.isEmpty())Column(Modifier.fillMaxWidth().padding(top=56.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.ContentPaste,null,Modifier.size(54.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(12.dp));Text("No clipboard items yet",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Enable background capture in Settings, then copy from another app.",textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=24.dp))}
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=12.dp)){itemsIndexed(items,key={_,it->it.id}){i,item->Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(item.type==ClipboardType.URL)Icons.Default.Link else if(item.type==ClipboardType.EMAIL)Icons.Default.Email else if(item.type==ClipboardType.PHONE)Icons.Default.Phone else Icons.Default.ContentPaste,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("#"+(i+1)+" • "+item.type.name,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold);Text("Expires in 24 hours",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};Text(item.content,Modifier.padding(top=8.dp,bottom=6.dp),maxLines=8);Row{TextButton({clipboard.setText(AnnotatedString(item.content))}){Text("Copy")};TextButton({vm.togglePin(item)}){Text(if(item.pinned)"Unpin" else "Pin")};TextButton({vm.delete(item)}){Text("Delete")}}}}}}
    }
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Clear clipboard history?")},text={Text("Saved List is not affected.")},confirmButton={TextButton({vm.clearAll();clear=false}){Text("Clear")}},dismissButton={TextButton({clear=false}){Text("Cancel")}})
}

@Composable private fun FilterRowV2(vm:MainViewModel){val filter by vm.filter.collectAsState();FilterChip(filter==null,{vm.setFilter(null)},label={Text("All")});FilterChip(filter==ClipboardType.TEXT,{vm.setFilter(ClipboardType.TEXT)},label={Text("Text")});FilterChip(filter==ClipboardType.URL,{vm.setFilter(ClipboardType.URL)},label={Text("URLs")});FilterChip(filter==ClipboardType.EMAIL,{vm.setFilter(ClipboardType.EMAIL)},label={Text("Email")});FilterChip(filter==ClipboardType.PHONE,{vm.setFilter(ClipboardType.PHONE)},label={Text("Phone")})}

@Composable private fun SavedV2(vm:SavedListViewModel=viewModel()){val items by vm.visibleItems.collectAsState();val query by vm.query.collectAsState();val clipboard=LocalClipboardManager.current;Column(Modifier.fillMaxSize().padding(14.dp)){Text("Saved List",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Permanent items",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(10.dp));OutlinedTextField(query,vm::setQuery,Modifier.fillMaxWidth(),singleLine=true,label={Text("Search saved list")});Spacer(Modifier.height(10.dp));if(items.isEmpty())Text("No saved items yet",Modifier.padding(18.dp))else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){itemsIndexed(items,key={_,it->it.id}){_,item->Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp)){Text(item.category,style=MaterialTheme.typography.labelMedium);Text(item.fileName,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(item.data,Modifier.padding(top=6.dp),maxLines=5);Row{TextButton({clipboard.setText(AnnotatedString(item.data))}){Text("Copy")};TextButton({vm.delete(item)}){Text("Delete")}}}}}}}}

@Composable private fun SettingsV2(){
    val context=LocalContext.current;var showTheme by remember{mutableStateOf(false)}
    val battery=if(Build.VERSION.SDK_INT>=23)!(context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName)?:false)else false
    LazyColumn(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Card(shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Image(painterResource(R.drawable.rss_clipboard_logo),"RSS Clipboard",Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)));Spacer(Modifier.width(14.dp));Column{Text("RSS Clipboard",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(UserPrefs.name(context),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
        item{SettingCard(Icons.Default.Security,"Reliable background capture","Monitors copies while RSS Clipboard is running in the background"){context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}
        item{SettingCard(Icons.Default.BubbleChart,"Floating Clipboard","Overlay shortcut and floating controls"){if(!Settings.canDrawOverlays(context))context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+context.packageName)))else startMonitor(context)}}
        item{SettingCard(Icons.Default.BatteryChargingFull,"Battery Optimization",if(battery)"Optimization enabled" else "Already unrestricted"){if(Build.VERSION.SDK_INT>=23)context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+context.packageName)))}}
        item{SettingCard(Icons.Default.Palette,"Theme","Light, Dark, System and platform styles"){showTheme=true}}
        item{Spacer(Modifier.height(14.dp));HorizontalDivider();Spacer(Modifier.height(10.dp));Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Image(painterResource(R.drawable.rss_logo_only),"Razeen Secure Solution",Modifier.size(54.dp));Text("Razeen Secure Solution",fontWeight=FontWeight.SemiBold);Text("www.rsscctvsolution.eu.cc",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
    }
    if(showTheme){var selected by remember{mutableStateOf(ThemePrefs.get(context))};AlertDialog(onDismissRequest={showTheme=false},title={Text("Theme")},text={Column{AppTheme.entries.forEach{theme->FilterChip(selected==theme,{selected=theme;ThemePrefs.set(context,theme);showTheme=false;(context as?Activity)?.recreate()},label={Text(theme.label)},modifier=Modifier.fillMaxWidth())}}},confirmButton={TextButton({showTheme=false}){Text("Cancel")}})}
}
@Composable private fun SettingCard(icon:ImageVector,title:String,subtitle:String,onClick:()->Unit){Card(shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,title,Modifier.size(28.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
