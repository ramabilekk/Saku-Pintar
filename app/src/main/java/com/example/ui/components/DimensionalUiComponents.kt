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
 * Ultra-subtle atmospheric canvas wash so the crisp white cards remain the focal point.
 */
@Composable
fun AppAmbientBackdrop(
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  val topWashAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.10f else 0.075f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "backdrop_top_wash",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    if (size.width <= 1f || size.height <= 1f) return@Canvas
    val radius = (size.width * 0.90f).coerceAtLeast(1f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          primary.copy(alpha = topWashAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.45f, 0f),
        radius = radius,
      ),
      radius = radius,
      center = Offset(size.width * 0.45f, 0f),
    )
    if (!isDarkTheme) {
      val secondaryRadius = (size.width * 0.65f).coerceAtLeast(1f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            secondary.copy(alpha = 0.045f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.95f, size.height * 0.14f),
          radius = secondaryRadius,
        ),
        radius = secondaryRadius,
        center = Offset(size.width * 0.95f, size.height * 0.14f),
      )
    }
  }
}

/**
 * Sleek Executive hero card: Deep Forest-Teal in Light Mode and Midnight Obsidian in Dark Mode,
 * with subtle specular hairline border and delicate luminous corner aura.
 */
@Composable
fun DimensionalVaultCard(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val vaultElevation by animateDpAsState(
    targetValue = if (isDarkTheme) 6.dp else 5.dp,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_elevation",
  )
  val gradStart by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF0B1120) else Color(0xFF0F2C29),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_grad_start",
  )
  val gradMid by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF141E33) else Color(0xFF14403A),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_grad_mid",
  )
  val gradEnd by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF0D282C),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_grad_end",
  )

  Surface(
    shape = RoundedCornerShape(24.dp),
    shadowElevation = vaultElevation,
    color = Color.Transparent,
    modifier = modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(gradStart, gradMid, gradEnd),
            start = Offset(0f, 0f),
            end = Offset(900f, 650f),
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = if (isDarkTheme) 0.16f else 0.24f),
              Color.White.copy(alpha = 0.05f),
            ),
          ),
          shape = RoundedCornerShape(24.dp),
        ),
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val r1 = (size.height * 0.85f).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              SkyPositiveAccent.copy(alpha = if (isDarkTheme) 0.16f else 0.22f),
              Color.Transparent,
            ),
            center = Offset(size.width * 0.92f, size.height * 0.12f),
            radius = r1,
          ),
          radius = r1,
          center = Offset(size.width * 0.92f, size.height * 0.12f),
        )
        val r2 = (size.height * 0.60f).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              GoldAccent.copy(alpha = 0.11f),
              Color.Transparent,
            ),
            center = Offset(size.width * 0.08f, size.height * 0.96f),
            radius = r2,
          ),
          radius = r2,
          center = Offset(size.width * 0.08f, size.height * 0.96f),
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

/**
 * Crisp Neo-FinTech card with clean 1.dp hairline border, soft elevation, tactile spring press,
 * and effortless Light & Dark harmony.
 */
@Composable
fun SculptedSurfaceCard(
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.outlineVariant,
  cornerRadius: Int = 20,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val shape = RoundedCornerShape(cornerRadius.dp)
  val containerColor by animateColorAsState(
    targetValue = MaterialTheme.colorScheme.surface,
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "card_container_color",
  )
  val borderColor by animateColorAsState(
    targetValue = if (accentColor == MaterialTheme.colorScheme.outlineVariant || accentColor == MaterialTheme.colorScheme.primary) {
      MaterialTheme.colorScheme.outlineVariant
    } else {
      accentColor.copy(alpha = if (isDarkTheme) 0.34f else 0.22f)
    },
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "card_border_color",
  )
  val elevation by animateDpAsState(
    targetValue = if (isDarkTheme) 0.dp else 2.dp,
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "card_elevation",
  )

  if (onClick != null) {
    val interactionSource = remember { MutableInteractionSource() }
    val tactile = rememberTactileFeedback()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
      targetValue = if (isPressed) 0.98f else 1f,
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
      border = BorderStroke(1.dp, borderColor),
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
      border = BorderStroke(1.dp, borderColor),
      modifier = modifier.fillMaxWidth(),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), content = content)
    }
  }
}
