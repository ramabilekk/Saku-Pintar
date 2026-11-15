package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = OnIndigoPrimary,
  primaryContainer = IndigoContainer,
  onPrimaryContainer = OnIndigoContainer,
  secondary = VioletSecondary,
  onSecondary = OnVioletSecondary,
  secondaryContainer = VioletContainer,
  onSecondaryContainer = OnVioletContainer,
  tertiary = AmberTertiary,
  onTertiary = OnAmberTertiary,
  tertiaryContainer = AmberContainer,
  onTertiaryContainer = OnAmberContainer,
  background = CoolAlabasterBackground,
  onBackground = SlateInk,
  surface = CardSurface,
  onSurface = SlateInk,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = SlateMuted,
  outline = OutlineSubtle,
  error = CoralDanger,
  errorContainer = CoralContainer,
  onErrorContainer = OnCoralContainer,
)

private val DarkColorScheme = darkColorScheme(
  primary = IndigoDarkPrimary,
  onPrimary = OnIndigoDarkPrimary,
  primaryContainer = IndigoDarkContainer,
  onPrimaryContainer = OnIndigoDarkContainer,
  secondary = VioletDarkSecondary,
  onSecondary = OnVioletDarkSecondary,
  secondaryContainer = VioletDarkContainer,
  onSecondaryContainer = OnVioletDarkContainer,
  tertiary = AmberDarkTertiary,
  onTertiary = OnAmberDarkTertiary,
  tertiaryContainer = AmberDarkContainer,
  onTertiaryContainer = OnAmberDarkContainer,
  background = MidnightBackground,
  onBackground = IceWhiteText,
  surface = MidnightSurface,
  onSurface = IceWhiteText,
  surfaceVariant = MidnightSurfaceVariant,
  onSurfaceVariant = MutedSlateText,
  outline = OutlineDark,
  error = CoralDanger,
  errorContainer = CoralContainer,
  onErrorContainer = OnCoralContainer,
)

val SakuShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SakuPintarTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = SakuShapes,
    content = content,
  )
}
