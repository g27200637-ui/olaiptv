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

private val ImmersiveColorScheme = darkColorScheme(
    primary = ImmersiveAccentBlue,
    onPrimary = ImmersiveTextWhite,
    secondary = ImmersiveLightBlue,
    onSecondary = ImmersiveTextWhite,
    tertiary = ImmersiveNeonLabel,
    background = ImmersiveBg,
    onBackground = ImmersiveTextMain,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextMain,
    surfaceVariant = ImmersiveCard,
    onSurfaceVariant = ImmersiveTextMain,
    outline = ImmersiveLightBlue.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false, // Disable to force our custom high-fidelity cinematic look
  content: @Composable () -> Unit,
) {
  val colorScheme = ImmersiveColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
