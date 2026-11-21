package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.budget.PaydayCycleManager
import com.example.data.local.CashflowEntry
import com.example.data.local.CashflowType
import com.example.data.model.Expense
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class MonthlyTrendBucket(
  val monthKey: String,
  val shortLabel: String,
  val fullLabel: String,
  val spent: Long,
  val income: Long,
  val txCount: Int,
) {
  val net: Long get() = income - spent
}

@Composable
fun MultiMonthTrendCard(
  selectedMonthKey: String,
  allHistoricalExpenses: List<Expense>,
  cashflowEntries: List<CashflowEntry>,
  paydayStartDay: Int,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  modifier: Modifier = Modifier,
) {
  var windowMonths by remember { mutableIntStateOf(6) }

  val buckets = remember(selectedMonthKey, allHistoricalExpenses, cashflowEntries, paydayStartDay, windowMonths, locale) {
    val endYm = runCatching { YearMonth.parse(selectedMonthKey) }.getOrElse { YearMonth.now() }
    val shortFmt = DateTimeFormatter.ofPattern("MMM", locale)
    val fullFmt = DateTimeFormatter.ofPattern("MMM yyyy", locale)
    val incomeEntries = cashflowEntries.filter { it.type == CashflowType.INCOME }

    (windowMonths - 1 downTo 0).map { offset ->
      val ym = endYm.minusMonths(offset.toLong())
      val mKey = ym.toString()
      val cycle = PaydayCycleManager.resolveCycle(mKey, paydayStartDay)
      val monthExpenses = if (paydayStartDay > 1) {
        allHistoricalExpenses.filter { cycle.contains(it.dateEpochDay) }
      } else {
        allHistoricalExpenses.filter { it.monthKey == mKey }
      }
      val monthIncome = if (paydayStartDay > 1) {
        incomeEntries.filter { cycle.contains(it.dateEpochDay) }.sumOf { it.amount }
      } else {
        incomeEntries.filter { it.monthKey == mKey }.sumOf { it.amount }
      }
      MonthlyTrendBucket(
        monthKey = mKey,
        shortLabel = ym.format(shortFmt).take(3),
        fullLabel = ym.format(fullFmt),
        spent = monthExpenses.sumOf { it.amount },
        income = monthIncome,
        txCount = monthExpenses.size,
      )
    }
  }

  var selectedIndex by remember(windowMonths, selectedMonthKey) {
    mutableIntStateOf((buckets.lastIndex).coerceAtLeast(0))
  }
  val safeSelectedIndex = selectedIndex.coerceIn(0, (buckets.lastIndex).coerceAtLeast(0))
  val focusedBucket = buckets.getOrNull(safeSelectedIndex)

  val totalSpent = remember(buckets) { buckets.sumOf { it.spent } }
  val totalIncome = remember(buckets) { buckets.sumOf { it.income } }
  val activeSpendingBuckets = remember(buckets) { buckets.filter { it.spent > 0L } }
  val avgMonthlySpent = remember(totalSpent, activeSpendingBuckets) {
    if (activeSpendingBuckets.isNotEmpty()) totalSpent / activeSpendingBuckets.size else 0L
  }
  val lowestMonth = remember(activeSpendingBuckets) {
    activeSpendingBuckets.minByOrNull { it.spent }
  }
  val highestMonth = remember(activeSpendingBuckets) {
    activeSpendingBuckets.maxByOrNull { it.spent }
  }
  val maxBarValue = remember(buckets) {
    buckets.maxOfOrNull { maxOf(it.spent, it.income) }?.coerceAtLeast(1L) ?: 1L
  }

  val animationProgress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 550),
    label = "trend_bar_anim",
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val guideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
  val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

  SculptedSurfaceCard(
    accentColor = primaryColor,
    cornerRadius = 22,
    modifier = modifier
      .fillMaxWidth()
      .testTag("multi_month_trend_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header + 6 Mo / 12 Mo Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = primaryColor.copy(alpha = 0.14f),
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.ShowChart,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Text(
            text = stringResource(R.string.trend_card_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Segmented Pill: 6 Bln | 12 Bln
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(modifier = Modifier.padding(3.dp)) {
            listOf(
              6 to stringResource(R.string.trend_6_months),
              12 to stringResource(R.string.trend_12_months),
            ).forEach { (months, label) ->
              val isSelected = windowMonths == months
              Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) primaryColor else Color.Transparent,
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { windowMonths = months }
                  .testTag("trend_window_${months}m"),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }
        }
      }

      // Legend Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          LegendDot(color = CoralDanger, label = stringResource(R.string.trend_legend_spending))
          LegendDot(color = EmeraldSafe, label = stringResource(R.string.trend_legend_income))
        }
        Text(
          text = stringResource(R.string.trend_tap_hint),
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Interactive Dual-Bar Canvas Chart
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .pointerInput(buckets) {
              detectTapGestures { offset ->
                if (buckets.isNotEmpty() && size.width > 0) {
                  val slotWidth = size.width.toFloat() / buckets.size.toFloat()
                  val tapped = (offset.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
                  selectedIndex = tapped
                }
              }
            }
            .testTag("trend_bar_canvas"),
        ) {
          val count = buckets.size
          if (count == 0) return@Canvas
          val slotWidth = size.width / count
          val topPad = 10.dp.toPx()
          val bottomPad = 4.dp.toPx()
          val usableHeight = (size.height - topPad - bottomPad).coerceAtLeast(10f)
          val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

          // Horizontal guides
          for (step in 0..2) {
            val y = topPad + usableHeight * (step / 2f)
            drawLine(
              color = guideColor,
              start = Offset(0f, y),
              end = Offset(size.width, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = if (step < 2) dashEffect else null,
            )
          }

          buckets.forEachIndexed { idx, bucket ->
            val slotLeft = idx * slotWidth
            if (idx == safeSelectedIndex) {
              drawRoundRect(
                color = highlightBg,
                topLeft = Offset(slotLeft + 2.dp.toPx(), 2.dp.toPx()),
                size = Size((slotWidth - 4.dp.toPx()).coerceAtLeast(2f), size.height - 4.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
              )
            }

            val barGap = if (count > 8) 2.dp.toPx() else 4.dp.toPx()
            val barWidth = ((slotWidth * 0.64f) - barGap).coerceAtLeast(4.dp.toPx()) / 2f
            val groupWidth = barWidth * 2f + barGap
            val groupLeft = slotLeft + (slotWidth - groupWidth) / 2f

            // Spending Bar (Coral)
            val spendRatio = (bucket.spent.toFloat() / maxBarValue.toFloat()).coerceIn(0f, 1f) * animationProgress
            val spendHeight = (usableHeight * spendRatio).coerceAtLeast(if (bucket.spent > 0L) 4.dp.toPx() else 2.dp.toPx())
            drawRoundRect(
              color = if (idx == safeSelectedIndex) CoralDanger else CoralDanger.copy(alpha = 0.68f),
              topLeft = Offset(groupLeft, topPad + usableHeight - spendHeight),
              size = Size(barWidth, spendHeight),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )

            // Income Bar (Emerald)
            val incRatio = (bucket.income.toFloat() / maxBarValue.toFloat()).coerceIn(0f, 1f) * animationProgress
            val incHeight = (usableHeight * incRatio).coerceAtLeast(if (bucket.income > 0L) 4.dp.toPx() else 2.dp.toPx())
            drawRoundRect(
              color = if (idx == safeSelectedIndex) EmeraldSafe else EmeraldSafe.copy(alpha = 0.62f),
              topLeft = Offset(groupLeft + barWidth + barGap, topPad + usableHeight - incHeight),
              size = Size(barWidth, incHeight),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
        }

        // X-Axis Month Labels
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          buckets.forEachIndexed { idx, bucket ->
            val isSelected = idx == safeSelectedIndex
            Text(
              text = if (windowMonths > 8) bucket.shortLabel.take(1) else bucket.shortLabel,
              style = MaterialTheme.typography.labelSmall,
              fontSize = if (windowMonths > 8) 10.sp else 11.sp,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
              color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              maxLines = 1,
              modifier = Modifier
                .weight(1f)
                .clickable { selectedIndex = idx },
            )
          }
        }
      }

      // Focused Month Inspector Strip
      if (focusedBucket != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = focusedBucket.fullLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.trend_tx_count, focusedBucket.txCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = stringResource(R.string.trend_legend_spending),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = Formatters.formatMoneyCompactMasked(focusedBucket.spent, currency, locale, hideBalances),
                  style = MaterialTheme.typography.labelMedium,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = CoralDanger,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = stringResource(R.string.trend_legend_income),
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = Formatters.formatMoneyCompactMasked(focusedBucket.income, currency, locale, hideBalances),
                  style = MaterialTheme.typography.labelMedium,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldSafe,
                )
              }
            }
          }
        }
      }

      // Period / Annual Summary Grid (Total Spent, Total Income, Avg/Month)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TrendStatPill(
          label = stringResource(R.string.trend_total_spent),
          value = Formatters.formatMoneyCompactMasked(totalSpent, currency, locale, hideBalances),
          accentColor = CoralDanger,
          modifier = Modifier.weight(1f),
        )
        TrendStatPill(
          label = stringResource(R.string.trend_total_income),
          value = Formatters.formatMoneyCompactMasked(totalIncome, currency, locale, hideBalances),
          accentColor = EmeraldSafe,
          modifier = Modifier.weight(1f),
        )
        TrendStatPill(
          label = stringResource(R.string.trend_avg_monthly),
          value = Formatters.formatMoneyCompactMasked(avgMonthlySpent, currency, locale, hideBalances),
          accentColor = GoldAccent,
          modifier = Modifier.weight(1f),
        )
      }

      // Best / Highest Month Highlights
      if (lowestMonth != null && highestMonth != null && activeSpendingBuckets.size >= 2) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.TrendingDown,
              contentDescription = null,
              tint = EmeraldSafe,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(
                R.string.trend_lowest_month,
                lowestMonth.fullLabel,
                Formatters.formatMoneyCompactMasked(lowestMonth.spent, currency, locale, hideBalances),
              ),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.TrendingUp,
              contentDescription = null,
              tint = CoralDanger,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(
                R.string.trend_highest_month,
                highestMonth.fullLabel,
                Formatters.formatMoneyCompactMasked(highestMonth.spent, currency, locale, hideBalances),
              ),
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

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun TrendStatPill(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = accentColor.copy(alpha = 0.08f),
    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.22f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
