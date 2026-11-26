package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.budget.BudgetAutoComposer
import com.example.data.budget.BudgetRolloverManager
import com.example.data.budget.PaydayCycleManager
import com.example.data.model.BudgetStrategy
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import androidx.compose.ui.graphics.Color
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DimensionalVaultCard
import com.example.ui.components.LiquidGlassAlertDialog
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.SculptedSurfaceCard
import com.example.ui.components.ThousandSeparatorVisualTransformation
import com.example.ui.components.rememberIsEffectiveDarkTheme
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactileClickable
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters
import java.time.YearMonth

@Composable
fun AutoBudgetScreen(
  data: FinanceDashboardData,
  currentUserId: String,
  isSaving: Boolean,
  onSaveBudget: (MonthlyBudget) -> Unit,
  onPreviousMonth: () -> Unit = {},
  onNextMonth: () -> Unit = {},
  onSelectMonth: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val tactile = rememberTactileFeedback()
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val paydayStartDay by PaydayCycleManager.observePaydayStartDay(context).collectAsStateWithLifecycle()
  val rolloverEnabled by BudgetRolloverManager.observeRolloverEnabled(context).collectAsStateWithLifecycle()
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  var showMonthPicker by remember { mutableStateOf(false) }
  var showPaydayPicker by remember { mutableStateOf(false) }
  val activeCycle = remember(data.monthKey, paydayStartDay) {
    PaydayCycleManager.resolveCycle(data.monthKey, paydayStartDay)
  }

  val existingBudget = data.budget
  var incomeText by remember(data.monthKey, existingBudget?.monthlyIncome, activeCurrency) {
    mutableStateOf(
      (existingBudget?.monthlyIncome?.takeIf { it > 0L } ?: activeCurrency.defaultMonthlyIncome).toString()
    )
  }
  var selectedStrategy by remember(data.monthKey, existingBudget?.strategy) {
    mutableStateOf(
      existingBudget?.strategyEnum?.takeIf { it != BudgetStrategy.CUSTOM }
        ?: BudgetStrategy.BALANCED_50_30_20
    )
  }

  val parsedIncome = incomeText.toLongOrNull() ?: 0L

  var composedDraft by remember(data.monthKey) {
    mutableStateOf(
      existingBudget ?: BudgetAutoComposer.composeBudget(
        userId = currentUserId,
        monthKey = data.monthKey,
        monthlyIncome = parsedIncome,
        strategy = selectedStrategy,
        historicalExpenses = data.allHistoricalExpenses,
        roundingUnit = activeCurrency.roundingUnit,
      )
    )
  }

  fun regenerateAutoDraft(income: Long, strategy: BudgetStrategy, currency: AppCurrency = activeCurrency) {
    composedDraft = BudgetAutoComposer.composeBudget(
      userId = currentUserId,
      monthKey = data.monthKey,
      monthlyIncome = income,
      strategy = strategy,
      historicalExpenses = data.allHistoricalExpenses,
      roundingUnit = currency.roundingUnit,
    )
  }

  LaunchedEffect(existingBudget, activeCurrency) {
    if (existingBudget != null) {
      composedDraft = existingBudget
    } else {
      regenerateAutoDraft(parsedIncome, selectedStrategy, activeCurrency)
    }
  }

  val spendableTotal = composedDraft.totalAllocatedSpendBudget
  val savingsAmount = (parsedIncome - spendableTotal).coerceAtLeast(0L)
  val daysInMonth = activeCycle.totalDays
  val estimatedDailyLimit = if (daysInMonth > 0) spendableTotal / daysInMonth else 0L

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Header & Monthly Income Input
      item {
        SculptedSurfaceCard(
          accentColor = MaterialTheme.colorScheme.primary,
          cornerRadius = 22,
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
                  text = stringResource(R.string.budget_builder_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = stringResource(
                    R.string.budget_month_subtitle,
                    Formatters.formatMonthTitle(data.monthKey, activeLocale),
                  ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            // Integrated Full-Width Calendar / Month Selector Bar inside Budget Builder Card
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(
                  onClick = {
                    tactile.tick()
                    onPreviousMonth()
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("prev_month_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.prev_month_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(50))
                    .tactileClickable(pressedScale = 0.94f) { showMonthPicker = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("month_picker_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = stringResource(R.string.month_picker_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = Formatters.formatMonthTitle(data.monthKey, activeLocale),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                  )
                }

                IconButton(
                  onClick = {
                    tactile.tick()
                    onNextMonth()
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("next_month_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }

            // Custom Payday Cycle Indicator + Quick Change
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
              border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
              onClick = { showPaydayPicker = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("budget_payday_cycle_button"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(R.string.payday_section_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = stringResource(R.string.payday_preview_label, activeCycle.formatRangeLabel(activeLocale)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                Text(
                  text = stringResource(R.string.payday_change_action),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }

            // Budget Rollover (Carry Over Unused Budget from Previous Month)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(
                0.8.dp,
                if (rolloverEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("budget_rollover_card"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = stringResource(R.string.budget_rollover_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = if (rolloverEnabled && data.dailyInsight.rolloverAmount > 0L) {
                      stringResource(
                        R.string.budget_rollover_active_bonus,
                        Formatters.formatMoney(data.dailyInsight.rolloverAmount, activeCurrency),
                      )
                    } else {
                      stringResource(R.string.budget_rollover_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rolloverEnabled && data.dailyInsight.rolloverAmount > 0L) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  )
                }
                Switch(
                  checked = rolloverEnabled,
                  onCheckedChange = { enabled ->
                    tactile.tick()
                    BudgetRolloverManager.setRolloverEnabled(context, enabled)
                  },
                  modifier = Modifier.testTag("budget_rollover_switch"),
                )
              }
            }

            val incomeVisualTransformation = remember(activeCurrency) {
              ThousandSeparatorVisualTransformation(
                separator = ThousandSeparatorVisualTransformation.groupingSeparatorFor(activeCurrency),
              )
            }
            OutlinedTextField(
              value = incomeText,
              onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.trimStart('0').take(11)
                incomeText = digits
                regenerateAutoDraft(digits.toLongOrNull() ?: 0L, selectedStrategy)
              },
              label = {
                Text(stringResource(R.string.budget_income_label, activeCurrency.prefixWithSpace.trim()))
              },
              prefix = {
                Text(
                  text = activeCurrency.prefixWithSpace,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                )
              },
              placeholder = {
                Text(
                  text = "0",
                  fontFamily = JetBrainsMonoFontFamily,
                )
              },
              textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
              ),
              visualTransformation = incomeVisualTransformation,
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_income_input"),
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              activeCurrency.incomePresets.forEach { (preset, label) ->
                val chipLabel = if (activeCurrency == AppCurrency.IDR) {
                  "${activeCurrency.prefixWithSpace}$label"
                } else {
                  label
                }
                SuggestionChip(
                  onClick = {
                    incomeText = preset.toString()
                    regenerateAutoDraft(preset, selectedStrategy)
                  },
                  label = { Text(chipLabel, fontFamily = JetBrainsMonoFontFamily) },
                )
              }
            }
          }
        }
      }

      // 2. Strategy Picker
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.budget_strategy_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          listOf(
            BudgetStrategy.BALANCED_50_30_20,
            BudgetStrategy.FRUGAL_60_10_30,
            BudgetStrategy.ADAPTIVE_HISTORY,
          ).forEach { strategy ->
            val selected = selectedStrategy == strategy
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surface
              },
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  shape = RoundedCornerShape(16.dp),
                )
                .clickable {
                  selectedStrategy = strategy
                  regenerateAutoDraft(parsedIncome, strategy)
                }
                .testTag("strategy_${strategy.name}"),
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(strategy.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (strategy == BudgetStrategy.ADAPTIVE_HISTORY && data.allHistoricalExpenses.isEmpty()) {
                      stringResource(R.string.budget_adaptive_no_history)
                    } else {
                      stringResource(strategy.subtitleRes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (selected) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.action_select),
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }

      // 3. 3D-Layered Summary Preview Card (Savings & Daily Target)
      item {
        DimensionalVaultCard {
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.budget_shopping_allocation),
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.84f),
                )
                Text(
                  text = Formatters.formatMoney(spendableTotal, activeCurrency),
                  style = MaterialTheme.typography.headlineSmall,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                )
              }
              Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.45f)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text(
                      text = stringResource(R.string.budget_savings_label),
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.84f),
                    )
                    Text(
                      text = Formatters.formatMoneyCompact(savingsAmount, activeCurrency, activeLocale),
                      style = MaterialTheme.typography.labelLarge,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFFDE68A),
                    )
                  }
                }
              }
            }

            // Stacked Bar Preview
            val needsTotal = ExpenseCategory.entries
              .filter { it.group.name == "NEEDS" }
              .sumOf { composedDraft.limitFor(it) }
            val wantsTotal = ExpenseCategory.entries
              .filter { it.group.name == "WANTS" }
              .sumOf { composedDraft.limitFor(it) }
            val totalForBar = maxOf(parsedIncome, 1L).toFloat()

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.20f)),
            ) {
              val needsW = (needsTotal / totalForBar).coerceIn(0f, 1f)
              val wantsW = (wantsTotal / totalForBar).coerceIn(0f, 1f)
              val savingsW = (savingsAmount / totalForBar).coerceIn(0f, 1f)
              if (needsW > 0f) {
                Box(
                  modifier = Modifier
                    .weight(needsW)
                    .height(10.dp)
                    .background(Color.White)
                )
              }
              if (wantsW > 0f) {
                Box(
                  modifier = Modifier
                    .weight(wantsW)
                    .height(10.dp)
                    .background(SkyPositiveAccent)
                )
              }
              if (savingsW > 0f) {
                Box(
                  modifier = Modifier
                    .weight(savingsW)
                    .height(10.dp)
                    .background(GoldAccent)
                )
              }
            }

            Text(
              text = stringResource(
                R.string.budget_daily_estimate,
                Formatters.formatMoney(estimatedDailyLimit, activeCurrency),
                daysInMonth,
              ),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.90f),
            )
          }
        }
      }

      // 4. Category Breakdown with currency-aware fine-tuning steps
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.budget_breakdown_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = stringResource(
                R.string.budget_breakdown_subtitle,
                Formatters.formatMoney(activeCurrency.budgetStepAmount, activeCurrency),
              ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          TextButton(
            onClick = { regenerateAutoDraft(parsedIncome, selectedStrategy) },
            modifier = Modifier.height(48.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.budget_reset_auto))
          }
        }
      }

      items(ExpenseCategory.entries, key = { it.name }) { category ->
        val currentLimit = composedDraft.limitFor(category)
        CategoryBudgetAdjustCard(
          category = category,
          amount = currentLimit,
          currency = activeCurrency,
          onAmountChange = { rawAmount ->
            val nextAmount = rawAmount.coerceAtLeast(0L)
            val updatedMap = composedDraft.categoryBudgets.toMutableMap().apply {
              put(category.name, nextAmount)
            }
            val newTotalAllocated = updatedMap.values.sum()
            val newSavings = (parsedIncome - newTotalAllocated).coerceAtLeast(0L)
            val safeIncome = maxOf(parsedIncome, 1L)
            val needsSum = ExpenseCategory.entries.filter { it.group.name == "NEEDS" }.sumOf { updatedMap[it.name] ?: 0L }
            val wantsSum = ExpenseCategory.entries.filter { it.group.name == "WANTS" }.sumOf { updatedMap[it.name] ?: 0L }
            val needsPct = ((needsSum * 100L) / safeIncome).coerceIn(0L, 100L)
            val wantsPct = ((wantsSum * 100L) / safeIncome).coerceIn(0L, 100L - needsPct)
            val savingsPct = (100L - needsPct - wantsPct).coerceIn(0L, 100L)

            composedDraft = composedDraft.copy(
              monthlyIncome = parsedIncome,
              strategy = BudgetStrategy.CUSTOM.name,
              savingsTarget = newSavings,
              needsPercent = needsPct,
              wantsPercent = wantsPct,
              savingsPercent = savingsPct,
              categoryBudgets = updatedMap,
            )
          },
        )
      }

      // 5. Save CTA
      item {
        Button(
          onClick = {
            val finalTotal = composedDraft.categoryBudgets.values.sum()
            val finalSavings = (parsedIncome - finalTotal).coerceAtLeast(0L)
            onSaveBudget(
              composedDraft.copy(
                monthlyIncome = parsedIncome,
                savingsTarget = finalSavings,
              )
            )
          },
          enabled = !isSaving && parsedIncome > 0L,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
            contentColor = if (isDarkTheme) ObsidianInk else Color.White,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("save_auto_budget_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = if (isDarkTheme) ObsidianInk else Color.White,
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.action_saving))
          } else {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.budget_apply_button),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentMonthKey = data.monthKey,
      locale = activeLocale,
      onSelectMonth = { newKey ->
        onSelectMonth(newKey)
        showMonthPicker = false
      },
      onDismiss = { showMonthPicker = false },
    )
  }

  if (showPaydayPicker) {
    var draftDay by remember(paydayStartDay) { mutableStateOf(paydayStartDay) }
    val previewCycle = remember(data.monthKey, draftDay) {
      PaydayCycleManager.resolveCycle(data.monthKey, draftDay)
    }
    LiquidGlassAlertDialog(
      onDismissRequest = { showPaydayPicker = false },
      title = { Text(stringResource(R.string.payday_section_title)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = stringResource(R.string.payday_cycle_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            listOf(
              1 to stringResource(R.string.payday_preset_1),
              25 to stringResource(R.string.payday_preset_25),
              28 to stringResource(R.string.payday_preset_28),
            ).forEach { (day, label) ->
              FilterChip(
                selected = draftDay == day,
                onClick = { draftDay = day },
                label = { Text(label) },
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.payday_day_label, draftDay),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { draftDay = (draftDay - 1).coerceIn(1, 28) },
                enabled = draftDay > 1,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp),
              ) {
                Icon(imageVector = Icons.Rounded.Remove, contentDescription = "-")
              }
              OutlinedButton(
                onClick = { draftDay = (draftDay + 1).coerceIn(1, 28) },
                enabled = draftDay < 28,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(44.dp),
              ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "+")
              }
            }
          }
          Text(
            text = stringResource(R.string.payday_preview_label, previewCycle.formatRangeLabel(activeLocale)),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            PaydayCycleManager.setPaydayStartDay(context, draftDay)
            showPaydayPicker = false
          },
          modifier = Modifier.height(48.dp),
        ) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showPaydayPicker = false },
          modifier = Modifier.height(48.dp),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun CategoryBudgetAdjustCard(
  category: ExpenseCategory,
  amount: Long,
  currency: AppCurrency,
  onAmountChange: (Long) -> Unit,
) {
  val categoryLabel = stringResource(category.labelRes)
  val stepAmount = currency.budgetStepAmount
  val stepLabel = currency.budgetStepCompactLabel

  SculptedSurfaceCard(
    cornerRadius = 18,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CategoryIconBadge(category = category, size = 38.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = categoryLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(category.group.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = Formatters.formatMoney(amount, currency),
          style = MaterialTheme.typography.titleSmall,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val categoryVisualTransformation = remember(currency) {
          ThousandSeparatorVisualTransformation(
            separator = ThousandSeparatorVisualTransformation.groupingSeparatorFor(currency),
          )
        }
        OutlinedTextField(
          value = if (amount > 0L) amount.toString() else "",
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.trimStart('0').take(11)
            onAmountChange(digits.toLongOrNull() ?: 0L)
          },
          prefix = {
            Text(
              text = currency.prefixWithSpace,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
            )
          },
          placeholder = {
            Text(
              text = "0",
              fontFamily = JetBrainsMonoFontFamily,
            )
          },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
          ),
          visualTransformation = categoryVisualTransformation,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("budget_input_${category.name}"),
        )

        OutlinedButton(
          onClick = { onAmountChange((amount - stepAmount).coerceAtLeast(0L)) },
          enabled = amount > 0L,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          contentPadding = PaddingValues(horizontal = 10.dp),
          modifier = Modifier
            .height(52.dp)
            .testTag("budget_minus_${category.name}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = "- $categoryLabel $stepLabel",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
          )
        }

        FilledTonalButton(
          onClick = { onAmountChange(amount + stepAmount) },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 10.dp),
          modifier = Modifier
            .height(52.dp)
            .testTag("budget_plus_${category.name}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "+ $categoryLabel $stepLabel",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
          )
        }
      }
    }
  }
}
