package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.AccountSecurityOtpManager
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.AppAmbientBackdrop
import com.example.ui.components.DimensionalVaultCard
import com.example.ui.components.LiquidGlassIconBadge
import com.example.ui.components.SculptedSurfaceCard
import com.example.ui.components.rememberIsEffectiveDarkTheme
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactilePressScale
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.SkyPositiveAccent

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val tactile = rememberTactileFeedback()
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoadingGoogle by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val triggerGoogleAuth: () -> Unit = {
    if (!isLoadingGoogle) {
      tactile.confirm()
      isLoadingGoogle = true
      errorMessage = null
      onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          isLoadingGoogle = false
          onAuthSuccess()
        },
        onAuthError = { msg ->
          isLoadingGoogle = false
          errorMessage = msg
        },
        scope = scope,
        onAuthCancelled = {
          isLoadingGoogle = false
        },
      )
    }
  }

  val triggerGuestEntry: () -> Unit = {
    if (!isLoadingGoogle) {
      tactile.confirm()
      errorMessage = null
      AccountSecurityOtpManager.startGuestVisitorSession(context = context)
      onAuthSuccess()
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      AppAmbientBackdrop()

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Minimal Brand Header matching inner TopAppBar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 430.dp)
            .padding(top = 4.dp),
          horizontalArrangement = Arrangement.Start,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            LiquidGlassIconBadge(
              size = 40.dp,
              shape = RoundedCornerShape(14.dp),
              tintColor = EmeraldSafe,
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_sakupintar_logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = stringResource(R.string.dashboard_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))
        Spacer(modifier = Modifier.height(16.dp))

        // Unified Auth Container — clean 2-option login (Google + Guest only)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 430.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // 1. Executive Obsidian Hero Card matching Dashboard & Savings
          DimensionalVaultCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  color = Color.White.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(50),
                  border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.26f)),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Shield,
                      contentDescription = null,
                      tint = SkyPositiveAccent,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = stringResource(R.string.app_name),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color.White.copy(alpha = 0.10f),
                  border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
                  modifier = Modifier.size(40.dp),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.img_sakupintar_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = stringResource(R.string.auth_simple_signin_title),
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                  letterSpacing = (-0.4).sp,
                )
                Text(
                  text = stringResource(R.string.auth_simple_signin_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.78f),
                  lineHeight = 19.sp,
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HeroHighlightRow(text = stringResource(R.string.auth_simple_feature_cloud))
                HeroHighlightRow(text = stringResource(R.string.auth_simple_feature_budget))
                HeroHighlightRow(text = stringResource(R.string.auth_simple_feature_ai))
              }
            }
          }

          // 2. Clean 2-Button Action Card (Google + Guest ONLY)
          SculptedSurfaceCard(cornerRadius = 22) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              GooglePrimaryAuthButton(
                label = stringResource(R.string.auth_simple_google_continue),
                isDarkTheme = isDarkTheme,
                isLoading = isLoadingGoogle,
                onClick = triggerGoogleAuth,
              )

              Spacer(modifier = Modifier.height(16.dp))

              MinimalAuthDivider(label = stringResource(R.string.auth_or_divider))

              Spacer(modifier = Modifier.height(16.dp))

              GuestOutlinedAuthButton(
                label = stringResource(R.string.auth_guest_simple_cta),
                enabled = !isLoadingGoogle,
                onClick = triggerGuestEntry,
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = stringResource(R.string.auth_guest_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )

              if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                AuthInlineErrorBanner(message = errorMessage ?: "")
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Spacer(modifier = Modifier.weight(1f, fill = true))
      }
    }
  }
}

@Composable
private fun HeroHighlightRow(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = Icons.Rounded.CheckCircle,
      contentDescription = null,
      tint = EmeraldSafe,
      modifier = Modifier.size(14.dp),
    )
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = Color.White.copy(alpha = 0.86f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun GooglePrimaryAuthButton(
  label: String,
  isDarkTheme: Boolean,
  isLoading: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk
  val contentColor = if (isDarkTheme) ObsidianInk else Color.White

  Button(
    onClick = onClick,
    enabled = !isLoading,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
    ),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp)
      .tactilePressScale(interactionSource)
      .testTag("google_sign_in_button"),
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = contentColor,
      )
      Spacer(modifier = Modifier.width(10.dp))
    } else {
      GoogleMarkIcon(modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(10.dp))
    }
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun GuestOutlinedAuthButton(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp)
      .tactilePressScale(interactionSource)
      .testTag("auth_guest_method_button"),
  ) {
    Icon(
      imageVector = Icons.Rounded.PersonOutline,
      contentDescription = null,
      modifier = Modifier.size(19.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MinimalAuthDivider(label: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outlineVariant,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outlineVariant,
    )
  }
}

@Composable
private fun AuthInlineErrorBanner(message: String) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.errorContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}

@Composable
private fun GoogleMarkIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val stroke = size.minDimension * 0.19f
    val inset = stroke / 2f
    val arcSize = Size(size.width - stroke, size.height - stroke)
    val topLeft = Offset(inset, inset)

    drawArc(
      color = Color(0xFFEA4335),
      startAngle = 215f,
      sweepAngle = 105f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 140f,
      sweepAngle = 78f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 42f,
      sweepAngle = 102f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -8f,
      sweepAngle = 55f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.52f, size.height * 0.50f),
      end = Offset(size.width - inset * 0.4f, size.height * 0.50f),
      strokeWidth = stroke,
      cap = StrokeCap.Butt,
    )
  }
}
