package com.example.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SkyPositiveAccent
import com.example.util.Formatters

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val effectiveLanguage = remember(activeLanguage) { activeLanguage.resolveEffectiveLanguage() }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showLanguageDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          // Brand Header + Language Switcher Pill
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
              Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 3.dp,
                modifier = Modifier.size(48.dp),
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
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.app_tagline),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier
                .clickable { showLanguageDialog = true }
                .testTag("auth_language_button"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Language,
                  contentDescription = stringResource(R.string.language_section_title),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = effectiveLanguage.shortCode,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                  imageVector = Icons.Rounded.ExpandMore,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Custom Hero Illustration Card
          FinanceHeroBanner(currency = activeCurrency)

          Spacer(modifier = Modifier.height(24.dp))

          Text(
            text = stringResource(R.string.auth_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = stringResource(R.string.auth_subheadline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Feature Highlights
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureHighlightRow(
              icon = Icons.AutoMirrored.Rounded.ReceiptLong,
              title = stringResource(R.string.auth_feature_daily),
              tint = MaterialTheme.colorScheme.primary,
            )
            FeatureHighlightRow(
              icon = Icons.Rounded.AutoAwesome,
              title = stringResource(R.string.auth_feature_auto_budget),
              tint = MaterialTheme.colorScheme.tertiary,
            )
            FeatureHighlightRow(
              icon = Icons.Rounded.CloudDone,
              title = stringResource(R.string.auth_feature_cloud),
              tint = MaterialTheme.colorScheme.secondary,
            )
          }
        }

        Column(modifier = Modifier.padding(top = 28.dp)) {
          if (errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            ) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }
          }

          Button(
            onClick = {
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
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
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
              )
            } else {
              Text(
                text = stringResource(R.string.auth_sign_in_google),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
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
private fun FinanceHeroBanner(currency: AppCurrency) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val sampleDailyLimit = remember(currency) {
    Formatters.formatMoney((currency.defaultMonthlyIncome * 7L) / 300L, currency)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(176.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(
        Brush.linearGradient(
          colors = listOf(primary, secondary),
        )
      ),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = Color.White.copy(alpha = 0.08f),
        radius = size.height * 0.7f,
        center = Offset(size.width * 0.88f, size.height * 0.2f),
      )
      drawCircle(
        color = SkyPositiveAccent.copy(alpha = 0.2f),
        radius = size.height * 0.45f,
        center = Offset(size.width * 0.14f, size.height * 0.9f),
      )

      val barBaseY = size.height * 0.78f
      val barWidth = 18.dp.toPx()
      val barGap = 12.dp.toPx()
      val startX = size.width * 0.56f
      val heights = listOf(0.28f, 0.45f, 0.36f, 0.58f, 0.50f)
      heights.forEachIndexed { idx, frac ->
        val h = size.height * frac
        val x = startX + idx * (barWidth + barGap)
        drawRoundRect(
          color = if (idx == 3) GoldAccent else Color.White.copy(alpha = 0.35f),
          topLeft = Offset(x, barBaseY - h),
          size = Size(barWidth, h),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )
      }

      val trendPath = Path().apply {
        moveTo(startX - 8.dp.toPx(), barBaseY - size.height * 0.22f)
        cubicTo(
          startX + 40.dp.toPx(), barBaseY - size.height * 0.55f,
          startX + 80.dp.toPx(), barBaseY - size.height * 0.30f,
          startX + 128.dp.toPx(), barBaseY - size.height * 0.64f,
        )
      }
      drawPath(
        path = trendPath,
        color = Color.White,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
      ) {
        Text(
          text = stringResource(R.string.auth_hero_badge),
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Column {
        Text(
          text = stringResource(R.string.auth_hero_limit_label),
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.85f),
        )
        Text(
          text = sampleDailyLimit,
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  tint: Color,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      shape = CircleShape,
      color = tint.copy(alpha = 0.12f),
      modifier = Modifier.size(40.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.Medium,
    )
  }
}
