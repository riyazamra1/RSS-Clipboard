package com.riyaz.rssclipboard

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme

private val UbuntuOrange = Color(0xFFE95420)
private val UbuntuPurple = Color(0xFF772953)
private val WindowsBlue = Color(0xFF0078D4)
private val AndroidGreen = Color(0xFF3DDC84)
private val MacBlue = Color(0xFF007AFF)
private val IosIndigo = Color(0xFF5856D6)

@Composable
fun RssClipboardTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val scheme: ColorScheme = when (theme) {
        AppTheme.WINDOWS -> if (systemDark) darkColorScheme(primary = WindowsBlue, secondary = Color(0xFF60AEEA)) else lightColorScheme(primary = WindowsBlue, secondary = Color(0xFF106EBE))
        AppTheme.LINUX -> if (systemDark) darkColorScheme(primary = UbuntuOrange, secondary = Color(0xFFC17AA0), tertiary = Color(0xFF9B4D7A)) else lightColorScheme(primary = UbuntuOrange, secondary = UbuntuPurple, tertiary = Color(0xFF5E2750))
        AppTheme.ANDROID -> if (systemDark) darkColorScheme(primary = AndroidGreen, onPrimary = Color(0xFF003919), secondary = Color(0xFF65DDB5)) else lightColorScheme(primary = AndroidGreen, onPrimary = Color(0xFF003919), secondary = Color(0xFF006C4C))
        AppTheme.MACOS -> if (systemDark) darkColorScheme(primary = MacBlue, secondary = Color(0xFF62A5FF)) else lightColorScheme(primary = MacBlue, secondary = Color(0xFF34C759))
        AppTheme.IOS -> if (systemDark) darkColorScheme(primary = IosIndigo, secondary = Color(0xFF8B88FF)) else lightColorScheme(primary = IosIndigo, secondary = Color(0xFF007AFF))
        AppTheme.CLASSIC -> if (systemDark) darkColorScheme(primary = Color(0xFF90A4AE), secondary = Color(0xFFB0BEC5)) else lightColorScheme(primary = Color(0xFF455A64), secondary = Color(0xFF607D8B))
        AppTheme.SYSTEM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (systemDark) darkColorScheme() else lightColorScheme()
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
