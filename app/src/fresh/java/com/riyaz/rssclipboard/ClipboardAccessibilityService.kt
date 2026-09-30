package com.riyaz.rssclipboard

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Patterns
import org.json.JSONArray
import org.json.JSONObject

/**
 * Optional Android 10+ clipboard-access fallback.
 *
 * Android restricts clipboard reads while an ordinary app is not focused.
 * The user must explicitly enable this service in Android Accessibility
 * settings for continuous closed-app capture on affected devices.
 */
class ClipboardAccessibilityService : AccessibilityService() {
    private lateinit var clipboard: ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
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

            val cutoff = now - DAY_MILLIS
            val retained = JSONArray()
            for (i in 0 until updated.length()) {
                val item = updated.optJSONObject(i) ?: continue
                if (item.optLong("time", 0L) >= cutoff) retained.put(item)
            }

            prefs.edit().putString(CLIPS_KEY, retained.toString()).apply()
            sendBroadcast(
                Intent(ACTION_CLIPBOARD_UPDATED).setPackage(packageName)
            )
        } catch (_: SecurityException) {
            // Device policy may still deny clipboard access.
        } catch (_: Exception) {
            // Never terminate the accessibility service because of clipboard data.
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        listener?.let {
            try { clipboard.removePrimaryClipChangedListener(it) } catch (_: Exception) {}
        }
        listener = null
        super.onDestroy()
    }

    companion object {
        private const val PREFS = "rss_clipboard"
        private const val CLIPS_KEY = "clips_json"
        private const val ACTION_CLIPBOARD_UPDATED =
            "com.riyaz.rssclipboard.CLIPBOARD_UPDATED"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
