package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.viewmodel.AiInsightUiState

@Composable
fun AiInsightCard(
  state: AiInsightUiState,
  currentMonthKey: String,
  onAnalyzeClick: () -> Unit,
  onOpenAiChat: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  SculptedSurfaceCard(
    cornerRadius = 20,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
          modifier = Modifier.size(42.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(21.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.ai_insight_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.ai_insight_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(50),
          color = GoldAccent.copy(alpha = 0.16f),
          border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.45f)),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = GoldAccent,
              modifier = Modifier.size(12.dp),
            )
            Text(
              text = "AI",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = GoldAccent,
            )
          }
        }
      }

      when {
        state is AiInsightUiState.Loading -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.ai_insight_loading),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        state is AiInsightUiState.Error -> {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp),
            )
          }
          FilledTonalButton(
            onClick = onAnalyzeClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("ai_insight_retry_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_insight_retry))
          }
        }

        state is AiInsightUiState.Success && state.monthKey == currentMonthKey -> {
          val insight = state.insight
          val (badgeColor, statusText) = when (insight.statusLevel.uppercase()) {
            "BOROS" -> CoralDanger to stringResource(R.string.hero_status_over)
            "PERHATIAN" -> GoldAccent to stringResource(R.string.hero_status_caution)
            else -> SkyPositiveAccent to stringResource(R.string.hero_status_safe)
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = badgeColor.copy(alpha = 0.14f),
            border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.35f)),
          ) {
            Text(
              text = stringResource(R.string.ai_insight_status_badge, statusText),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          Text(
            text = insight.headline,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Text(
            text = insight.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = insight.topCategoryHighlight,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.ai_insight_tips_title),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            insight.actionTips.forEachIndexed { index, tip ->
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Text(
                  text = "${index + 1}.",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = tip,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = onAnalyzeClick,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("ai_insight_refresh_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.ai_insight_refresh))
            }
            if (onOpenAiChat != null) {
              Button(
                onClick = onOpenAiChat,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
                  contentColor = if (isDarkTheme) ObsidianInk else Color.White,
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("ai_chat_card_open_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Forum,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.ai_chat_open_button), fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        else -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = onAnalyzeClick,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
                contentColor = if (isDarkTheme) ObsidianInk else Color.White,
              ),
              modifier = Modifier
                .weight(1.25f)
                .height(48.dp)
                .testTag("ai_insight_analyze_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.ai_insight_button),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
            }
            if (onOpenAiChat != null) {
              OutlinedButton(
                onClick = onOpenAiChat,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                  .weight(0.95f)
                  .height(48.dp)
                  .testTag("ai_chat_card_open_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Forum,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.ai_chat_open_button),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }
  }
}
