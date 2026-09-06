package com.riyaz.rssclipboard.data

import android.util.Patterns

object ClipboardDetector {
    private val sensitive = Regex("(?i)(password|passwd|pwd|otp|one[- ]time password|secret|api[_ -]?key|token)\\s*[:=]", RegexOption.IGNORE_CASE)
    private val phone = Regex("^\\+?[0-9][0-9 ()-]{6,}$")

    fun detect(text: String): ClipboardType {
        val value = text.trim()
        return when {
            sensitive.containsMatchIn(value) -> ClipboardType.SENSITIVE
            Patterns.EMAIL_ADDRESS.matcher(value).matches() -> ClipboardType.EMAIL
            Patterns.WEB_URL.matcher(value).matches() -> ClipboardType.URL
            phone.matches(value) -> ClipboardType.PHONE
            else -> ClipboardType.TEXT
        }
    }
}
