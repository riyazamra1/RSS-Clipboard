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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
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
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var hideJob: Job? = null
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
            val text = clip.getItemAt(0).coerceToText(this).toString().trim()
            if (text.isBlank()) return@OnPrimaryClipChangedListener
            scope.launch {
                repository.add(text)
                repository.deleteExpired()
                if (FloatingPrefs.openOnCopy(this@FloatingClipboardService)) mainScope.launch { showDialog() }
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
        if (Settings.canDrawOverlays(this) && FloatingPrefs.showBubble(this)) showBubble()
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("RSS Clipboard")
            .setContentText("Floating clipboard is active")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .build()
    }

    private fun overlayType() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    private fun showBubble() {
        if (bubble != null || !Settings.canDrawOverlays(this)) return
        val view = FloatingBubbleView(this) {
            showDialog()
            resetHideTimer()
        }
        bubble = view
        windowManager.addView(view, bubbleParams())
        resetHideTimer()
    }

    private fun resetHideTimer() {
        hideJob?.cancel()
        hideJob = null
        if (!FloatingPrefs.autoHide(this) || bubble == null) return
        val seconds = FloatingPrefs.hideTimerSeconds(this)
        if (seconds <= 0) return
        hideJob = mainScope.launch {
            delay(seconds * 1000L)
            if (bubble != null) hideBubble()
        }
    }

    private fun hideBubble() {
        hideJob?.cancel()
        hideJob = null
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
    }

    private fun showDialog() {
        if (dialog != null || !Settings.canDrawOverlays(this)) return
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
            text = "Close"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(12, 18, 12, 8)
            setOnClickListener { hideDialog() }
        })
        dialog = root
        windowManager.addView(root, dialogParams())
        scope.launch {
            repository.deleteExpired()
            val items = repository.items.first()
            mainScope.launch { populate(list, items) }
        }
    }

    private fun populate(list: LinearLayout, items: List<ClipboardItem>) {
        list.removeAllViews()
        if (items.isEmpty()) {
            list.addView(TextView(this).apply { text = "No clipboard items"; setPadding(0, 20, 0, 20) })
            return
        }
        items.take(50).forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 10, 8, 10)
                setOnClickListener {
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("RSS Clipboard", item.content))
                    if (FloatingPrefs.closeAfterCopy(this@FloatingClipboardService)) hideDialog()
                }
            }
            row.addView(TextView(this).apply {
                text = "#${index + 1}  ${item.type.name}${if (item.pinned) "  •  PINNED" else ""}"
                textSize = 12f
            })
            row.addView(TextView(this).apply {
                text = item.content
                textSize = 16f
                maxLines = 4
                setPadding(0, 4, 0, 0)
            })
            list.addView(row)
        }
    }

    private fun hideDialog() {
        dialog?.let { runCatching { windowManager.removeView(it) } }
        dialog = null
    }

    private fun bubbleParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        x = 18
    }

    private fun dialogParams() = WindowManager.LayoutParams(
        (resources.displayMetrics.widthPixels * dialogWidth()).toInt(),
        (resources.displayMetrics.heightPixels * dialogHeight()).toInt(),
        overlayType(),
        WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
        dimAmount = 0.35f
    }

    private fun dialogWidth() = when (FloatingPrefs.size(this)) { "small" -> 0.76f; "large" -> 0.94f; else -> 0.88f }
    private fun dialogHeight() = when (FloatingPrefs.size(this)) { "small" -> 0.52f; "large" -> 0.82f; else -> 0.68f }

    override fun onDestroy() {
        listener?.let { clipboard.removePrimaryClipChangedListener(it) }
        hideDialog()
        hideBubble()
        scope.cancel()
        mainScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            // A foreground service needs a usable notification channel. The user preference
            // controls notification importance, but never disables the FGS channel itself.
            val importance = NotificationManager.IMPORTANCE_LOW
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Clipboard monitor", importance).apply {
                description = "RSS Clipboard background monitoring notification"
            })
        }
    }

    companion object {
        private const val CHANNEL_ID = "rss_clipboard_monitor"
        private const val NOTIFICATION_ID = 4015
    }
}
