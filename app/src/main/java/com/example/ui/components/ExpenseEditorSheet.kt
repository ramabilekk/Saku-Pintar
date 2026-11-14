package com.example.ui.components

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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditorSheet(
  existingExpense: Expense?,
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
  var showDatePicker by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

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
        text = if (existingExpense == null) "Catat Pengeluaran Harian" else "Ubah Pengeluaran",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Amount Input + Quick Nominal Chips
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(10)
            amountText = digits
            validationError = null
          },
          label = { Text("Nominal (Rp)") },
          prefix = {
            Text(
              text = "Rp ",
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          },
          placeholder = { Text("35000") },
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
            text = "Terbaca: ${Formatters.formatRupiah(currentAmount)}",
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
          listOf(15_000L to "+15rb", 25_000L to "+25rb", 50_000L to "+50rb", 100_000L to "+100rb").forEach { (delta, label) ->
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
          label = { Text("Nama Pengeluaran") },
          placeholder = { Text("Contoh: Makan siang, Bensin, Kopi") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_title_input"),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(
            "Makan Siang" to ExpenseCategory.FOOD,
            "Kopi" to ExpenseCategory.FOOD,
            "Transport / Ojek" to ExpenseCategory.TRANSPORT,
            "Belanja Harian" to ExpenseCategory.SHOPPING,
            "Listrik & Air" to ExpenseCategory.BILLS,
          ).forEach { (preset, cat) ->
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
          text = "Kategori",
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
              label = { Text(category.labelId) },
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
          text = "Tanggal",
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
            label = { Text("Hari Ini") },
          )
          FilterChip(
            selected = selectedEpochDay == yesterdayEpoch,
            onClick = { selectedEpochDay = yesterdayEpoch },
            label = { Text("Kemarin") },
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
                contentDescription = "Pilih tanggal",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = Formatters.formatShortDate(selectedEpochDay),
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
          text = "Metode Pembayaran",
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
              label = { Text(method.labelId) },
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
        label = { Text("Catatan (Opsional)") },
        placeholder = { Text("Detail tambahan...") },
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
          Text("Batal")
        }
        Button(
          onClick = {
            val parsedAmount = amountText.toLongOrNull() ?: 0L
            when {
              parsedAmount <= 0L -> validationError = "Masukkan nominal lebih dari Rp 0."
              title.isBlank() -> validationError = "Masukkan nama pengeluaran."
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
          Text(if (existingExpense == null) "Simpan" else "Perbarui")
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
          Text("Pilih")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Batal")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
