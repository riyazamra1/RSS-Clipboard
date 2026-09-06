package com.riyaz.rssclipboard

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UbuntuOrange = Color(0xFFE95420)
private val UbuntuPurple = Color(0xFF772953)
private val WindowsBlue = Color(0xFF0078D4)
private val AndroidGreen = Color(0xFF3DDC84)
private val MacBlue = Color(0xFF007AFF)
private val IosIndigo = Color(0xFF5856D6)

@Composable
fun RssClipboardTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        AppTheme.WINDOWS -> lightColorScheme(primary = WindowsBlue, secondary = Color(0xFF106EBE))
        AppTheme.LINUX -> lightColorScheme(primary = UbuntuOrange, secondary = UbuntuPurple, tertiary = Color(0xFF5E2750))
        AppTheme.ANDROID -> lightColorScheme(primary = AndroidGreen, onPrimary = Color(0xFF003919), secondary = Color(0xFF006C4C))
        AppTheme.MACOS -> lightColorScheme(primary = MacBlue, secondary = Color(0xFF34C759))
        AppTheme.IOS -> lightColorScheme(primary = IosIndigo, secondary = Color(0xFF007AFF))
        AppTheme.CLASSIC -> lightColorScheme(primary = Color(0xFF455A64), secondary = Color(0xFF607D8B))
        AppTheme.SYSTEM -> MaterialTheme.colorScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
