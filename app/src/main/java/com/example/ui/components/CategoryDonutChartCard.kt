package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.CategoryProgress
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class SpendingChartMode {
  DONUT,
  BUDGET_BAR,
}

/**
 * Interactive Visual Dashboard for Monthly Spending Distribution by Category & Budget Overview.
 * Combines an interactive Donut chart, a Budget vs. Spent grouped Bar chart, a 3-KPI budget strip,
 * live tap-to-inspect glassmorphism tooltips, full category legend, MoM trend badge, and top expense highlight.
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
  categoryBreakdown: List<CategoryProgress> = emptyList(),
  totalBudget: Long = 0L,
  onSetupBudgetClick: () -> Unit = {},
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val tactile = rememberTactileFeedback()

  val totalCurrent = remember(expenses) { expenses.sumOf { it.amount } }
  val prevMonthKey = remember(monthKey) { Formatters.shiftMonthKey(monthKey, -1L) }
  val totalPrev = remember(allHistoricalExpenses, prevMonthKey) {
    allHistoricalExpenses.filter { it.monthKey == prevMonthKey }.sumOf { it.amount }
  }

  val allocatedByCategory = remember(categoryBreakdown) {
    categoryBreakdown.associate { it.category to it.allocated }
  }

  val spendingSlices = remember(expenses, totalCurrent, allocatedByCategory) {
    if (totalCurrent <= 0L) {
      emptyList()
    } else {
      expenses.groupBy { it.categoryEnum }
        .map { (cat, list) ->
          val sum = list.sumOf { it.amount }
          CategoryDistributionSlice(
            category = cat,
            spent = sum,
            allocated = allocatedByCategory[cat] ?: 0L,
            fraction = (sum.toFloat() / totalCurrent.toFloat()).coerceIn(0f, 1f),
            transactionCount = list.size,
          )
        }
        .sortedByDescending { it.spent }
    }
  }

  val barChartSlices = remember(expenses, totalCurrent, allocatedByCategory, spendingSlices) {
    val txCounts = expenses.groupingBy { it.categoryEnum }.eachCount()
    val spentMap = expenses.groupBy { it.categoryEnum }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val allCats = (spentMap.keys + allocatedByCategory.filterValues { it > 0L }.keys).distinct()
    allCats.map { cat ->
      val spent = spentMap[cat] ?: 0L
      CategoryDistributionSlice(
        category = cat,
        spent = spent,
        allocated = allocatedByCategory[cat] ?: 0L,
        fraction = if (totalCurrent > 0L) (spent.toFloat() / totalCurrent.toFloat()).coerceIn(0f, 1f) else 0f,
        transactionCount = txCounts[cat] ?: 0,
      )
    }.sortedByDescending { maxOf(it.spent, it.allocated) }
  }

  val topExpense = remember(expenses) { expenses.maxByOrNull { it.amount } }

  var chartMode by remember { mutableStateOf(SpendingChartMode.DONUT) }
  var selectedCategory by remember(monthKey) { mutableStateOf<ExpenseCategory?>(null) }

  // Clear selection if selected category no longer exists in current month's slices
  val activeSlices = if (chartMode == SpendingChartMode.DONUT) spendingSlices else barChartSlices
  val selectedSlice = remember(selectedCategory, activeSlices) {
    activeSlices.firstOrNull { it.category == selectedCategory }
  }

  var playAnim by remember(monthKey, totalCurrent) { mutableStateOf(false) }
  LaunchedEffect(monthKey, totalCurrent) {
    playAnim = true
  }
  val sweepProgress by animateFloatAsState(
    targetValue = if (playAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
    label = "donut_sweep",
  )

  SculptedSurfaceCard(
    accentColor = selectedSlice?.let {
      if (it.isOverBudget) CoralDanger else it.category.accentColor
    } ?: MaterialTheme.colorScheme.secondary,
    cornerRadius = 24,
    modifier = modifier.testTag("dashboard_donut_analytics_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        LiquidGlassIconBadge(
          size = 38.dp,
          shape = RoundedCornerShape(12.dp),
          tintColor = MaterialTheme.colorScheme.primary,
        ) {
          Icon(
            imageVector = if (chartMode == SpendingChartMode.DONUT) {
              Icons.Rounded.DonutLarge
            } else {
              Icons.Rounded.BarChart
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.analytics_donut_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = Formatters.formatMonthTitle(monthKey, locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }

      // 1b. Frosted Glass Chart Mode Switcher (full-width capsule with 48dp touch targets)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) {
          Color.White.copy(alpha = 0.08f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        },
        border = BorderStroke(
          0.8.dp,
          if (isDarkTheme) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.78f),
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ChartModeSegmentChip(
            selected = chartMode == SpendingChartMode.DONUT,
            icon = Icons.Rounded.DonutLarge,
            label = stringResource(R.string.analytics_mode_donut),
            testTag = "chart_mode_donut_tab",
            modifier = Modifier.weight(1f),
            onClick = {
              tactile.tick()
              chartMode = SpendingChartMode.DONUT
            },
          )
          ChartModeSegmentChip(
            selected = chartMode == SpendingChartMode.BUDGET_BAR,
            icon = Icons.Rounded.BarChart,
            label = stringResource(R.string.analytics_mode_budget_bar),
            testTag = "chart_mode_budget_bar_tab",
            modifier = Modifier.weight(1f),
            onClick = {
              tactile.tick()
              chartMode = SpendingChartMode.BUDGET_BAR
            },
          )
        }
      }

      // 2. Budget Overview KPI Strip (Total Budget / Total Spent / Remaining or Over)
      BudgetOverviewKpiStrip(
        totalBudget = totalBudget,
        totalSpent = totalCurrent,
        currency = currency,
        locale = locale,
        hideBalances = hideBalances,
        onSetupBudgetClick = onSetupBudgetClick,
      )

      // 3. Month-over-Month Trend Badge
      MonthOverMonthTrendPill(
        totalCurrent = totalCurrent,
        totalPrev = totalPrev,
      )

      // 4. Interactive Chart Body (Donut Distribution OR Budget vs Spent Grouped Bars)
      if (barChartSlices.isEmpty()) {
        SpendingDashboardEmptyState(
          hasBudget = totalBudget > 0L,
          onSetupBudgetClick = onSetupBudgetClick,
        )
      } else {
        AnimatedContent(
          targetState = chartMode,
          transitionSpec = {
            fadeIn(tween(240)) togetherWith fadeOut(tween(180))
          },
          label = "spending_chart_mode_transition",
        ) { mode ->
          when (mode) {
            SpendingChartMode.DONUT -> {
              if (spendingSlices.isEmpty()) {
                SpendingDashboardEmptyState(
                  hasBudget = totalBudget > 0L,
                  onSetupBudgetClick = onSetupBudgetClick,
                )
              } else {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  InteractiveCategoryDonutChart(
                    slices = spendingSlices,
                    selectedCategory = selectedCategory,
                    totalSpent = totalCurrent,
                    sweepProgress = sweepProgress,
                    currency = currency,
                    locale = locale,
                    hideBalances = hideBalances,
                    onSelectCategory = { picked ->
                      tactile.tick()
                      selectedCategory = picked
                    },
                  )
                }
              }
            }

            SpendingChartMode.BUDGET_BAR -> {
              InteractiveBudgetBarChart(
                slices = barChartSlices,
                selectedCategory = selectedCategory,
                currency = currency,
                locale = locale,
                hideBalances = hideBalances,
                onSelectCategory = { picked ->
                  tactile.tick()
                  selectedCategory = picked
                },
              )
            }
          }
        }

        // 5. Recharts-Style Floating Glass Tooltip (or subtle tap hint when nothing is selected)
        AnimatedVisibility(
          visible = selectedSlice != null,
          enter = expandVertically(tween(220)) + fadeIn(tween(220)),
          exit = shrinkVertically(tween(180)) + fadeOut(tween(160)),
        ) {
          selectedSlice?.let { slice ->
            CategoryGlassTooltipCard(
              slice = slice,
              currency = currency,
              hideBalances = hideBalances,
              onClearSelection = {
                tactile.tick()
                selectedCategory = null
              },
            )
          }
        }

        if (selectedSlice == null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.TouchApp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = stringResource(R.string.analytics_tap_chart_hint),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // 6. Full Interactive Category Distribution Legend
        InteractiveCategoryLegendList(
          slices = activeSlices,
          selectedCategory = selectedCategory,
          currency = currency,
          locale = locale,
          hideBalances = hideBalances,
          onSelectCategory = { picked ->
            tactile.tick()
            selectedCategory = picked
          },
        )
      }

      // 7. Largest Single Expense Highlight
      if (topExpense != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme) 0.34f else 0.45f),
          border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.28f)),
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

@Composable
private fun ChartModeSegmentChip(
  selected: Boolean,
  icon: ImageVector,
  label: String,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val bg = if (selected) {
    if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.92f)
  } else {
    Color.Transparent
  }
  val contentColor = if (selected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  Row(
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(11.dp))
      .background(bg)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = contentColor,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun BudgetOverviewKpiStrip(
  totalBudget: Long,
  totalSpent: Long,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onSetupBudgetClick: () -> Unit,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val hasBudget = totalBudget > 0L
  val remaining = totalBudget - totalSpent
  val isOver = hasBudget && remaining < 0L
  val usedPct = if (hasBudget) ((totalSpent.toDouble() / totalBudget.toDouble()) * 100.0).roundToInt() else 0

  val kpiTileBg = if (isDarkTheme) {
    Color.White.copy(alpha = 0.06f)
  } else {
    Color.White.copy(alpha = 0.54f)
  }
  val kpiBorder = if (isDarkTheme) {
    Color.White.copy(alpha = 0.12f)
  } else {
    Color.White.copy(alpha = 0.78f)
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // KPI 1: Budget
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = kpiTileBg,
      border = BorderStroke(0.8.dp, kpiBorder),
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(14.dp))
        .clickable(enabled = !hasBudget, onClick = onSetupBudgetClick),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = stringResource(R.string.analytics_budget_kpi_budget),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Text(
          text = if (hasBudget) {
            Formatters.formatMoneyCompactMasked(totalBudget, currency, locale, hideBalances)
          } else {
            stringResource(R.string.analytics_budget_not_set)
          },
          style = MaterialTheme.typography.labelLarge,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = if (hasBudget) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // KPI 2: Spent + % used
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = kpiTileBg,
      border = BorderStroke(0.8.dp, kpiBorder),
      modifier = Modifier.weight(1f),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = if (hasBudget) {
            stringResource(R.string.analytics_budget_used_pct, usedPct)
          } else {
            stringResource(R.string.analytics_budget_kpi_spent)
          },
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (isOver) FontWeight.Bold else FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = Formatters.formatMoneyCompactMasked(totalSpent, currency, locale, hideBalances),
          style = MaterialTheme.typography.labelLarge,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // KPI 3: Remaining / Over Budget
    val thirdAccent = when {
      !hasBudget -> MaterialTheme.colorScheme.onSurfaceVariant
      isOver -> CoralDanger
      else -> EmeraldSafe
    }
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = if (hasBudget) thirdAccent.copy(alpha = if (isDarkTheme) 0.14f else 0.10f) else kpiTileBg,
      border = BorderStroke(0.8.dp, if (hasBudget) thirdAccent.copy(alpha = 0.28f) else kpiBorder),
      modifier = Modifier.weight(1f),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = stringResource(
            if (isOver) R.string.analytics_budget_kpi_over else R.string.analytics_budget_kpi_remaining,
          ),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = thirdAccent,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (hasBudget) {
            Formatters.formatMoneyCompactMasked(abs(remaining), currency, locale, hideBalances)
          } else {
            "—"
          },
          style = MaterialTheme.typography.labelLarge,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = thirdAccent,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun MonthOverMonthTrendPill(
  totalCurrent: Long,
  totalPrev: Long,
) {
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
}

@Composable
private fun SpendingDashboardEmptyState(
  hasBudget: Boolean,
  onSetupBudgetClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 18.dp, horizontal = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    LiquidGlassIconBadge(
      size = 54.dp,
      shape = RoundedCornerShape(18.dp),
      tintColor = MaterialTheme.colorScheme.primary,
    ) {
      Icon(
        imageVector = Icons.Rounded.DonutLarge,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
      )
    }
    Text(
      text = stringResource(R.string.analytics_empty_title),
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(R.string.analytics_empty_subtitle),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (!hasBudget) {
      Spacer(modifier = Modifier.height(4.dp))
      FilledTonalButton(
        onClick = onSetupBudgetClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("empty_analytics_setup_budget_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.analytics_setup_budget_cta),
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}
