package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.SubscriptionCycle
import com.example.data.local.SubscriptionItem
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.i18n.CurrencyPreferenceManager
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyPositiveAccent
import com.example.util.Formatters

/**
 * Liquid Glass Subscription Tracker section displaying active recurring memberships,
 * monthly & annualized cost estimates, renewal countdown badges, and 1-tap Log Payment.
 */
@Composable
fun SubscriptionTrackerSection(
  subscriptions: List<SubscriptionItem>,
  onAddSubscription: (
    name: String,
    amount: Long,
    cycle: SubscriptionCycle,
    renewalDay: Int,
    category: ExpenseCategory,
    method: PaymentMethod,
    note: String,
  ) -> Unit,
  onUpdateSubscription: (SubscriptionItem) -> Unit,
  onToggleActive: (SubscriptionItem) -> Unit,
  onDeleteSubscription: (SubscriptionItem) -> Unit,
  onLogPayment: (SubscriptionItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val isDark = rememberIsEffectiveDarkTheme()

  var showAddDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<SubscriptionItem?>(null) }

  val activeSubs = remember(subscriptions) { subscriptions.filter { it.active } }
  val monthlyTotal = remember(activeSubs) { activeSubs.sumOf { it.monthlyEquivalent() } }
  val yearlyTotal = remember(activeSubs) { activeSubs.sumOf { it.yearlyEquivalent() } }

  SculptedSurfaceCard(
    cornerRadius = 24,
    accentColor = SkyPositiveAccent,
    modifier = modifier
      .fillMaxWidth()
      .testTag("subscription_tracker_section"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = SkyPositiveAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
            border = BorderStroke(1.dp, SkyPositiveAccent.copy(alpha = 0.32f)),
            modifier = Modifier.size(38.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Autorenew,
              contentDescription = null,
              tint = SkyPositiveAccent,
              modifier = Modifier.padding(8.dp),
            )
          }
          Column {
            Text(
              text = stringResource(R.string.sub_section_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.sub_section_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        FilledTonalButton(
          onClick = { showAddDialog = true },
          modifier = Modifier
            .height(48.dp)
            .testTag("add_subscription_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(stringResource(R.string.sub_add_button))
        }
      }

      if (subscriptions.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          LiquidGlassStatPill(
            label = stringResource(R.string.sub_monthly_total_label),
            value = Formatters.formatMoney(monthlyTotal, activeCurrency),
            subtitle = stringResource(R.string.sub_active_count, activeSubs.size),
            accent = SkyPositiveAccent,
            modifier = Modifier.weight(1f),
          )
          LiquidGlassStatPill(
            label = stringResource(R.string.sub_yearly_total_label),
            value = Formatters.formatMoney(yearlyTotal, activeCurrency),
            subtitle = null,
            accent = EmeraldSafe,
            modifier = Modifier.weight(1f),
          )
        }
      }

      if (subscriptions.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (isDark) 0.50f else 0.72f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.sub_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          subscriptions.forEachIndexed { index, sub ->
            if (index > 0) {
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
              )
            }
            SubscriptionRowItem(
              item = sub,
              activeCurrency = activeCurrency,
              onLogPayment = { onLogPayment(sub) },
              onToggleActive = { onToggleActive(sub) },
              onEdit = { editingItem = sub },
              onDelete = { onDeleteSubscription(sub) },
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddEditSubscriptionDialog(
      existing = null,
      onDismiss = { showAddDialog = false },
      onSave = { name, amount, cycle, day, cat, method, note ->
        onAddSubscription(name, amount, cycle, day, cat, method, note)
        showAddDialog = false
      },
    )
  }

  editingItem?.let { current ->
    AddEditSubscriptionDialog(
      existing = current,
      onDismiss = { editingItem = null },
      onSave = { name, amount, cycle, day, cat, method, note ->
        onUpdateSubscription(
          current.copy(
            name = name,
            amount = amount,
            billingCycle = cycle,
            renewalDay = day,
            category = cat,
            paymentMethod = method,
            note = note,
          )
        )
        editingItem = null
      },
    )
  }
}

@Composable
private fun LiquidGlassStatPill(
  label: String,
  value: String,
  subtitle: String?,
  accent: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  val isDark = rememberIsEffectiveDarkTheme()
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = accent.copy(alpha = if (isDark) 0.16f else 0.10f),
    border = BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.34f else 0.25f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = accent,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

@Composable
private fun SubscriptionRowItem(
  item: SubscriptionItem,
  activeCurrency: com.example.i18n.AppCurrency,
  onLogPayment: () -> Unit,
  onToggleActive: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val cycleLabel = when (item.billingCycle) {
    SubscriptionCycle.WEEKLY -> stringResource(R.string.sub_cycle_weekly)
    SubscriptionCycle.MONTHLY -> stringResource(R.string.sub_cycle_monthly)
    SubscriptionCycle.YEARLY -> stringResource(R.string.sub_cycle_yearly)
  }
  val daysLeft = item.daysUntilRenewal()
  val statusBadgeText = when {
    !item.active -> stringResource(R.string.sub_status_paused)
    daysLeft == 0 -> stringResource(R.string.sub_renews_today)
    else -> stringResource(R.string.sub_renews_in_days, daysLeft)
  }
  val statusColor = when {
    !item.active -> MaterialTheme.colorScheme.onSurfaceVariant
    daysLeft == 0 -> CoralDanger
    daysLeft <= 3 -> GoldAccent
    else -> EmeraldSafe
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("subscription_item_${item.id}"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CategoryIconBadge(category = item.category, size = 38.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = if (item.active) {
            MaterialTheme.colorScheme.onSurface
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = cycleLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = statusBadgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = Formatters.formatMoney(item.amount, activeCurrency),
          style = MaterialTheme.typography.labelLarge,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          color = if (item.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.billingCycle != SubscriptionCycle.MONTHLY && item.active) {
          Text(
            text = "${Formatters.formatMoney(item.monthlyEquivalent(), activeCurrency)}/mo",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (item.active) {
        TextButton(
          onClick = onLogPayment,
          modifier = Modifier
            .height(48.dp)
            .testTag("log_subscription_${item.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ReceiptLong,
            contentDescription = stringResource(R.string.sub_log_payment_cd),
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(stringResource(R.string.sub_log_payment_action))
        }
      }
      IconButton(
        onClick = onToggleActive,
        modifier = Modifier
          .size(48.dp)
          .testTag("toggle_subscription_${item.id}"),
      ) {
        Icon(
          imageVector = if (item.active) Icons.Rounded.PauseCircleOutline else Icons.Rounded.PlayCircleOutline,
          contentDescription = stringResource(R.string.sub_toggle_active_cd),
          tint = if (item.active) GoldAccent else EmeraldSafe,
          modifier = Modifier.size(20.dp),
        )
      }
      IconButton(
        onClick = onEdit,
        modifier = Modifier
          .size(48.dp)
          .testTag("edit_subscription_${item.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = stringResource(R.string.sub_edit_cd),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .size(48.dp)
          .testTag("delete_subscription_${item.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = stringResource(R.string.sub_delete_cd),
          tint = CoralDanger,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditSubscriptionDialog(
  existing: SubscriptionItem?,
  onDismiss: () -> Unit,
  onSave: (
    name: String,
    amount: Long,
    cycle: SubscriptionCycle,
    renewalDay: Int,
    category: ExpenseCategory,
    method: PaymentMethod,
    note: String,
  ) -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()

  var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
  var amountText by remember(existing) { mutableStateOf(existing?.amount?.toString().orEmpty()) }
  var selectedCycle by remember(existing) { mutableStateOf(existing?.billingCycle ?: SubscriptionCycle.MONTHLY) }
  var renewalDayText by remember(existing) { mutableStateOf((existing?.renewalDay ?: 1).toString()) }
  var selectedCategory by remember(existing) { mutableStateOf(existing?.category ?: ExpenseCategory.ENTERTAINMENT) }
  var selectedMethod by remember(existing) { mutableStateOf(existing?.paymentMethod ?: PaymentMethod.QRIS) }
  var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
  var errorText by remember { mutableStateOf<String?>(null) }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    accentColor = SkyPositiveAccent,
    title = {
      Text(
        text = stringResource(
          if (existing == null) R.string.sub_dialog_add_title else R.string.sub_dialog_edit_title
        ),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it.take(60)
            errorText = null
          },
          label = { Text(stringResource(R.string.sub_name_label)) },
          placeholder = { Text(stringResource(R.string.sub_name_hint)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sub_name_input"),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            amountText = input.filter { it.isDigit() }.take(10)
            errorText = null
          },
          label = { Text(stringResource(R.string.editor_amount_label, activeCurrency.symbol)) },
          prefix = { Text(activeCurrency.prefixWithSpace, fontFamily = JetBrainsMonoFontFamily) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sub_amount_input"),
        )
        Text(
          text = stringResource(R.string.sub_cycle_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SubscriptionCycle.entries.forEach { cycle ->
            val labelRes = when (cycle) {
              SubscriptionCycle.WEEKLY -> R.string.sub_cycle_weekly
              SubscriptionCycle.MONTHLY -> R.string.sub_cycle_monthly
              SubscriptionCycle.YEARLY -> R.string.sub_cycle_yearly
            }
            FilterChip(
              selected = selectedCycle == cycle,
              onClick = { selectedCycle = cycle },
              label = { Text(stringResource(labelRes)) },
            )
          }
        }
        OutlinedTextField(
          value = renewalDayText,
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(2)
            val parsed = digits.toIntOrNull()
            renewalDayText = if (parsed != null && parsed > 31) "31" else digits
          },
          label = { Text(stringResource(R.string.sub_renewal_day_label)) },
          placeholder = { Text("1 – 31") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sub_renewal_day_input"),
        )
        Text(
          text = stringResource(R.string.label_category),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ExpenseCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(stringResource(cat.shortLabelRes)) },
            )
          }
        }
        Text(
          text = stringResource(R.string.label_payment_method),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(stringResource(method.labelRes)) },
            )
          }
        }
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(160) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
          Text(
            text = errorText!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedAmount = amountText.toLongOrNull() ?: 0L
          val parsedDay = (renewalDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
          when {
            name.isBlank() -> errorText = context.getString(R.string.sub_error_name)
            parsedAmount <= 0L -> errorText = context.getString(R.string.sub_error_amount)
            else -> onSave(
              name.trim(),
              parsedAmount,
              selectedCycle,
              parsedDay,
              selectedCategory,
              selectedMethod,
              note.trim(),
            )
          }
        },
        modifier = Modifier
          .height(48.dp)
          .testTag("save_subscription_button"),
      ) {
        Text(stringResource(R.string.action_save))
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
