package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.AccountSecurityOtpManager
import com.example.auth.onGoogleSignInClicked
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.ThemeLogoToggle
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactileClickable
import com.example.ui.components.tactilePressScale
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.theme.ThemePreferenceManager
import com.example.util.Formatters

private enum class AuthFlowStep {
  LANDING,
  METHOD_SELECTION,
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val tactile = rememberTactileFeedback()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val themeMode by ThemePreferenceManager.observeThemeMode(context).collectAsStateWithLifecycle()
  val effectiveLanguage = remember(activeLanguage) { activeLanguage.resolveEffectiveLanguage() }

  val systemDark = isSystemInDarkTheme()
  val isDarkTheme = when (themeMode) {
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
    AppThemeMode.SYSTEM -> systemDark
  }

  var currentStep by remember { mutableStateOf(AuthFlowStep.LANDING) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  var isSignUpMode by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      // Multi-layered atmospheric backdrop (eliminates flat appearance)
      AuthAtmosphericBackdrop(isDarkTheme = isDarkTheme)

      val pageSlideEasing = remember { CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f) }

      AnimatedContent(
        targetState = currentStep,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
          val forward = targetState.ordinal >= initialState.ordinal
          val transform = if (forward) {
            (slideInHorizontally(
              animationSpec = tween(durationMillis = 380, easing = pageSlideEasing),
              initialOffsetX = { fullWidth -> fullWidth },
            ) +
              fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
              scaleIn(
                animationSpec = tween(durationMillis = 380, easing = pageSlideEasing),
                initialScale = 0.96f,
              ))
              .togetherWith(
                slideOutHorizontally(
                  animationSpec = tween(durationMillis = 360, easing = pageSlideEasing),
                  targetOffsetX = { fullWidth -> -fullWidth / 3 },
                ) +
                  fadeOut(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)) +
                  scaleOut(
                    animationSpec = tween(durationMillis = 360, easing = pageSlideEasing),
                    targetScale = 0.94f,
                  )
              )
          } else {
            (slideInHorizontally(
              animationSpec = tween(durationMillis = 380, easing = pageSlideEasing),
              initialOffsetX = { fullWidth -> -fullWidth / 3 },
            ) +
              fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
              scaleIn(
                animationSpec = tween(durationMillis = 380, easing = pageSlideEasing),
                initialScale = 0.94f,
              ))
              .togetherWith(
                slideOutHorizontally(
                  animationSpec = tween(durationMillis = 360, easing = pageSlideEasing),
                  targetOffsetX = { fullWidth -> fullWidth },
                ) +
                  fadeOut(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)) +
                  scaleOut(
                    animationSpec = tween(durationMillis = 360, easing = pageSlideEasing),
                    targetScale = 0.96f,
                  )
              )
          }
          transform.using(SizeTransform(clip = false)).apply {
            targetContentZIndex = targetState.ordinal.toFloat()
          }
        },
        label = "auth_flow_step_transition",
      ) { step ->
        when (step) {
          // =========================================================================
          // STEP 1: HALAMAN UTAMA LOGIN (TAMPILAN RAPIH, TERSTRUKTUR & PROFESIONAL)
          // =========================================================================
          AuthFlowStep.LANDING -> {
            Column(
              modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top Bar: Sculpted Brand Identity + Sun/Moon Theme Toggle + Language Switcher
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    val brandBorderColor by animateColorAsState(
                      targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.40f else 0.25f),
                      animationSpec = tween(420, easing = FastOutSlowInEasing),
                      label = "auth_brand_border",
                    )
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      shadowElevation = 4.dp,
                      border = BorderStroke(1.dp, brandBorderColor),
                      modifier = Modifier.size(42.dp),
                    ) {
                      Image(
                        painter = painterResource(id = R.drawable.sakupintar_app_icon_1794739414831),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                          .fillMaxSize()
                          .clip(RoundedCornerShape(14.dp)),
                      )
                    }

                    Column {
                      Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    // Sun & Moon Theme Switcher right on the Login Screen
                    ThemeLogoToggle(
                      themeMode = themeMode,
                      onSelectThemeMode = { mode ->
                        ThemePreferenceManager.setThemeMode(context, mode)
                      },
                      lightButtonTag = "auth_theme_light_button",
                      darkButtonTag = "auth_theme_dark_button",
                      modifier = Modifier.testTag("auth_theme_toggle"),
                    )

                    // Language Switcher Pill
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                      border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                      ),
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .tactileClickable(pressedScale = 0.94f) { showLanguageDialog = true }
                        .testTag("auth_language_button"),
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Language,
                          contentDescription = stringResource(R.string.language_section_title),
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(17.dp),
                        )
                        Text(
                          text = effectiveLanguage.shortCode,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                          imageVector = Icons.Rounded.ExpandMore,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(15.dp),
                        )
                      }
                    }
                  }
                }

                // Headline & Value Proposition (Clean Editorial Hierarchy)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = stringResource(R.string.auth_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  Text(
                    text = stringResource(R.string.auth_subheadline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                // 3D-Layered Finance Showcase Card (Self-Contained, Crisp Alignment)
                DimensionalFinanceShowcase(
                  currency = activeCurrency,
                  isDarkTheme = isDarkTheme,
                )

                // Unified Bento Feature Card (All 3 Core Pillars Neatly Grouped)
                val bentoColor by animateColorAsState(
                  targetValue = if (isDarkTheme) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                  } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                  },
                  animationSpec = tween(420, easing = FastOutSlowInEasing),
                  label = "auth_bento_color",
                )
                val bentoElevation by animateDpAsState(
                  targetValue = if (isDarkTheme) 0.dp else 3.dp,
                  animationSpec = tween(420, easing = FastOutSlowInEasing),
                  label = "auth_bento_elevation",
                )
                val bentoBorderColor by animateColorAsState(
                  targetValue = MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.22f else 0.16f),
                  animationSpec = tween(420, easing = FastOutSlowInEasing),
                  label = "auth_bento_border",
                )
                Surface(
                  shape = RoundedCornerShape(22.dp),
                  color = bentoColor,
                  shadowElevation = bentoElevation,
                  border = BorderStroke(width = 1.dp, color = bentoBorderColor),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 6.dp),
                  ) {
                    SculptedFeatureRow(
                      icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                      title = stringResource(R.string.auth_feature_daily),
                      badgeText = stringResource(R.string.receipt_badge_short),
                      accentColor = MaterialTheme.colorScheme.primary,
                      isDarkTheme = isDarkTheme,
                    )
                    HorizontalDivider(
                      thickness = 0.5.dp,
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                    )
                    SculptedFeatureRow(
                      icon = Icons.Rounded.AutoAwesome,
                      title = stringResource(R.string.auth_feature_auto_budget),
                      badgeText = "AI",
                      accentColor = MaterialTheme.colorScheme.tertiary,
                      isDarkTheme = isDarkTheme,
                    )
                    HorizontalDivider(
                      thickness = 0.5.dp,
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                    )
                    SculptedFeatureRow(
                      icon = Icons.Rounded.CloudDone,
                      title = stringResource(R.string.auth_feature_cloud),
                      badgeText = "Cloud",
                      accentColor = MaterialTheme.colorScheme.secondary,
                      isDarkTheme = isDarkTheme,
                    )
                  }
                }
              }

              // Landing Dock Card: Single "Masuk" button that opens the Sign In / Sign Up & Method Selection screen
              val dockColor by animateColorAsState(
                targetValue = if (isDarkTheme) {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
                } else {
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                },
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_dock_color",
              )
              val dockElevation by animateDpAsState(
                targetValue = if (isDarkTheme) 12.dp else 8.dp,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_dock_elevation",
              )
              val dockPrimaryBorder by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.45f else 0.25f),
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_dock_primary_border",
              )
              Surface(
                shape = RoundedCornerShape(24.dp),
                color = dockColor,
                shadowElevation = dockElevation,
                border = BorderStroke(
                  width = 1.dp,
                  brush = Brush.linearGradient(
                    colors = listOf(
                      dockPrimaryBorder,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    ),
                  ),
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 16.dp, bottom = 4.dp),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                      modifier = Modifier.size(26.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Rounded.VerifiedUser,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(14.dp),
                        )
                      }
                    }
                    Text(
                      text = stringResource(R.string.auth_landing_subtitle),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.weight(1f),
                    )
                  }

                  // Single Primary Action Button: "Masuk"
                  val enterInteractionSource = remember { MutableInteractionSource() }
                  Button(
                    onClick = {
                      tactile.navigation()
                      errorMessage = null
                      currentStep = AuthFlowStep.METHOD_SELECTION
                    },
                    interactionSource = enterInteractionSource,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                      defaultElevation = 4.dp,
                      pressedElevation = 1.dp,
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(54.dp)
                      .tactilePressScale(
                        interactionSource = enterInteractionSource,
                        pressedScale = 0.965f,
                        enablePressHaptic = false,
                      )
                      .testTag("auth_landing_enter_button"),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Rounded.Login,
                          contentDescription = null,
                          modifier = Modifier.size(20.dp),
                        )
                        Text(
                          text = stringResource(R.string.auth_landing_enter_action),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                        )
                      }
                      Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // =========================================================================
          // STEP 2: HALAMAN PILIHAN LOGIN / DAFTAR (GOOGLE & GUEST)
          // =========================================================================
          AuthFlowStep.METHOD_SELECTION -> {
            BackHandler {
              tactile.navigation()
              errorMessage = null
              currentStep = AuthFlowStep.LANDING
            }

            Column(
              modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("auth_method_selection_screen"),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              // Top Navigation Bar with Back Button + Security Badge
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                val backInteractionSource = remember { MutableInteractionSource() }
                FilledTonalButton(
                  onClick = {
                    tactile.navigation()
                    errorMessage = null
                    currentStep = AuthFlowStep.LANDING
                  },
                  interactionSource = backInteractionSource,
                  shape = RoundedCornerShape(14.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                  modifier = Modifier
                    .height(48.dp)
                    .tactilePressScale(
                      interactionSource = backInteractionSource,
                      pressedScale = 0.94f,
                      enablePressHaptic = false,
                    )
                    .testTag("auth_back_to_landing_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.auth_back_action),
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.auth_back_action),
                    fontWeight = FontWeight.Bold,
                  )
                }

                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Security,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = stringResource(R.string.app_name),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }

              // Header Title & Description for Method Selection Screen (with slide transition on mode switch)
              AnimatedContent(
                targetState = isSignUpMode,
                transitionSpec = {
                  val toSignUp = targetState
                  if (toSignUp) {
                    (slideInHorizontally(tween(280, easing = pageSlideEasing)) { it / 4 } +
                      fadeIn(tween(220)))
                      .togetherWith(
                        slideOutHorizontally(tween(220, easing = pageSlideEasing)) { -it / 4 } +
                          fadeOut(tween(180))
                      )
                  } else {
                    (slideInHorizontally(tween(280, easing = pageSlideEasing)) { -it / 4 } +
                      fadeIn(tween(220)))
                      .togetherWith(
                        slideOutHorizontally(tween(220, easing = pageSlideEasing)) { it / 4 } +
                          fadeOut(tween(180))
                      )
                  }
                },
                label = "method_header_slide_transition",
              ) { signUpSelected ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = if (signUpSelected) {
                      stringResource(R.string.auth_method_screen_sign_up_title)
                    } else {
                      stringResource(R.string.auth_method_screen_sign_in_title)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                  )
                  Text(
                    text = if (signUpSelected) {
                      stringResource(R.string.auth_choose_method_sign_up)
                    } else {
                      stringResource(R.string.auth_choose_method_sign_in)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              if (errorMessage != null) {
                Surface(
                  color = MaterialTheme.colorScheme.errorContainer,
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                  )
                }
              }

              val formCardColor by animateColorAsState(
                targetValue = if (isDarkTheme) {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                } else {
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                },
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_form_card_color",
              )
              val formCardElevation by animateDpAsState(
                targetValue = if (isDarkTheme) 10.dp else 8.dp,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_form_card_elevation",
              )
              val formCardBorderPrimary by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.45f else 0.25f),
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_form_card_border",
              )
              val segmentedTrackColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.surfaceVariant.copy(
                  alpha = if (isDarkTheme) 0.55f else 0.72f,
                ),
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "auth_segmented_track_color",
              )

              Surface(
                shape = RoundedCornerShape(24.dp),
                color = formCardColor,
                shadowElevation = formCardElevation,
                border = BorderStroke(
                  width = 1.dp,
                  brush = Brush.linearGradient(
                    colors = listOf(
                      formCardBorderPrimary,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    ),
                  ),
                ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  // Segmented Sign In / Sign Up Mode Selector
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = segmentedTrackColor,
                border = BorderStroke(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  val signInSelected = !isSignUpMode
                  val signUpSelected = isSignUpMode

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (signInSelected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      Color.Transparent
                    },
                    shadowElevation = if (signInSelected) 4.dp else 0.dp,
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .tactileClickable(pressedScale = 0.95f) {
                        isSignUpMode = false
                        errorMessage = null
                      }
                      .testTag("auth_mode_sign_in_tab"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = stringResource(R.string.auth_tab_sign_in),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (signInSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (signInSelected) {
                          MaterialTheme.colorScheme.onPrimary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (signUpSelected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      Color.Transparent
                    },
                    shadowElevation = if (signUpSelected) 4.dp else 0.dp,
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .tactileClickable(pressedScale = 0.95f) {
                        isSignUpMode = true
                        errorMessage = null
                      }
                      .testTag("auth_mode_sign_up_tab"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = stringResource(R.string.auth_tab_sign_up),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (signUpSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (signUpSelected) {
                          MaterialTheme.colorScheme.onPrimary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              }

              // Method Selection Subtitle inside Sign In / Sign Up
              Text(
                text = if (isSignUpMode) {
                  stringResource(R.string.auth_choose_method_sign_up)
                } else {
                  stringResource(R.string.auth_choose_method_sign_in)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
              )

              // Method 1: Google Sign In / Sign Up
              val googleInteractionSource = remember { MutableInteractionSource() }
              Button(
                onClick = {
                  tactile.confirm()
                  errorMessage = null
                  isLoading = true
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onAuthError = { msg ->
                      isLoading = false
                      errorMessage = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                      isLoading = false
                    },
                  )
                },
                interactionSource = googleInteractionSource,
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(
                  defaultElevation = 4.dp,
                  pressedElevation = 1.dp,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .tactilePressScale(
                    interactionSource = googleInteractionSource,
                    pressedScale = 0.965f,
                    enablePressHaptic = false,
                  )
                  .testTag("google_sign_in_button"),
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = stringResource(R.string.auth_connecting),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                } else {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    // Crisp White Google 'G' Emblem
                    Surface(
                      shape = CircleShape,
                      color = Color.White,
                      shadowElevation = 2.dp,
                      modifier = Modifier.size(34.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        GoogleGLogoCanvas(modifier = Modifier.size(18.dp))
                      }
                    }

                    Text(
                      text = if (isSignUpMode) {
                        stringResource(R.string.auth_sign_up_google)
                      } else {
                        stringResource(R.string.auth_sign_in_google)
                      },
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )

                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                      modifier = Modifier.size(32.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    }
                  }
                }
              }

              // Divider: Alternative Option (Guest Account)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                HorizontalDivider(
                  modifier = Modifier.weight(1f),
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Text(
                  text = stringResource(R.string.auth_or_other_methods),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(
                  modifier = Modifier.weight(1f),
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
              }

              // Method 2: Guest Account Secondary Option
              val guestBtnInteraction = remember { MutableInteractionSource() }
              FilledTonalButton(
                onClick = {
                  tactile.confirm()
                  errorMessage = null
                  // Guest visitor mode: enter immediately without cloud backup
                  AccountSecurityOtpManager.startGuestVisitorSession(context)
                  onAuthSuccess()
                },
                interactionSource = guestBtnInteraction,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .tactilePressScale(
                    interactionSource = guestBtnInteraction,
                    pressedScale = 0.96f,
                    enablePressHaptic = false,
                  )
                  .testTag("auth_guest_method_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.PersonOutline,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.auth_guest_continue),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Text(
                text = stringResource(R.string.auth_guest_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp),
              )

              // Subtle Cloud Security Footnote
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.VerifiedUser,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.profile_connected_google),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
        }
      }
    }
  }

  if (showLanguageDialog) {
    LanguagePickerDialog(
      selectedLanguage = activeLanguage,
      onLanguageSelected = {},
      onDismiss = { showLanguageDialog = false },
    )
  }
}

@Composable
private fun AuthAtmosphericBackdrop(isDarkTheme: Boolean) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary

  val topOrbAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.24f else 0.15f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "auth_top_orb_alpha",
  )
  val rightOrbAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.20f else 0.12f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "auth_right_orb_alpha",
  )
  val bottomOrbAlpha by animateFloatAsState(
    targetValue = if (isDarkTheme) 0.16f else 0.10f,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "auth_bottom_orb_alpha",
  )
  val gridColor by animateColorAsState(
    targetValue = if (isDarkTheme) {
      Color.White.copy(alpha = 0.035f)
    } else {
      primary.copy(alpha = 0.045f)
    },
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "auth_grid_color",
  )

  Canvas(modifier = Modifier.fillMaxSize()) {
    if (size.width <= 1f || size.height <= 1f) return@Canvas

    val topRadius = (size.width * 0.72f).coerceAtLeast(1f)
    // Top-left luminous cobalt orb
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          primary.copy(alpha = topOrbAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.12f, size.height * 0.08f),
        radius = topRadius,
      ),
      radius = topRadius,
      center = Offset(size.width * 0.12f, size.height * 0.08f),
    )

    val rightRadius = (size.width * 0.68f).coerceAtLeast(1f)
    // Center-right electric violet nebula
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          secondary.copy(alpha = rightOrbAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.92f, size.height * 0.36f),
        radius = rightRadius,
      ),
      radius = rightRadius,
      center = Offset(size.width * 0.92f, size.height * 0.36f),
    )

    val bottomRadius = (size.width * 0.65f).coerceAtLeast(1f)
    // Bottom-left warm amber/sky glow
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          tertiary.copy(alpha = bottomOrbAlpha),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.18f, size.height * 0.88f),
        radius = bottomRadius,
      ),
      radius = bottomRadius,
      center = Offset(size.width * 0.18f, size.height * 0.88f),
    )

    // Subtle architectural grid lines in top section
    val step = 36.dp.toPx().coerceAtLeast(8f)
    val maxGridY = size.height * 0.48f
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

@Composable
private fun DimensionalFinanceShowcase(
  currency: AppCurrency,
  isDarkTheme: Boolean,
) {
  val sampleDailyLimit = remember(currency) {
    Formatters.formatMoney((currency.defaultMonthlyIncome * 7L) / 300L, currency)
  }
  val sampleSavingsAmount = remember(currency) {
    Formatters.formatMoney(currency.defaultMonthlyIncome / 5L, currency)
  }

  val showcaseElevation by animateDpAsState(
    targetValue = if (isDarkTheme) 6.dp else 10.dp,
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "auth_showcase_elevation",
  )

  Surface(
    shape = RoundedCornerShape(24.dp),
    shadowElevation = showcaseElevation,
    color = Color.Transparent,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color(0xFF1E3A8A),
              Color(0xFF3730A3),
              Color(0xFF1E1B4B),
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 600f),
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.34f),
              Color.White.copy(alpha = 0.08f),
            ),
          ),
          shape = RoundedCornerShape(24.dp),
        ),
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val r1 = (size.height * 0.72f).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              SkyPositiveAccent.copy(alpha = 0.28f),
              Color.Transparent,
            ),
            center = Offset(size.width * 0.86f, size.height * 0.22f),
            radius = r1,
          ),
          radius = r1,
          center = Offset(size.width * 0.86f, size.height * 0.22f),
        )
        val r2 = (size.height * 0.55f).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              GoldAccent.copy(alpha = 0.20f),
              Color.Transparent,
            ),
            center = Offset(size.width * 0.14f, size.height * 0.88f),
            radius = r2,
          ),
          radius = r2,
          center = Offset(size.width * 0.14f, size.height * 0.88f),
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Top Header Badges
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.26f)),
          ) {
            Text(
              text = stringResource(R.string.auth_hero_badge),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            )
          }

          Surface(
            color = GoldAccent.copy(alpha = 0.22f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.50f)),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFDE68A),
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "Gemini AI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFEF3C7),
              )
            }
          }
        }

        // Middle Metric + Mini Chart Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .background(SkyPositiveAccent, CircleShape),
              )
              Text(
                text = stringResource(R.string.auth_hero_limit_label),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = sampleDailyLimit,
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = stringResource(R.string.hero_status_safe),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFBAE6FD),
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Clean self-contained mini bar + spline canvas
          Canvas(
            modifier = Modifier
              .width(116.dp)
              .height(58.dp),
          ) {
            val barWidth = 11.dp.toPx()
            val barGap = 7.dp.toPx()
            val heights = listOf(0.36f, 0.54f, 0.44f, 0.76f, 0.62f, 0.90f)
            heights.forEachIndexed { idx, frac ->
              val h = size.height * frac
              val x = idx * (barWidth + barGap)
              drawRoundRect(
                color = when (idx) {
                  5 -> GoldAccent
                  3 -> SkyPositiveAccent
                  else -> Color.White.copy(alpha = 0.25f)
                },
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
              )
            }

            val trendPath = Path().apply {
              moveTo(2.dp.toPx(), size.height * 0.68f)
              cubicTo(
                size.width * 0.32f, size.height * 0.28f,
                size.width * 0.62f, size.height * 0.54f,
                size.width - 10.dp.toPx(), size.height * 0.14f,
              )
            }
            drawPath(
              path = trendPath,
              color = Color.White,
              style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
              color = GoldAccent,
              radius = 4.2.dp.toPx(),
              center = Offset(size.width - 10.dp.toPx(), size.height * 0.14f),
            )
          }
        }

        // Integrated Bottom Glassmorphic Savings Strip (No protruding overlap)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color.White.copy(alpha = 0.12f),
          border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = GoldAccent.copy(alpha = 0.24f),
                modifier = Modifier.size(26.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = Color(0xFFFDE68A),
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
              Text(
                text = "${stringResource(R.string.hero_savings_target)} • 20%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.90f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
              )
            }

            Text(
              text = sampleSavingsAmount,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFFFDE68A),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SculptedFeatureRow(
  icon: ImageVector,
  title: String,
  badgeText: String,
  accentColor: Color,
  isDarkTheme: Boolean,
) {
  val iconBoxBgColor by animateColorAsState(
    targetValue = accentColor.copy(alpha = if (isDarkTheme) 0.20f else 0.12f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "feature_row_icon_bg",
  )
  val badgeBgColor by animateColorAsState(
    targetValue = accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.10f),
    animationSpec = tween(420, easing = FastOutSlowInEasing),
    label = "feature_row_badge_bg",
  )
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 11.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = iconBoxBgColor,
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(19.dp),
        )
      }
    }

    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.weight(1f),
    )

    Surface(
      shape = RoundedCornerShape(50),
      color = badgeBgColor,
    ) {
      Text(
        text = badgeText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun GoogleGLogoCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val strokeWidth = size.minDimension * 0.22f
    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

    // Red top arc
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = -145f,
      sweepAngle = 105f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Yellow left arc
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 140f,
      sweepAngle = 75f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Green bottom arc
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 45f,
      sweepAngle = 95f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    // Blue right arc + crossbar
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -12f,
      sweepAngle = 57f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.52f, size.height * 0.5f),
      end = Offset(size.width * 0.94f, size.height * 0.5f),
      strokeWidth = strokeWidth * 0.92f,
      cap = StrokeCap.Butt,
    )
  }
}
