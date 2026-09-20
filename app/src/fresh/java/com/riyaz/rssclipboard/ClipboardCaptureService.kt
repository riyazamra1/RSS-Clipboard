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
import org.json.JSONArray
import org.json.JSONObject
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
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        listener = ClipboardManager.OnPrimaryClipChangedListener { captureCurrentClip() }
        clipboard.addPrimaryClipChangedListener(listener)
    }

    private fun captureCurrentClip() {
        try {
            val text = clipboard.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
                ?.trim()
                ?: return
            if (text.isEmpty()) return

            val now = System.currentTimeMillis()
            val type = when {
                Patterns.EMAIL_ADDRESS.matcher(text).matches() -> "Email"
                Patterns.WEB_URL.matcher(text).matches() -> "URL"
                else -> "Text"
            }

            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            val json = JSONArray(prefs.getString(CLIPS_KEY, "[]"))
            val updated = JSONArray()

            // One canonical entry per clipboard value. A clipboard write caused by
            // the app itself is therefore not stored as a second copy.
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                if (item.optString("text") != text) updated.put(item)
            }

            updated.put(
                JSONObject()
                    .put("text", text)
                    .put("type", type)
                    .put("time", now)
            )

            // Keep the requested 24-hour clipboard memory.
            val cutoff = now - DAY_MILLIS
            val retained = JSONArray()
            for (i in 0 until updated.length()) {
                val item = updated.optJSONObject(i) ?: continue
                if (item.optLong("time", 0L) >= cutoff) retained.put(item)
            }

            prefs.edit().putString(CLIPS_KEY, retained.toString()).apply()

            // Tell an already-open UI to refresh without making the UI responsible
            // for clipboard capture.
            sendBroadcast(Intent(ACTION_CLIPBOARD_UPDATED).setPackage(packageName))
        } catch (_: SecurityException) {
            // Android/device clipboard policy can deny background access.
        } catch (_: Exception) {
            // Never let a malformed clipboard payload terminate the service.
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps RSS Clipboard capture active while the app UI is closed."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
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

    companion object {
        private const val PREFS = "rss_clipboard"
        private const val CLIPS_KEY = "clips_json"
        private const val CHANNEL_ID = "clipboard_capture"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_CLIPBOARD_UPDATED =
            "com.riyaz.rssclipboard.CLIPBOARD_UPDATED"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
