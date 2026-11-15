package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AiExpenseParseState
import com.example.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditorSheet(
  existingExpense: Expense?,
  aiParseState: AiExpenseParseState,
  onParseWithAi: (String) -> Unit,
  onScanReceiptUri: (Uri) -> Unit,
  onClearAiParse: () -> Unit,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    dateEpochDay: Long,
    note: String,
  ) -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeLocale = activeLanguage.toLocale()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val today = remember { LocalDate.now() }

  var title by remember(existingExpense) { mutableStateOf(existingExpense?.title ?: "") }
  var amountText by remember(existingExpense) {
    mutableStateOf(if (existingExpense != null && existingExpense.amount > 0L) existingExpense.amount.toString() else "")
  }
  var selectedCategory by remember(existingExpense) {
    mutableStateOf(existingExpense?.categoryEnum ?: ExpenseCategory.FOOD)
  }
  var selectedMethod by remember(existingExpense) {
    mutableStateOf(existingExpense?.paymentMethodEnum ?: PaymentMethod.QRIS)
  }
  var selectedEpochDay by remember(existingExpense) {
    mutableLongStateOf(existingExpense?.dateEpochDay ?: today.toEpochDay())
  }
  var note by remember(existingExpense) { mutableStateOf(existingExpense?.note ?: "") }
  var aiPromptText by remember(existingExpense) { mutableStateOf("") }
  var showDatePicker by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val receiptPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      onScanReceiptUri(uri)
    }
  }

  LaunchedEffect(aiParseState) {
    if (aiParseState is AiExpenseParseState.Success) {
      val parsed = aiParseState.parsed
      title = parsed.title
      amountText = parsed.amount.toString()
      selectedCategory = parsed.category
      selectedMethod = parsed.paymentMethod
      if (parsed.note.isNotBlank()) {
        note = parsed.note
      }
      validationError = null
      onClearAiParse()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = if (existingExpense == null) {
          stringResource(R.string.editor_new_title)
        } else {
          stringResource(R.string.editor_edit_title)
        },
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (existingExpense == null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = stringResource(R.string.ai_quick_fill_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            OutlinedTextField(
              value = aiPromptText,
              onValueChange = {
                aiPromptText = it.take(160)
                if (aiParseState is AiExpenseParseState.Error) onClearAiParse()
              },
              placeholder = { Text(stringResource(R.string.ai_quick_fill_hint)) },
              maxLines = 2,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_quick_fill_input"),
            )
            if (aiParseState is AiExpenseParseState.Error) {
              Text(
                text = aiParseState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
            FilledTonalButton(
              onClick = { onParseWithAi(aiPromptText) },
              enabled = aiParseState !is AiExpenseParseState.Loading && aiPromptText.isNotBlank(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ai_quick_fill_button"),
            ) {
              if (aiParseState is AiExpenseParseState.Loading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ai_quick_fill_loading))
              } else {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ai_quick_fill_action))
              }
            }
            OutlinedButton(
              onClick = {
                if (aiParseState is AiExpenseParseState.Error) onClearAiParse()
                receiptPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
              },
              enabled = aiParseState !is AiExpenseParseState.Loading,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ai_scan_receipt_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.ai_scan_receipt_action))
            }
          }
        }
      }

      // Amount Input + Quick Nominal Chips
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(10)
            amountText = digits
            validationError = null
          },
          label = { Text(stringResource(R.string.editor_amount_label, activeCurrency.symbol)) },
          prefix = {
            Text(
              text = activeCurrency.prefixWithSpace,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          },
          placeholder = { Text(activeCurrency.budgetStepAmount.toString()) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_amount_input"),
        )

        val currentAmount = amountText.toLongOrNull() ?: 0L
        if (currentAmount > 0L) {
          Text(
            text = stringResource(
              R.string.editor_parsed_preview,
              Formatters.formatMoney(currentAmount, activeCurrency),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = JetBrainsMonoFontFamily,
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          activeCurrency.expenseQuickDeltas.forEach { (delta, label) ->
            SuggestionChip(
              onClick = {
                val next = (amountText.toLongOrNull() ?: 0L) + delta
                amountText = next.toString()
                validationError = null
              },
              label = { Text(label, fontFamily = JetBrainsMonoFontFamily) },
            )
          }
        }
      }

      // Title Input + Quick Title Chips
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it.take(80)
            validationError = null
          },
          label = { Text(stringResource(R.string.editor_name_label)) },
          placeholder = { Text(stringResource(R.string.editor_name_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_title_input"),
        )

        val presetOptions = listOf(
          stringResource(R.string.preset_lunch) to ExpenseCategory.FOOD,
          stringResource(R.string.preset_coffee) to ExpenseCategory.FOOD,
          stringResource(R.string.preset_transport) to ExpenseCategory.TRANSPORT,
          stringResource(R.string.preset_groceries) to ExpenseCategory.SHOPPING,
          stringResource(R.string.preset_utilities) to ExpenseCategory.BILLS,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          presetOptions.forEach { (preset, cat) ->
            SuggestionChip(
              onClick = {
                title = preset
                selectedCategory = cat
                validationError = null
              },
              label = { Text(preset) },
            )
          }
        }
      }

      // Category Selector
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.label_category),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ExpenseCategory.entries.forEach { category ->
            val selected = selectedCategory == category
            FilterChip(
              selected = selected,
              onClick = { selectedCategory = category },
              label = { Text(stringResource(category.labelRes)) },
              leadingIcon = {
                Icon(
                  imageVector = category.iconVector(),
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = category.accentColor.copy(alpha = 0.16f),
                selectedLabelColor = category.accentColor,
                selectedLeadingIconColor = category.accentColor,
              ),
              modifier = Modifier.testTag("category_chip_${category.name}"),
            )
          }
        }
      }

      // Date Selector
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.label_date),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val todayEpoch = today.toEpochDay()
          val yesterdayEpoch = today.minusDays(1).toEpochDay()
          FilterChip(
            selected = selectedEpochDay == todayEpoch,
            onClick = { selectedEpochDay = todayEpoch },
            label = { Text(stringResource(R.string.label_today)) },
          )
          FilterChip(
            selected = selectedEpochDay == yesterdayEpoch,
            onClick = { selectedEpochDay = yesterdayEpoch },
            label = { Text(stringResource(R.string.label_yesterday)) },
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clickable { showDatePicker = true }
              .testTag("pick_date_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = stringResource(R.string.label_date),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = Formatters.formatShortDate(selectedEpochDay, activeLocale),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      // Payment Method
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.label_payment_method),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(stringResource(method.labelRes)) },
              leadingIcon = {
                Icon(
                  imageVector = method.iconVector(),
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
            )
          }
        }
      }

      // Optional Note
      OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(200) },
        label = { Text(stringResource(R.string.label_optional_note)) },
        placeholder = { Text(stringResource(R.string.label_additional_details)) },
        maxLines = 2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      if (validationError != null) {
        Text(
          text = validationError!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(52.dp),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
        Button(
          onClick = {
            val parsedAmount = amountText.toLongOrNull() ?: 0L
            when {
              parsedAmount <= 0L -> validationError = context.getString(R.string.editor_error_amount)
              title.isBlank() -> validationError = context.getString(R.string.editor_error_title)
              else -> onSave(title.trim(), parsedAmount, selectedCategory, selectedMethod, selectedEpochDay, note.trim())
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1.4f)
            .height(52.dp)
            .testTag("save_expense_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (existingExpense == null) {
              stringResource(R.string.action_save)
            } else {
              stringResource(R.string.action_update)
            }
          )
        }
      }
    }
  }

  if (showDatePicker) {
    val initialMillis = LocalDate.ofEpochDay(selectedEpochDay)
      .atStartOfDay(ZoneOffset.UTC)
      .toInstant()
      .toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
              selectedEpochDay = picked.toEpochDay()
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_select))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
