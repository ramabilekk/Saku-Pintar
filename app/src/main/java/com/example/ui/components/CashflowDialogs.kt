package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters

/**
 * Dialog for recording an Income entry and crediting the selected wallet balance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordIncomeDialog(
  currency: AppCurrency,
  onSaveIncome: (title: String, amount: Long, targetWallet: PaymentMethod, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var targetWallet by remember { mutableStateOf(PaymentMethod.TRANSFER) }
  var note by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  val quickSources = remember {
    listOf("Gaji", "Bonus", "Freelance", "Cashback")
  }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.cashflow_income_dialog_title),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          quickSources.forEach { preset ->
            SuggestionChip(
              onClick = {
                title = preset
                errorText = null
              },
              label = { Text(preset) },
            )
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it.take(60)
            errorText = null
          },
          label = { Text(stringResource(R.string.cashflow_income_source_label)) },
          placeholder = { Text(stringResource(R.string.cashflow_income_source_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("income_title_input"),
        )

        OutlinedTextField(
          value = amountText,
          onValueChange = { raw ->
            amountText = raw.filter { it.isDigit() }.take(12)
            errorText = null
          },
          label = { Text(stringResource(R.string.editor_amount_label, currency.symbol)) },
          prefix = {
            Text(
              text = currency.prefixWithSpace,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("income_amount_input"),
        )

        Text(
          text = stringResource(R.string.cashflow_income_target_wallet),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = targetWallet == method,
              onClick = { targetWallet = method },
              label = { Text(stringResource(method.labelRes)) },
              modifier = Modifier.testTag("income_wallet_${method.name}"),
            )
          }
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(120) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        if (errorText != null) {
          Text(
            text = errorText.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = CoralDanger,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanTitle = title.trim()
          val parsedAmount = amountText.toLongOrNull() ?: 0L
          if (cleanTitle.isEmpty()) {
            errorText = context.getString(R.string.editor_error_title)
          } else if (parsedAmount <= 0L) {
            errorText = context.getString(R.string.editor_error_amount)
          } else {
            onSaveIncome(cleanTitle, parsedAmount, targetWallet, note.trim())
            onDismiss()
          }
        },
        modifier = Modifier.testTag("save_income_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

/**
 * Dialog for transferring funds between two active wallets (e.g., Bank Transfer → QRIS / Cash).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletTransferDialog(
  walletBalances: Map<PaymentMethod, Long>,
  currency: AppCurrency,
  hideBalances: Boolean,
  onConfirmTransfer: (from: PaymentMethod, to: PaymentMethod, amount: Long, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var fromWallet by remember { mutableStateOf(PaymentMethod.TRANSFER) }
  var toWallet by remember { mutableStateOf(PaymentMethod.QRIS) }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  val sourceBalance = walletBalances[fromWallet] ?: 0L

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.cashflow_transfer_dialog_title),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = stringResource(R.string.cashflow_transfer_from_label),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = fromWallet == method,
              onClick = {
                fromWallet = method
                errorText = null
              },
              label = { Text(stringResource(method.labelRes)) },
              modifier = Modifier.testTag("transfer_from_${method.name}"),
            )
          }
        }
        Text(
          text = "${stringResource(fromWallet.labelRes)}: ${Formatters.formatMoneyMasked(sourceBalance, currency, hideBalances)}",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          color = EmeraldSafe,
        )

        Text(
          text = stringResource(R.string.cashflow_transfer_to_label),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = toWallet == method,
              onClick = {
                toWallet = method
                errorText = null
              },
              label = { Text(stringResource(method.labelRes)) },
              modifier = Modifier.testTag("transfer_to_${method.name}"),
            )
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { raw ->
            amountText = raw.filter { it.isDigit() }.take(12)
            errorText = null
          },
          label = { Text(stringResource(R.string.editor_amount_label, currency.symbol)) },
          prefix = {
            Text(
              text = currency.prefixWithSpace,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transfer_amount_input"),
        )

        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(120) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        if (errorText != null) {
          Text(
            text = errorText.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = CoralDanger,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedAmount = amountText.toLongOrNull() ?: 0L
          when {
            fromWallet == toWallet -> {
              errorText = context.getString(R.string.cashflow_transfer_same_wallet_error)
            }
            parsedAmount <= 0L -> {
              errorText = context.getString(R.string.editor_error_amount)
            }
            sourceBalance > 0L && parsedAmount > sourceBalance -> {
              errorText = context.getString(
                R.string.cashflow_transfer_insufficient,
                fromWallet.label(context),
              )
            }
            else -> {
              onConfirmTransfer(fromWallet, toWallet, parsedAmount, note.trim())
              onDismiss()
            }
          }
        },
        modifier = Modifier.testTag("confirm_transfer_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
