package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GoldAccent

/**
 * Icon-only theme switcher pill featuring Sun (Light theme) and Moon (Dark theme) logos
 * with a fluid spring-animated sliding thumb, smooth gradient morphing, and icon rotation.
 */
@Composable
fun ThemeLogoToggle(
  themeMode: AppThemeMode,
  onSelectThemeMode: (AppThemeMode) -> Unit,
  modifier: Modifier = Modifier,
  lightButtonTag: String = "theme_mode_LIGHT",
  darkButtonTag: String = "theme_mode_DARK",
) {
  val systemDark = isSystemInDarkTheme()
  val isDarkSelected = when (themeMode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> systemDark
  }
  val isLightSelected = !isDarkSelected

  val containerColor by animateColorAsState(
    targetValue = if (isDarkSelected) {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    },
    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
    label = "themeToggleContainer",
  )

  val borderColor by animateColorAsState(
    targetValue = if (isDarkSelected) {
      Color(0xFF60A5FA).copy(alpha = 0.28f)
    } else {
      MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    },
    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
    label = "themeToggleBorder",
  )

  // Each slot is 40.dp wide (minimumInteractiveComponentSize is 48.dp overall height/touch box;
  // inside horizontal pill spacing is 40.dp + 4.dp gap = 44.dp travel distance).
  val thumbOffsetX by animateDpAsState(
    targetValue = if (isDarkSelected) 44.dp else 0.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "themeThumbOffset",
  )

  val thumbTopColor by animateColorAsState(
    targetValue = if (isDarkSelected) Color(0xFF1E3A8A) else Color(0xFFFFFBEB),
    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
    label = "themeThumbTop",
  )
  val thumbBottomColor by animateColorAsState(
    targetValue = if (isDarkSelected) Color(0xFF312E81) else Color(0xFFFEF3C7),
    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
    label = "themeThumbBottom",
  )
  val thumbBorderColor by animateColorAsState(
    targetValue = if (isDarkSelected) {
      Color(0xFF60A5FA).copy(alpha = 0.65f)
    } else {
      GoldAccent.copy(alpha = 0.65f)
    },
    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
    label = "themeThumbBorder",
  )

  val sunTint by animateColorAsState(
    targetValue = if (isLightSelected) {
      Color(0xFFD97706)
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
    },
    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
    label = "sunTint",
  )
  val moonTint by animateColorAsState(
    targetValue = if (isDarkSelected) {
      Color(0xFFFDE68A)
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
    },
    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
    label = "moonTint",
  )

  val sunScale by animateFloatAsState(
    targetValue = if (isLightSelected) 1.06f else 0.88f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "sunScale",
  )
  val sunRotation by animateFloatAsState(
    targetValue = if (isLightSelected) 0f else -30f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "sunRotation",
  )

  val moonScale by animateFloatAsState(
    targetValue = if (isDarkSelected) 1.06f else 0.88f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "moonScale",
  )
  val moonRotation by animateFloatAsState(
    targetValue = if (isDarkSelected) 0f else 22f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "moonRotation",
  )

  Surface(
    shape = RoundedCornerShape(50),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      // Smoothly sliding active circular thumb behind the two icon slots
      Surface(
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, thumbBorderColor),
        modifier = Modifier
          .offset(x = thumbOffsetX + 2.dp)
          .size(36.dp),
      ) {
        Box(
          modifier = Modifier.background(
            brush = Brush.linearGradient(
              colors = listOf(thumbTopColor, thumbBottomColor),
            ),
            shape = CircleShape,
          ),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // Sun Logo (Light Theme)
        ThemeLogoSlotButton(
          onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
          contentDescription = stringResource(R.string.theme_mode_light),
          modifier = Modifier.testTag(lightButtonTag),
        ) {
          Icon(
            imageVector = if (isLightSelected) Icons.Rounded.WbSunny else Icons.Outlined.WbSunny,
            contentDescription = stringResource(R.string.theme_mode_light),
            tint = sunTint,
            modifier = Modifier
              .size(19.dp)
              .graphicsLayer {
                scaleX = sunScale
                scaleY = sunScale
                rotationZ = sunRotation
              },
          )
        }

        // Moon Logo (Dark Theme)
        ThemeLogoSlotButton(
          onClick = { onSelectThemeMode(AppThemeMode.DARK) },
          contentDescription = stringResource(R.string.theme_mode_dark),
          modifier = Modifier.testTag(darkButtonTag),
        ) {
          Icon(
            imageVector = if (isDarkSelected) Icons.Rounded.DarkMode else Icons.Outlined.DarkMode,
            contentDescription = stringResource(R.string.theme_mode_dark),
            tint = moonTint,
            modifier = Modifier
              .size(19.dp)
              .graphicsLayer {
                scaleX = moonScale
                scaleY = moonScale
                rotationZ = moonRotation
              },
          )
        }
      }
    }
  }
}

@Composable
private fun ThemeLogoSlotButton(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  icon: @Composable () -> Unit,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(40.dp)
      .minimumInteractiveComponentSize()
      .clip(CircleShape)
      .tactileClickable(
        pressedScale = 0.88f,
        role = Role.RadioButton,
        onClickLabel = contentDescription,
        onClick = onClick,
      ),
  ) {
    icon()
  }
}
