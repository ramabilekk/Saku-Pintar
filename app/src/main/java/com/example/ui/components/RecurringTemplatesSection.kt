package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.i18n.CurrencyPreferenceManager
import com.example.notifications.BillDueReminderManager
import com.example.notifications.BillDueSchedule
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters

@Composable
fun RecurringTemplatesSection(
  templates: List<RecurringTemplate>,
  onLogTemplate: (RecurringTemplate) -> Unit,
  onAddTemplate: (
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    note: String,
    dueDayOfMonth: Int?,
    autoLogEnabled: Boolean,
  ) -> Unit,
  onDeleteTemplate: (String) -> Unit,
  onScheduleChanged: (templateId: String, schedule: BillDueSchedule?) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val dueSchedules by BillDueReminderManager.observeSchedules(context).collectAsStateWithLifecycle()
  var showAddDialog by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<RecurringTemplate?>(null) }
  var editingScheduleTemplate by remember { mutableStateOf<RecurringTemplate?>(null) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Repeat,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = stringResource(R.string.recurring_section_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
      TextButton(
        onClick = { showAddDialog = true },
        modifier = Modifier
          .height(48.dp)
          .testTag("add_recurring_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.recurring_add_action))
      }
    }

    if (templates.isEmpty()) {
      SculptedSurfaceCard(
        cornerRadius = 16,
        onClick = { showAddDialog = true },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = stringResource(R.string.recurring_empty_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        templates.forEach { template ->
          LiquidGlassDialogSurface(
            cornerRadius = 18,
            accentColor = template.categoryEnum.accentColor,
            modifier = Modifier
              .widthIn(min = 176.dp, max = 230.dp)
              .clip(RoundedCornerShape(18.dp))
              .clickable { onLogTemplate(template) }
              .testTag("recurring_card_${template.id}"),
          ) {
            Row(
              modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CategoryIconBadge(category = template.categoryEnum, size = 34.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = template.title,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = Formatters.formatMoney(template.amount, activeCurrency),
                  style = MaterialTheme.typography.labelMedium,
                  fontFamily = JetBrainsMonoFontFamily,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                )
                val schedule = dueSchedules[template.id]
                if (schedule != null) {
                  val daysLeft = schedule.daysUntilDue()
                  val (badgeText, badgeColor) = when {
                    daysLeft == 0 -> stringResource(R.string.bill_due_today_badge) to CoralDanger
                    daysLeft <= 3 -> stringResource(R.string.bill_due_in_days_badge, daysLeft) to GoldAccent
                    else -> stringResource(R.string.bill_due_day_format, schedule.dueDayOfMonth) to MaterialTheme.colorScheme.primary
                  }
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.NotificationsActive,
                      contentDescription = null,
                      tint = badgeColor,
                      modifier = Modifier.size(12.dp),
                    )
                    Text(
                      text = badgeText,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = badgeColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    if (schedule.autoLogEnabled) {
                      Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldSafe.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, EmeraldSafe.copy(alpha = 0.45f)),
                      ) {
                        Text(
                          text = stringResource(R.string.bill_autolog_badge),
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = EmeraldSafe,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                      }
                    }
                  }
                } else {
                  Text(
                    text = stringResource(R.string.recurring_tap_to_log),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              IconButton(
                onClick = { editingScheduleTemplate = template },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("edit_bill_due_${template.id}"),
              ) {
                val hasReminder = dueSchedules[template.id] != null
                Icon(
                  imageVector = if (hasReminder) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                  contentDescription = stringResource(R.string.bill_due_edit_cd, template.title),
                  tint = if (hasReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(
                onClick = { pendingDelete = template },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("delete_recurring_${template.id}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "${stringResource(R.string.action_delete)} ${template.title}",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddRecurringTemplateDialog(
      onDismiss = { showAddDialog = false },
      onSave = { title, amount, category, method, note, dueDay, autoLog ->
        onAddTemplate(title, amount, category, method, note, dueDay, autoLog)
        showAddDialog = false
      },
    )
  }

  editingScheduleTemplate?.let { target ->
    val currentSchedule = dueSchedules[target.id]
    var dueDayText by remember(target.id) {
      mutableStateOf(currentSchedule?.dueDayOfMonth?.toString().orEmpty())
    }
    var autoLogEnabled by remember(target.id) {
      mutableStateOf(currentSchedule?.autoLogEnabled ?: false)
    }
    val parsedDueDay = dueDayText.toIntOrNull()?.takeIf { it in 1..31 }
    LiquidGlassAlertDialog(
      onDismissRequest = { editingScheduleTemplate = null },
      title = {
        Text(
          text = stringResource(R.string.bill_due_edit_title, target.title),
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.bill_due_edit_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = dueDayText,
            onValueChange = { raw ->
              val digits = raw.filter { it.isDigit() }.take(2)
              val parsed = digits.toIntOrNull()
              dueDayText = if (parsed != null && parsed > 31) "31" else digits
            },
            label = { Text(stringResource(R.string.bill_due_day_input_label)) },
            placeholder = { Text(stringResource(R.string.bill_due_day_input_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("edit_bill_due_day_input"),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = stringResource(R.string.bill_autolog_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.bill_autolog_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = autoLogEnabled && parsedDueDay != null,
              onCheckedChange = { enabled ->
                autoLogEnabled = enabled
                if (enabled && parsedDueDay == null) {
                  dueDayText = "1"
                }
              },
              modifier = Modifier.testTag("edit_bill_autolog_switch"),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val day = dueDayText.toIntOrNull()?.takeIf { it in 1..31 }
            val saved = BillDueReminderManager.setDueSchedule(
              context = context,
              templateId = target.id,
              title = target.title,
              dueDayOfMonth = day,
              reminderEnabled = day != null,
              autoLogEnabled = day != null && autoLogEnabled,
            )
            onScheduleChanged(target.id, saved)
            editingScheduleTemplate = null
          },
          modifier = Modifier.testTag("save_bill_due_button"),
        ) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (currentSchedule != null) {
            TextButton(
              onClick = {
                BillDueReminderManager.removeDueSchedule(context, target.id)
                onScheduleChanged(target.id, null)
                editingScheduleTemplate = null
              },
              modifier = Modifier.testTag("clear_bill_due_button"),
            ) {
              Text(stringResource(R.string.bill_due_clear_action), color = CoralDanger)
            }
          }
          TextButton(onClick = { editingScheduleTemplate = null }) {
            Text(stringResource(R.string.action_cancel))
          }
        }
      },
    )
  }

  pendingDelete?.let { target ->
    LiquidGlassAlertDialog(
      onDismissRequest = { pendingDelete = null },
      accentColor = CoralDanger,
      title = { Text(stringResource(R.string.recurring_delete_title)) },
      text = {
        Text(
          stringResource(
            R.string.recurring_delete_message,
            target.title,
            Formatters.formatMoney(target.amount, activeCurrency),
          )
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteTemplate(target.id)
            pendingDelete = null
          },
        ) {
          Text(stringResource(R.string.action_delete), color = CoralDanger)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRecurringTemplateDialog(
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    note: String,
    dueDayOfMonth: Int?,
    autoLogEnabled: Boolean,
  ) -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()

  var title by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(ExpenseCategory.BILLS) }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.TRANSFER) }
  var note by remember { mutableStateOf("") }
  var dueDayText by remember { mutableStateOf("") }
  var autoLogEnabled by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.recurring_add_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it.take(80)
            validationError = null
          },
          label = { Text(stringResource(R.string.recurring_name_label)) },
          placeholder = { Text(stringResource(R.string.recurring_name_hint)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("recurring_title_input"),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            amountText = input.filter { it.isDigit() }.take(10)
            validationError = null
          },
          label = { Text(stringResource(R.string.editor_amount_label, activeCurrency.symbol)) },
          prefix = { Text(activeCurrency.prefixWithSpace, fontFamily = JetBrainsMonoFontFamily) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("recurring_amount_input"),
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
          value = dueDayText,
          onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(2)
            val parsed = digits.toIntOrNull()
            dueDayText = if (parsed != null && parsed > 31) "31" else digits
          },
          label = { Text(stringResource(R.string.bill_due_day_label)) },
          placeholder = { Text("1 – 31") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("recurring_due_day_input"),
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
            FilterChip(
              selected = dueDayText.toIntOrNull() == day,
              onClick = {
                dueDayText = if (dueDayText.toIntOrNull() == day) "" else day.toString()
              },
              label = { Text(stringResource(R.string.bill_due_day_format, day)) },
            )
          }
        }
        val parsedDue = dueDayText.toIntOrNull()?.takeIf { it in 1..31 }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = stringResource(R.string.bill_autolog_title),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = stringResource(R.string.bill_autolog_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = autoLogEnabled && parsedDue != null,
            onCheckedChange = { enabled ->
              autoLogEnabled = enabled
              if (enabled && parsedDue == null) {
                dueDayText = "1"
              }
            },
            modifier = Modifier.testTag("add_recurring_autolog_switch"),
          )
        }
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(200) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (validationError != null) {
          Text(
            text = validationError!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = amountText.toLongOrNull() ?: 0L
          val dueDay = dueDayText.toIntOrNull()?.takeIf { it in 1..31 }
          when {
            title.isBlank() -> validationError = context.getString(R.string.recurring_error_name)
            parsed <= 0L -> validationError = context.getString(R.string.recurring_error_amount)
            else -> onSave(
              title.trim(),
              parsed,
              selectedCategory,
              selectedMethod,
              note.trim(),
              dueDay,
              dueDay != null && autoLogEnabled,
            )
          }
        },
        modifier = Modifier
          .height(48.dp)
          .testTag("save_recurring_button"),
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
