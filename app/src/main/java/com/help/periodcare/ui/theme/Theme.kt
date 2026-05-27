package com.help.periodcare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = PinkPrimary,
  onPrimary = Color.White,
  primaryContainer = LightPinkPill,
  onPrimaryContainer = DarkText,
  secondary = PinkAccent,
  onSecondary = Color.White,
  background = SoftPinkBg,
  onBackground = DarkText,
  surface = Color.White,
  onSurface = DarkText,
  surfaceVariant = SelectionPill,
  onSurfaceVariant = SoftGrayText,
  outline = DividerColor
)

private val DarkColorScheme = darkColorScheme(
  primary = PinkPrimary,
  onPrimary = Color.White,
  primaryContainer = DarkText,
  onPrimaryContainer = LightPinkPill,
  secondary = PinkAccent,
  onSecondary = Color.White,
  background = Color(0xFF201A1A),
  onBackground = SoftPinkBg,
  surface = Color(0xFF2B2222),
  onSurface = SoftPinkBg,
  surfaceVariant = Color(0xFF3B2E2E),
  onSurfaceVariant = SoftGrayText,
  outline = Color(0xFF4C3E3E)
)

@Composable
fun PeriodCareTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) {
    DarkColorScheme
  } else {
    LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
