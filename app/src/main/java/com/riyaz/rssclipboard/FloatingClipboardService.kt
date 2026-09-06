package com.riyaz.rssclipboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.riyaz.rssclipboard.data.AppDatabase
import com.riyaz.rssclipboard.data.ClipboardItem
import com.riyaz.rssclipboard.data.ClipboardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class FloatingClipboardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var clipboard: ClipboardManager
    private lateinit var repository: ClipboardRepository
    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var dialog: View? = null
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        repository = ClipboardRepository(AppDatabase.get(this).clipboardDao())
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboard.primaryClip ?: return@OnPrimaryClipChangedListener
            val text = clip.getItemAt(0).coerceToText(this).toString()
            if (text.isNotBlank()) scope.launch { repository.add(text) }
        }
        clipboard.addPrimaryClipChangedListener(listener)
        if (Settings.canDrawOverlays(this)) showBubble()
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("RSS Clipboard")
            .setContentText("Clipboard monitor is active")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    private fun showBubble() {
        if (bubble != null || !Settings.canDrawOverlays(this)) return
        val view = TextView(this).apply {
            text = "📋"
            textSize = 22f
            gravity = Gravity.CENTER
            setBackgroundResource(android.R.drawable.btn_default)
            setPadding(18, 12, 18, 12)
            setOnClickListener { showDialog() }
        }
        bubble = view
        windowManager.addView(view, bubbleParams())
    }

    private fun showDialog() {
        if (dialog != null) return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 16)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(TextView(this).apply { text = "RSS Clipboard"; textSize = 20f; setPadding(0, 0, 0, 12) })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "Close"; textSize = 16f; gravity = Gravity.CENTER; setPadding(12, 18, 12, 8)
            setOnClickListener { hideDialog() }
        })
        dialog = root
        windowManager.addView(root, dialogParams())
        scope.launch {
            val items = repository.items.first()
            withContext(Dispatchers.Main) { populate(list, items) }
        }
    }

    private fun populate(list: LinearLayout, items: List<ClipboardItem>) {
        list.removeAllViews()
        if (items.isEmpty()) {
            list.addView(TextView(this).apply { text = "No saved clipboard items"; setPadding(0, 20, 0, 20) })
            return
        }
        items.take(50).forEach { item ->
            list.addView(TextView(this).apply {
                text = item.content; textSize = 16f; maxLines = 4; setPadding(8, 18, 8, 18)
                setOnClickListener {
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("RSS Clipboard", item.content))
                    hideDialog()
                }
            })
        }
    }

    private fun hideDialog() {
        dialog?.let { runCatching { windowManager.removeView(it) } }
        dialog = null
    }

    private fun bubbleParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 18 }

    private fun dialogParams() = WindowManager.LayoutParams(
        (resources.displayMetrics.widthPixels * 0.88f).toInt(), (resources.displayMetrics.heightPixels * 0.68f).toInt(),
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_DIM_BEHIND, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.CENTER; dimAmount = 0.35f }

    override fun onDestroy() {
        listener?.let { clipboard.removePrimaryClipChangedListener(it) }
        hideDialog()
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Clipboard monitor", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object { private const val CHANNEL_ID = "rss_clipboard_monitor"; private const val NOTIFICATION_ID = 4015 }
}
