package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.SavingsHistoryEntry
import com.example.data.local.SavingsHistoryManager
import com.example.data.local.SavingsHistoryType
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CreateSavingsGoalDialog(
  onConfirm: (
    title: String,
    targetAmount: Long,
    initialSavedAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()

  var title by remember { mutableStateOf("") }
  var targetInput by remember { mutableStateOf("") }
  var initialInput by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(SavingsGoalCategory.EMERGENCY) }

  val targetAmount = targetInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val initialAmount = initialInput.filter { it.isDigit() }.toLongOrNull() ?: 0L

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.savings_create_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(60) },
          label = { Text(stringResource(R.string.savings_goal_name_label)) },
          placeholder = { Text(stringResource(R.string.savings_goal_name_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_title_input"),
        )

        OutlinedTextField(
          value = targetInput,
          onValueChange = { targetInput = it.filter { ch -> ch.isDigit() }.take(11) },
          label = { Text(stringResource(R.string.savings_target_amount_label, activeCurrency.symbol)) },
          prefix = { Text(activeCurrency.prefixWithSpace) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_target_input"),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          activeCurrency.savingsTargetPresets.forEach { (preset, label) ->
            FilterChip(
              selected = targetAmount == preset,
              onClick = { targetInput = preset.toString() },
              label = { Text(label) },
            )
          }
        }

        OutlinedTextField(
          value = initialInput,
          onValueChange = { initialInput = it.filter { ch -> ch.isDigit() }.take(11) },
          label = { Text(stringResource(R.string.savings_initial_balance_label)) },
          prefix = { Text(activeCurrency.prefixWithSpace) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_initial_input"),
        )

        Text(
          text = stringResource(R.string.savings_goal_category_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SavingsGoalCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(stringResource(cat.labelRes)) },
            )
          }
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(120) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          placeholder = { Text(stringResource(R.string.savings_note_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(title, targetAmount, initialAmount, selectedCategory, note)
        },
        enabled = title.isNotBlank() && targetAmount > 0L,
        modifier = Modifier
          .height(48.dp)
          .testTag("create_goal_submit_button"),
      ) {
        Text(stringResource(R.string.savings_save_goal))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
fun SavingsTransactionDialog(
  goal: SavingsGoal,
  isDeposit: Boolean,
  onConfirm: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()

  var amountInput by remember { mutableStateOf("") }
  val amount = amountInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val isValid = if (isDeposit) amount > 0L else amount in 1L..goal.savedAmount

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    accentColor = if (isDeposit) EmeraldSafe else CoralDanger,
    title = {
      Text(
        if (isDeposit) {
          stringResource(R.string.savings_tx_deposit_title, goal.title)
        } else {
          stringResource(R.string.savings_tx_withdraw_title, goal.title)
        }
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = stringResource(R.string.savings_tx_currently_saved),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${Formatters.formatMoney(goal.savedAmount, activeCurrency)} / ${Formatters.formatMoney(goal.targetAmount, activeCurrency)}",
              style = MaterialTheme.typography.titleSmall,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it.filter { ch -> ch.isDigit() }.take(11) },
          label = {
            Text(
              if (isDeposit) {
                stringResource(R.string.savings_tx_deposit_label, activeCurrency.symbol)
              } else {
                stringResource(R.string.savings_tx_withdraw_label, activeCurrency.symbol)
              }
            )
          },
          prefix = { Text(activeCurrency.prefixWithSpace) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          isError = !isDeposit && amount > goal.savedAmount,
          supportingText = if (!isDeposit && amount > goal.savedAmount) {
            {
              Text(
                stringResource(
                  R.string.savings_tx_exceeds,
                  Formatters.formatMoney(goal.savedAmount, activeCurrency),
                )
              )
            }
          } else null,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_tx_amount_input"),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          activeCurrency.savingsTxPresets.forEach { (preset, label) ->
            FilterChip(
              selected = amount == preset,
              onClick = { amountInput = preset.toString() },
              label = { Text(label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(amount) },
        enabled = isValid,
        modifier = Modifier
          .height(48.dp)
          .testTag("savings_tx_confirm_button"),
      ) {
        Text(
          if (isDeposit) {
            stringResource(R.string.savings_tx_confirm_deposit)
          } else {
            stringResource(R.string.savings_tx_confirm_withdraw)
          }
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
fun WalletBalanceDialog(
  paymentMethod: PaymentMethod,
  currentBalance: Long,
  onConfirm: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val methodLabel = stringResource(paymentMethod.labelRes)

  var balanceInput by remember(currentBalance) { mutableStateOf(currentBalance.toString()) }
  val parsedBalance = balanceInput.filter { it.isDigit() }.toLongOrNull() ?: 0L

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.wallet_dialog_title, methodLabel)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.wallet_dialog_desc, methodLabel),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = balanceInput,
          onValueChange = { balanceInput = it.filter { ch -> ch.isDigit() }.take(11) },
          label = { Text(stringResource(R.string.wallet_dialog_label, activeCurrency.symbol)) },
          prefix = { Text(activeCurrency.prefixWithSpace) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_balance_input"),
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          activeCurrency.walletQuickDeltas.forEach { (delta, label) ->
            OutlinedButton(
              onClick = { balanceInput = (parsedBalance + delta).toString() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Text(label)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(parsedBalance) },
        modifier = Modifier
          .height(48.dp)
          .testTag("wallet_balance_save_button"),
      ) {
        Text(stringResource(R.string.wallet_dialog_save))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
fun EditSavingsGoalDialog(
  goal: SavingsGoal,
  onConfirm: (
    title: String,
    targetAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()

  var title by remember(goal.id) { mutableStateOf(goal.title) }
  var targetInput by remember(goal.id) { mutableStateOf(goal.targetAmount.toString()) }
  var note by remember(goal.id) { mutableStateOf(goal.note) }
  var selectedCategory by remember(goal.id) { mutableStateOf(goal.categoryEnum) }

  val targetAmount = targetInput.filter { it.isDigit() }.toLongOrNull() ?: 0L

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.savings_edit_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(60) },
          label = { Text(stringResource(R.string.savings_goal_name_label)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_goal_title_input"),
        )

        OutlinedTextField(
          value = targetInput,
          onValueChange = { targetInput = it.filter { ch -> ch.isDigit() }.take(11) },
          label = { Text(stringResource(R.string.savings_target_amount_label, activeCurrency.symbol)) },
          prefix = { Text(activeCurrency.prefixWithSpace) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_goal_target_input"),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          activeCurrency.savingsTargetPresets.forEach { (preset, label) ->
            FilterChip(
              selected = targetAmount == preset,
              onClick = { targetInput = preset.toString() },
              label = { Text(label) },
            )
          }
        }

        Text(
          text = stringResource(R.string.savings_goal_category_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SavingsGoalCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(stringResource(cat.labelRes)) },
            )
          }
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(120) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          placeholder = { Text(stringResource(R.string.savings_note_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title.trim(), targetAmount, selectedCategory, note.trim()) },
        enabled = title.isNotBlank() && targetAmount > 0L,
        modifier = Modifier
          .height(48.dp)
          .testTag("edit_goal_save_button"),
      ) {
        Text(stringResource(R.string.savings_edit_save))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalHistorySheet(
  goal: SavingsGoal,
  entries: List<SavingsHistoryEntry>,
  hideBalances: Boolean,
  onDepositClick: () -> Unit,
  onEditClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val locale = remember(activeLanguage) { activeLanguage.toLocale() }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val totalDeposits = remember(entries) {
    entries.filter { it.type == SavingsHistoryType.DEPOSIT || it.type == SavingsHistoryType.INITIAL }
      .sumOf { it.amount }
  }
  val totalWithdrawals = remember(entries) {
    entries.filter { it.type == SavingsHistoryType.WITHDRAW }.sumOf { it.amount }
  }
  val estimate = remember(goal, entries) {
    SavingsHistoryManager.estimateCompletion(goal, entries)
  }
  val dateFormatter = remember(locale) {
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale)
  }
  val targetDateFormatter = remember(locale) {
    DateTimeFormatter.ofPattern("d MMM yyyy", locale)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = rememberLiquidGlassSheetContainerColor(),
    tonalElevation = 0.dp,
    dragHandle = { LiquidGlassSheetDragHandle() },
  ) {
    LiquidGlassWindowBlurEffect()
    Box(modifier = Modifier.fillMaxWidth()) {
      AppAmbientBackdrop()
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .testTag("savings_history_sheet"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.savings_history_sheet_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = goal.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = "${(goal.progressFraction * 100f).toInt()}%",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = if (goal.isCompleted) EmeraldSafe else MaterialTheme.colorScheme.primary,
        )
      }

      // Progress Bar + Amounts
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
          progress = { goal.progressFraction },
          color = if (goal.isCompleted) EmeraldSafe else MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          strokeCap = StrokeCap.Round,
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50)),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = Formatters.formatMoneyMasked(goal.savedAmount, activeCurrency, hideBalances),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = Formatters.formatMoneyMasked(goal.targetAmount, activeCurrency, hideBalances),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Estimated Completion Banner
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (goal.isCompleted) EmeraldSafe.copy(alpha = 0.12f) else GoldAccent.copy(alpha = 0.12f),
        border = BorderStroke(
          1.dp,
          if (goal.isCompleted) EmeraldSafe.copy(alpha = 0.35f) else GoldAccent.copy(alpha = 0.35f),
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.EventAvailable,
            contentDescription = null,
            tint = if (goal.isCompleted) EmeraldSafe else GoldAccent,
            modifier = Modifier.size(22.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = stringResource(R.string.savings_history_eta_label),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val etaText = when {
              goal.isCompleted -> stringResource(R.string.savings_history_eta_done)
              estimate == null -> stringResource(R.string.savings_history_eta_need_more)
              estimate.estimatedDaysLeft >= 60 -> stringResource(
                R.string.savings_history_eta_months,
                (estimate.estimatedDaysLeft / 30).coerceAtLeast(1),
                estimate.estimatedTargetDate.format(targetDateFormatter),
              )
              else -> stringResource(
                R.string.savings_history_eta_days,
                estimate.estimatedDaysLeft,
                estimate.estimatedTargetDate.format(targetDateFormatter),
              )
            }
            Text(
              text = etaText,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      // Summary Row: Total Deposits vs Withdrawals
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = EmeraldSafe.copy(alpha = 0.10f),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = stringResource(R.string.savings_history_total_deposits),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = Formatters.formatMoneyMasked(totalDeposits, activeCurrency, hideBalances),
              style = MaterialTheme.typography.labelLarge,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = EmeraldSafe,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CoralDanger.copy(alpha = 0.10f),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = stringResource(R.string.savings_history_total_withdrawals),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = Formatters.formatMoneyMasked(totalWithdrawals, activeCurrency, hideBalances),
              style = MaterialTheme.typography.labelLarge,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = CoralDanger,
            )
          }
        }
      }

      // Quick Actions: Deposit & Edit Goal
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = {
            onDismiss()
            onDepositClick()
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
        ) {
          Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.savings_deposit), fontWeight = FontWeight.Bold)
        }
        FilledTonalButton(
          onClick = {
            onDismiss()
            onEditClick()
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
        ) {
          Icon(imageVector = Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.savings_edit_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Chronological History List
      if (entries.isEmpty()) {
        Text(
          text = stringResource(R.string.savings_history_empty),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp),
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          entries.forEach { entry ->
            val isOut = entry.type == SavingsHistoryType.WITHDRAW
            val accent = when (entry.type) {
              SavingsHistoryType.WITHDRAW -> CoralDanger
              SavingsHistoryType.INITIAL -> GoldAccent
              SavingsHistoryType.DEPOSIT -> EmeraldSafe
            }
            val typeLabel = when (entry.type) {
              SavingsHistoryType.INITIAL -> stringResource(R.string.savings_history_initial_label)
              SavingsHistoryType.DEPOSIT -> stringResource(R.string.savings_history_deposit_label)
              SavingsHistoryType.WITHDRAW -> stringResource(R.string.savings_history_withdraw_label)
            }
            val timeText = remember(entry.timestampMillis, locale) {
              Instant.ofEpochMilli(entry.timestampMillis)
                .atZone(ZoneId.systemDefault())
                .format(dateFormatter)
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = accent.copy(alpha = 0.14f),
                  modifier = Modifier.size(34.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = when (entry.type) {
                        SavingsHistoryType.WITHDRAW -> Icons.Rounded.ArrowUpward
                        SavingsHistoryType.INITIAL -> Icons.Rounded.Savings
                        SavingsHistoryType.DEPOSIT -> Icons.Rounded.ArrowDownward
                      },
                      contentDescription = null,
                      tint = accent,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  val sign = if (isOut) "-" else "+"
                  Text(
                    text = "$sign${Formatters.formatMoneyMasked(entry.amount, activeCurrency, hideBalances)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                  )
                  Text(
                    text = stringResource(
                      R.string.savings_history_balance_after,
                      Formatters.formatMoneyMasked(entry.balanceAfter, activeCurrency, hideBalances),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}
