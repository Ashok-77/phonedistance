package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
  primary = CyanPrimaryDark,
  onPrimary = CyanOnPrimaryDark,
  primaryContainer = CyanContainerDark,
  onPrimaryContainer = CyanOnContainerDark,
  secondary = CobaltSecondaryDark,
  onSecondary = Color(0xFF002033),
  secondaryContainer = CobaltContainerDark,
  onSecondaryContainer = CobaltOnContainerDark,
  tertiary = CalibratedEmerald,
  onTertiary = Color(0xFF002116),
  tertiaryContainer = CalibratedEmeraldContainerDark,
  onTertiaryContainer = Color(0xFFA7F3D0),
  background = DarkBackground,
  onBackground = DarkOnBackground,
  surface = DarkSurface,
  onSurface = DarkOnBackground,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  error = ErrorCoral,
  onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
  primary = CyanPrimaryLight,
  onPrimary = CyanOnPrimaryLight,
  primaryContainer = CyanContainerLight,
  onPrimaryContainer = CyanOnContainerLight,
  secondary = CobaltSecondaryLight,
  onSecondary = Color.White,
  secondaryContainer = CobaltContainerLight,
  onSecondaryContainer = CobaltOnContainerLight,
  tertiary = Color(0xFF059669),
  onTertiary = Color.White,
  tertiaryContainer = CalibratedEmeraldContainerLight,
  onTertiaryContainer = Color(0xFF065F46),
  background = LightBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnBackground,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  error = Color(0xFFDC2626),
  onError = Color.White,
)

val VisionMeasureShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VisionMeasureTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val useDarkTheme = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    useDarkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = VisionMeasureTypography,
    shapes = VisionMeasureShapes,
    content = content,
  )
}
