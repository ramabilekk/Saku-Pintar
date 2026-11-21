package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DonutSlice(
  val category: ExpenseCategory,
  val amount: Long,
  val fraction: Float,
)

/**
 * Animated Donut Chart + Month-over-Month spending trend comparison + Largest Expense highlight.
 */
@Composable
fun CategoryDonutChartCard(
  monthKey: String,
  expenses: List<Expense>,
  allHistoricalExpenses: List<Expense>,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  modifier: Modifier = Modifier,
) {
  val totalCurrent = remember(expenses) { expenses.sumOf { it.amount } }
  val prevMonthKey = remember(monthKey) { Formatters.shiftMonthKey(monthKey, -1L) }
  val totalPrev = remember(allHistoricalExpenses, prevMonthKey) {
    allHistoricalExpenses.filter { it.monthKey == prevMonthKey }.sumOf { it.amount }
  }

  val slices = remember(expenses, totalCurrent) {
    if (totalCurrent <= 0L) {
      emptyList()
    } else {
      expenses.groupBy { it.categoryEnum }
        .map { (cat, list) ->
          val sum = list.sumOf { it.amount }
          DonutSlice(
            category = cat,
            amount = sum,
            fraction = (sum.toFloat() / totalCurrent.toFloat()).coerceIn(0f, 1f),
          )
        }
        .sortedByDescending { it.amount }
    }
  }

  val topExpense = remember(expenses) { expenses.maxByOrNull { it.amount } }

  var playAnim by remember(monthKey, totalCurrent) { mutableStateOf(false) }
  LaunchedEffect(monthKey, totalCurrent) {
    playAnim = true
  }
  val sweepProgress by animateFloatAsState(
    targetValue = if (playAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
    label = "donut_sweep",
  )

  SculptedSurfaceCard(
    accentColor = MaterialTheme.colorScheme.secondary,
    cornerRadius = 22,
    modifier = modifier.testTag("dashboard_donut_analytics_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Title & MoM comparison badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier.size(32.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.DonutLarge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Text(
            text = stringResource(R.string.analytics_donut_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      // Month-over-Month Trend Pill
      val momDeltaPercent: Int? = if (totalPrev > 0L && totalCurrent > 0L) {
        (((totalCurrent - totalPrev).toDouble() / totalPrev.toDouble()) * 100.0).roundToInt()
      } else {
        null
      }

      val (trendText, trendColor, trendIcon) = when {
        momDeltaPercent == null -> Triple(
          stringResource(R.string.analytics_mom_no_prev),
          MaterialTheme.colorScheme.onSurfaceVariant,
          Icons.Rounded.HorizontalRule,
        )
        momDeltaPercent < 0 -> Triple(
          stringResource(R.string.analytics_mom_lower, abs(momDeltaPercent)),
          EmeraldSafe,
          Icons.AutoMirrored.Rounded.TrendingDown,
        )
        momDeltaPercent > 0 -> Triple(
          stringResource(R.string.analytics_mom_higher, momDeltaPercent),
          CoralDanger,
          Icons.AutoMirrored.Rounded.TrendingUp,
        )
        else -> Triple(
          stringResource(R.string.analytics_mom_same),
          MaterialTheme.colorScheme.primary,
          Icons.Rounded.HorizontalRule,
        )
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = trendColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, trendColor.copy(alpha = 0.25f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = trendIcon,
            contentDescription = null,
            tint = trendColor,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = trendText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = trendColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (slices.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Animated Canvas Donut Ring
          Box(
            modifier = Modifier.size(126.dp),
            contentAlignment = Alignment.Center,
          ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.size(120.dp)) {
              val strokeWidth = 18.dp.toPx()
              val diameter = size.minDimension - strokeWidth
              val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f,
              )
              val arcSize = Size(diameter, diameter)

              drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
              )

              var startAngle = -90f
              val gapDegrees = if (slices.size > 1) 3f else 0f
              slices.forEach { slice ->
                val rawSweep = (slice.fraction * 360f * sweepProgress)
                val visibleSweep = (rawSweep - gapDegrees).coerceAtLeast(1.5f)
                drawArc(
                  color = slice.category.accentColor,
                  startAngle = startAngle,
                  sweepAngle = visibleSweep,
                  useCenter = false,
                  topLeft = topLeft,
                  size = arcSize,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += rawSweep
              }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = stringResource(R.string.analytics_donut_total_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = Formatters.formatMoneyCompactMasked(
                  amount = totalCurrent,
                  currency = currency,
                  locale = locale,
                  hideBalances = hideBalances,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
              )
            }
          }

          // Top 4 category breakdown legend
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            slices.take(4).forEach { slice ->
              val pct = (slice.fraction * 100f).roundToInt()
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .background(slice.category.accentColor, CircleShape),
                )
                Text(
                  text = stringResource(slice.category.shortLabelRes),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "$pct%",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = slice.category.accentColor,
                )
              }
            }
          }
        }
      }

      // Largest Single Expense Highlight
      if (topExpense != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.EmojiEvents,
              contentDescription = null,
              tint = GoldAccent,
              modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.analytics_top_expense_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = topExpense.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = Formatters.formatMoneyMasked(topExpense.amount, currency, hideBalances),
              style = MaterialTheme.typography.labelLarge,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}
