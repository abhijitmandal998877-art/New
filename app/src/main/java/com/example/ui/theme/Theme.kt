package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Rose80,
    secondary = Purple80,
    tertiary = SageCalm,
    background = SoftPinkDarkBg,
    surface = SoftPinkDarkBg,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = SoftPinkBg,
    onSurface = SoftPinkBg
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RosePrimary,
    secondary = LavenderSecondary,
    tertiary = SageCalm,
    background = SoftPinkBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText,
    primaryContainer = RoseContainer,
    onPrimaryContainer = OnRoseContainer,
    secondaryContainer = PurpleContainer,
    onSecondaryContainer = OnPurpleContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to keep our beautiful palette consistent
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, content = content)
}
