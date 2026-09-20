package com.riyaz.rssclipboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Patterns
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardCaptureService : Service() {
    private lateinit var clipboard: ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                1001,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        listener = ClipboardManager.OnPrimaryClipChangedListener { captureCurrentClip() }
        clipboard.addPrimaryClipChangedListener(listener)
    }

    private fun captureCurrentClip() {
        try {
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
            if (text.isNullOrEmpty()) return

            val prefs = getSharedPreferences("rss_clipboard", MODE_PRIVATE)
            val values = prefs.getStringSet("clips", emptySet())?.toMutableSet() ?: mutableSetOf()
            val type = when {
                Patterns.EMAIL_ADDRESS.matcher(text).matches() -> "Email"
                Patterns.WEB_URL.matcher(text).matches() -> "URL"
                else -> "Text"
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            values.removeAll { it.substringBefore("|") == text }
            values.add("$text|$type|$stamp")
            prefs.edit().putStringSet("clips", values).apply()
        } catch (_: Exception) {
            // Clipboard access can be restricted by Android/device policy.
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "clipboard_capture",
                "Clipboard capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps RSS Clipboard capture active while the app UI is closed."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, "clipboard_capture")
            .setSmallIcon(R.drawable.rss_clipboard_logo)
            .setContentTitle("RSS Clipboard")
            .setContentText("Clipboard capture is active")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    override fun onDestroy() {
        listener?.let { clipboard.removePrimaryClipChangedListener(it) }
        listener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
