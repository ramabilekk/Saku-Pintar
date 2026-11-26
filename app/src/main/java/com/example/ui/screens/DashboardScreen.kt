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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.budget.PaydayCycleManager
import com.example.data.local.CashflowLedgerManager
import com.example.data.model.Expense
import com.example.data.model.RecurringTemplate
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.notifications.BillDueReminderManager
import com.example.notifications.UpcomingBillAlert
import com.example.security.PrivacySecurityManager
import com.example.ui.components.AiInsightCard
import com.example.ui.components.CategoryDonutChartCard
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DimensionalVaultCard
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.MultiMonthTrendCard
import com.example.ui.components.ReceiptPhotoViewerDialog
import com.example.ui.components.ReceiptThumbnailBadge
import com.example.ui.components.SculptedSurfaceCard
import com.example.ui.components.SevenDaySpendingChart
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactileClickable
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianInk
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
  onQuickScanReceipt: () -> Unit = {},
  onLogRecurringTemplate: (RecurringTemplate) -> Unit = {},
  userId: String = "",
  onOpenAiChat: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val dueSchedules by BillDueReminderManager.observeSchedules(context).collectAsStateWithLifecycle()
  val hideBalances by PrivacySecurityManager.observeHideBalances(context).collectAsStateWithLifecycle()
  val onboardingDismissed by PrivacySecurityManager.observeOnboardingDismissed(context).collectAsStateWithLifecycle()
  val cashflowFlow = remember(context, userId) { CashflowLedgerManager.observeEntries(context, userId) }
  val allCashflowEntries by cashflowFlow.collectAsStateWithLifecycle()
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }
  val upcomingBills = remember(data.recurringTemplates, dueSchedules) {
    BillDueReminderManager.computeUpcomingBills(
      templates = data.recurringTemplates,
      schedules = dueSchedules,
      withinDays = 5,
    )
  }
  val hasBudgetStep = data.budget != null
  val hasWalletStep = totalWalletBalance > 0L
  val hasExpenseStep = data.allHistoricalExpenses.isNotEmpty()
  val showOnboardingCard = !onboardingDismissed && !(hasBudgetStep && hasWalletStep && hasExpenseStep)
  var viewingReceiptExpense by remember { mutableStateOf<Expense?>(null) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 132.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. 3D-Layered Hero Monthly Summary + Integrated Calendar Selector + Daily Safe Limit Card
      item {
        HeroSummaryCard(
          data = data,
          currency = activeCurrency,
          locale = activeLocale,
          hideBalances = hideBalances,
          onToggleHideBalances = { PrivacySecurityManager.toggleHideBalances(context) },
          onPreviousMonth = onPreviousMonth,
          onNextMonth = onNextMonth,
          onSelectMonth = onSelectMonth,
          onSetupBudgetClick = onNavigateToAutoBudget,
        )
      }

      // 1.0. 3-Step Quick-Start Onboarding Checklist (auto-hides when complete or dismissed)
      if (showOnboardingCard) {
        item {
          QuickStartOnboardingCard(
            hasBudget = hasBudgetStep,
            hasWallet = hasWalletStep,
            hasExpense = hasExpenseStep,
            onStepBudgetClick = onNavigateToAutoBudget,
            onStepWalletClick = onNavigateToSavings,
            onStepExpenseClick = onQuickScanReceipt,
            onDismiss = { PrivacySecurityManager.dismissOnboarding(context) },
          )
        }
      }

      // 1a. Upcoming Bills Due-Date Alert Strip (when recurring bills are due within 5 days)
      if (upcomingBills.isNotEmpty()) {
        item {
          UpcomingBillsAlertCard(
            alerts = upcomingBills,
            currency = activeCurrency,
            hideBalances = hideBalances,
            onPayNow = onLogRecurringTemplate,
          )
        }
      }

      // 1b. 2-Column Quick-Access Bento Row: Tabungan & Dompet + Rekap Laporan
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Left Bento Card: Tabungan & Dompet
          SculptedSurfaceCard(
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
                    Formatters.formatMoneyCompactMasked(totalSavedAmount, activeCurrency, activeLocale, hideBalances),
                    Formatters.formatMoneyCompactMasked(totalWalletBalance, activeCurrency, activeLocale, hideBalances),
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

      // 3. Gemini AI Financial Advisor Card + Interactive Ask AI
      item {
        AiInsightCard(
          state = aiInsightState,
          currentMonthKey = data.monthKey,
          onAnalyzeClick = onRequestAiInsight,
          onOpenAiChat = onOpenAiChat,
        )
      }

      // 4. Interactive Monthly Spending Distribution & Budget Visual Dashboard
      item {
        CategoryDonutChartCard(
          monthKey = data.monthKey,
          expenses = data.expenses,
          allHistoricalExpenses = data.allHistoricalExpenses,
          currency = activeCurrency,
          locale = activeLocale,
          hideBalances = hideBalances,
          categoryBreakdown = data.categoryBreakdown,
          totalBudget = data.dailyInsight.totalBudget,
          onSetupBudgetClick = onNavigateToAutoBudget,
        )
      }

      // 4b. 7-Day Daily Pace Chart
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

      // 4c. 6-12 Month Multi-Month Trend & Annual Recap Card
      item {
        MultiMonthTrendCard(
          selectedMonthKey = data.monthKey,
          allHistoricalExpenses = data.allHistoricalExpenses,
          cashflowEntries = allCashflowEntries,
          paydayStartDay = data.paydayStartDay,
          currency = activeCurrency,
          locale = activeLocale,
          hideBalances = hideBalances,
        )
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
          Text(
            text = stringResource(R.string.dashboard_category_allocation),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
          )
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
            hideBalances = hideBalances,
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
          Text(
            text = stringResource(R.string.dashboard_recent_expenses),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
          )
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
            hideBalances = hideBalances,
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
        amountFormatted = Formatters.formatMoneyMasked(exp.amount, activeCurrency, hideBalances),
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
  hideBalances: Boolean,
  onToggleHideBalances: () -> Unit,
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

      val activeCycle = remember(data.monthKey, data.paydayStartDay) {
        PaydayCycleManager.resolveCycle(data.monthKey, data.paydayStartDay)
      }
      if (activeCycle.isCustomCycle) {
        Surface(
          color = Color.White.copy(alpha = 0.12f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.8.dp, Color(0xFFFDE68A).copy(alpha = 0.45f)),
        ) {
          Text(
            text = stringResource(R.string.payday_badge_label, activeCycle.formatRangeLabel(locale)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFDE68A),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      // Main Budget Amount Headline + Privacy Eye Toggle
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (budget != null) {
              stringResource(R.string.hero_remaining_budget)
            } else {
              stringResource(R.string.hero_total_spent_month)
            },
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
          )
          IconButton(
            onClick = {
              tactile.tick()
              onToggleHideBalances()
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("privacy_eye_toggle"),
          ) {
            Surface(
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.16f),
              border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)),
              modifier = Modifier.size(34.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                  contentDescription = stringResource(
                    if (hideBalances) R.string.privacy_toggle_show_cd else R.string.privacy_toggle_hide_cd,
                  ),
                  tint = Color.White,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
        Text(
          text = if (budget != null) {
            Formatters.formatMoneyMasked(insight.remainingBudget, currency, hideBalances)
          } else {
            Formatters.formatMoneyMasked(insight.totalSpent, currency, hideBalances)
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
                Formatters.formatMoneyCompactMasked(insight.totalSpent, currency, locale, hideBalances),
              ),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Text(
              text = stringResource(
                R.string.hero_of_budget_label,
                Formatters.formatMoneyCompactMasked(insight.totalBudget, currency, locale, hideBalances),
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
                text = Formatters.formatMoneyMasked(insight.dailySafeLimit, currency, hideBalances),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Text(
                text = stringResource(
                  R.string.hero_today_spent,
                  Formatters.formatMoneyCompactMasked(insight.todaySpent, currency, locale, hideBalances),
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
                text = Formatters.formatMoneyMasked(budget.savingsTarget, currency, hideBalances),
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
            Formatters.formatMoneyMasked(insight.todaySpent, currency, hideBalances),
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.9f),
        )
        Button(
          onClick = onSetupBudgetClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = ObsidianInk,
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
private fun QuickStartOnboardingCard(
  hasBudget: Boolean,
  hasWallet: Boolean,
  hasExpense: Boolean,
  onStepBudgetClick: () -> Unit,
  onStepWalletClick: () -> Unit,
  onStepExpenseClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val completedCount = listOf(hasBudget, hasWallet, hasExpense).count { it }
  val progress by animateFloatAsState(
    targetValue = completedCount / 3f,
    animationSpec = tween(350),
    label = "onboarding_progress",
  )

  SculptedSurfaceCard(
    accentColor = EmeraldSafe,
    cornerRadius = 20,
    modifier = Modifier.testTag("quickstart_onboarding_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = EmeraldSafe.copy(alpha = 0.16f),
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.RocketLaunch,
                contentDescription = null,
                tint = EmeraldSafe,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Column {
            Text(
              text = stringResource(R.string.onboarding_title),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.onboarding_progress_format, completedCount),
              style = MaterialTheme.typography.labelSmall,
              color = EmeraldSafe,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("onboarding_dismiss_button"),
        ) {
          Text(
            text = stringResource(R.string.onboarding_dismiss),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(50)),
        color = EmeraldSafe,
        trackColor = EmeraldSafe.copy(alpha = 0.14f),
        strokeCap = StrokeCap.Round,
      )

      OnboardingStepRow(
        done = hasBudget,
        title = stringResource(R.string.onboarding_step_budget_title),
        subtitle = stringResource(R.string.onboarding_step_budget_sub),
        testTag = "onboarding_step_budget",
        onClick = onStepBudgetClick,
      )
      OnboardingStepRow(
        done = hasWallet,
        title = stringResource(R.string.onboarding_step_wallet_title),
        subtitle = stringResource(R.string.onboarding_step_wallet_sub),
        testTag = "onboarding_step_wallet",
        onClick = onStepWalletClick,
      )
      OnboardingStepRow(
        done = hasExpense,
        title = stringResource(R.string.onboarding_step_expense_title),
        subtitle = stringResource(R.string.onboarding_step_expense_sub),
        testTag = "onboarding_step_expense",
        onClick = onStepExpenseClick,
      )
    }
  }
}

@Composable
private fun OnboardingStepRow(
  done: Boolean,
  title: String,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (done) {
      EmeraldSafe.copy(alpha = 0.08f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    },
    border = BorderStroke(
      1.dp,
      if (done) EmeraldSafe.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (done) EmeraldSafe else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (!done) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
private fun UpcomingBillsAlertCard(
  alerts: List<UpcomingBillAlert>,
  currency: AppCurrency,
  hideBalances: Boolean,
  onPayNow: (RecurringTemplate) -> Unit,
) {
  SculptedSurfaceCard(
    accentColor = GoldAccent,
    cornerRadius = 20,
    modifier = Modifier.testTag("dashboard_upcoming_bills_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = GoldAccent.copy(alpha = 0.18f),
          modifier = Modifier.size(32.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = GoldAccent,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        Text(
          text = stringResource(R.string.bill_due_upcoming_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      alerts.take(3).forEach { alert ->
        val isDueToday = alert.daysUntilDue == 0
        val badgeColor = if (isDueToday) CoralDanger else GoldAccent
        val badgeText = if (isDueToday) {
          stringResource(R.string.bill_due_today_badge)
        } else {
          stringResource(R.string.bill_due_in_days_badge, alert.daysUntilDue)
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.30f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CategoryIconBadge(category = alert.template.categoryEnum, size = 36.dp)
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = alert.template.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = badgeColor.copy(alpha = 0.16f),
                ) {
                  Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
                Text(
                  text = Formatters.formatMoneyMasked(alert.template.amount, currency, hideBalances),
                  style = MaterialTheme.typography.labelMedium,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            FilledTonalButton(
              onClick = { onPayNow(alert.template) },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("pay_upcoming_bill_${alert.template.id}"),
            ) {
              Text(
                text = stringResource(R.string.bill_due_pay_now),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
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
  hideBalances: Boolean,
) {
  val isOver = item.allocated > 0L && item.spent > item.allocated
  val progressColor = if (isOver) CoralDanger else item.category.accentColor
  val animatedProgress by animateFloatAsState(
    targetValue = item.progressFraction.coerceIn(0f, 1f),
    animationSpec = tween(400),
    label = "cat_progress",
  )

  SculptedSurfaceCard(
    accentColor = if (isOver) CoralDanger else MaterialTheme.colorScheme.outlineVariant,
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
                stringResource(R.string.cat_remaining, Formatters.formatMoneyMasked(item.remaining, currency, hideBalances))
              } else {
                stringResource(R.string.cat_over, Formatters.formatMoneyMasked(-item.remaining, currency, hideBalances))
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
            text = Formatters.formatMoneyMasked(item.spent, currency, hideBalances),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurface,
          )
          if (item.allocated > 0L) {
            Text(
              text = "/ ${Formatters.formatMoneyCompactMasked(item.allocated, currency, locale, hideBalances)}",
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
  hideBalances: Boolean,
  onClick: () -> Unit,
  onViewReceipt: () -> Unit,
) {
  SculptedSurfaceCard(
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
        text = if (hideBalances) Formatters.formatMoneyMasked(expense.amount, currency, true) else "-${Formatters.formatMoney(expense.amount, currency)}",
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
