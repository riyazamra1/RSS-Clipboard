package com.riyaz.rssclipboard

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.riyaz.rssclipboard.data.AppDatabase
import com.riyaz.rssclipboard.data.ClipboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ClipboardAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var clipboard: ClipboardManager
    private lateinit var repository: ClipboardRepository
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var lastContent: String? = null
    private var lastAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        repository = ClipboardRepository(AppDatabase.get(this).clipboardDao())
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboard.primaryClip ?: return@OnPrimaryClipChangedListener
            if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
            val text = clip.getItemAt(0).coerceToText(this).toString().trim()
            if (text.isBlank()) return@OnPrimaryClipChangedListener
            val now = System.currentTimeMillis()
            if (text == lastContent && now - lastAt < 1200L) return@OnPrimaryClipChangedListener
            lastContent = text
            lastAt = now
            scope.launch {
                repository.add(text)
                repository.deleteExpired()
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        listener?.let { clipboard.removePrimaryClipChangedListener(it) }
        scope.cancel()
        super.onDestroy()
    }
}
