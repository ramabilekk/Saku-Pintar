package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Expense
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthYearPickerDialog(
  currentMonthKey: String,
  locale: Locale,
  onSelectMonth: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val tactile = rememberTactileFeedback()
  val parsedYm = remember(currentMonthKey) {
    runCatching { YearMonth.parse(currentMonthKey) }.getOrElse { YearMonth.now() }
  }
  val nowYm = remember { YearMonth.now() }
  var pickerYear by remember(parsedYm) { mutableIntStateOf(parsedYm.year) }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.month_picker_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Year Selector Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = {
              tactile.tick()
              pickerYear -= 1
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("picker_prev_year"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = stringResource(R.string.prev_month_cd),
            )
          }
          Text(
            text = pickerYear.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("picker_year_label"),
          )
          IconButton(
            onClick = {
              tactile.tick()
              pickerYear += 1
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("picker_next_year"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = stringResource(R.string.next_month_cd),
            )
          }
        }

        // 3x4 Month Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          for (rowIdx in 0 until 4) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              for (colIdx in 0 until 3) {
                val monthNumber = rowIdx * 3 + colIdx + 1
                val cellYm = YearMonth.of(pickerYear, monthNumber)
                val isSelected = cellYm == parsedYm
                val isCurrentRealMonth = cellYm == nowYm
                val monthLabel = Month.of(monthNumber)
                  .getDisplayName(TextStyle.SHORT, locale)
                  .replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
                  }

                Surface(
                  onClick = {
                    tactile.tick()
                    onSelectMonth(cellYm.toString())
                    onDismiss()
                  },
                  shape = RoundedCornerShape(12.dp),
                  color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isCurrentRealMonth -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                  },
                  border = if (isCurrentRealMonth && !isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                  } else {
                    BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                  },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("month_grid_item_$monthNumber"),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = monthLabel,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected || isCurrentRealMonth) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                      } else {
                        MaterialTheme.colorScheme.onSurface
                      },
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          tactile.tick()
          onSelectMonth(nowYm.toString())
          onDismiss()
        },
        modifier = Modifier.testTag("month_picker_this_month_button"),
      ) {
        Text(stringResource(R.string.month_picker_this_month))
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          tactile.tick()
          onDismiss()
        },
      ) {
        Text(stringResource(R.string.action_close))
      }
    },
  )
}

@Composable
fun MonthlyCalendarCard(
  monthKey: String,
  expenses: List<Expense>,
  dailySafeLimit: Long,
  selectedEpochDay: Long?,
  currency: AppCurrency,
  locale: Locale,
  onSelectEpochDay: (Long?) -> Unit,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSelectMonth: (String) -> Unit,
  onAddExpenseOnDate: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tactile = rememberTactileFeedback()
  var isExpanded by remember { mutableStateOf(true) }
  var showMonthYearPicker by remember { mutableStateOf(false) }

  val yearMonth = remember(monthKey) {
    runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.now() }
  }
  val today = remember { LocalDate.now() }
  val todayEpochDay = remember(today) { today.toEpochDay() }

  val totalsByEpochDay = remember(expenses) {
    expenses.groupBy { it.dateEpochDay }
      .mapValues { (_, list) -> list.sumOf { it.amount } }
  }
  val countsByEpochDay = remember(expenses) {
    expenses.groupingBy { it.dateEpochDay }.eachCount()
  }

  val firstDayOfMonth = yearMonth.atDay(1)
  val leadingEmptyCells = (firstDayOfMonth.dayOfWeek.value - 1) // Monday = 0 .. Sunday = 6
  val daysInMonth = yearMonth.lengthOfMonth()
  val totalSlots = leadingEmptyCells + daysInMonth
  val rowCount = (totalSlots + 6) / 7

  SculptedSurfaceCard(
    accentColor = MaterialTheme.colorScheme.primary,
    cornerRadius = 20,
    modifier = modifier.testTag("monthly_calendar_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Calendar Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .tactileClickable(pressedScale = 0.95f) { isExpanded = !isExpanded }
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .testTag("calendar_collapse_toggle"),
        ) {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.calendar_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Icon(
            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }

        // Compact Month Switcher inside Calendar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          IconButton(
            onClick = {
              tactile.tick()
              onPreviousMonth()
            },
            modifier = Modifier
              .size(36.dp)
              .testTag("calendar_prev_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = stringResource(R.string.prev_month_cd),
              modifier = Modifier.size(18.dp),
            )
          }
          Text(
            text = Formatters.formatMonthTitle(monthKey, locale),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .tactileClickable(pressedScale = 0.94f) { showMonthYearPicker = true }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("calendar_month_title_button"),
          )
          IconButton(
            onClick = {
              tactile.tick()
              onNextMonth()
            },
            modifier = Modifier
              .size(36.dp)
              .testTag("calendar_next_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = stringResource(R.string.next_month_cd),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          // Weekday Headers (Mon..Sun)
          Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dayOfWeek ->
              Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(3),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
              )
            }
          }

          // Day Grid
          for (week in 0 until rowCount) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              for (col in 0 until 7) {
                val cellIndex = week * 7 + col
                val dayOfMonth = cellIndex - leadingEmptyCells + 1
                if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                  Spacer(
                    modifier = Modifier
                      .weight(1f)
                      .height(42.dp),
                  )
                } else {
                  val cellDate = yearMonth.atDay(dayOfMonth)
                  val cellEpochDay = cellDate.toEpochDay()
                  val dayTotal = totalsByEpochDay[cellEpochDay] ?: 0L
                  val isSelected = selectedEpochDay == cellEpochDay
                  val isToday = cellEpochDay == todayEpochDay
                  val isOverSafeLimit = dailySafeLimit > 0L && dayTotal > dailySafeLimit

                  val containerColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    dayTotal > 0L -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                  }
                  val contentColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                  }
                  val borderModifier = if (isToday && !isSelected) {
                    Modifier.border(
                      width = 1.dp,
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                      shape = RoundedCornerShape(10.dp),
                    )
                  } else {
                    Modifier
                  }

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(42.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(containerColor)
                      .then(borderModifier)
                      .tactileClickable(pressedScale = 0.90f) {
                        onSelectEpochDay(if (isSelected) null else cellEpochDay)
                      }
                      .testTag("calendar_day_$dayOfMonth"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Text(
                        text = dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected || isToday || dayTotal > 0L) {
                          FontWeight.Bold
                        } else {
                          FontWeight.Normal
                        },
                        color = contentColor,
                      )
                      if (dayTotal > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                          modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                              when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isOverSafeLimit -> CoralDanger
                                else -> MaterialTheme.colorScheme.primary
                              }
                            ),
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(top = 2.dp),
          )

          // Selected Date Summary / Quick Action Bar
          if (selectedEpochDay != null) {
            val selectedAmount = totalsByEpochDay[selectedEpochDay] ?: 0L
            val selectedCount = countsByEpochDay[selectedEpochDay] ?: 0
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(
                    R.string.calendar_selected_date_filter,
                    Formatters.formatShortDate(selectedEpochDay, locale),
                  ),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "$selectedCount • ${Formatters.formatMoney(selectedAmount, currency)}",
                  style = MaterialTheme.typography.bodySmall,
                  fontFamily = JetBrainsMonoFontFamily,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                TextButton(
                  onClick = { onAddExpenseOnDate(selectedEpochDay) },
                  modifier = Modifier
                    .height(48.dp)
                    .testTag("calendar_add_on_date_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = stringResource(R.string.calendar_add_on_date),
                    maxLines = 1,
                  )
                }
                IconButton(
                  onClick = { onSelectEpochDay(null) },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("clear_calendar_date_filter"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.calendar_clear_filter),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          } else {
            Text(
              text = stringResource(R.string.calendar_day_summary_none),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 2.dp),
            )
          }
        }
      }
    }
  }

  if (showMonthYearPicker) {
    MonthYearPickerDialog(
      currentMonthKey = monthKey,
      locale = locale,
      onSelectMonth = onSelectMonth,
      onDismiss = { showMonthYearPicker = false },
    )
  }
}
