package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.i18n.CurrencyPreferenceManager
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters

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

  AlertDialog(
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

  AlertDialog(
    onDismissRequest = onDismiss,
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

  AlertDialog(
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
