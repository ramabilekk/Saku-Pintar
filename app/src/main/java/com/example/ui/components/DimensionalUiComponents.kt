package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GlassMeshCyanDark
import com.example.ui.theme.GlassMeshCyanLight
import com.example.ui.theme.GlassMeshEmeraldDark
import com.example.ui.theme.GlassMeshEmeraldLight
import com.example.ui.theme.GlassMeshGoldDark
import com.example.ui.theme.GlassMeshIrisDark
import com.example.ui.theme.GlassMeshIrisLight
import com.example.ui.theme.GlassMeshPearlLight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.theme.ThemePreferenceManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight controller for subtle, elegant haptic feedback across navigation, buttons, and cards.
 */
@Stable
class TactileFeedbackController(
  private val view: View,
  private val hapticFeedback: HapticFeedback,
  private val isEnabled: () -> Boolean = { true },
) {
  /** Subtle, crisp tap feedback for general buttons, chips, and interactive surfaces. */
  fun tick() {
    if (!isEnabled()) return
    runCatching {
      val performed = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
      if (!performed) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
    }
  }

  /** Distinct, smooth tactile pulse when switching primary navigation tabs or pages. */
  fun navigation() {
    if (!isEnabled()) return
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
    if (!isEnabled()) return
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
  val appContext = view.context.applicationContext
  return remember(view, hapticFeedback, appContext) {
    TactileFeedbackController(
      view = view,
      hapticFeedback = hapticFeedback,
      isEnabled = { com.example.security.PrivacySecurityManager.isHapticFeedbackEnabled(appContext) },
    )
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
 * Living Liquid Glass refractive mesh canvas: soft, slowly drifting multi-chromatic radial
 * light pools behind the entire viewport so translucent glass cards, pills, and headers
 * visibly refract depth and color in both Light and Dark themes.
 */
@Composable
fun AppAmbientBackdrop(
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val infiniteTransition = rememberInfiniteTransition(label = "liquid_mesh_drift")
  val driftPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 18_000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "liquid_mesh_phase",
  )

  val baseTop by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF040813) else Color(0xFFE7F2F7),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "mesh_base_top",
  )
  val baseMid by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF081224) else Color(0xFFEDF4F8),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "mesh_base_mid",
  )
  val baseBottom by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF050B16) else Color(0xFFE5EEF6),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "mesh_base_bottom",
  )

  val orbEmerald by animateColorAsState(
    targetValue = if (isDarkTheme) GlassMeshEmeraldDark else GlassMeshEmeraldLight,
    animationSpec = tween(420),
    label = "mesh_orb_emerald",
  )
  val orbCyan by animateColorAsState(
    targetValue = if (isDarkTheme) GlassMeshCyanDark else GlassMeshCyanLight,
    animationSpec = tween(420),
    label = "mesh_orb_cyan",
  )
  val orbIris by animateColorAsState(
    targetValue = if (isDarkTheme) GlassMeshIrisDark else GlassMeshIrisLight,
    animationSpec = tween(420),
    label = "mesh_orb_iris",
  )
  val orbWarm by animateColorAsState(
    targetValue = if (isDarkTheme) GlassMeshGoldDark else GlassMeshPearlLight,
    animationSpec = tween(420),
    label = "mesh_orb_warm",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    if (size.width <= 1f || size.height <= 1f) return@Canvas

    // Atmospheric base wash
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(baseTop, baseMid, baseBottom),
      ),
    )

    val sinA = sin(driftPhase)
    val cosA = cos(driftPhase)
    val sinB = sin(driftPhase * 0.7f + 1.2f)
    val cosB = cos(driftPhase * 0.7f + 1.2f)

    // Orb 1: Top-left luminous emerald-mint refraction pool
    val r1 = (size.width * 0.92f).coerceAtLeast(1f)
    val c1 = Offset(
      x = size.width * (0.18f + 0.08f * cosA),
      y = size.height * (0.06f + 0.04f * sinA),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          orbEmerald.copy(alpha = if (isDarkTheme) 0.24f else 0.30f),
          orbEmerald.copy(alpha = if (isDarkTheme) 0.08f else 0.11f),
          Color.Transparent,
        ),
        center = c1,
        radius = r1,
      ),
      radius = r1,
      center = c1,
    )

    // Orb 2: Upper-right glacial cerulean-cyan glow
    val r2 = (size.width * 0.86f).coerceAtLeast(1f)
    val c2 = Offset(
      x = size.width * (0.90f + 0.07f * sinB),
      y = size.height * (0.24f + 0.05f * cosB),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          orbCyan.copy(alpha = if (isDarkTheme) 0.22f else 0.26f),
          orbCyan.copy(alpha = if (isDarkTheme) 0.06f else 0.09f),
          Color.Transparent,
        ),
        center = c2,
        radius = r2,
      ),
      radius = r2,
      center = c2,
    )

    // Orb 3: Mid-left soft iris-violet prism aura
    val r3 = (size.width * 0.82f).coerceAtLeast(1f)
    val c3 = Offset(
      x = size.width * (0.10f + 0.07f * sinA),
      y = size.height * (0.58f + 0.05f * cosA),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          orbIris.copy(alpha = if (isDarkTheme) 0.18f else 0.18f),
          orbIris.copy(alpha = if (isDarkTheme) 0.05f else 0.06f),
          Color.Transparent,
        ),
        center = c3,
        radius = r3,
      ),
      radius = r3,
      center = c3,
    )

    // Orb 4: Bottom-right warm champagne-mint reflection pool (under floating bottom glass pill)
    val r4 = (size.width * 0.88f).coerceAtLeast(1f)
    val c4 = Offset(
      x = size.width * (0.78f + 0.08f * cosB),
      y = size.height * (0.90f + 0.04f * sinB),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          orbWarm.copy(alpha = if (isDarkTheme) 0.12f else 0.22f),
          orbEmerald.copy(alpha = if (isDarkTheme) 0.09f else 0.10f),
          Color.Transparent,
        ),
        center = c4,
        radius = r4,
      ),
      radius = r4,
      center = c4,
    )
  }
}

/**
 * Reusable Liquid Glass circular/rounded icon orb with overhead specular glaze and prismatic rim.
 */
@Composable
fun LiquidGlassIconBadge(
  modifier: Modifier = Modifier,
  size: Dp = 38.dp,
  shape: Shape = RoundedCornerShape(13.dp),
  tintColor: Color = MaterialTheme.colorScheme.primary,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val fillBrush = remember(isDarkTheme, tintColor) {
    Brush.linearGradient(
      colors = if (isDarkTheme) {
        listOf(
          tintColor.copy(alpha = 0.26f),
          Color(0xFF152238).copy(alpha = 0.62f),
        )
      } else {
        listOf(
          Color.White.copy(alpha = 0.86f),
          tintColor.copy(alpha = 0.16f),
        )
      },
      start = Offset.Zero,
      end = Offset(120f, 120f),
    )
  }
  val borderBrush = remember(isDarkTheme, tintColor) {
    Brush.linearGradient(
      colors = if (isDarkTheme) {
        listOf(
          Color.White.copy(alpha = 0.34f),
          tintColor.copy(alpha = 0.38f),
          Color.White.copy(alpha = 0.08f),
        )
      } else {
        listOf(
          Color.White.copy(alpha = 0.96f),
          tintColor.copy(alpha = 0.30f),
          Color.White.copy(alpha = 0.45f),
        )
      },
    )
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(fillBrush)
      .border(width = 1.dp, brush = borderBrush, shape = shape),
    contentAlignment = Alignment.Center,
    content = content,
  )
}

/**
 * Smoked Liquid Crystal Hero Card: deep translucent tinted optical glass with animated diagonal
 * caustic shimmer, glowing corner refraction orbs, overhead specular reflection band, and
 * crisp prismatic rim border. Always keeps pure white text contrast across Light & Dark themes.
 */
@Composable
fun DimensionalVaultCard(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val infiniteTransition = rememberInfiniteTransition(label = "vault_caustic")
  val shimmerShift by infiniteTransition.animateFloat(
    initialValue = -0.35f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7_500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "vault_shimmer_shift",
  )

  val vaultElevation by animateDpAsState(
    targetValue = if (isDarkTheme) 10.dp else 8.dp,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_elevation",
  )
  val gradStart by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF0A1629).copy(alpha = 0.88f) else Color(0xFF0B3534).copy(alpha = 0.88f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_grad_start",
  )
  val gradMid by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF10243E).copy(alpha = 0.82f) else Color(0xFF104F4A).copy(alpha = 0.84f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_grad_mid",
  )
  val gradEnd by animateColorAsState(
    targetValue = if (isDarkTheme) Color(0xFF091326).copy(alpha = 0.90f) else Color(0xFF0B2F3B).copy(alpha = 0.90f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "vault_grad_end",
  )
  val shape = RoundedCornerShape(26.dp)

  Surface(
    shape = shape,
    shadowElevation = vaultElevation,
    color = Color.Transparent,
    modifier = modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(
          Brush.linearGradient(
            colors = listOf(gradStart, gradMid, gradEnd),
            start = Offset(0f, 0f),
            end = Offset(950f, 700f),
          )
        )
        .border(
          width = 1.3.dp,
          brush = Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = if (isDarkTheme) 0.36f else 0.48f),
              SkyPositiveAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.30f),
              Color.White.copy(alpha = 0.08f),
              Color.White.copy(alpha = if (isDarkTheme) 0.16f else 0.22f),
            ),
            start = Offset.Zero,
            end = Offset(900f, 650f),
          ),
          shape = shape,
        ),
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas

        // Top curved liquid glass specular glare band
        drawRoundRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color.White.copy(alpha = if (isDarkTheme) 0.15f else 0.20f),
              Color.White.copy(alpha = 0.03f),
              Color.Transparent,
            ),
            startY = 0f,
            endY = size.height * 0.42f,
          ),
          size = Size(size.width, size.height * 0.42f),
          cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
        )

        // Subtle animated diagonal caustic shimmer ribbon
        val bandStartX = size.width * shimmerShift
        drawRect(
          brush = Brush.linearGradient(
            colors = listOf(
              Color.Transparent,
              Color.White.copy(alpha = if (isDarkTheme) 0.055f else 0.08f),
              SkyPositiveAccent.copy(alpha = if (isDarkTheme) 0.05f else 0.07f),
              Color.Transparent,
            ),
            start = Offset(bandStartX - size.width * 0.28f, 0f),
            end = Offset(bandStartX + size.width * 0.28f, size.height),
          ),
        )

        // Internal luminous refraction orbs
        val r1 = (size.height * 0.92f).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              SkyPositiveAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.30f),
              Color.Transparent,
            ),
            center = Offset(size.width * 0.92f, size.height * 0.10f),
            radius = r1,
          ),
          radius = r1,
          center = Offset(size.width * 0.92f, size.height * 0.10f),
        )
        val r2 = (size.height * 0.68f).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              GoldAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.18f),
              Color.Transparent,
            ),
            center = Offset(size.width * 0.06f, size.height * 0.96f),
            radius = r2,
          ),
          radius = r2,
          center = Offset(size.width * 0.06f, size.height * 0.96f),
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
 * Authentic Liquid Glass Card: frosted translucent optical body, curved top specular sheen,
 * subtle bottom internal refraction bounce, prismatic dual-tone beveled rim border, and
 * springy tactile press response with gentle luminance lift.
 */
@Composable
fun SculptedSurfaceCard(
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.outlineVariant,
  cornerRadius: Int = 22,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val shape = RoundedCornerShape(cornerRadius.dp)
  val isNeutralAccent = accentColor == MaterialTheme.colorScheme.outlineVariant ||
    accentColor == MaterialTheme.colorScheme.primary

  val glassTopColor by animateColorAsState(
    targetValue = if (isDarkTheme) {
      Color(0xFF1D2E4A).copy(alpha = 0.58f)
    } else {
      Color.White.copy(alpha = 0.72f)
    },
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "glass_top_color",
  )
  val glassBottomColor by animateColorAsState(
    targetValue = if (isDarkTheme) {
      Color(0xFF0D1729).copy(alpha = 0.44f)
    } else {
      Color(0xFFF0F8FB).copy(alpha = 0.48f)
    },
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "glass_bottom_color",
  )
  val rimAccent by animateColorAsState(
    targetValue = if (isNeutralAccent) {
      if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.24f) else Color(0xFF2DD4BF).copy(alpha = 0.20f)
    } else {
      accentColor.copy(alpha = if (isDarkTheme) 0.44f else 0.36f)
    },
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "glass_rim_accent",
  )
  val elevation by animateDpAsState(
    targetValue = if (isDarkTheme) 3.dp else 5.dp,
    animationSpec = tween(360, easing = FastOutSlowInEasing),
    label = "card_elevation",
  )

  val glassFillBrush = remember(glassTopColor, glassBottomColor, rimAccent, isNeutralAccent) {
    Brush.linearGradient(
      colors = listOf(
        glassTopColor,
        glassBottomColor,
        if (isNeutralAccent) glassBottomColor else rimAccent.copy(alpha = rimAccent.alpha * 0.24f),
      ),
      start = Offset.Zero,
      end = Offset(750f, 600f),
    )
  }

  val specularBorderBrush = remember(isDarkTheme, rimAccent) {
    Brush.linearGradient(
      colors = if (isDarkTheme) {
        listOf(
          Color.White.copy(alpha = 0.36f),
          rimAccent,
          Color.White.copy(alpha = 0.07f),
          Color.White.copy(alpha = 0.18f),
        )
      } else {
        listOf(
          Color.White.copy(alpha = 0.98f),
          Color.White.copy(alpha = 0.66f),
          rimAccent,
          Color.White.copy(alpha = 0.78f),
        )
      },
      start = Offset.Zero,
      end = Offset(700f, 550f),
    )
  }

  val cornerPxModifier = Modifier.drawBehind {
    if (size.width <= 1f || size.height <= 1f) return@drawBehind
    val corner = cornerRadius.dp.toPx()
    // Top overhead specular glass glaze
    drawRoundRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.White.copy(alpha = if (isDarkTheme) 0.12f else 0.58f),
          Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.14f),
          Color.Transparent,
        ),
        startY = 0f,
        endY = (size.height * 0.36f).coerceAtLeast(1f),
      ),
      size = Size(size.width, size.height * 0.36f),
      cornerRadius = CornerRadius(corner, corner),
    )
    // Subtle bottom-right internal bounce tint
    val bounceRadius = (size.width * 0.55f).coerceAtLeast(1f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          rimAccent.copy(alpha = if (isDarkTheme) 0.12f else 0.11f),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.88f, size.height * 0.94f),
        radius = bounceRadius,
      ),
      radius = bounceRadius,
      center = Offset(size.width * 0.88f, size.height * 0.94f),
    )
    // Crisp inner specular micro-bevel hairline
    val inset = 1.4.dp.toPx()
    drawRoundRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.55f),
          Color.Transparent,
        ),
      ),
      topLeft = Offset(inset, inset),
      size = Size(
        (size.width - inset * 2f).coerceAtLeast(1f),
        (size.height - inset * 2f).coerceAtLeast(1f),
      ),
      cornerRadius = CornerRadius((corner - inset).coerceAtLeast(1f)),
      style = Stroke(width = 0.65.dp.toPx()),
    )
  }

  if (onClick != null) {
    val interactionSource = remember { MutableInteractionSource() }
    val tactile = rememberTactileFeedback()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
      targetValue = if (isPressed) 0.976f else 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
      label = "card_press_scale",
    )
    val pressGlowAlpha by animateFloatAsState(
      targetValue = if (isPressed) (if (isDarkTheme) 0.10f else 0.24f) else 0f,
      animationSpec = tween(140),
      label = "card_press_glow",
    )

    Surface(
      onClick = {
        tactile.tick()
        onClick()
      },
      interactionSource = interactionSource,
      shape = shape,
      color = Color.Transparent,
      shadowElevation = elevation,
      border = BorderStroke(1.2.dp, specularBorderBrush),
      modifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(glassFillBrush)
          .then(cornerPxModifier)
          .background(Color.White.copy(alpha = pressGlowAlpha)),
        content = content,
      )
    }
  } else {
    Surface(
      shape = shape,
      color = Color.Transparent,
      shadowElevation = elevation,
      border = BorderStroke(1.2.dp, specularBorderBrush),
      modifier = modifier.fillMaxWidth(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(glassFillBrush)
          .then(cornerPxModifier),
        content = content,
      )
    }
  }
}
