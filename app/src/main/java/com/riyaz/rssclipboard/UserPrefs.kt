package com.riyaz.rssclipboard

import android.content.Context

object UserPrefs {
    private const val NAME = "rss_user"
    private const val REGISTERED = "registered"
    private const val USER_NAME = "user_name"
    private const val EMAIL = "email"
    private const val WELCOME_SEEN = "welcome_seen"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun isRegistered(context: Context) = prefs(context).getBoolean(REGISTERED, false)
    fun name(context: Context) = prefs(context).getString(USER_NAME, "") ?: ""
    fun email(context: Context) = prefs(context).getString(EMAIL, "") ?: ""
    fun welcomeSeen(context: Context) = prefs(context).getBoolean(WELCOME_SEEN, false)
    fun setWelcomeSeen(context: Context) { prefs(context).edit().putBoolean(WELCOME_SEEN, true).apply() }
    fun register(context: Context, name: String, email: String) {
        prefs(context).edit().putBoolean(REGISTERED, true).putString(USER_NAME, name.trim()).putString(EMAIL, email.trim()).apply()
    }
}
