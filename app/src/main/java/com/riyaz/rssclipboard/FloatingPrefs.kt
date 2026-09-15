package com.riyaz.rssclipboard

import android.content.Context

object FloatingPrefs {
    private const val NAME = "floating_clipboard"
    private const val ENABLED = "enabled"
    private const val SHOW_BUBBLE = "show_bubble"
    private const val OPEN_ON_COPY = "open_on_copy"
    private const val CLOSE_AFTER_COPY = "close_after_copy"
    private const val AUTO_HIDE = "auto_hide"
    private const val HIDE_TIMER = "hide_timer_seconds"
    private const val SIZE = "size"
    private const val NOTIFICATIONS = "notifications"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun enabled(context: Context) = prefs(context).getBoolean(ENABLED, false)
    fun showBubble(context: Context) = prefs(context).getBoolean(SHOW_BUBBLE, true)
    fun openOnCopy(context: Context) = prefs(context).getBoolean(OPEN_ON_COPY, false)
    fun closeAfterCopy(context: Context) = prefs(context).getBoolean(CLOSE_AFTER_COPY, true)
    fun autoHide(context: Context) = prefs(context).getBoolean(AUTO_HIDE, false)
    fun hideTimerSeconds(context: Context) = prefs(context).getInt(HIDE_TIMER, 15)
    fun size(context: Context) = prefs(context).getString(SIZE, "medium") ?: "medium"
    fun notifications(context: Context) = prefs(context).getBoolean(NOTIFICATIONS, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(ENABLED, value).apply()
    fun setShowBubble(context: Context, value: Boolean) = prefs(context).edit().putBoolean(SHOW_BUBBLE, value).apply()
    fun setOpenOnCopy(context: Context, value: Boolean) = prefs(context).edit().putBoolean(OPEN_ON_COPY, value).apply()
    fun setCloseAfterCopy(context: Context, value: Boolean) = prefs(context).edit().putBoolean(CLOSE_AFTER_COPY, value).apply()
    fun setAutoHide(context: Context, value: Boolean) = prefs(context).edit().putBoolean(AUTO_HIDE, value).apply()
    fun setHideTimerSeconds(context: Context, value: Int) = prefs(context).edit().putInt(HIDE_TIMER, value).apply()
    fun setSize(context: Context, value: String) = prefs(context).edit().putString(SIZE, value).apply()
    fun setNotifications(context: Context, value: Boolean) = prefs(context).edit().putBoolean(NOTIFICATIONS, value).apply()
}
