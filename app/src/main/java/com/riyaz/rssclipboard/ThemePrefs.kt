package com.riyaz.rssclipboard

import android.content.Context

enum class AppTheme(val label: String) {
    WINDOWS("Windows"),
    LINUX("Linux / Ubuntu"),
    ANDROID("Android"),
    MACOS("macOS"),
    IOS("iOS"),
    CLASSIC("Other / Classic"),
    SYSTEM("System Default")
}

object ThemePrefs {
    private const val NAME = "app_theme"
    private const val KEY = "theme"

    fun get(context: Context): AppTheme {
        val value = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY, AppTheme.LINUX.name) ?: AppTheme.LINUX.name
        return runCatching { AppTheme.valueOf(value) }.getOrDefault(AppTheme.LINUX)
    }

    fun set(context: Context, theme: AppTheme) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY, theme.name).apply()
    }
}
