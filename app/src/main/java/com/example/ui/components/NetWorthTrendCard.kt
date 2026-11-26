package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.NetWorthSnapshot
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyPositiveAccent
import com.example.util.Formatters
import kotlin.math.abs

/**
 * Liquid Glass Net Worth Tracker card with live Assets vs. Liabilities breakdown
 * and an interactive monthly area/line trend chart.
 */
@Composable
fun NetWorthTrendCard(
  totalAssets: Long,
  totalLiabilities: Long,
  totalReceivables: Long,
  snapshots: List<NetWorthSnapshot>,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeLocale = activeLanguage.toLocale()
  val isDark = rememberIsEffectiveDarkTheme()

  val currentMonthKey = remember { Formatters.currentMonthKey() }
  val liveNetWorth = totalAssets - totalLiabilities
  val chartPoints = remember(snapshots, totalAssets, totalLiabilities, currentMonthKey) {
    val liveSnapshot = NetWorthSnapshot(
      monthKey = currentMonthKey,
      totalAssets = totalAssets.coerceAtLeast(0L),
      totalLiabilities = totalLiabilities.coerceAtLeast(0L),
      netWorth = liveNetWorth,
    )
    (snapshots.filterNot { it.monthKey == currentMonthKey } + liveSnapshot)
      .sortedBy { it.monthKey }
      .takeLast(6)
  }

  var selectedIndex by remember(chartPoints.size) {
    mutableIntStateOf((chartPoints.size - 1).coerceAtLeast(0))
  }
  val safeIndex = selectedIndex.coerceIn(0, (chartPoints.size - 1).coerceAtLeast(0))
  val inspected = chartPoints.getOrNull(safeIndex)
  val prevPoint = chartPoints.getOrNull(chartPoints.size - 2)
  val monthDelta = if (prevPoint != null) liveNetWorth - prevPoint.netWorth else null
  val netWorthAccent = if (liveNetWorth >= 0L) EmeraldSafe else CoralDanger

  SculptedSurfaceCard(
    cornerRadius = 24,
    accentColor = netWorthAccent,
    modifier = modifier
      .fillMaxWidth()
      .testTag("net_worth_trend_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = netWorthAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
            border = BorderStroke(1.dp, netWorthAccent.copy(alpha = 0.32f)),
            modifier = Modifier.size(38.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AccountBalance,
              contentDescription = null,
              tint = netWorthAccent,
              modifier = Modifier.padding(8.dp),
            )
          }
          Column {
            Text(
              text = stringResource(R.string.net_worth_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.net_worth_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (monthDelta != null) {
          val isUp = monthDelta >= 0L
          val badgeColor = if (isUp) EmeraldSafe else CoralDanger
          Surface(
            shape = RoundedCornerShape(50),
            color = badgeColor.copy(alpha = if (isDark) 0.22f else 0.12f),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = if (isUp) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = stringResource(
                  if (isUp) R.string.net_worth_delta_up else R.string.net_worth_delta_down,
                  Formatters.formatMoneyCompact(abs(monthDelta), activeCurrency, activeLocale),
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
              )
            }
          }
        }
      }

      // Live Hero Net Worth Figure
      Text(
        text = if (liveNetWorth < 0L) {
          "-${Formatters.formatMoney(abs(liveNetWorth), activeCurrency)}"
        } else {
          Formatters.formatMoney(liveNetWorth, activeCurrency)
        },
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        color = netWorthAccent,
      )

      // Assets & Liabilities Breakdown Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        NetWorthMetricPill(
          label = stringResource(R.string.net_worth_assets_label),
          value = Formatters.formatMoney(totalAssets, activeCurrency),
          color = EmeraldSafe,
          modifier = Modifier.weight(1f),
        )
        NetWorthMetricPill(
          label = stringResource(R.string.net_worth_liabilities_label),
          value = Formatters.formatMoney(totalLiabilities, activeCurrency),
          color = CoralDanger,
          modifier = Modifier.weight(1f),
        )
      }

      if (totalReceivables > 0L) {
        Surface(
          shape = RoundedCornerShape(50),
          color = SkyPositiveAccent.copy(alpha = if (isDark) 0.16f else 0.10f),
          border = BorderStroke(1.dp, SkyPositiveAccent.copy(alpha = 0.28f)),
        ) {
          Text(
            text = stringResource(
              R.string.net_worth_receivables_pill,
              Formatters.formatMoney(totalReceivables, activeCurrency),
            ),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = SkyPositiveAccent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      // Interactive Monthly Area / Line Chart
      if (chartPoints.size >= 2) {
        val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        val dotFillColor = MaterialTheme.colorScheme.surface
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .pointerInput(chartPoints) {
              detectTapGestures { offset ->
                if (chartPoints.size <= 1) return@detectTapGestures
                val padX = 24f
                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                val stepX = usableW / (chartPoints.size - 1)
                val idx = ((offset.x - padX) / stepX).toInt()
                  .coerceIn(0, chartPoints.size - 1)
                selectedIndex = idx
              }
            }
            .testTag("net_worth_chart_canvas"),
        ) {
          val padX = 14.dp.toPx()
          val padTop = 14.dp.toPx()
          val padBottom = 14.dp.toPx()
          val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
          val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)

          val values = chartPoints.map { it.netWorth.toFloat() }
          val minV = values.minOrNull() ?: 0f
          val maxV = values.maxOrNull() ?: 1f
          val span = (maxV - minV).takeIf { it > 1f } ?: (abs(maxV).coerceAtLeast(100_000f))

          // Subtle horizontal reference grid lines
          repeat(3) { row ->
            val y = padTop + usableH * (row / 2f)
            drawLine(
              color = gridLineColor,
              start = Offset(padX, y),
              end = Offset(size.width - padX, y),
              strokeWidth = 1.dp.toPx(),
            )
          }

          val stepX = usableW / (chartPoints.size - 1).coerceAtLeast(1)
          val coords = chartPoints.mapIndexed { idx, snap ->
            val x = padX + idx * stepX
            val normalizedY = if (maxV == minV) 0.5f else (snap.netWorth.toFloat() - minV) / span
            val y = padTop + usableH * (1f - normalizedY.coerceIn(0f, 1f))
            Offset(x, y)
          }

          val linePath = Path().apply {
            coords.forEachIndexed { idx, pt ->
              if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
          }
          val areaPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, size.height - padBottom)
            lineTo(coords.first().x, size.height - padBottom)
            close()
          }

          drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                netWorthAccent.copy(alpha = if (isDark) 0.34f else 0.24f),
                netWorthAccent.copy(alpha = 0.02f),
              ),
              startY = padTop,
              endY = size.height - padBottom,
            ),
          )
          drawPath(
            path = linePath,
            color = netWorthAccent,
            style = Stroke(
              width = 2.8.dp.toPx(),
              cap = StrokeCap.Round,
              join = StrokeJoin.Round,
            ),
          )

          coords.forEachIndexed { idx, pt ->
            val isSelected = idx == safeIndex
            if (isSelected) {
              drawCircle(
                color = netWorthAccent.copy(alpha = 0.24f),
                radius = 10.dp.toPx(),
                center = pt,
              )
            }
            drawCircle(
              color = netWorthAccent,
              radius = if (isSelected) 5.5.dp.toPx() else 3.8.dp.toPx(),
              center = pt,
            )
            drawCircle(
              color = dotFillColor,
              radius = if (isSelected) 2.5.dp.toPx() else 1.8.dp.toPx(),
              center = pt,
            )
          }
        }

        // X-axis Month Labels
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          chartPoints.forEachIndexed { idx, snap ->
            val isSelected = idx == safeIndex
            Text(
              text = Formatters.formatMonthShort(snap.monthKey, activeLocale),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) netWorthAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Selected Month Inspector Pill
        inspected?.let { snap ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (isDark) 0.55f else 0.78f),
            border = BorderStroke(1.dp, netWorthAccent.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = Formatters.formatMonthTitle(snap.monthKey, activeLocale),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = if (snap.netWorth < 0L) {
                  "-${Formatters.formatMoney(abs(snap.netWorth), activeCurrency)}"
                } else {
                  Formatters.formatMoney(snap.netWorth, activeCurrency)
                },
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (snap.netWorth >= 0L) EmeraldSafe else CoralDanger,
              )
            }
          }
        }
      } else {
        Text(
          text = stringResource(R.string.net_worth_single_month_hint),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun NetWorthMetricPill(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  val isDark = rememberIsEffectiveDarkTheme()
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = color.copy(alpha = if (isDark) 0.14f else 0.08f),
    border = BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.30f else 0.22f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
