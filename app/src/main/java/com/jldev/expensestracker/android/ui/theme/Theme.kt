package com.jldev.expensestracker.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palettes carried over from the Expo app's tailwind.config.js.
object DodgerBlue {
    val c100 = Color(0xFFE1EEFD)
    val c200 = Color(0xFFBCDCFB)
    val c300 = Color(0xFF81C1F8)
    val c400 = Color(0xFF3DA1F3)
    val c500 = Color(0xFF1D8EEB)
    val c600 = Color(0xFF0768C2)
    val c700 = Color(0xFF07539D)
    val c800 = Color(0xFF0A4782)
    val c900 = Color(0xFF0F3C6B)
    val c950 = Color(0xFF0A2647)
}

object PersianRed {
    val c400 = Color(0xFFFF676C)
    val c600 = Color(0xFFDA141A)
    val c800 = Color(0xFFA31115)
    val c900 = Color(0xFF861619)
}

// Tailwind colors the Expo app uses outside its custom palettes.
object Tailwind {
    val neutral900 = Color(0xFF171717)
    val green500 = Color(0xFF22C55E)
    val yellow500 = Color(0xFFEAB308)
    val orange500 = Color(0xFFF97316)
}

private val DarkColors = darkColorScheme(
    primary = DodgerBlue.c500,
    onPrimary = Color.White,
    primaryContainer = DodgerBlue.c900,
    onPrimaryContainer = DodgerBlue.c300,
    secondary = DodgerBlue.c300,
    error = PersianRed.c400,
    errorContainer = PersianRed.c800,
    onErrorContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceContainer = DodgerBlue.c950,
)

// The Expo app forces dark mode (userInterfaceStyle: "dark"), so there is no light scheme.
@Composable
fun ExpensesTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
