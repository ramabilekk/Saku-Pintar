package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.AiInsightUiState

@Composable
fun AiInsightCard(
  state: AiInsightUiState,
  currentMonthKey: String,
  onAnalyzeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.ai_insight_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.ai_insight_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
              text = "Gemini AI sedang menganalisis pengeluaran & anggaran Anda...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        state is AiInsightUiState.Error -> {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
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
            Text("Coba Analisis Ulang")
          }
        }

        state is AiInsightUiState.Success && state.monthKey == currentMonthKey -> {
          val insight = state.insight
          val badgeColor = when (insight.statusLevel.uppercase()) {
            "BOROS" -> CoralDanger
            "PERHATIAN" -> GoldAccent
            else -> EmeraldBright
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = badgeColor.copy(alpha = 0.14f),
          ) {
            Text(
              text = "Status AI: ${insight.statusLevel}",
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
              text = "Rekomendasi Langkah Hemat",
              style = MaterialTheme.typography.labelLarge,
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

          FilledTonalButton(
            onClick = onAnalyzeClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("ai_insight_refresh_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_insight_refresh))
          }
        }

        else -> {
          Button(
            onClick = onAnalyzeClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("ai_insight_analyze_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.ai_insight_button),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}
