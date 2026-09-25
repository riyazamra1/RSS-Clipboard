@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.riyaz.rssclipboard

import android.Manifest
import android.content.*
import android.os.*
import android.provider.Settings
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

private const val CORE_URL = "https://rsscore.cv"
private const val APP_ID = "rss-clipboard"
private const val PREFS = "rss_clipboard"
private const val DAY = 24L * 60L * 60L * 1000L

private data class ClipItem(val text:String,val type:String,val time:Long)
private data class Account(val name:String,val email:String,val appKey:String,val verified:Boolean,val expiresAt:Long?)
private data class UiPage(val title:String,val body:String,val icon:ImageVector)

class ClipboardActivity : ComponentActivity() {
    private var receiver: BroadcastReceiver? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        startClipboardCapture()
        receiver = object: BroadcastReceiver() {
            override fun onReceive(context:Context?, intent:Intent?) {
                if (intent?.action == "com.riyaz.rssclipboard.CLIPBOARD_UPDATED") sendRefresh()
            }
        }
        setContent { App() }
    }

    override fun onStart() {
        super.onStart()
        receiver?.let { ContextCompat.registerReceiver(this,it,IntentFilter("com.riyaz.rssclipboard.CLIPBOARD_UPDATED"),ContextCompat.RECEIVER_NOT_EXPORTED) }
    }

    override fun onStop() {
        receiver?.let { try { unregisterReceiver(it) } catch (_:Exception) {} }
        super.onStop()
    }

    private fun sendRefresh() { /* UI polls/reloads when resumed; capture remains service-owned. */ }

    private fun startClipboardCapture() {
        ContextCompat.startForegroundService(this,Intent(this,ClipboardCaptureService::class.java))
    }

    private fun deviceId():String = Settings.Secure.getString(contentResolver,Settings.Secure.ANDROID_ID) ?: "android-"+Build.FINGERPRINT.hashCode()

    @Composable private fun App() {
        val prefs=getSharedPreferences(PREFS,MODE_PRIVATE)
        var theme by remember { mutableStateOf(prefs.getString("theme","system") ?: "system") }
        var registered by remember { mutableStateOf(prefs.getBoolean("registered",false) && !prefs.getString("app_key",null).isNullOrBlank()) }
        var stage by remember { mutableStateOf(if(registered) "main" else "register") }
        var drawer by remember { mutableStateOf(false) }
        var selectedPage by remember { mutableStateOf("home") }
        var account by remember { mutableStateOf(loadAccount()) }

        val dark = when(theme) {
            "dark" -> true
            "light" -> false
            else -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        val scheme = if(dark) darkColorScheme(
            primary=Color(0xFFD2A94C), secondary=Color(0xFFB794F4), background=Color(0xFF101010), surface=Color(0xFF171717)
        ) else lightColorScheme(
            primary=Color(0xFFB4862E), secondary=Color(0xFF7357E8), background=Color(0xFFF8F7F3), surface=Color.White
        )

        MaterialTheme(colorScheme=scheme) {
            SideEffect { window.statusBarColor=scheme.background.toArgb(); window.navigationBarColor=scheme.background.toArgb() }
            when(stage) {
                "register" -> RegisterScreen { name,email -> register(name,email) { a ->
                    account=a; registered=true; prefs.edit().putBoolean("registered",true).putString("name",a.name).putString("email",a.email).putString("app_key",a.appKey).putLong("verification_expires",a.expiresAt ?: 0L).apply()
                    stage="welcome"
                } }
                "welcome" -> WelcomeScreen(account?.name ?: "there") { stage="features" }
                "features" -> FeaturesScreen { stage="main" }
                "main" -> MainScreen(account, onAccountUpdate={account=it}, onMenu={drawer=true}, onRefresh={})
                "settings" -> SettingsScreen(theme, {theme=it;prefs.edit().putString("theme",it).apply()}, account, onBack={stage="main"})
                "about" -> StaticScreen(UiPage("About RSS Clipboard","RSS Clipboard is a lightweight clipboard organizer by Razeen Secure Solution.",Icons.Default.Info),{stage="main"})
                "contact" -> StaticScreen(UiPage("Contact","Razeen Secure Solution\nEmail: rsscctvsolution@gmail.com\n077 115 5504 | 070 155 5504",Icons.Default.Email),{stage="main"})
                "privacy" -> StaticScreen(UiPage("Privacy","Clipboard content is kept locally by default. Cloud backup is opt-in and scoped to the signed-in RSS Clipboard account. Android and device security restrictions apply to clipboard access.",Icons.Default.PrivacyTip),{stage="main"})
                "terms" -> StaticScreen(UiPage("Terms","Use RSS Clipboard only with content you are permitted to copy and store. Service availability depends on Android, RSS Core and network conditions.",Icons.Default.Description),{stage="main"})
            }
            if(drawer) Drawer(
                account=account,
                onClose={drawer=false},
                onNavigate={p -> drawer=false; selectedPage=p; stage=when(p){"home"->"main";"settings"->"settings";"about"->"about";"contact"->"contact";"privacy"->"privacy";else->"terms"}}
            )
        }
    }

    private fun loadAccount():Account? {
        val p=getSharedPreferences(PREFS,MODE_PRIVATE)
        val email=p.getString("email",null) ?: return null
        val key=p.getString("app_key",null) ?: return null
        return Account(p.getString("name","") ?: "",email,key,p.getBoolean("verified",false),p.getLong("verification_expires",0L).takeIf{it>0})
    }

    private fun register(name:String,email:String,onSuccess:(Account)->Unit) {
        val n=name.trim(); val e=email.trim().lowercase(Locale.US)
        if(n.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(e).matches()) return
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            try {
                val result=CoreClient.post("/v1/license/register",JSONObject().put("email",e).put("display_name",n).put("project_key",APP_ID).put("device_id",deviceId()))
                val key=result.optString("app_key")
                if(key.isBlank()) return@launch
                val expires=if(result.optBoolean("verification_required",false)) System.currentTimeMillis()+DAY else 0L
                val a0=Account(n,e,key,!result.optBoolean("verification_required",false),if(expires>0)expires else null)
                val verification=if(!a0.verified) try { CoreClient.get("/api/v1/license/verification-status?email="+java.net.URLEncoder.encode(e,"UTF-8")+"&project_key="+APP_ID) } catch (_:Exception) { JSONObject() } else JSONObject()
                val serverExpires=verification.optLong("verification_expires_at",0L)
                val a=if(serverExpires>0) a0.copy(expiresAt=serverExpires) else a0
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("verified",a.verified).apply()
                onSuccess(a)
                syncNow(a)
            } catch (_:Exception) {}
        }
    }

    private fun syncNow(a:Account) {\n        if(!getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("cloud_sync_enabled",true)) return
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val local=JSONObject().put("schemaVersion",1).put("updatedAt",System.currentTimeMillis()).put("clips",JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString("clips_json","[]"))).put("lists",JSONObject(getListsJson()))
                val remote=CoreClient.get("/api/v1/clipboard/sync",a.appKey)
                val snap=remote.optString("snapshot","")
                if(snap.isNotBlank()) {
                    val r=JSONObject(snap)
                    val remoteUpdated=r.optLong("updatedAt",0L)
                    if(remoteUpdated>local.optLong("updatedAt")) saveRemote(r)
                    else CoreClient.put("/api/v1/clipboard/sync",a.appKey,JSONObject().put("snapshot",local.toString()))
                } else CoreClient.put("/api/v1/clipboard/sync",a.appKey,JSONObject().put("snapshot",local.toString()))
            } catch (_:Exception) {}
        }
    }

    private fun getListsJson():String {
        val p=getSharedPreferences(PREFS,MODE_PRIVATE)
        val out=JSONObject()
        p.getStringSet("clip_lists",emptySet()).orEmpty().forEach { name -> out.put(name,JSONArray(p.getStringSet("list_$name",emptySet()).orEmpty().toList())) }
        return out.toString()
    }

    private fun saveRemote(r:JSONObject) {
        val p=getSharedPreferences(PREFS,MODE_PRIVATE)
        val clips=r.optJSONArray("clips") ?: JSONArray()
        p.edit().putString("clips_json",clips.toString()).apply()
        val lists=r.optJSONObject("lists") ?: JSONObject()
        val names=mutableSetOf<String>()
        val ed=p.edit()
        lists.keys().forEach { n ->
            names.add(n)
            val arr=lists.optJSONArray(n) ?: JSONArray()
            val set=mutableSetOf<String>(); for(i in 0 until arr.length()) set.add(arr.optString(i))
            ed.putStringSet("list_$n",set)
        }
        ed.putStringSet("clip_lists",names).apply()
    }

    @Composable private fun AnimatedBackground() {
        val infinite=rememberInfiniteTransition(label="bg")
        val x by infinite.animateFloat(0f,1f,infiniteRepeatable(tween(7000,easing=LinearEasing),RepeatMode.Reverse),label="x")
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.size(220.dp).offset(x.dp*80f,(-40).dp).alpha(.08f).background(MaterialTheme.colorScheme.primary,CircleShape))
            Box(Modifier.size(180.dp).align(Alignment.BottomEnd).offset((-30).dp,x.dp*60f).alpha(.06f).background(MaterialTheme.colorScheme.secondary,CircleShape))
        }
    }

    @Composable private fun Logo(modifier:Modifier=Modifier) = Image(painterResource(R.drawable.rss_clipboard_logo),"RSS Clipboard",modifier,contentScale=ContentScale.Fit)

    @Composable private fun RegisterScreen(done:(String,String)->Unit) {
        var name by remember{mutableStateOf("")}; var email by remember{mutableStateOf("")}; var cloudSync by remember{mutableStateOf(true)}; var busy by remember{mutableStateOf(false)}
        Box(Modifier.fillMaxSize()) { AnimatedBackground(); Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Logo(Modifier.size(120.dp)); Spacer(Modifier.height(18.dp)); Text("Create your RSS account",fontSize=28.sp,fontWeight=FontWeight.Bold); Text("One RSS account for your app and devices.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp)); OutlinedTextField(name,{name=it},label={Text("Full name")},leadingIcon={Icon(Icons.Default.Person,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp)); OutlinedTextField(email,{email=it},label={Text("Email address")},leadingIcon={Icon(Icons.Default.Email,null)},singleLine=true,modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Checkbox(cloudSync,{cloudSync=it});Text("Enable RSS Cloud backup across my devices",fontSize=13.sp)}; Spacer(Modifier.height(10.dp)); Button(enabled=!busy && name.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches(),onClick={busy=true;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("cloud_sync_enabled",cloudSync).apply();done(name,email)},Modifier.fillMaxWidth()){Text(if(busy)"Connecting to RSS Core…" else "Create account")}
            Spacer(Modifier.height(10.dp)); Text("Email verification is required. You can enter the app while verification is pending.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
    }

    @Composable private fun WelcomeScreen(name:String,next:()->Unit) { Box(Modifier.fillMaxSize()){AnimatedBackground();Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Logo(Modifier.size(140.dp));Text("Welcome, $name",fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Your clipboard, organized and ready.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(24.dp));Button(next){Text("Continue")}}}}

    @Composable private fun FeaturesScreen(next:()->Unit) {
        var page by remember{mutableStateOf(0)}
        val fs=listOf(
            UiPage("Capture automatically","Clipboard capture stays in the foreground service even after the app UI is closed, subject to Android/device policy.",Icons.Default.ContentCopy),
            UiPage("Organize instantly","Keep a 24-hour history, classify URLs and email addresses, and save items into named lists.",Icons.Default.Folder),
            UiPage("Sync across devices","RSS Core provides account-scoped cloud backup for RSS Clipboard when network access is available.",Icons.Default.CloudSync),
            UiPage("Your settings","Use Light, Dark or System theme and access RSS information, privacy, terms and contact pages.",Icons.Default.Settings)
        )
        val f=fs[page]
        Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            AnimatedContent(page,label="feature"){ Icon(f.icon,null,Modifier.size(76.dp),tint=MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(22.dp));AnimatedContent(page,label="title"){Text(f.title,fontSize=28.sp,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.height(10.dp));Text(f.body,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){fs.indices.forEach{i->Box(Modifier.size(if(i==page)10.dp else 7.dp).clip(CircleShape).background(if(i==page)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))}}
            Spacer(Modifier.height(28.dp));Button({if(page<fs.lastIndex)page++ else next()},Modifier.fillMaxWidth()){Text(if(page<fs.lastIndex)"Next" else "Open RSS Clipboard")}
        }
    }

    @Composable private fun MainScreen(account:Account?,onAccountUpdate:(Account?)->Unit,onMenu:()->Unit,onRefresh:()->Unit) {
        var query by remember{mutableStateOf("")}; var clips by remember{mutableStateOf(loadClips())}; var saveText by remember{mutableStateOf<String?>(null)}
        LaunchedEffect(Unit){while(true){delay(1500);clips=loadClips()}}
        LaunchedEffect(account?.appKey){ if(account!=null && getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("cloud_sync_enabled",true)) while(true){ delay(30000); syncNow(account) } }
        val pending=account?.verified==false
        Scaffold(topBar={TopAppBar(title={Row(verticalAlignment=Alignment.CenterVertically){Logo(Modifier.size(38.dp));Spacer(Modifier.width(10.dp));Text("RSS Clipboard")}},navigationIcon={IconButton(onClick=onMenu){Icon(Icons.Default.Menu,"Menu")}})}){pad->
            LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=28.dp)){
                if(pending)item{VerificationBanner(account!!){updated -> onAccountUpdate(updated)}}
                item{OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Search clipboard")},leadingIcon={Icon(Icons.Default.Search,null)})}
                val filtered=clips.filter{it.text.contains(query,true)}
                if(filtered.isEmpty())item{Box(Modifier.fillMaxWidth().height(260.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.ContentCopy,null,Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary);Text("No clipboard items yet",fontWeight=FontWeight.Bold);Text("Copy something outside RSS Clipboard.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
                items(filtered,key={it.time.toString()+it.text}){c->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(c.type=="URL")Icons.Default.Link else if(c.type=="Email")Icons.Default.Email else Icons.Default.ContentCopy,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text(c.type,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date(c.time)),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Spacer(Modifier.height(8.dp));Text(c.text,maxLines=6);Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton({copy(c.text)}){Text("Copy again")};TextButton({saveText=c.text}){Text("Save to list")}}}}}
            }
        }
        if(saveText!=null)SaveDialog(saveText!!,{saveText=null})
    }

    @Composable private fun VerificationBanner(a:Account,onUpdated:(Account)->Unit) {
        var remaining by remember{mutableLongStateOf((a.expiresAt?:0L)-System.currentTimeMillis())};var status by remember{mutableStateOf("")}
        LaunchedEffect(a.email){while(true){remaining=(a.expiresAt?:0L)-System.currentTimeMillis();delay(1000)}}
        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){Column(Modifier.padding(14.dp)){Row{Icon(Icons.Default.MarkEmailUnread,null);Spacer(Modifier.width(8.dp));Text("Email Verification Pending",fontWeight=FontWeight.Bold)};Text(if(remaining>0)"Verify your email. Time remaining: "+formatCountdown(remaining) else "Verification window expired; request a new verification email.",fontSize=13.sp);Row{TextButton({checkVerification(a,onUpdated){status=it}}){Text("Check Status")};TextButton({resendVerification(a){status=it}}){Text("Resend")}};if(status.isNotBlank())Text(status,fontSize=12.sp)}}}
    
    private fun checkVerification(a:Account,onUpdated:(Account)->Unit,show:(String)->Unit){kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO){try{val r=CoreClient.get("/api/v1/license/verification-status?email="+java.net.URLEncoder.encode(a.email,"UTF-8")+"&project_key="+APP_ID);val ok=r.optBoolean("email_verified",false);val expires=r.optLong("verification_expires_at",0L); getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("verified",ok).putLong("verification_expires",expires).apply(); withContext(Dispatchers.Main){ onUpdated(a.copy(verified=ok,expiresAt=expires.takeIf{it>0})); show(if(ok)"Email verified." else "Still pending.")}}catch(_:Exception){withContext(Dispatchers.Main){show("Could not reach RSS Core.")}}}}
    private fun resendVerification(a:Account,show:(String)->Unit){kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO){try{CoreClient.post("/api/v1/license/resend-verification",JSONObject().put("email",a.email).put("display_name",a.name).put("project_key",APP_ID).put("device_id",deviceId()));withContext(Dispatchers.Main){show("Verification email sent.")}}catch(_:Exception){withContext(Dispatchers.Main){show("Resend failed. Check your connection.")}}}}

    private fun formatCountdown(ms:Long):String {val s=(ms/1000).coerceAtLeast(0);return String.format(Locale.US,"%02dh %02dm %02ds",s/3600,(s%3600)/60,s%60)}
    private fun copy(s:String){(getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(ClipData.newPlainText("RSS Clipboard",s))}
    private fun loadClips():List<ClipItem>{val j=JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString("clips_json","[]"));return buildList{for(i in 0 until j.length()){val o=j.optJSONObject(i)?:continue;val t=o.optString("text");val tm=o.optLong("time");if(t.isNotBlank()&&tm>=System.currentTimeMillis()-DAY)add(ClipItem(t,o.optString("type","Text"),tm))}}.sortedByDescending{it.time}}
    
    @Composable private fun SaveDialog(text:String,close:()->Unit) {
        var selected by remember{mutableStateOf<String?>(null)};var newName by remember{mutableStateOf("")};val lists=loadLists()
        AlertDialog(onDismissRequest=close,title={Text("Save to list")},text={Column{if(lists.isEmpty())Text("Create a list first.") else lists.forEach{name->TextButton({saveToList(name,text);close()},Modifier.fillMaxWidth()){Icon(Icons.Default.Folder,null);Spacer(Modifier.width(8.dp));Text(name)}};OutlinedTextField(newName,{newName=it},label={Text("New list")},singleLine=true,modifier=Modifier.fillMaxWidth())}},confirmButton={Button(enabled=newName.isNotBlank(),onClick={createList(newName.trim());saveToList(newName.trim(),text);close()}){Text("Create & Save")}},dismissButton={TextButton(close){Text("Cancel")}})
    }
    private fun loadLists(): Set<String> =getSharedPreferences(PREFS,MODE_PRIVATE).getStringSet("clip_lists",emptySet()).orEmpty()
    private fun createList(n:String){val p=getSharedPreferences(PREFS,MODE_PRIVATE);val s=p.getStringSet("clip_lists",emptySet()).orEmpty().toMutableSet();s.add(n);p.edit().putStringSet("clip_lists",s).apply()}
    private fun saveToList(n:String,t:String){val p=getSharedPreferences(PREFS,MODE_PRIVATE);val s=p.getStringSet("list_$n",emptySet()).orEmpty().toMutableSet();s.add(t);p.edit().putStringSet("list_$n",s).apply();loadAccount()?.let{syncNow(it)}}

    @Composable private fun SettingsScreen(theme:String,setTheme:(String)->Unit,a:Account?,onBack:()->Unit) {Scaffold(topBar={TopAppBar(title={Text("Settings")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")}})}){p->Column(Modifier.fillMaxSize().padding(p).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Appearance",fontWeight=FontWeight.Bold);listOf("system" to "System","light" to "Light","dark" to "Dark").forEach{(k,l)->ListItem(headlineContent={Text(l)},leadingContent={Icon(if(k=="dark")Icons.Default.DarkMode else if(k=="light")Icons.Default.LightMode else Icons.Default.Settings,null)},trailingContent={RadioButton(theme==k){setTheme(k)}})};Divider();Text("Account",fontWeight=FontWeight.Bold);Text(a?.email ?: "Not signed in");Text("RSS Core: $CORE_URL",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

    @Composable private fun StaticScreen(page:UiPage,back:()->Unit){Scaffold(topBar={TopAppBar(title={Text(page.title)},navigationIcon={IconButton(back){Icon(Icons.Default.ArrowBack,"Back")}})}){p->Column(Modifier.fillMaxSize().padding(p).padding(24.dp)){Icon(page.icon,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(18.dp));Text(page.body,fontSize=16.sp,lineHeight=25.sp)}}}

    @Composable private fun Drawer(account:Account?,onClose:()->Unit,onNavigate:(String)->Unit) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.22f))) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(310.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Logo(Modifier.size(58.dp));Spacer(Modifier.width(12.dp));Column{Text("RSS Clipboard",fontWeight=FontWeight.Bold,fontSize=20.sp);Text(account?.email ?: "RSS account",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                    DrawerItem(Icons.Default.Home,"Home"){onNavigate("home")};DrawerItem(Icons.Default.Settings,"Settings"){onNavigate("settings")};DrawerItem(Icons.Default.Info,"About"){onNavigate("about")};DrawerItem(Icons.Default.Email,"Contact"){onNavigate("contact")};DrawerItem(Icons.Default.PrivacyTip,"Privacy"){onNavigate("privacy")};DrawerItem(Icons.Default.Description,"Terms"){onNavigate("terms")}
                    Spacer(Modifier.weight(1f));Divider()
                    Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){Image(painterResource(R.drawable.rss_company_logo),"Razeen Secure Solution",Modifier.size(78.dp),contentScale=ContentScale.Fit);Spacer(Modifier.height(8.dp));Text("Razeen Secure Solution",fontWeight=FontWeight.Bold);Text("www.rssapps.cv",fontSize=12.sp);Text("RSS Clipboard",fontSize=12.sp);Text("Version 2.0.0",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
                Spacer(Modifier.weight(1f).fillMaxHeight().clickable(onClick=onClose))
            }
        }
    }
    @Composable private fun DrawerItem(icon:ImageVector,title:String,click:()->Unit){ListItem(headlineContent={Text(title)},leadingContent={Icon(icon,null,tint=MaterialTheme.colorScheme.primary)},modifier=Modifier.clickable{click()})}
}

private object CoreClient {
    private fun request(path:String,method:String,body:JSONObject?,appKey:String?=null):JSONObject {
        val c=URL(CORE_URL+path).openConnection() as HttpURLConnection
        c.requestMethod=method;c.connectTimeout=10000;c.readTimeout=15000;c.setRequestProperty("Accept","application/json");c.setRequestProperty("X-RSS-App-Id",APP_ID)
        if(appKey!=null)c.setRequestProperty("X-RSS-App-Key",appKey)
        if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray())}}
        val stream=if(c.responseCode in 200..299)c.inputStream else c.errorStream
        val raw=stream?.bufferedReader()?.use{it.readText()} ?: "{}"
        if(c.responseCode !in 200..299)throw IllegalStateException(raw)
        return JSONObject(raw)
    }
    suspend fun post(path:String,body:JSONObject,appKey:String?=null)=withContext(Dispatchers.IO){request(path,"POST",body,appKey)}
    suspend fun get(path:String,appKey:String?=null)=withContext(Dispatchers.IO){request(path,"GET",null,appKey)}
    suspend fun put(path:String,appKey:String,body:JSONObject)=withContext(Dispatchers.IO){request(path,"PUT",body,appKey)}
}
