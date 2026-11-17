package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.theme.CoralDanger
import com.example.util.Formatters
import java.time.LocalDate
import java.time.format.TextStyle

fun ExpenseCategory.iconVector(): ImageVector = when (this) {
  ExpenseCategory.FOOD -> Icons.Rounded.Restaurant
  ExpenseCategory.BILLS -> Icons.Rounded.Home
  ExpenseCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
  ExpenseCategory.HEALTH -> Icons.Rounded.Favorite
  ExpenseCategory.SHOPPING -> Icons.Rounded.ShoppingBag
  ExpenseCategory.ENTERTAINMENT -> Icons.Rounded.Movie
  ExpenseCategory.OTHER -> Icons.Rounded.Category
}

fun PaymentMethod.iconVector(): ImageVector = when (this) {
  PaymentMethod.QRIS -> Icons.Rounded.QrCode2
  PaymentMethod.CASH -> Icons.Rounded.Payments
  PaymentMethod.TRANSFER -> Icons.Rounded.AccountBalance
  PaymentMethod.CARD -> Icons.Rounded.CreditCard
}

@Composable
fun CategoryIconBadge(
  category: ExpenseCategory,
  size: Dp = 42.dp,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = category.accentColor.copy(alpha = 0.15f),
    border = BorderStroke(1.dp, category.accentColor.copy(alpha = 0.30f)),
    modifier = modifier.size(size),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = category.iconVector(),
        contentDescription = stringResource(category.labelRes),
        tint = category.accentColor,
        modifier = Modifier.size(size * 0.52f),
      )
    }
  }
}

@Composable
fun MonthSelectorBar(
  monthKey: String,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSelectMonth: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLang by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val locale = activeLang.toLocale()
  var showMonthPicker by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(50),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
      IconButton(
        onClick = onPreviousMonth,
        modifier = Modifier
          .size(48.dp)
          .testTag("prev_month_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
          contentDescription = stringResource(R.string.prev_month_cd),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
          .heightIn(min = 40.dp)
          .clip(RoundedCornerShape(50))
          .then(
            if (onSelectMonth != null) {
              Modifier
                .clickable { showMonthPicker = true }
                .testTag("month_picker_button")
            } else {
              Modifier
            }
          )
          .padding(horizontal = 8.dp),
      ) {
        if (onSelectMonth != null) {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = stringResource(R.string.month_picker_title),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
        }
        Text(
          text = Formatters.formatMonthTitle(monthKey, locale),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      IconButton(
        onClick = onNextMonth,
        modifier = Modifier
          .size(48.dp)
          .testTag("next_month_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = stringResource(R.string.next_month_cd),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }

  if (showMonthPicker && onSelectMonth != null) {
    MonthYearPickerDialog(
      currentMonthKey = monthKey,
      locale = locale,
      onSelectMonth = onSelectMonth,
      onDismiss = { showMonthPicker = false },
    )
  }
}

@Composable
fun SevenDaySpendingChart(
  expenses: List<Expense>,
  dailySafeLimit: Long,
  referenceDate: LocalDate = LocalDate.now(),
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLang by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLocale = activeLang.toLocale()

  val days = (6 downTo 0).map { offset -> referenceDate.minusDays(offset.toLong()) }
  val totalsByDay = days.map { day ->
    val epoch = day.toEpochDay()
    expenses.filter { it.dateEpochDay == epoch }.sumOf { it.amount }
  }
  val maxAmount = maxOf(
    totalsByDay.maxOrNull() ?: 0L,
    dailySafeLimit,
    activeCurrency.chartMinMax,
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
  val targetLineColor = MaterialTheme.colorScheme.tertiary

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.chart_seven_days_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (dailySafeLimit > 0L) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(targetLineColor, CircleShape)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(
              R.string.chart_limit_badge,
              Formatters.formatMoneyCompact(dailySafeLimit, activeCurrency, activeLocale),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(128.dp),
    ) {
      val barCount = days.size
      val slotWidth = size.width / barCount
      val barWidth = (slotWidth * 0.44f).coerceAtMost(28.dp.toPx())
      val chartHeight = size.height - 8.dp.toPx()

      totalsByDay.forEachIndexed { idx, amount ->
        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
        drawRoundRect(
          color = trackColor,
          topLeft = Offset(left, 4.dp.toPx()),
          size = Size(barWidth, chartHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )

        if (amount > 0L) {
          val ratio = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0.05f, 1f)
          val barH = chartHeight * ratio
          val isOver = dailySafeLimit > 0L && amount > dailySafeLimit
          val topColor = if (isOver) CoralDanger else primaryColor
          val bottomColor = if (isOver) CoralDanger.copy(alpha = 0.65f) else primaryColor.copy(alpha = 0.65f)
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(topColor, bottomColor),
              startY = 4.dp.toPx() + (chartHeight - barH),
              endY = 4.dp.toPx() + chartHeight,
            ),
            topLeft = Offset(left, 4.dp.toPx() + (chartHeight - barH)),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
          )
        }
      }

      if (dailySafeLimit > 0L) {
        val targetRatio = (dailySafeLimit.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
        val lineY = 4.dp.toPx() + chartHeight * (1f - targetRatio)
        drawLine(
          color = targetLineColor,
          start = Offset(0f, lineY),
          end = Offset(size.width, lineY),
          strokeWidth = 2.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    val todayLabel = stringResource(R.string.label_today)
    Row(modifier = Modifier.fillMaxWidth()) {
      days.forEachIndexed { idx, day ->
        val isToday = idx == days.lastIndex
        Text(
          text = if (isToday) todayLabel else day.dayOfWeek.getDisplayName(TextStyle.SHORT, activeLocale),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
          color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
