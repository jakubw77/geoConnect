package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MinimalDarkPrimary,
    onPrimary = MinimalDarkOnPrimary,
    primaryContainer = MinimalDarkPrimaryContainer,
    onPrimaryContainer = MinimalDarkOnPrimaryContainer,
    secondary = MinimalDarkSecondary,
    onSecondary = MinimalDarkOnSecondary,
    secondaryContainer = MinimalDarkSecondaryContainer,
    onSecondaryContainer = MinimalDarkOnSecondaryContainer,
    background = MinimalDarkBackground,
    onBackground = MinimalDarkOnBackground,
    surface = MinimalDarkSurface,
    onSurface = MinimalDarkOnSurface,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurfaceVariant = MinimalDarkOnSurfaceVariant,
    outline = MinimalOutline,
    outlineVariant = MinimalOutlineVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalPrimary,
    onPrimary = MinimalOnPrimary,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    secondary = MinimalSecondary,
    onSecondary = MinimalOnSecondary,
    secondaryContainer = MinimalSecondaryContainer,
    onSecondaryContainer = MinimalOnSecondaryContainer,
    tertiary = MinimalTertiary,
    onTertiary = MinimalOnTertiary,
    tertiaryContainer = MinimalTertiaryContainer,
    onTertiaryContainer = MinimalOnTertiaryContainer,
    background = MinimalBackground,
    onBackground = MinimalOnBackground,
    surface = MinimalSurface,
    onSurface = MinimalOnSurface,
    surfaceVariant = MinimalSurfaceVariant,
    onSurfaceVariant = MinimalOnSurfaceVariant,
    outline = MinimalOutline,
    outlineVariant = MinimalOutlineVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
