package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

data class FloatingNavItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

/**
 * Floating translucent pill-shaped bottom navigation toolbar with minimalist icon-only items,
 * circular soft-glow active indicator, and centered Scan Struk action.
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
  val accentColor = EmeraldSafe

  val pillBackgroundBrush = remember(isDarkTheme) {
    if (isDarkTheme) {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFF1D2228).copy(alpha = 0.86f),
          Color(0xFF13161A).copy(alpha = 0.90f),
        ),
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          Color(0xFFFFFFFF).copy(alpha = 0.95f),
          Color(0xFFF3F8F5).copy(alpha = 0.92f),
        ),
      )
    }
  }

  val pillBorderColor = if (isDarkTheme) {
    Color.White.copy(alpha = 0.13f)
  } else {
    accentColor.copy(alpha = 0.16f)
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
      shadowElevation = if (isDarkTheme) 14.dp else 10.dp,
      border = BorderStroke(1.dp, pillBorderColor),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp)
        .height(66.dp)
        .testTag("floating_bottom_toolbar"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(pillBackgroundBrush)
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
    targetValue = if (isSelected) 1f else 0.72f,
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
    targetValue = if (isSelected) 1.08f else 1.0f,
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
        Color(0xFF9AA3B0)
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
      }
    },
    animationSpec = tween(durationMillis = 180),
    label = "pill_icon_tint_${tab.route}",
  )

  val activeCircleColor = if (isDarkTheme) {
    accentColor.copy(alpha = 0.22f)
  } else {
    accentColor.copy(alpha = 0.14f)
  }

  Box(
    modifier = Modifier
      .size(54.dp)
      .clip(CircleShape)
      .tactilePressScale(
        interactionSource = interactionSource,
        pressedScale = 0.90f,
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
    Box(
      modifier = Modifier
        .size(52.dp)
        .graphicsLayer {
          scaleX = indicatorScale
          scaleY = indicatorScale
          alpha = indicatorAlpha
        }
        .clip(CircleShape)
        .background(activeCircleColor),
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

  val circleFill = if (isDarkTheme) {
    accentColor.copy(alpha = 0.18f)
  } else {
    accentColor.copy(alpha = 0.14f)
  }
  val circleBorder = if (isDarkTheme) {
    accentColor.copy(alpha = 0.46f)
  } else {
    accentColor.copy(alpha = 0.36f)
  }

  Box(
    modifier = Modifier
      .size(52.dp)
      .clip(CircleShape)
      .tactilePressScale(
        interactionSource = interactionSource,
        pressedScale = 0.88f,
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
      color = circleFill,
      border = BorderStroke(1.2.dp, circleBorder),
      modifier = Modifier.size(46.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.DocumentScanner,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}
