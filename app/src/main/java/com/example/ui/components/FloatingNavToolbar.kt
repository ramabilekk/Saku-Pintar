package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.SkyPositiveAccent

data class FloatingNavItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

/**
 * Floating Liquid Glass capsule bottom navigation toolbar with overhead specular curvature glare,
 * prismatic beveled rim, raised liquid-droplet active indicator, and jewel-like emerald glass
 * Scan Struk center lens.
 */
@Composable
fun FloatingPillBottomBar(
  tabs: List<FloatingNavItem>,
  currentRoute: String?,
  onSelectTab: (String) -> Unit,
  onQuickScanClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val accentColor = if (isDarkTheme) SkyPositiveAccent else EmeraldSafe

  val pillBackgroundBrush = remember(isDarkTheme) {
    if (isDarkTheme) {
      Brush.linearGradient(
        colors = listOf(
          Color(0xFF1D2E4A).copy(alpha = 0.72f),
          Color(0xFF0E182B).copy(alpha = 0.66f),
          Color(0xFF122338).copy(alpha = 0.74f),
        ),
        start = Offset.Zero,
        end = Offset(900f, 180f),
      )
    } else {
      Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.78f),
          Color(0xFFEEF7FA).copy(alpha = 0.62f),
          Color.White.copy(alpha = 0.74f),
        ),
        start = Offset.Zero,
        end = Offset(900f, 180f),
      )
    }
  }

  val pillRimBrush = remember(isDarkTheme, accentColor) {
    if (isDarkTheme) {
      Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.36f),
          accentColor.copy(alpha = 0.30f),
          Color.White.copy(alpha = 0.10f),
          Color.White.copy(alpha = 0.22f),
        ),
      )
    } else {
      Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.98f),
          Color.White.copy(alpha = 0.70f),
          accentColor.copy(alpha = 0.28f),
          Color.White.copy(alpha = 0.84f),
        ),
      )
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = CircleShape,
      color = Color.Transparent,
      shadowElevation = if (isDarkTheme) 18.dp else 12.dp,
      border = BorderStroke(1.3.dp, pillRimBrush),
      modifier = Modifier
        .widthIn(max = 420.dp)
        .fillMaxWidth()
        .height(68.dp)
        .testTag("floating_bottom_toolbar"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(pillBackgroundBrush)
          .drawBehind {
            if (size.width <= 1f || size.height <= 1f) return@drawBehind
            // Curved liquid glass top specular reflection arc
            val radius = size.height / 2f
            drawRoundRect(
              brush = Brush.verticalGradient(
                colors = listOf(
                  Color.White.copy(alpha = if (isDarkTheme) 0.18f else 0.62f),
                  Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.14f),
                  Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.54f,
              ),
              size = Size(size.width, size.height * 0.54f),
              cornerRadius = CornerRadius(radius, radius),
            )
          }
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEachIndexed { index, tab ->
          if (index == 2) {
            FloatingScanCenterItem(
              onClick = onQuickScanClick,
              isDarkTheme = isDarkTheme,
              accentColor = accentColor,
            )
          }

          val isSelected = currentRoute == tab.route
          FloatingPillTabItem(
            tab = tab,
            isSelected = isSelected,
            isDarkTheme = isDarkTheme,
            accentColor = accentColor,
            onClick = { onSelectTab(tab.route) },
          )
        }
      }
    }
  }
}

@Composable
private fun FloatingPillTabItem(
  tab: FloatingNavItem,
  isSelected: Boolean,
  isDarkTheme: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
) {
  val label = stringResource(tab.labelRes)
  val interactionSource = remember { MutableInteractionSource() }

  val indicatorScale by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0.68f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "pill_indicator_scale_${tab.route}",
  )
  val indicatorAlpha by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "pill_indicator_alpha_${tab.route}",
  )
  val iconScale by animateFloatAsState(
    targetValue = if (isSelected) 1.10f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "pill_icon_scale_${tab.route}",
  )
  val iconTint by animateColorAsState(
    targetValue = if (isSelected) {
      accentColor
    } else {
      if (isDarkTheme) {
        Color(0xFFB4C4DA).copy(alpha = 0.78f)
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
      }
    },
    animationSpec = tween(durationMillis = 180),
    label = "pill_icon_tint_${tab.route}",
  )

  val dropletFill = remember(isDarkTheme, accentColor) {
    if (isDarkTheme) {
      Brush.verticalGradient(
        colors = listOf(
          accentColor.copy(alpha = 0.30f),
          accentColor.copy(alpha = 0.14f),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.92f),
          accentColor.copy(alpha = 0.18f),
        ),
      )
    }
  }
  val dropletRim = remember(isDarkTheme, accentColor) {
    Brush.linearGradient(
      colors = if (isDarkTheme) {
        listOf(
          Color.White.copy(alpha = 0.42f),
          accentColor.copy(alpha = 0.48f),
          Color.White.copy(alpha = 0.10f),
        )
      } else {
        listOf(
          Color.White.copy(alpha = 0.98f),
          accentColor.copy(alpha = 0.36f),
          Color.White.copy(alpha = 0.60f),
        )
      },
    )
  }

  Box(
    modifier = Modifier
      .size(54.dp)
      .clip(CircleShape)
      .tactilePressScale(
        interactionSource = interactionSource,
        pressedScale = 0.88f,
        enablePressHaptic = false,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = accentColor),
        role = Role.Tab,
        onClick = onClick,
      )
      .semantics {
        contentDescription = label
        selected = isSelected
      }
      .testTag(tab.testTag),
    contentAlignment = Alignment.Center,
  ) {
    // Raised Liquid Glass Droplet active indicator
    Box(
      modifier = Modifier
        .size(50.dp)
        .graphicsLayer {
          scaleX = indicatorScale
          scaleY = indicatorScale
          alpha = indicatorAlpha
        }
        .clip(CircleShape)
        .background(dropletFill)
        .border(width = 1.1.dp, brush = dropletRim, shape = CircleShape),
    )

    Icon(
      imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier
        .size(25.dp)
        .graphicsLayer {
          scaleX = iconScale
          scaleY = iconScale
        },
    )
  }
}

@Composable
private fun FloatingScanCenterItem(
  onClick: () -> Unit,
  isDarkTheme: Boolean,
  accentColor: Color,
) {
  val scanDesc = stringResource(R.string.ai_scan_quick_title)
  val interactionSource = remember { MutableInteractionSource() }

  val orbFill = remember(isDarkTheme) {
    Brush.linearGradient(
      colors = if (isDarkTheme) {
        listOf(
          Color(0xFF14B8A6).copy(alpha = 0.92f),
          Color(0xFF0284C7).copy(alpha = 0.88f),
        )
      } else {
        listOf(
          Color(0xFF0D9488).copy(alpha = 0.94f),
          Color(0xFF0284C7).copy(alpha = 0.90f),
        )
      },
      start = Offset.Zero,
      end = Offset(130f, 130f),
    )
  }
  val orbRim = remember(isDarkTheme) {
    Brush.linearGradient(
      colors = listOf(
        Color.White.copy(alpha = if (isDarkTheme) 0.72f else 0.88f),
        SkyPositiveAccent.copy(alpha = 0.55f),
        Color.White.copy(alpha = 0.25f),
      ),
    )
  }

  Box(
    modifier = Modifier
      .size(54.dp)
      .clip(CircleShape)
      .tactilePressScale(
        interactionSource = interactionSource,
        pressedScale = 0.86f,
        enablePressHaptic = false,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = accentColor),
        role = Role.Button,
        onClick = onClick,
      )
      .semantics { contentDescription = scanDesc }
      .testTag("nav_center_scan_button"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = CircleShape,
      color = Color.Transparent,
      shadowElevation = if (isDarkTheme) 10.dp else 6.dp,
      border = BorderStroke(1.3.dp, orbRim),
      modifier = Modifier.size(48.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(orbFill)
          .drawBehind {
            // Curved liquid lens glare crescent at top
            drawCircle(
              brush = Brush.verticalGradient(
                colors = listOf(
                  Color.White.copy(alpha = 0.45f),
                  Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.52f,
              ),
              radius = size.width * 0.48f,
              center = Offset(size.width * 0.5f, size.height * 0.28f),
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.DocumentScanner,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}
