package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.budget.BudgetPaceStatus
import com.example.data.model.Expense
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.components.AiInsightCard
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DimensionalVaultCard
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.ReceiptPhotoViewerDialog
import com.example.ui.components.ReceiptThumbnailBadge
import com.example.ui.components.SculptedSurfaceCard
import com.example.ui.components.SevenDaySpendingChart
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactileClickable
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.viewmodel.AiInsightUiState
import com.example.ui.viewmodel.CategoryProgress
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters
import java.util.Locale

@Composable
fun DashboardScreen(
  data: FinanceDashboardData,
  displayName: String,
  customPhotoBase64: String?,
  googlePhotoUrl: String?,
  totalSavedAmount: Long,
  totalWalletBalance: Long,
  aiInsightState: AiInsightUiState,
  onRequestAiInsight: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenExportReport: () -> Unit,
  onNavigateToExpenses: () -> Unit,
  onNavigateToAutoBudget: () -> Unit,
  onNavigateToSavings: () -> Unit,
  onEditExpense: (Expense) -> Unit,
  onPreviousMonth: () -> Unit = {},
  onNextMonth: () -> Unit = {},
  onSelectMonth: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }
  var viewingReceiptExpense by remember { mutableStateOf<Expense?>(null) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. 3D-Layered Hero Monthly Summary + Integrated Calendar Selector + Daily Safe Limit Card
      item {
        HeroSummaryCard(
          data = data,
          currency = activeCurrency,
          locale = activeLocale,
          onPreviousMonth = onPreviousMonth,
          onNextMonth = onNextMonth,
          onSelectMonth = onSelectMonth,
          onSetupBudgetClick = onNavigateToAutoBudget,
        )
      }

      // 1b. 2-Column Quick-Access Bento Row: Tabungan & Dompet + Rekap Laporan
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Left Bento Card: Tabungan & Dompet
          SculptedSurfaceCard(
            accentColor = GoldAccent,
            cornerRadius = 20,
            onClick = onNavigateToSavings,
            modifier = Modifier
              .weight(1f)
              .testTag("dashboard_savings_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = GoldAccent.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.Savings,
                      contentDescription = null,
                      tint = GoldAccent,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Surface(
                  shape = CircleShape,
                  color = GoldAccent.copy(alpha = 0.14f),
                  modifier = Modifier.size(28.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                      contentDescription = stringResource(R.string.dashboard_savings_cd),
                      tint = GoldAccent,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = stringResource(R.string.dashboard_savings_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = stringResource(
                    R.string.dashboard_savings_subtitle,
                    Formatters.formatMoneyCompact(totalSavedAmount, activeCurrency, activeLocale),
                    Formatters.formatMoneyCompact(totalWalletBalance, activeCurrency, activeLocale),
                  ),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          // Right Bento Card: Rekap Laporan Bulanan
          SculptedSurfaceCard(
            accentColor = MaterialTheme.colorScheme.primary,
            cornerRadius = 20,
            onClick = onOpenExportReport,
            modifier = Modifier
              .weight(1f)
              .testTag("dashboard_export_report_button"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.TableChart,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  modifier = Modifier.size(28.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.FileDownload,
                      contentDescription = stringResource(R.string.dashboard_recap_cd),
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = stringResource(R.string.dashboard_recap_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = stringResource(R.string.dashboard_recap_subtitle),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // 2. If no budget exists yet, show prominent Auto-Budget Generator Prompt
      if (data.budget == null) {
        item {
          AutoBudgetPromptCard(onClick = onNavigateToAutoBudget)
        }
      }

      // 3. Gemini AI Financial Advisor Card
      item {
        AiInsightCard(
          state = aiInsightState,
          currentMonthKey = data.monthKey,
          onAnalyzeClick = onRequestAiInsight,
        )
      }

      // 4. 7-Day Daily Pace Chart
      item {
        SculptedSurfaceCard(
          accentColor = MaterialTheme.colorScheme.primary,
          cornerRadius = 22,
        ) {
          SevenDaySpendingChart(
            expenses = data.allHistoricalExpenses,
            dailySafeLimit = data.dailyInsight.dailySafeLimit,
            modifier = Modifier.padding(18.dp),
          )
        }
      }

      // 5. Category Allocation Breakdown
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .width(4.dp)
                .height(18.dp),
            ) {}
            Text(
              text = stringResource(R.string.dashboard_category_allocation),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          TextButton(
            onClick = onNavigateToAutoBudget,
            modifier = Modifier.testTag("edit_budget_link"),
          ) {
            Text(
              text = stringResource(R.string.dashboard_auto_arrange),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      val activeCategories = data.categoryBreakdown.filter { it.allocated > 0L || it.spent > 0L }
      if (activeCategories.isEmpty()) {
        item {
          SculptedSurfaceCard(cornerRadius = 18) {
            Text(
              text = stringResource(
                R.string.dashboard_empty_categories,
                Formatters.formatMonthTitle(data.monthKey, activeLocale),
              ),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(18.dp),
            )
          }
        }
      } else {
        items(activeCategories, key = { it.category.name }) { item ->
          CategoryProgressRowCard(
            item = item,
            currency = activeCurrency,
            locale = activeLocale,
          )
        }
      }

      // 6. Recent Daily Expenses
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier
                .width(4.dp)
                .height(18.dp),
            ) {}
            Text(
              text = stringResource(R.string.dashboard_recent_expenses),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
          TextButton(
            onClick = onNavigateToExpenses,
            modifier = Modifier.testTag("see_all_expenses_button"),
          ) {
            Text(
              text = stringResource(R.string.dashboard_see_all),
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      val recent = data.expenses.take(4)
      if (recent.isEmpty()) {
        item {
          SculptedSurfaceCard(cornerRadius = 18) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = stringResource(R.string.dashboard_empty_expenses_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = stringResource(R.string.dashboard_empty_expenses_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(recent, key = { it.id }) { expense ->
          CompactExpenseRow(
            expense = expense,
            currency = activeCurrency,
            locale = activeLocale,
            onClick = { onEditExpense(expense) },
            onViewReceipt = { viewingReceiptExpense = expense },
          )
        }
      }
    }
  }

  viewingReceiptExpense?.let { exp ->
    if (exp.hasReceiptPhoto) {
      ReceiptPhotoViewerDialog(
        receiptPhotoBase64 = exp.receiptPhotoBase64,
        title = exp.title,
        subtitle = Formatters.formatShortDate(exp.dateEpochDay, activeLocale),
        amountFormatted = Formatters.formatMoney(exp.amount, activeCurrency),
        onDismiss = { viewingReceiptExpense = null },
      )
    }
  }
}

@Composable
private fun HeroSummaryCard(
  data: FinanceDashboardData,
  currency: AppCurrency,
  locale: Locale,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSelectMonth: (String) -> Unit,
  onSetupBudgetClick: () -> Unit,
) {
  val insight = data.dailyInsight
  val budget = data.budget
  val primary = MaterialTheme.colorScheme.primary
  val tactile = rememberTactileFeedback()
  var showMonthPicker by remember { mutableStateOf(false) }

  val progressRatio = if (insight.totalBudget > 0L) {
    (insight.totalSpent.toFloat() / insight.totalBudget.toFloat()).coerceIn(0f, 1f)
  } else 0f
  val animatedProgress by animateFloatAsState(
    targetValue = progressRatio,
    animationSpec = tween(durationMillis = 450),
    label = "budget_progress",
  )

  val (statusLabel, statusColor, statusIcon) = when (insight.paceStatus) {
    BudgetPaceStatus.SAFE -> Triple(stringResource(R.string.hero_status_safe), SkyPositiveAccent, Icons.Rounded.Shield)
    BudgetPaceStatus.CAUTION -> Triple(stringResource(R.string.hero_status_caution), GoldAccent, Icons.Rounded.WarningAmber)
    BudgetPaceStatus.OVER -> Triple(stringResource(R.string.hero_status_over), CoralDanger, Icons.Rounded.WarningAmber)
  }

  DimensionalVaultCard {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      // Top Integrated Calendar / Month Selector + Status Badge Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = Color.White.copy(alpha = 0.15f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 2.dp),
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
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(50))
                .tactileClickable(
                  pressedScale = 0.94f,
                  rippleColor = Color.White,
                ) { showMonthPicker = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("month_picker_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = stringResource(R.string.month_picker_title),
                tint = Color(0xFFFDE68A),
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = Formatters.formatMonthTitle(data.monthKey, locale),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
              )
              Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp),
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
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        if (budget != null) {
          Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f)),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }
          }
        }
      }

      // Main Budget Amount Headline
      Column {
        Text(
          text = if (budget != null) {
            stringResource(R.string.hero_remaining_budget)
          } else {
            stringResource(R.string.hero_total_spent_month)
          },
          style = MaterialTheme.typography.labelLarge,
          color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (budget != null) {
            Formatters.formatMoney(insight.remainingBudget, currency)
          } else {
            Formatters.formatMoney(insight.totalSpent, currency)
          },
          style = MaterialTheme.typography.displaySmall,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
        )
      }

      if (budget != null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(50)),
            color = if (insight.paceStatus == BudgetPaceStatus.OVER) CoralDanger else SkyPositiveAccent,
            trackColor = Color.White.copy(alpha = 0.20f),
            strokeCap = StrokeCap.Round,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(
                R.string.hero_spent_label,
                Formatters.formatMoneyCompact(insight.totalSpent, currency, locale),
              ),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Text(
              text = stringResource(
                R.string.hero_of_budget_label,
                Formatters.formatMoneyCompact(insight.totalBudget, currency, locale),
              ),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
          }
        }

        // Daily Safe-to-Spend & Savings Glassmorphic Pill Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = stringResource(R.string.hero_daily_safe_limit),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.84f),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = Formatters.formatMoney(insight.dailySafeLimit, currency),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Text(
                text = stringResource(
                  R.string.hero_today_spent,
                  Formatters.formatMoneyCompact(insight.todaySpent, currency, locale),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFBAE6FD),
              )
            }
          }

          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.36f)),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.Savings,
                  contentDescription = null,
                  tint = GoldAccent,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.hero_savings_target),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.84f),
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = Formatters.formatMoney(budget.savingsTarget, currency),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Text(
                text = stringResource(R.string.hero_percent_income, budget.savingsPercent),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFDE68A),
              )
            }
          }
        }
      } else {
        Text(
          text = stringResource(
            R.string.hero_today_spending_line,
            Formatters.formatMoney(insight.todaySpent, currency),
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.9f),
        )
        Button(
          onClick = onSetupBudgetClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = primary,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("hero_setup_budget_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.hero_setup_budget_cta),
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentMonthKey = data.monthKey,
      locale = locale,
      onSelectMonth = onSelectMonth,
      onDismiss = { showMonthPicker = false },
    )
  }
}

@Composable
private fun AutoBudgetPromptCard(onClick: () -> Unit) {
  SculptedSurfaceCard(
    accentColor = MaterialTheme.colorScheme.tertiary,
    cornerRadius = 20,
    onClick = onClick,
    modifier = Modifier.testTag("auto_budget_prompt_card"),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.prompt_budget_missing_title),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(R.string.prompt_budget_missing_body),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
        contentDescription = stringResource(R.string.prompt_budget_missing_cd),
        tint = MaterialTheme.colorScheme.tertiary,
      )
    }
  }
}

@Composable
private fun CategoryProgressRowCard(
  item: CategoryProgress,
  currency: AppCurrency,
  locale: Locale,
) {
  val isOver = item.allocated > 0L && item.spent > item.allocated
  val progressColor = if (isOver) CoralDanger else item.category.accentColor
  val animatedProgress by animateFloatAsState(
    targetValue = item.progressFraction.coerceIn(0f, 1f),
    animationSpec = tween(400),
    label = "cat_progress",
  )

  SculptedSurfaceCard(
    accentColor = progressColor,
    cornerRadius = 18,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CategoryIconBadge(category = item.category, size = 42.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(item.category.labelRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = if (item.allocated > 0L) {
              if (item.remaining >= 0L) {
                stringResource(R.string.cat_remaining, Formatters.formatMoney(item.remaining, currency))
              } else {
                stringResource(R.string.cat_over, Formatters.formatMoney(-item.remaining, currency))
              }
            } else {
              stringResource(R.string.cat_no_limit)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = Formatters.formatMoney(item.spent, currency),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurface,
          )
          if (item.allocated > 0L) {
            Text(
              text = "/ ${Formatters.formatMoneyCompact(item.allocated, currency, locale)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (item.allocated > 0L) {
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
          color = progressColor,
          trackColor = item.category.accentColor.copy(alpha = 0.14f),
          strokeCap = StrokeCap.Round,
        )
      }
    }
  }
}

@Composable
private fun CompactExpenseRow(
  expense: Expense,
  currency: AppCurrency,
  locale: Locale,
  onClick: () -> Unit,
  onViewReceipt: () -> Unit,
) {
  SculptedSurfaceCard(
    accentColor = expense.categoryEnum.accentColor,
    cornerRadius = 18,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CategoryIconBadge(category = expense.categoryEnum, size = 42.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = expense.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${Formatters.formatShortDate(expense.dateEpochDay, locale)} • ${stringResource(expense.paymentMethodEnum.labelRes)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (expense.hasReceiptPhoto) {
        ReceiptThumbnailBadge(
          receiptPhotoBase64 = expense.receiptPhotoBase64,
          onClick = onViewReceipt,
          modifier = Modifier.testTag("dashboard_receipt_badge_${expense.id}"),
        )
      }
      Text(
        text = "-${Formatters.formatMoney(expense.amount, currency)}",
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
