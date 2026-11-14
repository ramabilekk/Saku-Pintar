package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.budget.BudgetAutoComposer
import com.example.data.model.BudgetStrategy
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters
import java.time.YearMonth

@Composable
fun AutoBudgetScreen(
  data: FinanceDashboardData,
  currentUserId: String,
  isSaving: Boolean,
  onSaveBudget: (MonthlyBudget) -> Unit,
  modifier: Modifier = Modifier,
) {
  val existingBudget = data.budget
  var incomeText by remember(data.monthKey, existingBudget?.monthlyIncome) {
    mutableStateOf(
      (existingBudget?.monthlyIncome?.takeIf { it > 0L } ?: 8_000_000L).toString()
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
      )
    )
  }

  // Recompute automatically whenever income or strategy is changed by the user
  fun regenerateAutoDraft(income: Long, strategy: BudgetStrategy) {
    composedDraft = BudgetAutoComposer.composeBudget(
      userId = currentUserId,
      monthKey = data.monthKey,
      monthlyIncome = income,
      strategy = strategy,
      historicalExpenses = data.allHistoricalExpenses,
    )
  }

  LaunchedEffect(existingBudget) {
    if (existingBudget != null) {
      composedDraft = existingBudget
    } else {
      regenerateAutoDraft(parsedIncome, selectedStrategy)
    }
  }

  val spendableTotal = composedDraft.totalAllocatedSpendBudget
  val savingsAmount = (parsedIncome - spendableTotal).coerceAtLeast(0L)
  val daysInMonth = runCatching { YearMonth.parse(data.monthKey).lengthOfMonth() }.getOrElse { 30 }
  val estimatedDailyLimit = if (daysInMonth > 0) spendableTotal / daysInMonth else 0L

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Header & Monthly Income Input
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Column {
                Text(
                  text = "Penyusun Anggaran Otomatis",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Bulan ${Formatters.formatMonthTitle(data.monthKey)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            OutlinedTextField(
              value = incomeText,
              onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(11)
                incomeText = digits
                regenerateAutoDraft(digits.toLongOrNull() ?: 0L, selectedStrategy)
              },
              label = { Text("Pendapatan Bulanan (Rp)") },
              prefix = {
                Text(
                  text = "Rp ",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                )
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              shape = RoundedCornerShape(14.dp),
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
              listOf(
                5_000_000L to "5 jt",
                8_000_000L to "8 jt",
                10_000_000L to "10 jt",
                15_000_000L to "15 jt",
              ).forEach { (preset, label) ->
                SuggestionChip(
                  onClick = {
                    incomeText = preset.toString()
                    regenerateAutoDraft(preset, selectedStrategy)
                  },
                  label = { Text("Rp $label", fontFamily = JetBrainsMonoFontFamily) },
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
            text = "Pilih Strategi Alokasi Otomatis",
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
              shape = RoundedCornerShape(18.dp),
              color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
              } else {
                MaterialTheme.colorScheme.surface
              },
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                  shape = RoundedCornerShape(18.dp),
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
                    text = strategy.titleId,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (strategy == BudgetStrategy.ADAPTIVE_HISTORY && data.allHistoricalExpenses.isEmpty()) {
                      "Belum ada riwayat — akan memakai rasio seimbang otomatis."
                    } else {
                      strategy.subtitleId
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (selected) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Dipilih",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }

      // 3. Summary Preview Card (Savings & Daily Target)
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Alokasi Belanja Bulanan",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
                Text(
                  text = Formatters.formatRupiah(spendableTotal),
                  style = MaterialTheme.typography.headlineSmall,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
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
                      text = "Tabungan",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                    Text(
                      text = Formatters.formatRupiahCompact(savingsAmount),
                      style = MaterialTheme.typography.labelLarge,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary,
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
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
            ) {
              val needsW = (needsTotal / totalForBar).coerceIn(0f, 1f)
              val wantsW = (wantsTotal / totalForBar).coerceIn(0f, 1f)
              val savingsW = (savingsAmount / totalForBar).coerceIn(0f, 1f)
              if (needsW > 0f) {
                Box(
                  modifier = Modifier
                    .weight(needsW)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.onPrimary)
                )
              }
              if (wantsW > 0f) {
                Box(
                  modifier = Modifier
                    .weight(wantsW)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
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
              text = "Estimasi Batas Harian: ${Formatters.formatRupiah(estimatedDailyLimit)} / hari ($daysInMonth hari)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
          }
        }
      }

      // 4. Category Breakdown with +/- 50rb Fine-Tuning
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Rincian Otomatis per Kategori",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          TextButton(
            onClick = { regenerateAutoDraft(parsedIncome, selectedStrategy) },
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Otomatis")
          }
        }
      }

      items(ExpenseCategory.entries, key = { it.name }) { category ->
        val currentLimit = composedDraft.limitFor(category)
        CategoryBudgetAdjustCard(
          category = category,
          amount = currentLimit,
          onDelta = { delta ->
            val nextAmount = (currentLimit + delta).coerceAtLeast(0L)
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
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("save_auto_budget_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Menyimpan...")
          } else {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Terapkan Anggaran Bulan Ini",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryBudgetAdjustCard(
  category: ExpenseCategory,
  amount: Long,
  onDelta: (Long) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CategoryIconBadge(category = category, size = 38.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = category.labelId,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = category.group.labelId,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { onDelta(-50_000L) },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = "Kurangi anggaran ${category.labelId}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = Formatters.formatRupiahCompact(amount),
          style = MaterialTheme.typography.titleSmall,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
          onClick = { onDelta(50_000L) },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Tambah anggaran ${category.labelId}",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}
