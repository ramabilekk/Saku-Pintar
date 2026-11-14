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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.ui.theme.CoralDanger
import com.example.util.Formatters
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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
    shape = RoundedCornerShape(12.dp),
    color = category.accentColor.copy(alpha = 0.14f),
    modifier = modifier.size(size),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = category.iconVector(),
        contentDescription = category.labelId,
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
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
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
          contentDescription = "Bulan sebelumnya",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = Formatters.formatMonthTitle(monthKey),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 6.dp),
      )
      IconButton(
        onClick = onNextMonth,
        modifier = Modifier
          .size(48.dp)
          .testTag("next_month_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = "Bulan berikutnya",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun SevenDaySpendingChart(
  expenses: List<Expense>,
  dailySafeLimit: Long,
  referenceDate: LocalDate = LocalDate.now(),
  modifier: Modifier = Modifier,
) {
  val localeId = Locale.forLanguageTag("id-ID")
  val days = (6 downTo 0).map { offset -> referenceDate.minusDays(offset.toLong()) }
  val totalsByDay = days.map { day ->
    val epoch = day.toEpochDay()
    expenses.filter { it.dateEpochDay == epoch }.sumOf { it.amount }
  }
  val maxAmount = maxOf(
    totalsByDay.maxOrNull() ?: 0L,
    dailySafeLimit,
    100_000L,
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
        text = "Pengeluaran 7 Hari Terakhir",
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
            text = "Batas ${Formatters.formatRupiahCompact(dailySafeLimit)}",
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
        // Background track bar
        drawRoundRect(
          color = trackColor,
          topLeft = Offset(left, 4.dp.toPx()),
          size = Size(barWidth, chartHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )

        if (amount > 0L) {
          val ratio = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0.05f, 1f)
          val barH = chartHeight * ratio
          val barColor = if (dailySafeLimit > 0L && amount > dailySafeLimit) CoralDanger else primaryColor
          drawRoundRect(
            color = barColor,
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

    Row(modifier = Modifier.fillMaxWidth()) {
      days.forEachIndexed { idx, day ->
        val isToday = idx == days.lastIndex
        Text(
          text = if (isToday) "Hari ini" else day.dayOfWeek.getDisplayName(TextStyle.SHORT, localeId),
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
