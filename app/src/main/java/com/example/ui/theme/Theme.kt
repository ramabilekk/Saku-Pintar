package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val THEME_TRANSITION_DURATION_MS = 420

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
  surfaceContainerLowest = CardSurface,
  surfaceContainerLow = Color(0xFFF2F8FA),
  surfaceContainer = Color(0xFFE6F0F4),
  surfaceContainerHigh = Color(0xFFD9E8EE),
  surfaceContainerHighest = Color(0xFFCAE0E8),
  outline = Color(0xFFBBD3DC),
  outlineVariant = HairlineLight,
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
  surfaceContainerLowest = MidnightBackground,
  surfaceContainerLow = MidnightSurface,
  surfaceContainer = ObsidianSurface,
  surfaceContainerHigh = MidnightSurfaceVariant,
  surfaceContainerHighest = Color(0xFF283C5C),
  outline = Color(0xFF334E73),
  outlineVariant = HairlineDark,
  error = Color(0xFFF87171),
  errorContainer = Color(0xFF450A0A),
  onErrorContainer = Color(0xFFFECACA),
)

val SakuShapes = Shapes(
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
private fun animatedColor(targetValue: Color, label: String): Color {
  val color by animateColorAsState(
    targetValue = targetValue,
    animationSpec = tween(
      durationMillis = THEME_TRANSITION_DURATION_MS,
      easing = FastOutSlowInEasing,
    ),
    label = label,
  )
  return color
}

@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
  return target.copy(
    primary = animatedColor(target.primary, "theme_primary"),
    onPrimary = animatedColor(target.onPrimary, "theme_onPrimary"),
    primaryContainer = animatedColor(target.primaryContainer, "theme_primaryContainer"),
    onPrimaryContainer = animatedColor(target.onPrimaryContainer, "theme_onPrimaryContainer"),
    secondary = animatedColor(target.secondary, "theme_secondary"),
    onSecondary = animatedColor(target.onSecondary, "theme_onSecondary"),
    secondaryContainer = animatedColor(target.secondaryContainer, "theme_secondaryContainer"),
    onSecondaryContainer = animatedColor(target.onSecondaryContainer, "theme_onSecondaryContainer"),
    tertiary = animatedColor(target.tertiary, "theme_tertiary"),
    onTertiary = animatedColor(target.onTertiary, "theme_onTertiary"),
    tertiaryContainer = animatedColor(target.tertiaryContainer, "theme_tertiaryContainer"),
    onTertiaryContainer = animatedColor(target.onTertiaryContainer, "theme_onTertiaryContainer"),
    background = animatedColor(target.background, "theme_background"),
    onBackground = animatedColor(target.onBackground, "theme_onBackground"),
    surface = animatedColor(target.surface, "theme_surface"),
    onSurface = animatedColor(target.onSurface, "theme_onSurface"),
    surfaceVariant = animatedColor(target.surfaceVariant, "theme_surfaceVariant"),
    onSurfaceVariant = animatedColor(target.onSurfaceVariant, "theme_onSurfaceVariant"),
    surfaceContainerLowest = animatedColor(target.surfaceContainerLowest, "theme_surfaceContainerLowest"),
    surfaceContainerLow = animatedColor(target.surfaceContainerLow, "theme_surfaceContainerLow"),
    surfaceContainer = animatedColor(target.surfaceContainer, "theme_surfaceContainer"),
    surfaceContainerHigh = animatedColor(target.surfaceContainerHigh, "theme_surfaceContainerHigh"),
    surfaceContainerHighest = animatedColor(target.surfaceContainerHighest, "theme_surfaceContainerHighest"),
    outline = animatedColor(target.outline, "theme_outline"),
    outlineVariant = animatedColor(target.outlineVariant, "theme_outlineVariant"),
    error = animatedColor(target.error, "theme_error"),
    errorContainer = animatedColor(target.errorContainer, "theme_errorContainer"),
    onErrorContainer = animatedColor(target.onErrorContainer, "theme_onErrorContainer"),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakuPintarTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val colorScheme = rememberAnimatedColorScheme(targetColorScheme)
  val rippleConfiguration = remember(darkTheme, targetColorScheme.primary) {
    RippleConfiguration(
      color = targetColorScheme.primary,
      rippleAlpha = if (darkTheme) {
        RippleAlpha(
          draggedAlpha = 0.18f,
          focusedAlpha = 0.14f,
          hoveredAlpha = 0.10f,
          pressedAlpha = 0.22f,
        )
      } else {
        RippleAlpha(
          draggedAlpha = 0.14f,
          focusedAlpha = 0.11f,
          hoveredAlpha = 0.08f,
          pressedAlpha = 0.16f,
        )
      },
    )
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = SakuShapes,
  ) {
    CompositionLocalProvider(
      LocalRippleConfiguration provides rippleConfiguration,
      content = content,
    )
  }
}
