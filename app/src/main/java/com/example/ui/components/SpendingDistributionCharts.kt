package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ExpenseCategory
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Aggregated category metric combining actual spending, share of monthly total,
 * transaction count, and optional monthly budget allocation.
 */
internal data class CategoryDistributionSlice(
  val category: ExpenseCategory,
  val spent: Long,
  val allocated: Long,
  val fraction: Float,
  val transactionCount: Int,
) {
  val remaining: Long get() = allocated - spent
  val isOverBudget: Boolean get() = allocated > 0L && spent > allocated
  val budgetUsagePercent: Int
    get() = if (allocated > 0L) ((spent.toDouble() / allocated.toDouble()) * 100.0).roundToInt() else 0
}

/**
 * Interactive Donut Chart with radial tap hit-testing, animated sweep-in,
 * active-slice radial pop-out expansion with glowing halo, and live center readout.
 */
@Composable
internal fun InteractiveCategoryDonutChart(
  slices: List<CategoryDistributionSlice>,
  selectedCategory: ExpenseCategory?,
  totalSpent: Long,
  sweepProgress: Float,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onSelectCategory: (ExpenseCategory?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val trackColor = if (isDarkTheme) {
    Color.White.copy(alpha = 0.08f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
  }
  val selectedSlice = slices.firstOrNull { it.category == selectedCategory }

  val activeExpansion by animateFloatAsState(
    targetValue = if (selectedSlice != null) 1f else 0f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.78f),
    label = "donut_active_expansion",
  )

  Box(
    modifier = modifier
      .size(182.dp)
      .testTag("spending_donut_canvas_container"),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .size(176.dp)
        .pointerInput(slices, selectedCategory) {
          detectTapGestures { tapOffset ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val dx = tapOffset.x - cx
            val dy = tapOffset.y - cy
            val radius = hypot(dx, dy)
            val maxR = minOf(size.width, size.height).toFloat() / 2f
            val innerR = maxR * 0.42f

            if (radius < innerR) {
              // Tapping center resets selection
              onSelectCategory(null)
              return@detectTapGestures
            }
            if (radius <= maxR && slices.isNotEmpty()) {
              var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
              // Align 0 degrees to top (-90 deg in Canvas coordinates)
              angleDeg = (angleDeg + 90f + 360f) % 360f
              var cumulative = 0f
              var hit: ExpenseCategory? = null
              for (slice in slices) {
                val span = slice.fraction * 360f
                if (angleDeg >= cumulative && angleDeg <= cumulative + span) {
                  hit = slice.category
                  break
                }
                cumulative += span
              }
              if (hit != null) {
                onSelectCategory(if (selectedCategory == hit) null else hit)
              }
            }
          }
        },
    ) {
      val baseStroke = 22.dp.toPx()
      val activeExtraStroke = 6.dp.toPx() * activeExpansion
      val outerPadding = 12.dp.toPx()
      val diameter = (size.minDimension - baseStroke - outerPadding * 2f).coerceAtLeast(1f)
      val baseTopLeft = Offset(
        x = (size.width - diameter) / 2f,
        y = (size.height - diameter) / 2f,
      )
      val baseArcSize = Size(diameter, diameter)

      // Frosted background track ring
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = baseTopLeft,
        size = baseArcSize,
        style = Stroke(width = baseStroke),
      )

      var startAngle = -90f
      val gapDegrees = if (slices.size > 1) 2.6f else 0f
      slices.forEach { slice ->
        val rawSweep = slice.fraction * 360f * sweepProgress
        val visibleSweep = (rawSweep - gapDegrees).coerceAtLeast(1.8f)
        val isSelected = slice.category == selectedCategory
        val hasSelection = selectedCategory != null

        val sliceColor = if (slice.isOverBudget) CoralDanger else slice.category.accentColor
        val alpha = when {
          !hasSelection -> 0.96f
          isSelected -> 1f
          else -> 0.34f
        }

        if (isSelected && activeExpansion > 0.05f) {
          // Outer translucent glow halo behind expanded slice
          val haloDiameter = diameter + 8.dp.toPx() * activeExpansion
          val haloTopLeft = Offset(
            x = (size.width - haloDiameter) / 2f,
            y = (size.height - haloDiameter) / 2f,
          )
          drawArc(
            color = sliceColor.copy(alpha = 0.24f * activeExpansion),
            startAngle = startAngle - 1f,
            sweepAngle = visibleSweep + 2f,
            useCenter = false,
            topLeft = haloTopLeft,
            size = Size(haloDiameter, haloDiameter),
            style = Stroke(width = baseStroke + activeExtraStroke + 8.dp.toPx(), cap = StrokeCap.Butt),
          )
        }

        val currentStroke = if (isSelected) baseStroke + activeExtraStroke else baseStroke
        val expandOffset = if (isSelected) 3.dp.toPx() * activeExpansion else 0f
        val sliceDiameter = diameter + expandOffset * 2f
        val sliceTopLeft = Offset(
          x = (size.width - sliceDiameter) / 2f,
          y = (size.height - sliceDiameter) / 2f,
        )

        drawArc(
          color = sliceColor.copy(alpha = alpha),
          startAngle = startAngle,
          sweepAngle = visibleSweep,
          useCenter = false,
          topLeft = sliceTopLeft,
          size = Size(sliceDiameter, sliceDiameter),
          style = Stroke(width = currentStroke, cap = StrokeCap.Butt),
        )
        startAngle += rawSweep
      }
    }

    // Center Live Readout (Total Spent or Selected Category)
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 24.dp),
    ) {
      if (selectedSlice != null) {
        val sharePct = (selectedSlice.fraction * 100f).roundToInt()
        Text(
          text = stringResource(selectedSlice.category.shortLabelRes),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (selectedSlice.isOverBudget) CoralDanger else selectedSlice.category.accentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$sharePct%",
          style = MaterialTheme.typography.titleLarge,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = Formatters.formatMoneyCompactMasked(
            amount = selectedSlice.spent,
            currency = currency,
            locale = locale,
            hideBalances = hideBalances,
          ),
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMonoFontFamily,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      } else {
        Text(
          text = stringResource(R.string.analytics_donut_total_label),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = Formatters.formatMoneyCompactMasked(
            amount = totalSpent,
            currency = currency,
            locale = locale,
            hideBalances = hideBalances,
          ),
          style = MaterialTheme.typography.titleMedium,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
        )
        Text(
          text = "${slices.size} cat",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
      }
    }
  }
}

/**
 * Interactive Grouped Bar Chart comparing Allocated Budget vs Actual Spent per category
 * with dashed reference gridlines, compact Y-axis labels, and tap-to-inspect cursor column.
 */
@Composable
internal fun InteractiveBudgetBarChart(
  slices: List<CategoryDistributionSlice>,
  selectedCategory: ExpenseCategory?,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onSelectCategory: (ExpenseCategory?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val chartItems = slices.take(8)
  val maxValue = (chartItems.maxOfOrNull { maxOf(it.spent, it.allocated) } ?: 1L).coerceAtLeast(1L)

  val animProgress by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    label = "bar_chart_grow",
  )

  val gridColor = if (isDarkTheme) {
    Color.White.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  }
  val budgetBarColor = if (isDarkTheme) {
    Color.White.copy(alpha = 0.24f)
  } else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
  }
  val cursorColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.14f else 0.09f)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("spending_budget_bar_chart"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Mini Legend for Grouped Bars
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ChartBarLegendDot(
        color = budgetBarColor,
        label = stringResource(R.string.analytics_legend_allocated),
      )
      ChartBarLegendDot(
        color = MaterialTheme.colorScheme.primary,
        label = stringResource(R.string.analytics_legend_spent),
      )
      ChartBarLegendDot(
        color = CoralDanger,
        label = stringResource(R.string.analytics_legend_over),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Compact Y-Axis Tick Column
      Column(
        modifier = Modifier
          .height(148.dp)
          .padding(bottom = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        Text(
          text = Formatters.formatMoneyCompactMasked(maxValue, currency, locale, hideBalances),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          fontFamily = JetBrainsMonoFontFamily,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Text(
          text = Formatters.formatMoneyCompactMasked(maxValue / 2L, currency, locale, hideBalances),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          fontFamily = JetBrainsMonoFontFamily,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Text(
          text = "0",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          fontFamily = JetBrainsMonoFontFamily,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Scrollable / Responsive Bar Columns
      val minColWidth = 54.dp
      Row(
        modifier = Modifier
          .weight(1f)
          .horizontalScroll(rememberScrollState()),
      ) {
        Column {
          val totalWidth = (minColWidth * chartItems.size.coerceAtLeast(1))
          Canvas(
            modifier = Modifier
              .width(totalWidth)
              .height(148.dp)
              .pointerInput(chartItems, selectedCategory) {
                detectTapGestures { tapOffset ->
                  if (chartItems.isEmpty()) return@detectTapGestures
                  val colW = size.width / chartItems.size.toFloat()
                  val idx = (tapOffset.x / colW).toInt().coerceIn(0, chartItems.lastIndex)
                  val tappedCat = chartItems[idx].category
                  onSelectCategory(if (selectedCategory == tappedCat) null else tappedCat)
                }
              },
          ) {
            val topPad = 6.dp.toPx()
            val bottomPad = 4.dp.toPx()
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // 3 Horizontal Dashed Gridlines (0%, 50%, 100%)
            listOf(0f, 0.5f, 1f).forEach { level ->
              val y = topPad + chartH * level
              drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (level < 1f) dashEffect else null,
              )
            }

            if (chartItems.isEmpty()) return@Canvas
            val colW = size.width / chartItems.size.toFloat()
            val barW = (colW * 0.27f).coerceIn(10.dp.toPx(), 16.dp.toPx())
            val pairGap = 4.dp.toPx()
            val cornerR = CornerRadius(5.dp.toPx(), 5.dp.toPx())

            chartItems.forEachIndexed { index, slice ->
              val colStart = index * colW
              val colCenter = colStart + colW / 2f
              val isSelected = selectedCategory == slice.category
              val hasSelection = selectedCategory != null

              if (isSelected) {
                drawRoundRect(
                  color = cursorColor,
                  topLeft = Offset(colStart + 3.dp.toPx(), 0f),
                  size = Size(colW - 6.dp.toPx(), size.height),
                  cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                )
              }

              val alphaScale = if (!hasSelection || isSelected) 1f else 0.36f
              val baseY = topPad + chartH

              // Left Bar: Allocated Budget
              val budgetH = if (slice.allocated > 0L) {
                ((slice.allocated.toFloat() / maxValue.toFloat()) * chartH * animProgress)
                  .coerceAtLeast(4.dp.toPx())
              } else {
                0f
              }
              if (budgetH > 0f) {
                drawRoundRect(
                  color = budgetBarColor.copy(alpha = budgetBarColor.alpha * alphaScale),
                  topLeft = Offset(colCenter - pairGap / 2f - barW, baseY - budgetH),
                  size = Size(barW, budgetH),
                  cornerRadius = cornerR,
                )
              }

              // Right Bar: Actual Spent (centered if no budget set)
              val spentColor = if (slice.isOverBudget) CoralDanger else slice.category.accentColor
              val spentH = if (slice.spent > 0L) {
                ((slice.spent.toFloat() / maxValue.toFloat()) * chartH * animProgress)
                  .coerceAtLeast(4.dp.toPx())
              } else {
                0f
              }
              if (spentH > 0f) {
                val spentX = if (budgetH > 0f) colCenter + pairGap / 2f else colCenter - barW / 2f
                drawRoundRect(
                  brush = Brush.verticalGradient(
                    colors = listOf(
                      spentColor.copy(alpha = alphaScale),
                      spentColor.copy(alpha = 0.72f * alphaScale),
                    ),
                    startY = baseY - spentH,
                    endY = baseY,
                  ),
                  topLeft = Offset(spentX, baseY - spentH),
                  size = Size(barW, spentH),
                  cornerRadius = cornerR,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // X-Axis Category Labels (tappable)
          Row(modifier = Modifier.width(totalWidth)) {
            chartItems.forEach { slice ->
              val isSelected = selectedCategory == slice.category
              Column(
                modifier = Modifier
                  .width(minColWidth)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    onSelectCategory(if (isSelected) null else slice.category)
                  }
                  .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .background(
                      if (slice.isOverBudget) CoralDanger else slice.category.accentColor,
                      CircleShape,
                    ),
                )
                Text(
                  text = stringResource(slice.category.shortLabelRes),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                  color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChartBarLegendDot(
  color: Color,
  label: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Box(
      modifier = Modifier
        .size(9.dp)
        .background(color, RoundedCornerShape(3.dp)),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * Recharts-inspired frosted glass Tooltip Card showing full budget & spending metrics
 * for the currently tapped category slice or bar.
 */
@Composable
internal fun CategoryGlassTooltipCard(
  slice: CategoryDistributionSlice,
  currency: AppCurrency,
  hideBalances: Boolean,
  onClearSelection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val accent = if (slice.isOverBudget) CoralDanger else slice.category.accentColor
  val sharePct = (slice.fraction * 100f).roundToInt()

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isDarkTheme) {
      Color(0xFF131D30).copy(alpha = 0.88f)
    } else {
      Color.White.copy(alpha = 0.84f)
    },
    border = BorderStroke(1.2.dp, accent.copy(alpha = if (isDarkTheme) 0.48f else 0.36f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("spending_category_tooltip_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CategoryIconBadge(category = slice.category, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(slice.category.labelRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${stringResource(R.string.analytics_tooltip_share, sharePct)} • ${
              stringResource(R.string.analytics_tooltip_tx_count, slice.transactionCount)
            }",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onClearSelection,
          modifier = Modifier
            .size(48.dp)
            .testTag("clear_category_tooltip_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.analytics_clear_selection_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      // 3-Column Spent / Budget / Remaining Breakdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TooltipMetricPill(
          label = stringResource(R.string.analytics_budget_kpi_spent),
          value = Formatters.formatMoneyMasked(slice.spent, currency, hideBalances),
          valueColor = accent,
          modifier = Modifier.weight(1f),
        )
        TooltipMetricPill(
          label = stringResource(R.string.analytics_budget_kpi_budget),
          value = if (slice.allocated > 0L) {
            Formatters.formatMoneyMasked(slice.allocated, currency, hideBalances)
          } else {
            stringResource(R.string.analytics_tooltip_no_budget)
          },
          valueColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        if (slice.allocated > 0L) {
          val isOver = slice.remaining < 0L
          TooltipMetricPill(
            label = stringResource(
              if (isOver) R.string.analytics_budget_kpi_over else R.string.analytics_budget_kpi_remaining,
            ),
            value = Formatters.formatMoneyMasked(kotlin.math.abs(slice.remaining), currency, hideBalances),
            valueColor = if (isOver) CoralDanger else EmeraldSafe,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Budget Usage Progress Bar when a limit exists
      if (slice.allocated > 0L) {
        val ratio = (slice.spent.toFloat() / slice.allocated.toFloat()).coerceIn(0f, 1f)
        val statusColor = when {
          slice.isOverBudget -> CoralDanger
          slice.budgetUsagePercent >= 80 -> GoldAccent
          else -> EmeraldSafe
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.analytics_tooltip_budget_usage, slice.budgetUsagePercent),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = statusColor,
            )
            Text(
              text = "$sharePct%",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = JetBrainsMonoFontFamily,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          LinearProgressIndicator(
            progress = { ratio },
            color = statusColor,
            trackColor = statusColor.copy(alpha = 0.16f),
            strokeCap = StrokeCap.Round,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(50)),
          )
        }
      }
    }
  }
}

@Composable
private fun TooltipMetricPill(
  label: String,
  value: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelMedium,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      color = valueColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Interactive Category Distribution Legend displaying all active categories
 * with proportional progress bars, exact amounts, share percentages, and tap selection.
 */
@Composable
internal fun InteractiveCategoryLegendList(
  slices: List<CategoryDistributionSlice>,
  selectedCategory: ExpenseCategory?,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onSelectCategory: (ExpenseCategory?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    slices.forEach { slice ->
      val isSelected = selectedCategory == slice.category
      val accent = if (slice.isOverBudget) CoralDanger else slice.category.accentColor
      val pct = (slice.fraction * 100f).roundToInt()

      val rowBg by animateColorAsState(
        targetValue = if (isSelected) {
          accent.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
        } else {
          Color.Transparent
        },
        animationSpec = tween(220),
        label = "legend_row_bg",
      )

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = rowBg,
        border = if (isSelected) BorderStroke(1.dp, accent.copy(alpha = 0.42f)) else null,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { onSelectCategory(if (isSelected) null else slice.category) }
          .testTag("spending_legend_row_${slice.category.name}"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(accent, CircleShape),
            )
            Text(
              text = stringResource(slice.category.shortLabelRes),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = Formatters.formatMoneyCompactMasked(slice.spent, currency, locale, hideBalances),
              style = MaterialTheme.typography.labelMedium,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = accent.copy(alpha = 0.14f),
            ) {
              Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }

          // Proportional Share Bar
          LinearProgressIndicator(
            progress = { slice.fraction.coerceIn(0f, 1f) },
            color = accent,
            trackColor = accent.copy(alpha = 0.12f),
            strokeCap = StrokeCap.Round,
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(50)),
          )
        }
      }
    }
  }
}
