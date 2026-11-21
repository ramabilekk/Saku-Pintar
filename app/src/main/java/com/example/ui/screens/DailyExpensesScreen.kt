package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.budget.PaydayCycleManager
import com.example.data.local.CustomCategoryManager
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.security.PrivacySecurityManager
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DimensionalVaultCard
import com.example.ui.components.MonthlyCalendarCard
import com.example.ui.components.ReceiptPhotoViewerDialog
import com.example.ui.components.ReceiptThumbnailBadge
import com.example.ui.components.RecurringTemplatesSection
import com.example.ui.components.SculptedSurfaceCard
import com.example.ui.components.iconVector
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianInk
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters

@Composable
fun DailyExpensesScreen(
  data: FinanceDashboardData,
  onSearchQueryChange: (String) -> Unit,
  onCategoryFilterSelect: (ExpenseCategory?) -> Unit,
  onEditExpense: (Expense) -> Unit,
  onDeleteExpense: (String) -> Unit,
  onLogRecurringTemplate: (RecurringTemplate) -> Unit,
  onAddRecurringTemplate: (
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    note: String,
    dueDayOfMonth: Int?,
  ) -> Unit,
  onDeleteRecurringTemplate: (String) -> Unit,
  onOpenExportReport: () -> Unit,
  onPreviousMonth: () -> Unit = {},
  onNextMonth: () -> Unit = {},
  onSelectMonth: (String) -> Unit = {},
  onAddExpenseOnDate: (Long) -> Unit = {},
  onQuickScanReceipt: () -> Unit = {},
  onDuplicateExpense: (Expense) -> Unit = {},
  onDeleteExpenseWithUndo: ((Expense) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val customCategories by CustomCategoryManager.observeCategories(context).collectAsStateWithLifecycle()
  val hideBalances by PrivacySecurityManager.observeHideBalances(context).collectAsStateWithLifecycle()
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }

  var pendingDeleteExpense by remember { mutableStateOf<Expense?>(null) }
  var viewingReceiptExpense by remember { mutableStateOf<Expense?>(null) }
  var selectedDateEpochDay by remember(data.monthKey) { mutableStateOf<Long?>(null) }
  var selectedPaymentFilter by remember { mutableStateOf<PaymentMethod?>(null) }
  var receiptOnlyFilter by remember { mutableStateOf(false) }
  var sortOption by remember { mutableStateOf(ExpenseSortOption.NEWEST) }
  var showSortMenu by remember { mutableStateOf(false) }

  val hasActiveExtraFilters = selectedPaymentFilter != null ||
    receiptOnlyFilter ||
    sortOption != ExpenseSortOption.NEWEST ||
    selectedDateEpochDay != null ||
    data.selectedCategoryFilter != null ||
    data.searchQuery.isNotBlank()

  val visibleExpenses = remember(
    data.filteredExpenses,
    selectedDateEpochDay,
    selectedPaymentFilter,
    receiptOnlyFilter,
    sortOption,
  ) {
    val targetDay = selectedDateEpochDay
    val filtered = data.filteredExpenses.filter { exp ->
      val matchesDay = targetDay == null || exp.dateEpochDay == targetDay
      val matchesPayment = selectedPaymentFilter == null || exp.paymentMethodEnum == selectedPaymentFilter
      val matchesReceipt = !receiptOnlyFilter || exp.hasReceiptPhoto
      matchesDay && matchesPayment && matchesReceipt
    }
    when (sortOption) {
      ExpenseSortOption.NEWEST -> filtered.sortedByDescending { it.dateEpochDay }
      ExpenseSortOption.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
      ExpenseSortOption.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
      ExpenseSortOption.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
    }
  }

  val groupedByDay = remember(visibleExpenses) {
    visibleExpenses.groupBy { it.dateEpochDay }
      .toSortedMap(compareByDescending { it })
  }
  val totalFiltered = remember(visibleExpenses) {
    visibleExpenses.sumOf { it.amount }
  }
  val attachedReceiptsCount = remember(visibleExpenses) {
    visibleExpenses.count { it.hasReceiptPhoto }
  }
  val paydayCycle = remember(data.monthKey, data.paydayStartDay) {
    PaydayCycleManager.resolveCycle(data.monthKey, data.paydayStartDay)
  }

  val todayLabel = stringResource(R.string.label_today)
  val yesterdayLabel = stringResource(R.string.label_yesterday)

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxSize(),
    ) {
      // Compact Sticky Search & Category Filter Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = data.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text(stringResource(R.string.search_expenses_hint)) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = stringResource(R.string.search_cd),
            )
          },
          trailingIcon = {
            AnimatedVisibility(
              visible = data.searchQuery.isNotEmpty(),
              enter = fadeIn(tween(150)) + scaleIn(tween(150)),
              exit = fadeOut(tween(120)) + scaleOut(tween(120)),
            ) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = stringResource(R.string.clear_search_cd),
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
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
            selected = data.selectedCategoryFilter == null && data.searchQuery.isBlank(),
            onClick = {
              onCategoryFilterSelect(null)
              if (data.searchQuery.startsWith("#")) onSearchQueryChange("")
            },
            label = { Text(stringResource(R.string.label_all)) },
          )
          ExpenseCategory.entries.forEach { category ->
            val selected = data.selectedCategoryFilter == category
            FilterChip(
              selected = selected,
              onClick = { onCategoryFilterSelect(category) },
              label = { Text(stringResource(category.shortLabelRes)) },
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
          customCategories.forEach { customCat ->
            val isSelected = data.searchQuery.trim().equals(customCat.noteTag, ignoreCase = true)
            FilterChip(
              selected = isSelected,
              onClick = {
                if (isSelected) {
                  onSearchQueryChange("")
                } else {
                  onCategoryFilterSelect(null)
                  onSearchQueryChange(customCat.noteTag)
                }
              },
              label = { Text(customCat.name) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Label,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = customCat.accentColor.copy(alpha = 0.18f),
                selectedLabelColor = customCat.accentColor,
                selectedLeadingIconColor = customCat.accentColor,
              ),
              modifier = Modifier.testTag("filter_custom_cat_${customCat.id}"),
            )
          }
        }

        // Secondary Advanced Filter & Sort Row (Sort dropdown, Has Receipt, Payment Methods, Reset)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box {
            FilterChip(
              selected = sortOption != ExpenseSortOption.NEWEST,
              onClick = { showSortMenu = true },
              label = {
                Text("${stringResource(R.string.sort_label_prefix)} ${stringResource(sortOption.labelRes)}")
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Sort,
                  contentDescription = stringResource(R.string.filter_sort_cd),
                  modifier = Modifier.size(16.dp),
                )
              },
              modifier = Modifier.testTag("expense_sort_chip"),
            )
            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false },
            ) {
              ExpenseSortOption.entries.forEach { option ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = stringResource(option.labelRes),
                      fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                  onClick = {
                    sortOption = option
                    showSortMenu = false
                  },
                  modifier = Modifier.testTag("sort_option_${option.name}"),
                )
              }
            }
          }

          FilterChip(
            selected = receiptOnlyFilter,
            onClick = { receiptOnlyFilter = !receiptOnlyFilter },
            label = { Text(stringResource(R.string.filter_with_receipt)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Receipt,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
              )
            },
            modifier = Modifier.testTag("filter_receipt_only_chip"),
          )

          PaymentMethod.entries.forEach { method ->
            val isSelected = selectedPaymentFilter == method
            FilterChip(
              selected = isSelected,
              onClick = {
                selectedPaymentFilter = if (isSelected) null else method
              },
              label = { Text(stringResource(method.labelRes)) },
              modifier = Modifier.testTag("filter_payment_${method.name}"),
            )
          }

          if (hasActiveExtraFilters) {
            FilterChip(
              selected = false,
              onClick = {
                selectedPaymentFilter = null
                receiptOnlyFilter = false
                sortOption = ExpenseSortOption.NEWEST
                selectedDateEpochDay = null
                onCategoryFilterSelect(null)
                onSearchQueryChange("")
              },
              label = { Text(stringResource(R.string.filter_reset)) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.FilterAltOff,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
              },
              modifier = Modifier.testTag("filter_reset_chip"),
            )
          }
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 148.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Dimensional Vault Summary & Export Card
        item(key = "filtered_summary_and_export") {
          DimensionalVaultCard {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = selectedDateEpochDay?.let { epoch ->
                      stringResource(
                        R.string.calendar_selected_date_filter,
                        Formatters.formatShortDate(epoch, activeLocale),
                      )
                    } ?: stringResource(
                      R.string.expenses_count_in_month,
                      visibleExpenses.size,
                      Formatters.formatMonthTitle(data.monthKey, activeLocale),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.88f),
                  )
                  if (paydayCycle.isCustomCycle && selectedDateEpochDay == null) {
                    Text(
                      text = stringResource(R.string.payday_badge_label, paydayCycle.formatRangeLabel(activeLocale)),
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFFFDE68A),
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color.White.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Receipt,
                      contentDescription = null,
                      tint = Color(0xFF6EE7B7),
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = stringResource(R.string.report_receipt_attached_count, attachedReceiptsCount),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                    )
                  }
                }
              }

              AnimatedContent(
                targetState = totalFiltered,
                transitionSpec = {
                  fadeIn(tween(200)).togetherWith(fadeOut(tween(140)))
                },
                label = "daily_total_filtered_anim",
              ) { sum ->
                Text(
                  text = Formatters.formatMoneyMasked(sum, activeCurrency, hideBalances),
                  style = MaterialTheme.typography.headlineSmall,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                FilledTonalButton(
                  onClick = onQuickScanReceipt,
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmeraldSafe,
                    contentColor = Color.White,
                  ),
                  modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .testTag("expenses_scan_receipt_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.ai_scan_quick_title),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                OutlinedButton(
                  onClick = onOpenExportReport,
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("export_report_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.TableChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.export_report_action),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        item(key = "monthly_calendar_card") {
          MonthlyCalendarCard(
            monthKey = data.monthKey,
            expenses = data.expenses,
            dailySafeLimit = data.dailyInsight.dailySafeLimit,
            selectedEpochDay = selectedDateEpochDay,
            currency = activeCurrency,
            locale = activeLocale,
            onSelectEpochDay = { selectedDateEpochDay = it },
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectMonth = onSelectMonth,
            onAddExpenseOnDate = onAddExpenseOnDate,
            modifier = Modifier.padding(bottom = 2.dp),
          )
        }

        item(key = "recurring_templates_section") {
          RecurringTemplatesSection(
            templates = data.recurringTemplates,
            onLogTemplate = onLogRecurringTemplate,
            onAddTemplate = onAddRecurringTemplate,
            onDeleteTemplate = onDeleteRecurringTemplate,
            modifier = Modifier.padding(bottom = 4.dp),
          )
        }

        if (groupedByDay.isEmpty()) {
          item(key = "empty_expenses_state") {
            SculptedSurfaceCard(
              accentColor = MaterialTheme.colorScheme.primary,
              cornerRadius = 22,
              modifier = Modifier.padding(vertical = 8.dp),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
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
                  text = stringResource(R.string.empty_expenses_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = stringResource(R.string.empty_expenses_body),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else if (sortOption != ExpenseSortOption.NEWEST) {
          items(visibleExpenses, key = { it.id }) { expense ->
            DetailedExpenseCard(
              expense = expense,
              currency = activeCurrency,
              hideBalances = hideBalances,
              onEdit = { onEditExpense(expense) },
              onViewReceipt = { viewingReceiptExpense = expense },
              onDuplicate = { onDuplicateExpense(expense) },
              onRequestDelete = {
                if (onDeleteExpenseWithUndo != null) {
                  onDeleteExpenseWithUndo(expense)
                } else {
                  pendingDeleteExpense = expense
                }
              },
            )
          }
        } else {
          groupedByDay.forEach { (epochDay, dayExpenses) ->
            val dayTotal = dayExpenses.sumOf { it.amount }
            item(key = "header_$epochDay") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = Formatters.formatDateGroupLabel(
                    epochDay = epochDay,
                    todayLabel = todayLabel,
                    yesterdayLabel = yesterdayLabel,
                    locale = activeLocale,
                  ),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                )
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                  Text(
                    text = Formatters.formatMoneyMasked(dayTotal, activeCurrency, hideBalances),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }

            items(dayExpenses, key = { it.id }) { expense ->
              DetailedExpenseCard(
                expense = expense,
                currency = activeCurrency,
                hideBalances = hideBalances,
                onEdit = { onEditExpense(expense) },
                onViewReceipt = { viewingReceiptExpense = expense },
                onDuplicate = { onDuplicateExpense(expense) },
                onRequestDelete = {
                  if (onDeleteExpenseWithUndo != null) {
                    onDeleteExpenseWithUndo(expense)
                  } else {
                    pendingDeleteExpense = expense
                  }
                },
              )
            }
          }
        }
      }
    }
  }

  viewingReceiptExpense?.let { expense ->
    ReceiptPhotoViewerDialog(
      receiptPhotoBase64 = expense.receiptPhotoBase64,
      title = expense.title,
      subtitle = "${Formatters.formatShortDate(expense.dateEpochDay, activeLocale)} • ${stringResource(expense.paymentMethodEnum.labelRes)}",
      amountFormatted = Formatters.formatMoneyMasked(expense.amount, activeCurrency, hideBalances),
      onDismiss = { viewingReceiptExpense = null },
    )
  }

  pendingDeleteExpense?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteExpense = null },
      title = { Text(stringResource(R.string.delete_expense_title)) },
      text = {
        Text(
          stringResource(
            R.string.delete_expense_message,
            target.title,
            Formatters.formatMoney(target.amount, activeCurrency),
          ),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteExpense(target.id)
            pendingDeleteExpense = null
          },
        ) {
          Text(stringResource(R.string.action_delete), color = CoralDanger)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteExpense = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun DetailedExpenseCard(
  expense: Expense,
  currency: AppCurrency,
  hideBalances: Boolean,
  onEdit: () -> Unit,
  onViewReceipt: () -> Unit,
  onDuplicate: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  SculptedSurfaceCard(
    cornerRadius = 18,
    onClick = onEdit,
    modifier = Modifier.testTag("expense_card_${expense.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CategoryIconBadge(category = expense.categoryEnum, size = 44.dp)

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = expense.title,
          style = MaterialTheme.typography.titleSmall,
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
            color = expense.categoryEnum.accentColor.copy(alpha = 0.14f),
          ) {
            Text(
              text = stringResource(expense.categoryEnum.shortLabelRes),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = expense.categoryEnum.accentColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "• ${stringResource(expense.paymentMethodEnum.labelRes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (expense.note.isNotBlank()) {
          Text(
            text = expense.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (expense.hasReceiptPhoto) {
          Spacer(modifier = Modifier.height(2.dp))
          ReceiptThumbnailBadge(
            receiptPhotoBase64 = expense.receiptPhotoBase64,
            onClick = onViewReceipt,
            modifier = Modifier.testTag("expense_receipt_badge_${expense.id}"),
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = if (hideBalances) {
            Formatters.formatMoneyMasked(expense.amount, currency, true)
          } else {
            "-${Formatters.formatMoney(expense.amount, currency)}"
          },
          style = MaterialTheme.typography.titleSmall,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
          IconButton(
            onClick = onDuplicate,
            modifier = Modifier
              .size(48.dp)
              .testTag("duplicate_expense_${expense.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.ContentCopy,
              contentDescription = "${stringResource(R.string.duplicate_expense_cd)} ${expense.title}",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(
            onClick = onRequestDelete,
            modifier = Modifier
              .size(48.dp)
              .testTag("delete_expense_${expense.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = "${stringResource(R.string.action_delete)} ${expense.title}",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

private enum class ExpenseSortOption(val labelRes: Int) {
  NEWEST(R.string.sort_newest),
  HIGHEST_AMOUNT(R.string.sort_highest),
  LOWEST_AMOUNT(R.string.sort_lowest),
  TITLE_AZ(R.string.sort_title_az),
}

