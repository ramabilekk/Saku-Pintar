package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.budget.BudgetPaceStatus
import com.example.data.model.Expense
import com.example.ui.components.AiInsightCard
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.SevenDaySpendingChart
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AiInsightUiState
import com.example.ui.viewmodel.CategoryProgress
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters

@Composable
fun DashboardScreen(
  data: FinanceDashboardData,
  aiInsightState: AiInsightUiState,
  onRequestAiInsight: () -> Unit,
  onNavigateToExpenses: () -> Unit,
  onNavigateToAutoBudget: () -> Unit,
  onEditExpense: (Expense) -> Unit,
  modifier: Modifier = Modifier,
) {
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
      // 1. Hero Monthly Summary + Daily Safe Limit Card
      item {
        HeroSummaryCard(
          data = data,
          onSetupBudgetClick = onNavigateToAutoBudget,
        )
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
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SevenDaySpendingChart(
            expenses = data.allHistoricalExpenses,
            dailySafeLimit = data.dailyInsight.dailySafeLimit,
            modifier = Modifier.padding(18.dp),
          )
        }
      }

      // 4. Category Allocation Breakdown
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Alokasi Kategori Bulan Ini",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          TextButton(
            onClick = onNavigateToAutoBudget,
            modifier = Modifier.testTag("edit_budget_link"),
          ) {
            Text("Atur Otomatis")
          }
        }
      }

      val activeCategories = data.categoryBreakdown.filter { it.allocated > 0L || it.spent > 0L }
      if (activeCategories.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Belum ada anggaran kategori atau pengeluaran di bulan ${Formatters.formatMonthTitle(data.monthKey)}.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(18.dp),
            )
          }
        }
      } else {
        items(activeCategories, key = { it.category.name }) { item ->
          CategoryProgressRowCard(item = item)
        }
      }

      // 5. Recent Daily Expenses
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Pengeluaran Terbaru",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          TextButton(
            onClick = onNavigateToExpenses,
            modifier = Modifier.testTag("see_all_expenses_button"),
          ) {
            Text("Lihat Semua")
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
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "Belum ada pengeluaran tercatat",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Ketuk tombol + Catat Pengeluaran di bawah untuk mencatat transaksi harian pertama Anda.",
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
            onClick = { onEditExpense(expense) },
          )
        }
      }
    }
  }
}

@Composable
private fun HeroSummaryCard(
  data: FinanceDashboardData,
  onSetupBudgetClick: () -> Unit,
) {
  val insight = data.dailyInsight
  val budget = data.budget
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  val progressRatio = if (insight.totalBudget > 0L) {
    (insight.totalSpent.toFloat() / insight.totalBudget.toFloat()).coerceIn(0f, 1f)
  } else 0f
  val animatedProgress by animateFloatAsState(
    targetValue = progressRatio,
    animationSpec = tween(durationMillis = 450),
    label = "budget_progress",
  )

  val (statusLabel, statusColor, statusIcon) = when (insight.paceStatus) {
    BudgetPaceStatus.SAFE -> Triple("Aman Terkendali", EmeraldBright, Icons.Rounded.Shield)
    BudgetPaceStatus.CAUTION -> Triple("Mendekati Batas", GoldAccent, Icons.Rounded.WarningAmber)
    BudgetPaceStatus.OVER -> Triple("Melebihi Anggaran", CoralDanger, Icons.Rounded.WarningAmber)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(26.dp))
      .background(Brush.linearGradient(listOf(primary, secondary)))
      .padding(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = if (budget != null) "Sisa Anggaran Belanja" else "Total Pengeluaran Bulan Ini",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (budget != null) {
              Formatters.formatRupiah(insight.remainingBudget)
            } else {
              Formatters.formatRupiah(insight.totalSpent)
            },
            style = MaterialTheme.typography.displaySmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }

        if (budget != null) {
          Surface(
            color = Color.White.copy(alpha = 0.18f),
            shape = RoundedCornerShape(50),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      if (budget != null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(50)),
            color = if (insight.paceStatus == BudgetPaceStatus.OVER) CoralDanger else Color.White,
            trackColor = Color.White.copy(alpha = 0.22f),
            strokeCap = StrokeCap.Round,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Terpakai ${Formatters.formatRupiahCompact(insight.totalSpent)}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Text(
              text = "Dari ${Formatters.formatRupiahCompact(insight.totalBudget)}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
          }
        }

        // Daily Safe-to-Spend & Savings Pill Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Batas Aman Harian",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = Formatters.formatRupiah(insight.dailySafeLimit),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Text(
                text = "Hari ini: ${Formatters.formatRupiahCompact(insight.todaySpent)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
              )
            }
          }

          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp),
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
                  text = "Target Tabungan",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.82f),
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = Formatters.formatRupiah(budget.savingsTarget),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Text(
                text = "${budget.savingsPercent}% pendapatan",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
              )
            }
          }
        }
      } else {
        Text(
          text = "Pengeluaran hari ini: ${Formatters.formatRupiah(insight.todaySpent)}",
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
            text = "Susun Anggaran Bulan Ini Otomatis",
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun AutoBudgetPromptCard(onClick: () -> Unit) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("auto_budget_prompt_card"),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
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
          text = "Anggaran Bulanan Belum Disusun",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Masukkan pendapatan bulanan untuk membagi batas makan, transport, & tabungan secara otomatis.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
        contentDescription = "Buka penyusun anggaran",
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}

@Composable
private fun CategoryProgressRowCard(item: CategoryProgress) {
  val isOver = item.allocated > 0L && item.spent > item.allocated
  val progressColor = if (isOver) CoralDanger else item.category.accentColor
  val animatedProgress by animateFloatAsState(
    targetValue = item.progressFraction.coerceIn(0f, 1f),
    animationSpec = tween(400),
    label = "cat_progress",
  )

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    modifier = Modifier.fillMaxWidth(),
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
        CategoryIconBadge(category = item.category, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.category.labelId,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = if (item.allocated > 0L) {
              if (item.remaining >= 0L) "Sisa ${Formatters.formatRupiah(item.remaining)}"
              else "Lewat ${Formatters.formatRupiah(-item.remaining)}"
            } else {
              "Belum ada batas anggaran"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = Formatters.formatRupiah(item.spent),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (isOver) CoralDanger else MaterialTheme.colorScheme.onSurface,
          )
          if (item.allocated > 0L) {
            Text(
              text = "/ ${Formatters.formatRupiahCompact(item.allocated)}",
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
  onClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CategoryIconBadge(category = expense.categoryEnum, size = 40.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = expense.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${Formatters.formatShortDate(expense.dateEpochDay)} • ${expense.paymentMethodEnum.labelId}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = "-${Formatters.formatRupiah(expense.amount)}",
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
