package com.example.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.ui.theme.SkyPositiveAccent

/**
 * Applies native Android 12+ (API 31+) window background blur behind Compose Dialogs and
 * ModalBottomSheets for true GPU frosted glassmorphism, gracefully falling back on API 24-30.
 */
@Composable
fun LiquidGlassWindowBlurEffect(
  isDarkTheme: Boolean = rememberIsEffectiveDarkTheme(),
  blurRadiusPx: Int = 44,
) {
  val view = LocalView.current
  SideEffect {
    runCatching {
      val window = (view.parent as? DialogWindowProvider)?.window ?: return@runCatching
      window.setDimAmount(if (isDarkTheme) 0.44f else 0.24f)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        val params = window.attributes
        params.blurBehindRadius = blurRadiusPx
        window.attributes = params
      }
    }
  }
}

/**
 * Reusable Glassmorphism dialog surface used by [LiquidGlassAlertDialog] and custom dialogs
 * (such as the receipt photo viewer). Renders frosted translucent gradient fill, curved overhead
 * specular reflection, internal refraction glow, and double-bevel prismatic rim border.
 */
@Composable
fun LiquidGlassDialogSurface(
  modifier: Modifier = Modifier,
  cornerRadius: Int = 28,
  shape: Shape = RoundedCornerShape(cornerRadius.dp),
  accentColor: Color = Color.Unspecified,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val resolvedAccent = if (accentColor != Color.Unspecified) {
    accentColor
  } else {
    if (isDarkTheme) Color(0xFF38BDF8) else SkyPositiveAccent
  }

  val topFill by animateColorAsState(
    targetValue = if (isDarkTheme) {
      Color(0xFF192944).copy(alpha = 0.84f)
    } else {
      Color.White.copy(alpha = 0.86f)
    },
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "dialog_glass_top",
  )
  val bottomFill by animateColorAsState(
    targetValue = if (isDarkTheme) {
      Color(0xFF0C1628).copy(alpha = 0.78f)
    } else {
      Color(0xFFECF6FA).copy(alpha = 0.76f)
    },
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "dialog_glass_bottom",
  )

  val fillBrush = remember(topFill, bottomFill, resolvedAccent, isDarkTheme) {
    Brush.linearGradient(
      colors = listOf(
        topFill,
        bottomFill,
        if (isDarkTheme) {
          Color(0xFF10203A).copy(alpha = 0.82f)
        } else {
          resolvedAccent.copy(alpha = 0.08f)
        },
      ),
      start = Offset.Zero,
      end = Offset(820f, 760f),
    )
  }

  val rimBrush = remember(isDarkTheme, resolvedAccent) {
    Brush.linearGradient(
      colors = if (isDarkTheme) {
        listOf(
          Color.White.copy(alpha = 0.42f),
          resolvedAccent.copy(alpha = 0.36f),
          Color.White.copy(alpha = 0.09f),
          Color.White.copy(alpha = 0.22f),
        )
      } else {
        listOf(
          Color.White.copy(alpha = 0.98f),
          Color.White.copy(alpha = 0.72f),
          resolvedAccent.copy(alpha = 0.32f),
          Color.White.copy(alpha = 0.84f),
        )
      },
      start = Offset.Zero,
      end = Offset(820f, 760f),
    )
  }

  Surface(
    shape = shape,
    color = Color.Transparent,
    shadowElevation = if (isDarkTheme) 20.dp else 14.dp,
    border = BorderStroke(1.3.dp, rimBrush),
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .clip(shape)
        .background(fillBrush)
        .drawBehind {
          if (size.width <= 1f || size.height <= 1f) return@drawBehind
          val radiusPx = cornerRadius.dp.toPx()

          // Curved overhead specular glass glaze
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color.White.copy(alpha = if (isDarkTheme) 0.14f else 0.62f),
                Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.16f),
                Color.Transparent,
              ),
              startY = 0f,
              endY = (size.height * 0.34f).coerceAtLeast(1f),
            ),
            size = Size(size.width, size.height * 0.34f),
            cornerRadius = CornerRadius(radiusPx, radiusPx),
          )

          // Top-right subtle prismatic refraction orb
          val topOrbRadius = (size.width * 0.58f).coerceAtLeast(1f)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                resolvedAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.12f),
                Color.Transparent,
              ),
              center = Offset(size.width * 0.92f, size.height * 0.08f),
              radius = topOrbRadius,
            ),
            radius = topOrbRadius,
            center = Offset(size.width * 0.92f, size.height * 0.08f),
          )

          // Crisp inner specular micro-bevel hairline
          val inset = 1.5.dp.toPx()
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color.White.copy(alpha = if (isDarkTheme) 0.16f else 0.58f),
                Color.Transparent,
              ),
            ),
            topLeft = Offset(inset, inset),
            size = Size(
              (size.width - inset * 2f).coerceAtLeast(1f),
              (size.height - inset * 2f).coerceAtLeast(1f),
            ),
            cornerRadius = CornerRadius((radiusPx - inset).coerceAtLeast(1f)),
            style = Stroke(width = 0.75.dp.toPx()),
          )
        },
      content = content,
    )
  }
}

/**
 * Drop-in Glassmorphism replacement for Material 3 [androidx.compose.material3.AlertDialog],
 * matching the Liquid Glass theme across every confirmation, form, and picker dialog in the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiquidGlassAlertDialog(
  onDismissRequest: () -> Unit,
  confirmButton: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  dismissButton: (@Composable () -> Unit)? = null,
  icon: (@Composable () -> Unit)? = null,
  title: (@Composable () -> Unit)? = null,
  text: (@Composable () -> Unit)? = null,
  shape: Shape = RoundedCornerShape(28.dp),
  accentColor: Color = Color.Unspecified,
  properties: DialogProperties = DialogProperties(),
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = properties,
  ) {
    LiquidGlassWindowBlurEffect(isDarkTheme = isDarkTheme)

    LiquidGlassDialogSurface(
      shape = shape,
      accentColor = accentColor,
      modifier = modifier
        .widthIn(min = 280.dp, max = 520.dp)
        .fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        if (icon != null) {
          Box(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
          ) {
            CompositionLocalProvider(
              LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) {
              icon()
            }
          }
        }

        if (title != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .align(if (icon != null) Alignment.CenterHorizontally else Alignment.Start),
          ) {
            CompositionLocalProvider(
              LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
              ProvideTextStyle(
                value = MaterialTheme.typography.titleLarge,
                content = title,
              )
            }
          }
        }

        if (text != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(weight = 1f, fill = false),
          ) {
            CompositionLocalProvider(
              LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
              ProvideTextStyle(
                value = MaterialTheme.typography.bodyMedium,
                content = text,
              )
            }
          }
        }

        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.CenterEnd,
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            dismissButton?.invoke()
            confirmButton()
          }
        }
      }
    }
  }
}

/**
 * Translucent Glassmorphism container color for Material 3 ModalBottomSheets.
 */
@Composable
fun rememberLiquidGlassSheetContainerColor(): Color {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val target = if (isDarkTheme) {
    Color(0xFF0E1A2F).copy(alpha = 0.90f)
  } else {
    Color(0xFFF3F9FC).copy(alpha = 0.92f)
  }
  val animated by animateColorAsState(
    targetValue = target,
    animationSpec = tween(320, easing = FastOutSlowInEasing),
    label = "glass_sheet_bg",
  )
  return animated
}

/**
 * Subtle frosted glass top rim bar for ModalBottomSheet content headers.
 */
@Composable
fun LiquidGlassSheetDragHandle(
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 12.dp, bottom = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .widthIn(min = 42.dp, max = 42.dp)
        .padding(vertical = 2.dp)
        .clip(RoundedCornerShape(50))
        .background(
          if (isDarkTheme) Color.White.copy(alpha = 0.28f) else Color(0xFF0C2324).copy(alpha = 0.18f),
        )
        .border(
          width = 0.6.dp,
          color = Color.White.copy(alpha = if (isDarkTheme) 0.35f else 0.85f),
          shape = RoundedCornerShape(50),
        )
        .padding(horizontal = 21.dp, vertical = 2.5.dp),
    )
  }
}
