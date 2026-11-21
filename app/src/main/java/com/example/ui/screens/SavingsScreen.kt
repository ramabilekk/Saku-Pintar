package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.CashflowEntry
import com.example.data.local.CashflowLedgerManager
import com.example.data.local.CashflowType
import com.example.data.local.SavingsHistoryManager
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.security.PrivacySecurityManager
import com.example.ui.components.CreateSavingsGoalDialog
import com.example.ui.components.DimensionalVaultCard
import com.example.ui.components.EditSavingsGoalDialog
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.RecordIncomeDialog
import com.example.ui.components.SavingsGoalHistorySheet
import com.example.ui.components.SavingsTransactionDialog
import com.example.ui.components.SculptedSurfaceCard
import com.example.ui.components.WalletBalanceDialog
import com.example.ui.components.WalletTransferDialog
import com.example.ui.components.rememberIsEffectiveDarkTheme
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactileClickable
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.viewmodel.SavingsUiData
import com.example.util.Formatters
import java.util.Locale

@Composable
fun SavingsScreen(
  data: SavingsUiData,
  onCreateGoal: (
    title: String,
    targetAmount: Long,
    initialSavedAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) -> Unit,
  onDepositGoal: (SavingsGoal, Long) -> Unit,
  onWithdrawGoal: (SavingsGoal, Long) -> Unit,
  onDeleteGoal: (String) -> Unit,
  onSetWalletBalance: (PaymentMethod, Long) -> Unit,
  onUpdateGoal: (
    goalId: String,
    title: String,
    targetAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) -> Unit = { _, _, _, _, _ -> },
  userId: String = "",
  onRecordIncome: (title: String, amount: Long, targetWallet: PaymentMethod, note: String) -> Unit = { _, _, _, _ -> },
  onTransferWallet: (from: PaymentMethod, to: PaymentMethod, amount: Long, note: String) -> Unit = { _, _, _, _ -> },
  onDeleteCashflowEntry: (String) -> Unit = {},
  monthKey: String = Formatters.currentMonthKey(),
  onPreviousMonth: () -> Unit = {},
  onNextMonth: () -> Unit = {},
  onSelectMonth: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val hideBalances by PrivacySecurityManager.observeHideBalances(context).collectAsStateWithLifecycle()
  val cashflowFlow = remember(context, userId) { CashflowLedgerManager.observeEntries(context, userId) }
  val allCashflowEntries by cashflowFlow.collectAsStateWithLifecycle()
  val historyFlow = remember(context, userId) { SavingsHistoryManager.observeEntries(context, userId) }
  val allSavingsHistory by historyFlow.collectAsStateWithLifecycle()
  val monthlyCashflow = remember(allCashflowEntries, monthKey) {
    allCashflowEntries.filter { it.monthKey == monthKey }
  }
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  var showCreateDialog by remember { mutableStateOf(false) }
  var showIncomeDialog by remember { mutableStateOf(false) }
  var showTransferDialog by remember { mutableStateOf(false) }
  var transactionTarget by remember { mutableStateOf<Pair<SavingsGoal, Boolean>?>(null) }
  var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
  var historyGoal by remember { mutableStateOf<SavingsGoal?>(null) }
  var pendingDeleteGoal by remember { mutableStateOf<SavingsGoal?>(null) }
  var editingWallet by remember { mutableStateOf<PaymentMethod?>(null) }

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
      // 1. Hero Total Saved & Wallet Overview + Integrated Calendar Selector
      item {
        SavingsHeroCard(
          data = data,
          monthKey = monthKey,
          currency = activeCurrency,
          locale = activeLocale,
          hideBalances = hideBalances,
          onToggleHideBalances = { PrivacySecurityManager.toggleHideBalances(context) },
          onPreviousMonth = onPreviousMonth,
          onNextMonth = onNextMonth,
          onSelectMonth = onSelectMonth,
        )
      }

      // 1b. Quick Cashflow Actions: Catat Pemasukan & Transfer Antar-Dompet
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { showIncomeDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = EmeraldSafe,
              contentColor = Color.White,
            ),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("record_income_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.ArrowDownward,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.cashflow_record_income_action),
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          OutlinedButton(
            onClick = { showTransferDialog = true },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("wallet_transfer_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.SwapHoriz,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.cashflow_transfer_action),
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // 2. Wallet Balances Section
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.savings_wallets_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          val methods = PaymentMethod.entries
          methods.chunked(2).forEach { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              pair.forEach { method ->
                val balance = data.walletBalances[method] ?: 0L
                WalletCard(
                  paymentMethod = method,
                  balance = balance,
                  currency = activeCurrency,
                  hideBalances = hideBalances,
                  onClick = { editingWallet = method },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // 2b. Monthly Cashflow History (Incomes & Wallet Transfers)
      if (monthlyCashflow.isNotEmpty()) {
        item {
          MonthlyCashflowHistoryCard(
            entries = monthlyCashflow,
            currency = activeCurrency,
            locale = activeLocale,
            hideBalances = hideBalances,
            onDeleteEntry = onDeleteCashflowEntry,
          )
        }
      }

      // 3. Savings Goals Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.savings_goals_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = stringResource(R.string.savings_goals_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = { showCreateDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
              contentColor = if (isDarkTheme) ObsidianInk else Color.White,
            ),
            modifier = Modifier
              .height(48.dp)
              .testTag("add_savings_goal_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.savings_new_goal_button),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      // 4. Savings Goals List or Empty State
      if (data.goals.isEmpty()) {
        item {
          SculptedSurfaceCard(
            cornerRadius = 20,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = GoldAccent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.28f)),
                modifier = Modifier.size(56.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
              Text(
                text = stringResource(R.string.savings_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = stringResource(R.string.savings_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
                  contentColor = if (isDarkTheme) ObsidianInk else Color.White,
                ),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("empty_create_goal_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.savings_empty_cta),
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      } else {
        items(data.goals, key = { it.id }) { goal ->
          SavingsGoalCard(
            goal = goal,
            currency = activeCurrency,
            locale = activeLocale,
            hideBalances = hideBalances,
            onDeposit = { transactionTarget = goal to true },
            onWithdraw = { transactionTarget = goal to false },
            onEdit = { editingGoal = goal },
            onViewHistory = { historyGoal = goal },
            onDelete = { pendingDeleteGoal = goal },
          )
        }
      }

      // 5. Reassuring info notice
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = stringResource(R.string.savings_disclaimer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateSavingsGoalDialog(
      onConfirm = { title, target, initial, cat, note ->
        onCreateGoal(title, target, initial, cat, note)
        showCreateDialog = false
      },
      onDismiss = { showCreateDialog = false },
    )
  }

  if (showIncomeDialog) {
    RecordIncomeDialog(
      currency = activeCurrency,
      onSaveIncome = { title, amount, targetWallet, note ->
        onRecordIncome(title, amount, targetWallet, note)
      },
      onDismiss = { showIncomeDialog = false },
    )
  }

  if (showTransferDialog) {
    WalletTransferDialog(
      walletBalances = data.walletBalances,
      currency = activeCurrency,
      hideBalances = hideBalances,
      onConfirmTransfer = { from, to, amount, note ->
        onTransferWallet(from, to, amount, note)
      },
      onDismiss = { showTransferDialog = false },
    )
  }

  transactionTarget?.let { (goal, isDeposit) ->
    SavingsTransactionDialog(
      goal = goal,
      isDeposit = isDeposit,
      onConfirm = { amount ->
        if (isDeposit) {
          onDepositGoal(goal, amount)
        } else {
          onWithdrawGoal(goal, amount)
        }
        transactionTarget = null
      },
      onDismiss = { transactionTarget = null },
    )
  }

  editingWallet?.let { method ->
    WalletBalanceDialog(
      paymentMethod = method,
      currentBalance = data.walletBalances[method] ?: 0L,
      onConfirm = { newBalance ->
        onSetWalletBalance(method, newBalance)
        editingWallet = null
      },
      onDismiss = { editingWallet = null },
    )
  }

  pendingDeleteGoal?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteGoal = null },
      title = { Text(stringResource(R.string.savings_delete_title)) },
      text = {
        Text(
          stringResource(
            R.string.savings_delete_message,
            target.title,
            Formatters.formatMoney(target.savedAmount, activeCurrency),
          ),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteGoal(target.id)
            pendingDeleteGoal = null
          },
        ) {
          Text(stringResource(R.string.action_delete), color = CoralDanger)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteGoal = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  editingGoal?.let { targetGoal ->
    EditSavingsGoalDialog(
      goal = targetGoal,
      onConfirm = { title, targetAmount, category, note ->
        onUpdateGoal(targetGoal.id, title, targetAmount, category, note)
        editingGoal = null
      },
      onDismiss = { editingGoal = null },
    )
  }

  historyGoal?.let { targetGoal ->
    val liveGoal = data.goals.firstOrNull { it.id == targetGoal.id } ?: targetGoal
    val goalEntries = remember(allSavingsHistory, liveGoal.id) {
      allSavingsHistory.filter { it.goalId == liveGoal.id }
    }
    SavingsGoalHistorySheet(
      goal = liveGoal,
      entries = goalEntries,
      hideBalances = hideBalances,
      onDepositClick = { transactionTarget = liveGoal to true },
      onEditClick = { editingGoal = liveGoal },
      onDismiss = { historyGoal = null },
    )
  }
}

@Composable
private fun SavingsHeroCard(
  data: SavingsUiData,
  monthKey: String,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onToggleHideBalances: () -> Unit,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSelectMonth: (String) -> Unit,
) {
  var showMonthPicker by remember { mutableStateOf(false) }
  val tactile = rememberTactileFeedback()
  val animatedProgress by animateFloatAsState(
    targetValue = data.overallProgressFraction,
    animationSpec = tween(durationMillis = 450),
    label = "savings_total_progress",
  )

  DimensionalVaultCard {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Top Integrated Calendar / Month Selector + Gold Vault Badge Row
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
                text = Formatters.formatMonthTitle(monthKey, locale),
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

        Surface(
          color = GoldAccent.copy(alpha = 0.22f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.50f)),
        ) {
          Icon(
            imageVector = Icons.Rounded.Savings,
            contentDescription = null,
            tint = Color(0xFFFDE68A),
            modifier = Modifier
              .padding(9.dp)
              .size(22.dp),
          )
        }
      }

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.savings_hero_total),
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
              .testTag("privacy_eye_toggle_savings"),
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
          text = Formatters.formatMoneyMasked(data.totalSaved, currency, hideBalances),
          style = MaterialTheme.typography.displaySmall,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
        )
      }

      if (data.totalTarget > 0L) {
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
          color = GoldAccent,
          trackColor = Color.White.copy(alpha = 0.22f),
          strokeCap = StrokeCap.Round,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.savings_hero_pct, (data.overallProgressFraction * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
          )
          Text(
            text = stringResource(
              R.string.savings_hero_target,
              Formatters.formatMoneyCompactMasked(data.totalTarget, currency, locale, hideBalances),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }

      Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AccountBalanceWallet,
              contentDescription = null,
              tint = SkyPositiveAccent,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = stringResource(R.string.savings_hero_wallet_total),
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
          Text(
            text = Formatters.formatMoneyMasked(data.totalWalletBalance, currency, hideBalances),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
      }
    }
  }

  if (showMonthPicker) {
    MonthYearPickerDialog(
      currentMonthKey = monthKey,
      locale = locale,
      onSelectMonth = { newKey ->
        onSelectMonth(newKey)
        showMonthPicker = false
      },
      onDismiss = { showMonthPicker = false },
    )
  }
}

@Composable
private fun WalletCard(
  paymentMethod: PaymentMethod,
  balance: Long,
  currency: AppCurrency,
  hideBalances: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val methodLabel = stringResource(paymentMethod.labelRes)
  val accentColor = when (paymentMethod) {
    PaymentMethod.QRIS -> MaterialTheme.colorScheme.primary
    PaymentMethod.CASH -> GoldAccent
    PaymentMethod.TRANSFER -> SkyPositiveAccent
    PaymentMethod.CARD -> MaterialTheme.colorScheme.secondary
  }
  SculptedSurfaceCard(
    cornerRadius = 18,
    onClick = onClick,
    modifier = modifier.testTag("wallet_card_${paymentMethod.name}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = accentColor.copy(alpha = 0.15f),
            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.30f)),
            modifier = Modifier.size(30.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = paymentMethod.walletIcon(),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          Text(
            text = methodLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = "${stringResource(R.string.action_edit)} $methodLabel",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = Formatters.formatMoneyMasked(balance, currency, hideBalances),
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MonthlyCashflowHistoryCard(
  entries: List<CashflowEntry>,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onDeleteEntry: (String) -> Unit,
) {
  SculptedSurfaceCard(
    accentColor = EmeraldSafe,
    cornerRadius = 20,
    modifier = Modifier.testTag("monthly_cashflow_history_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.cashflow_history_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      entries.take(8).forEach { entry ->
        val isIncome = entry.type == CashflowType.INCOME
        val accent = if (isIncome) EmeraldSafe else MaterialTheme.colorScheme.primary
        val subtitle = if (isIncome) {
          "${Formatters.formatShortDate(entry.dateEpochDay, locale)} • ${stringResource(entry.targetWallet.labelRes)}"
        } else {
          val fromLabel = entry.sourceWallet?.let { stringResource(it.labelRes) } ?: ""
          val toLabel = stringResource(entry.targetWallet.labelRes)
          "${Formatters.formatShortDate(entry.dateEpochDay, locale)} • $fromLabel → $toLabel"
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = accent.copy(alpha = 0.16f),
              modifier = Modifier.size(34.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.SwapHoriz,
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = entry.title.ifBlank {
                  stringResource(
                    if (isIncome) R.string.cashflow_history_income_badge else R.string.cashflow_history_transfer_badge,
                  )
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = if (hideBalances) {
                Formatters.formatMoneyMasked(entry.amount, currency, true)
              } else if (isIncome) {
                "+${Formatters.formatMoney(entry.amount, currency)}"
              } else {
                Formatters.formatMoney(entry.amount, currency)
              },
              style = MaterialTheme.typography.labelLarge,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              color = accent,
            )
            IconButton(
              onClick = { onDeleteEntry(entry.id) },
              modifier = Modifier
                .size(48.dp)
                .testTag("delete_cashflow_${entry.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SavingsGoalCard(
  goal: SavingsGoal,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onDeposit: () -> Unit,
  onWithdraw: () -> Unit,
  onEdit: () -> Unit,
  onViewHistory: () -> Unit,
  onDelete: () -> Unit,
) {
  val category = goal.categoryEnum
  val categoryLabel = stringResource(category.labelRes)
  val animatedProgress by animateFloatAsState(
    targetValue = goal.progressFraction,
    animationSpec = tween(durationMillis = 400),
    label = "goal_progress_${goal.id}",
  )

  val isDarkTheme = rememberIsEffectiveDarkTheme()

  SculptedSurfaceCard(
    cornerRadius = 20,
    onClick = onViewHistory,
    modifier = Modifier.testTag("savings_goal_card_${goal.id}"),
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = category.accentColor.copy(alpha = 0.14f),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = category.iconVector(),
              contentDescription = null,
              tint = category.accentColor,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = goal.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (goal.isCompleted) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = SkyPositiveAccent,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Text(
            text = if (goal.note.isNotBlank()) "$categoryLabel • ${goal.note}" else categoryLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onViewHistory,
            modifier = Modifier
              .size(48.dp)
              .testTag("history_goal_${goal.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = stringResource(R.string.savings_history_cd),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = onEdit,
            modifier = Modifier
              .size(48.dp)
              .testTag("edit_goal_${goal.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.savings_edit_cd),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(19.dp),
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(48.dp)
              .testTag("delete_goal_${goal.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = "${stringResource(R.string.action_delete)} ${goal.title}",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = Formatters.formatMoneyMasked(goal.savedAmount, currency, hideBalances),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (goal.isCompleted) SkyPositiveAccent else MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(
              R.string.savings_card_target,
              Formatters.formatMoneyCompactMasked(goal.targetAmount, currency, locale, hideBalances),
              (goal.progressFraction * 100).toInt(),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
          color = if (goal.isCompleted) SkyPositiveAccent else category.accentColor,
          trackColor = category.accentColor.copy(alpha = 0.14f),
          strokeCap = StrokeCap.Round,
        )
        if (!goal.isCompleted) {
          Text(
            text = stringResource(
              R.string.savings_card_remaining,
              Formatters.formatMoneyMasked(goal.remainingAmount, currency, hideBalances),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onDeposit,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
            contentColor = if (isDarkTheme) ObsidianInk else Color.White,
          ),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("deposit_goal_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.savings_deposit),
            fontWeight = FontWeight.SemiBold,
          )
        }
        OutlinedButton(
          onClick = onWithdraw,
          enabled = goal.savedAmount > 0L,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("withdraw_goal_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.savings_withdraw),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

private fun PaymentMethod.walletIcon(): ImageVector = when (this) {
  PaymentMethod.QRIS -> Icons.Rounded.QrCode2
  PaymentMethod.CASH -> Icons.Rounded.Payments
  PaymentMethod.TRANSFER -> Icons.Rounded.AccountBalance
  PaymentMethod.CARD -> Icons.Rounded.CreditCard
}

private fun SavingsGoalCategory.iconVector(): ImageVector = when (this) {
  SavingsGoalCategory.EMERGENCY -> Icons.Rounded.Shield
  SavingsGoalCategory.GADGET -> Icons.Rounded.LaptopMac
  SavingsGoalCategory.TRAVEL -> Icons.Rounded.FlightTakeoff
  SavingsGoalCategory.VEHICLE -> Icons.Rounded.DirectionsCar
  SavingsGoalCategory.EDUCATION -> Icons.Rounded.School
  SavingsGoalCategory.HOME -> Icons.Rounded.Home
  SavingsGoalCategory.OTHER -> Icons.Rounded.Savings
}
