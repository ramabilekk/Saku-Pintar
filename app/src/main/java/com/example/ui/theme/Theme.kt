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
  primary = EmeraldPrimary,
  onPrimary = OnEmeraldPrimary,
  primaryContainer = EmeraldContainer,
  onPrimaryContainer = OnEmeraldContainer,
  secondary = MintSecondary,
  onSecondary = OnMintSecondary,
  secondaryContainer = MintContainer,
  onSecondaryContainer = OnMintContainer,
  tertiary = AmberTertiary,
  onTertiary = OnAmberTertiary,
  tertiaryContainer = AmberContainer,
  onTertiaryContainer = OnAmberContainer,
  background = WarmIvoryBackground,
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
  primary = EmeraldDarkPrimary,
  onPrimary = OnEmeraldDarkPrimary,
  primaryContainer = EmeraldDarkContainer,
  onPrimaryContainer = OnEmeraldDarkContainer,
  secondary = MintDarkSecondary,
  onSecondary = OnMintDarkSecondary,
  secondaryContainer = MintDarkContainer,
  onSecondaryContainer = OnMintDarkContainer,
  tertiary = AmberDarkTertiary,
  onTertiary = OnAmberDarkTertiary,
  tertiaryContainer = AmberDarkContainer,
  onTertiaryContainer = OnAmberDarkContainer,
  background = ObsidianBackground,
  onBackground = IvoryText,
  surface = ObsidianSurface,
  onSurface = IvoryText,
  surfaceVariant = ObsidianSurfaceVariant,
  onSurfaceVariant = MutedSageText,
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
