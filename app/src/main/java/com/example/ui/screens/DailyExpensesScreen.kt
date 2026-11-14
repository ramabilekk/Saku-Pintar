package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.iconVector
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters

@Composable
fun DailyExpensesScreen(
  data: FinanceDashboardData,
  onSearchQueryChange: (String) -> Unit,
  onCategoryFilterSelect: (ExpenseCategory?) -> Unit,
  onEditExpense: (Expense) -> Unit,
  onDeleteExpense: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pendingDeleteExpense by remember { mutableStateOf<Expense?>(null) }

  val groupedByDay = remember(data.filteredExpenses) {
    data.filteredExpenses.groupBy { it.dateEpochDay }
      .toSortedMap(compareByDescending { it })
  }
  val totalFiltered = remember(data.filteredExpenses) {
    data.filteredExpenses.sumOf { it.amount }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
    ) {
      // Search & Filter Controls
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = data.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Cari pengeluaran atau catatan...") },
          leadingIcon = {
            Icon(imageVector = Icons.Rounded.Search, contentDescription = "Cari")
          },
          trailingIcon = {
            if (data.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Hapus pencarian")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_search_input"),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = data.selectedCategoryFilter == null,
            onClick = { onCategoryFilterSelect(null) },
            label = { Text("Semua") },
          )
          ExpenseCategory.entries.forEach { category ->
            val selected = data.selectedCategoryFilter == category
            FilterChip(
              selected = selected,
              onClick = { onCategoryFilterSelect(category) },
              label = { Text(category.shortLabel) },
              leadingIcon = {
                Icon(
                  imageVector = category.iconVector(),
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = category.accentColor.copy(alpha = 0.16f),
                selectedLabelColor = category.accentColor,
                selectedLeadingIconColor = category.accentColor,
              ),
            )
          }
        }

        // Filtered Summary Strip
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${data.filteredExpenses.size} transaksi di ${Formatters.formatMonthTitle(data.monthKey)}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = Formatters.formatRupiah(totalFiltered),
              style = MaterialTheme.typography.titleSmall,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      if (groupedByDay.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              modifier = Modifier.size(64.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(30.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Tidak ada catatan pengeluaran",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Catat pengeluaran harian Anda dengan tombol + di kanan bawah agar pantauan bulanan selalu akurat.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          groupedByDay.forEach { (epochDay, dayExpenses) ->
            val dayTotal = dayExpenses.sumOf { it.amount }
            item(key = "header_$epochDay") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = Formatters.formatDateGroupLabel(epochDay),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = Formatters.formatRupiah(dayTotal),
                  style = MaterialTheme.typography.labelLarge,
                  fontFamily = JetBrainsMonoFontFamily,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            items(dayExpenses, key = { it.id }) { expense ->
              DetailedExpenseCard(
                expense = expense,
                onEdit = { onEditExpense(expense) },
                onRequestDelete = { pendingDeleteExpense = expense },
              )
            }
          }
        }
      }
    }
  }

  pendingDeleteExpense?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteExpense = null },
      title = { Text("Hapus Pengeluaran?") },
      text = {
        Text("Hapus catatan \"${target.title}\" sebesar ${Formatters.formatRupiah(target.amount)}?")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteExpense(target.id)
            pendingDeleteExpense = null
          },
        ) {
          Text("Hapus", color = CoralDanger)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteExpense = null }) {
          Text("Batal")
        }
      },
    )
  }
}

@Composable
private fun DetailedExpenseCard(
  expense: Expense,
  onEdit: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onEdit)
      .testTag("expense_card_${expense.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CategoryIconBadge(category = expense.categoryEnum, size = 42.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = expense.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${expense.categoryEnum.labelId} • ${expense.paymentMethodEnum.labelId}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expense.note.isNotBlank()) {
          Text(
            text = expense.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Text(
        text = "-${Formatters.formatRupiah(expense.amount)}",
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      IconButton(
        onClick = onRequestDelete,
        modifier = Modifier
          .size(48.dp)
          .testTag("delete_expense_${expense.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "Hapus ${expense.title}",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
