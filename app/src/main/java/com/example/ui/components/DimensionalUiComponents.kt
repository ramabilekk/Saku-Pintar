package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.theme.ThemePreferenceManager

/**
 * Lightweight controller for subtle, elegant haptic feedback across navigation, buttons, and cards.
 */
@Stable
class TactileFeedbackController(
  private val view: View,
  private val hapticFeedback: HapticFeedback,
) {
  /** Subtle, crisp tap feedback for general buttons, chips, and interactive surfaces. */
  fun tick() {
    runCatching {
      val performed = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
      if (!performed) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
    }
  }

  /** Distinct, smooth tactile pulse when switching primary navigation tabs or pages. */
  fun navigation() {
    runCatching {
      val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.GESTURE_START
      } else {
        HapticFeedbackConstants.VIRTUAL_KEY
      }
      val performed = view.performHapticFeedback(constant)
      if (!performed) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
    }
  }

  /** Tactile confirmation pulse for primary actions (saving, verifying, logging). */
  fun confirm() {
    runCatching {
      val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
      } else {
        HapticFeedbackConstants.VIRTUAL_KEY
      }
      val performed = view.performHapticFeedback(constant)
      if (!performed) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
      }
    }
  }
}

@Composable
fun rememberTactileFeedback(): TactileFeedbackController {
  val view = LocalView.current
  val hapticFeedback = LocalHapticFeedback.current
  return remember(view, hapticFeedback) {
    TactileFeedbackController(view = view, hapticFeedback = hapticFeedback)
  }
}

/**
 * Adds a smooth spring scale-down effect on press along with an optional subtle haptic tick when
 * the user's finger touches down on a button or interactive component.
 */
fun Modifier.tactilePressScale(
  interactionSource: MutableInteractionSource,
  pressedScale: Float = 0.965f,
  enablePressHaptic: Boolean = true,
): Modifier = composed {
  val tactile = rememberTactileFeedback()
  val isPressed by interactionSource.collectIsPressedAsState()
  LaunchedEffect(isPressed) {
    if (isPressed && enablePressHaptic) {
      tactile.tick()
    }
  }
  val scale by animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "tactile_press_scale",
  )
  this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

/**
 * Combines a Material 3 ripple effect, spring press scale animation, and subtle haptic feedback
 * into a single modifier for custom interactive pills, tabs, and calendar cells.
 */
fun Modifier.tactileClickable(
  enabled: Boolean = true,
  pressedScale: Float = 0.96f,
  rippleColor: Color = Color.Unspecified,
  bounded: Boolean = true,
  role: Role? = null,
  onClickLabel: String? = null,
  onClick: () -> Unit,
): Modifier = composed {
  val interactionSource = remember { MutableInteractionSource() }
  val tactile = rememberTactileFeedback()
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "tactile_clickable_scale",
  )
  this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .clickable(
      interactionSource = interactionSource,
      indication = ripple(bounded = bounded, color = rippleColor),
      enabled = enabled,
      onClickLabel = onClickLabel,
      role = role,
      onClick = {
        tactile.tick()
        onClick()
      },
    )
}

@Composable
fun rememberIsEffectiveDarkTheme(): Boolean {
  val context = LocalContext.current
  val themeMode by ThemePreferenceManager.observeThemeMode(context).collectAsStateWithLifecycle()
  val systemDark = isSystemInDarkTheme()
  return when (themeMode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> systemDark
  }
}

/**
 * Subtle multi-layered atmospheric background mesh with radial glow orbs and perspective grid lines
 * used across the inner app screens to match the dimensional login screen aesthetic.
 */
@Composable
fun AppAmbientBackdrop(
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary

  val topOrbAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.20f else 0.11f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "backdrop_top_alpha",
  )
  val rightOrbAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.16f else 0.09f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "backdrop_right_alpha",
  )
  val bottomOrbAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.13f else 0.07f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "backdrop_bottom_alpha",
  )
  val gridColor by animateColorAsState(
    targetValue = if (isDarkTheme) {
      Color.White.copy(alpha = 0.028f)
    } else {
      primary.copy(alpha = 0.036f)
    },
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "backdrop_grid_color",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    if (size.width <= 1f || size.height <= 1f) return@Canvas

    val topRadius = (size.width * 0.70f).coerceAtLeast(1f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          primary.copy(alpha = topOrbAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.10f, size.height * 0.06f),
        radius = topRadius,
      ),
      radius = topRadius,
      center = Offset(size.width * 0.10f, size.height * 0.06f),
    )

    val rightRadius = (size.width * 0.65f).coerceAtLeast(1f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          secondary.copy(alpha = rightOrbAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.92f, size.height * 0.34f),
        radius = rightRadius,
      ),
      radius = rightRadius,
      center = Offset(size.width * 0.92f, size.height * 0.34f),
    )

    val bottomRadius = (size.width * 0.62f).coerceAtLeast(1f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          tertiary.copy(alpha = bottomOrbAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.16f, size.height * 0.86f),
        radius = bottomRadius,
      ),
      radius = bottomRadius,
      center = Offset(size.width * 0.16f, size.height * 0.86f),
    )

    val step = 40.dp.toPx().coerceAtLeast(8f)
    val maxGridY = size.height * 0.38f
    var x = step
    while (x < size.width) {
      drawLine(
        color = gridColor,
        start = Offset(x, 0f),
        end = Offset(x, maxGridY),
        strokeWidth = 1.dp.toPx(),
      )
      x += step
    }
    var y = step
    while (y < maxGridY) {
      drawLine(
        color = gridColor,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
      )
      y += step
    }
  }
}

/**
 * 3D-layered hero vault card with a tilted rear perspective layer, royal indigo-violet diagonal
 * gradient, luminous internal radial orbs, and a 1.dp specular highlight border.
 */
@Composable
fun DimensionalVaultCard(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  val rearTopColor by animateColorAsState(
    targetValue = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDarkTheme) 0.42f else 0.26f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_rear_top_color",
  )
  val rearBottomColor by animateColorAsState(
    targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.32f else 0.20f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_rear_bottom_color",
  )
  val vaultElevation by animateDpAsState(
    targetValue = if (isDarkTheme) 12.dp else 10.dp,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_elevation",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 6.dp),
  ) {
    // Tilted Rear Accent Layer for 3D Depth
    Box(
      modifier = Modifier
        .matchParentSize()
        .padding(horizontal = 10.dp)
        .offset(y = 7.dp)
        .graphicsLayer {
          rotationZ = -1.8f
        }
        .clip(RoundedCornerShape(26.dp))
        .background(
          Brush.linearGradient(
            colors = listOf(rearTopColor, rearBottomColor),
          )
        ),
    )

    // Primary Elevated Vault Surface
    Surface(
      shape = RoundedCornerShape(26.dp),
      shadowElevation = vaultElevation,
      color = Color.Transparent,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF1D4ED8),
                Color(0xFF4338CA),
                Color(0xFF1E1B4B),
              ),
              start = Offset(0f, 0f),
              end = Offset(900f, 650f),
            )
          )
          .border(
            width = 1.dp,
            brush = Brush.linearGradient(
              colors = listOf(
                Color.White.copy(alpha = 0.36f),
                Color.White.copy(alpha = 0.08f),
              ),
            ),
            shape = RoundedCornerShape(26.dp),
          ),
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          if (size.width <= 1f || size.height <= 1f) return@Canvas
          val r1 = (size.height * 0.78f).coerceAtLeast(1f)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                SkyPositiveAccent.copy(alpha = 0.28f),
                Color.Transparent,
              ),
              center = Offset(size.width * 0.88f, size.height * 0.18f),
              radius = r1,
            ),
            radius = r1,
            center = Offset(size.width * 0.88f, size.height * 0.18f),
          )
          val r2 = (size.height * 0.58f).coerceAtLeast(1f)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                GoldAccent.copy(alpha = 0.20f),
                Color.Transparent,
              ),
              center = Offset(size.width * 0.12f, size.height * 0.92f),
              radius = r2,
            ),
            radius = r2,
            center = Offset(size.width * 0.12f, size.height * 0.92f),
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          content = content,
        )
      }
    }
  }
}

/**
 * Sculpted bento surface card with subtle gradient border, tonal depth, soft shadow,
 * Material ripple, haptic feedback, and smooth spring press animations.
 */
@Composable
fun SculptedSurfaceCard(
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  cornerRadius: Int = 20,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val shape = RoundedCornerShape(cornerRadius.dp)
  val containerColor by animateColorAsState(
    targetValue = if (isDarkTheme) {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
      MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    },
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "card_container_color",
  )
  val accentBorderColor by animateColorAsState(
    targetValue = accentColor.copy(alpha = if (isDarkTheme) 0.34f else 0.22f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "card_border_accent",
  )
  val outlineBorderColor by animateColorAsState(
    targetValue = MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.20f else 0.14f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "card_border_outline",
  )
  val elevation by animateDpAsState(
    targetValue = if (isDarkTheme) 0.dp else 3.dp,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "card_elevation",
  )
  val borderBrush = Brush.linearGradient(
    colors = listOf(accentBorderColor, outlineBorderColor),
  )

  if (onClick != null) {
    val interactionSource = remember { MutableInteractionSource() }
    val tactile = rememberTactileFeedback()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
      targetValue = if (isPressed) 0.974f else 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
      label = "card_press_scale",
    )

    Surface(
      onClick = {
        tactile.tick()
        onClick()
      },
      interactionSource = interactionSource,
      shape = shape,
      color = containerColor,
      shadowElevation = elevation,
      border = BorderStroke(1.dp, borderBrush),
      modifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
    ) {
      Box(modifier = Modifier.fillMaxWidth(), content = content)
    }
  } else {
    Surface(
      shape = shape,
      color = containerColor,
      shadowElevation = elevation,
      border = BorderStroke(1.dp, borderBrush),
      modifier = modifier.fillMaxWidth(),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), content = content)
    }
  }
}
