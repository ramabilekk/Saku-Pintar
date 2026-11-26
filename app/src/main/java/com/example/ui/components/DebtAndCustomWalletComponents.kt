package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomWallet
import com.example.data.local.CustomWalletManager
import com.example.data.local.CustomWalletType
import com.example.data.local.DebtRecord
import com.example.data.local.DebtType
import com.example.i18n.AppCurrency
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianInk
import com.example.util.Formatters
import java.time.LocalDate
import java.util.Locale

private fun CustomWalletType.iconVector(): ImageVector = when (this) {
  CustomWalletType.BANK -> Icons.Rounded.AccountBalance
  CustomWalletType.EWALLET -> Icons.Rounded.AccountBalanceWallet
  CustomWalletType.CASH -> Icons.Rounded.Payments
  CustomWalletType.CARD -> Icons.Rounded.CreditCard
  CustomWalletType.INVESTMENT -> Icons.Rounded.TrendingUp
}

private fun CustomWalletType.labelRes(): Int = when (this) {
  CustomWalletType.BANK -> R.string.custom_wallet_type_bank
  CustomWalletType.EWALLET -> R.string.custom_wallet_type_ewallet
  CustomWalletType.CASH -> R.string.custom_wallet_type_cash
  CustomWalletType.CARD -> R.string.custom_wallet_type_card
  CustomWalletType.INVESTMENT -> R.string.custom_wallet_type_investment
}

@Composable
fun CustomWalletsCardSection(
  wallets: List<CustomWallet>,
  currency: AppCurrency,
  hideBalances: Boolean,
  onAddClick: () -> Unit,
  onEditClick: (CustomWallet) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.custom_wallets_section_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = stringResource(R.string.custom_wallets_section_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      FilledTonalButton(
        onClick = onAddClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("add_custom_wallet_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = stringResource(R.string.custom_wallets_add_button),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    if (wallets.isEmpty()) {
      SculptedSurfaceCard(cornerRadius = 18) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.16f else 0.10f),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Text(
            text = stringResource(R.string.custom_wallets_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
        }
      }
    } else {
      wallets.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          pair.forEach { wallet ->
            CustomWalletItemCard(
              wallet = wallet,
              currency = currency,
              hideBalances = hideBalances,
              onClick = { onEditClick(wallet) },
              modifier = Modifier.weight(1f),
            )
          }
          if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun CustomWalletItemCard(
  wallet: CustomWallet,
  currency: AppCurrency,
  hideBalances: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = wallet.accentColor
  SculptedSurfaceCard(
    cornerRadius = 18,
    accentColor = accent,
    onClick = onClick,
    modifier = modifier.testTag("custom_wallet_card_${wallet.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = 0.15f),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.30f)),
            modifier = Modifier.size(32.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = wallet.walletType.iconVector(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = wallet.name,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(wallet.walletType.labelRes()),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = "${stringResource(R.string.action_edit)} ${wallet.name}",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = Formatters.formatMoneyMasked(wallet.balance, currency, hideBalances),
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCustomWalletDialog(
  existingWallet: CustomWallet?,
  currency: AppCurrency,
  onSave: (name: String, type: CustomWalletType, balance: Long, colorHex: Long, existingId: String?) -> Unit,
  onDelete: ((String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var name by remember(existingWallet) { mutableStateOf(existingWallet?.name ?: "") }
  var selectedType by remember(existingWallet) {
    mutableStateOf(existingWallet?.walletType ?: CustomWalletType.BANK)
  }
  var balanceText by remember(existingWallet) {
    mutableStateOf(
      if (existingWallet != null && existingWallet.balance > 0L) {
        existingWallet.balance.toString()
      } else {
        ""
      }
    )
  }
  var selectedColor by remember(existingWallet) {
    mutableLongStateOf(existingWallet?.colorHex ?: CustomWalletManager.presetColorPalette.first())
  }
  var errorText by remember { mutableStateOf<String?>(null) }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(
          if (existingWallet == null) {
            R.string.custom_wallet_dialog_add_title
          } else {
            R.string.custom_wallet_dialog_edit_title
          }
        ),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it.take(40)
            errorText = null
          },
          label = { Text(stringResource(R.string.custom_wallet_name_label)) },
          placeholder = { Text(stringResource(R.string.custom_wallet_name_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_wallet_name_input"),
        )

        OutlinedTextField(
          value = balanceText,
          onValueChange = { input ->
            balanceText = input.filter { it.isDigit() }.take(11)
          },
          label = { Text(stringResource(R.string.custom_wallet_balance_label, currency.symbol)) },
          prefix = {
            Text(
              text = currency.prefixWithSpace,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          },
          placeholder = { Text("0") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_wallet_balance_input"),
        )

        Text(
          text = stringResource(R.string.custom_wallet_type_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CustomWalletType.entries.forEach { walletType ->
            FilterChip(
              selected = selectedType == walletType,
              onClick = { selectedType = walletType },
              leadingIcon = {
                Icon(
                  imageVector = walletType.iconVector(),
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(stringResource(walletType.labelRes())) },
              modifier = Modifier.testTag("custom_wallet_type_${walletType.name}"),
            )
          }
        }

        Text(
          text = stringResource(R.string.custom_wallet_color_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
          CustomWalletManager.presetColorPalette.forEach { hex ->
            val isChosen = selectedColor == hex
            Surface(
              onClick = { selectedColor = hex },
              shape = CircleShape,
              color = Color(hex),
              border = if (isChosen) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
              modifier = Modifier.size(34.dp),
            ) {
              if (isChosen) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }

        if (errorText != null) {
          Text(
            text = errorText!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }

        if (existingWallet != null && onDelete != null) {
          OutlinedButton(
            onClick = { onDelete(existingWallet.id) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("delete_custom_wallet_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_delete), fontWeight = FontWeight.SemiBold)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            errorText = context.getString(R.string.custom_wallet_error_name)
          } else {
            onSave(
              name.trim(),
              selectedType,
              balanceText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
              selectedColor,
              existingWallet?.id,
            )
          }
        },
        modifier = Modifier
          .height(48.dp)
          .testTag("save_custom_wallet_button"),
      ) {
        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

private enum class DebtFilter {
  ACTIVE,
  ALL,
  SETTLED,
}

@Composable
fun DebtTrackerSection(
  records: List<DebtRecord>,
  currency: AppCurrency,
  locale: Locale,
  hideBalances: Boolean,
  onAddClick: () -> Unit,
  onToggleSettled: (DebtRecord) -> Unit,
  onDelete: (DebtRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  var filter by remember { mutableStateOf(DebtFilter.ACTIVE) }
  val todayEpoch = remember { LocalDate.now().toEpochDay() }

  val totalOwedToMe = remember(records) {
    records.filter { !it.settled && it.type == DebtType.OWED_TO_ME }.sumOf { it.amount }
  }
  val totalIOwe = remember(records) {
    records.filter { !it.settled && it.type == DebtType.I_OWE }.sumOf { it.amount }
  }
  val visibleRecords = remember(records, filter) {
    when (filter) {
      DebtFilter.ACTIVE -> records.filter { !it.settled }
      DebtFilter.SETTLED -> records.filter { it.settled }
      DebtFilter.ALL -> records
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("debt_tracker_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.debt_section_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = stringResource(R.string.debt_section_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      FilledTonalButton(
        onClick = onAddClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("add_debt_record_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = stringResource(R.string.debt_add_button),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    // Two-column Summary Strip: Owed to Me vs I Owe
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      DebtSummaryMiniCard(
        label = stringResource(R.string.debt_summary_owed_to_me),
        amount = totalOwedToMe,
        accent = EmeraldSafe,
        currency = currency,
        hideBalances = hideBalances,
        modifier = Modifier.weight(1f),
      )
      DebtSummaryMiniCard(
        label = stringResource(R.string.debt_summary_i_owe),
        amount = totalIOwe,
        accent = CoralDanger,
        currency = currency,
        hideBalances = hideBalances,
        modifier = Modifier.weight(1f),
      )
    }

    if (records.isNotEmpty()) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        listOf(
          DebtFilter.ACTIVE to R.string.debt_filter_active,
          DebtFilter.ALL to R.string.debt_filter_all,
          DebtFilter.SETTLED to R.string.debt_filter_settled,
        ).forEach { (option, labelRes) ->
          FilterChip(
            selected = filter == option,
            onClick = { filter = option },
            label = { Text(stringResource(labelRes)) },
            modifier = Modifier.testTag("debt_filter_${option.name}"),
          )
        }
      }
    }

    if (visibleRecords.isEmpty()) {
      SculptedSurfaceCard(cornerRadius = 18) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = GoldAccent.copy(alpha = if (isDarkTheme) 0.16f else 0.12f),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Text(
            text = stringResource(R.string.debt_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
        }
      }
    } else {
      SculptedSurfaceCard(cornerRadius = 20) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          visibleRecords.forEach { record ->
            val isReceivable = record.type == DebtType.OWED_TO_ME
            val accent = when {
              record.settled -> MaterialTheme.colorScheme.onSurfaceVariant
              isReceivable -> EmeraldSafe
              else -> CoralDanger
            }
            val isOverdue = !record.settled && record.dueDateEpochDay < todayEpoch
            val dueText = stringResource(
              R.string.debt_due_label,
              Formatters.formatShortDate(record.dueDateEpochDay, locale),
            )
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (record.settled) 0.28f else 0.44f),
              border = BorderStroke(
                1.dp,
                if (isOverdue) CoralDanger.copy(alpha = 0.45f) else accent.copy(alpha = 0.22f),
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("debt_record_row_${record.id}"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                IconButton(
                  onClick = { onToggleSettled(record) },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("debt_toggle_settled_${record.id}"),
                ) {
                  Icon(
                    imageVector = if (record.settled) {
                      Icons.Rounded.CheckCircle
                    } else {
                      Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = stringResource(R.string.debt_mark_settled_cd),
                    tint = if (record.settled) EmeraldSafe else accent,
                    modifier = Modifier.size(22.dp),
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = record.personName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = if (record.settled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      } else {
                        MaterialTheme.colorScheme.onSurface
                      },
                      textDecoration = if (record.settled) TextDecoration.LineThrough else TextDecoration.None,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false),
                    )
                    if (record.settled) {
                      Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldSafe.copy(alpha = 0.16f),
                      ) {
                        Text(
                          text = stringResource(R.string.debt_status_settled),
                          style = MaterialTheme.typography.labelSmall,
                          color = EmeraldSafe,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                    } else if (isOverdue) {
                      Surface(
                        shape = RoundedCornerShape(50),
                        color = CoralDanger.copy(alpha = 0.16f),
                      ) {
                        Text(
                          text = stringResource(R.string.debt_status_overdue),
                          style = MaterialTheme.typography.labelSmall,
                          color = CoralDanger,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                    }
                  }
                  Text(
                    text = buildString {
                      append(
                        stringResource(
                          if (isReceivable) R.string.debt_summary_owed_to_me else R.string.debt_summary_i_owe,
                        )
                      )
                      append(" • ")
                      append(dueText)
                      if (record.note.isNotBlank()) {
                        append(" • ")
                        append(record.note)
                      }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Text(
                  text = Formatters.formatMoneyMasked(record.amount, currency, hideBalances),
                  style = MaterialTheme.typography.labelLarge,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  color = accent,
                )

                IconButton(
                  onClick = { onDelete(record) },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("debt_delete_${record.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete),
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
  }
}

@Composable
private fun DebtSummaryMiniCard(
  label: String,
  amount: Long,
  accent: Color,
  currency: AppCurrency,
  hideBalances: Boolean,
  modifier: Modifier = Modifier,
) {
  SculptedSurfaceCard(
    cornerRadius = 16,
    accentColor = accent,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = Formatters.formatMoneyMasked(amount, currency, hideBalances),
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDebtRecordDialog(
  currency: AppCurrency,
  onSave: (personName: String, type: DebtType, amount: Long, dueDateEpochDay: Long, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val today = remember { LocalDate.now() }

  var selectedType by remember { mutableStateOf(DebtType.OWED_TO_ME) }
  var personName by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var dueDaysOffset by remember { mutableIntStateOf(7) }
  var note by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  // Inline Split Bill Calculator state
  var expandSplitBill by remember { mutableStateOf(false) }
  var splitTotalBillText by remember { mutableStateOf("") }
  var splitPeopleCount by remember { mutableIntStateOf(2) }
  val splitPerPerson = remember(splitTotalBillText, splitPeopleCount) {
    val total = splitTotalBillText.toLongOrNull() ?: 0L
    if (total > 0L && splitPeopleCount >= 2) total / splitPeopleCount else 0L
  }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.debt_dialog_title),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = selectedType == DebtType.OWED_TO_ME,
            onClick = { selectedType = DebtType.OWED_TO_ME },
            label = { Text(stringResource(R.string.debt_type_owed_to_me)) },
            modifier = Modifier.testTag("debt_type_owed_to_me_chip"),
          )
          FilterChip(
            selected = selectedType == DebtType.I_OWE,
            onClick = { selectedType = DebtType.I_OWE },
            label = { Text(stringResource(R.string.debt_type_i_owe)) },
            modifier = Modifier.testTag("debt_type_i_owe_chip"),
          )
        }

        // Collapsible Split Bill Calculator Card
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDarkTheme) 0.25f else 0.38f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
              onClick = { expandSplitBill = !expandSplitBill },
              color = Color.Transparent,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("split_bill_toggle_button"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = stringResource(R.string.split_bill_toggle_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Icon(
                  imageVector = if (expandSplitBill) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }

            AnimatedVisibility(
              visible = expandSplitBill,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut(),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedTextField(
                  value = splitTotalBillText,
                  onValueChange = { splitTotalBillText = it.filter { ch -> ch.isDigit() }.take(10) },
                  label = { Text(stringResource(R.string.split_bill_total_label, currency.symbol)) },
                  prefix = { Text(currency.prefixWithSpace, fontFamily = JetBrainsMonoFontFamily) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("split_bill_total_input"),
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${stringResource(R.string.split_bill_people_label)}: $splitPeopleCount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                      onClick = { splitPeopleCount = (splitPeopleCount - 1).coerceAtLeast(2) },
                      enabled = splitPeopleCount > 2,
                      contentPadding = PaddingValues(0.dp),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.size(44.dp),
                    ) {
                      Icon(Icons.Rounded.Remove, contentDescription = "-")
                    }
                    OutlinedButton(
                      onClick = { splitPeopleCount = (splitPeopleCount + 1).coerceAtMost(30) },
                      contentPadding = PaddingValues(0.dp),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.size(44.dp),
                    ) {
                      Icon(Icons.Rounded.Add, contentDescription = "+")
                    }
                  }
                }

                if (splitPerPerson > 0L) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = stringResource(
                        R.string.split_bill_result_preview,
                        Formatters.formatMoney(splitPerPerson, currency),
                      ),
                      style = MaterialTheme.typography.labelLarge,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      color = EmeraldSafe,
                    )
                    FilledTonalButton(
                      onClick = {
                        amountText = splitPerPerson.toString()
                        if (note.isBlank()) {
                          note = "Split bill ($splitPeopleCount org)"
                        }
                        errorText = null
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .height(40.dp)
                        .testTag("split_bill_apply_button"),
                    ) {
                      Text(stringResource(R.string.split_bill_apply_action))
                    }
                  }
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = personName,
          onValueChange = {
            personName = it.take(60)
            errorText = null
          },
          label = { Text(stringResource(R.string.debt_person_label)) },
          placeholder = { Text(stringResource(R.string.debt_person_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_person_input"),
        )

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it.filter { ch -> ch.isDigit() }.take(10)
            errorText = null
          },
          label = { Text(stringResource(R.string.debt_amount_label, currency.symbol)) },
          prefix = {
            Text(
              text = currency.prefixWithSpace,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_amount_input"),
        )

        Text(
          text = stringResource(R.string.debt_due_date_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            7 to R.string.debt_due_7d,
            14 to R.string.debt_due_14d,
            30 to R.string.debt_due_30d,
          ).forEach { (days, labelRes) ->
            FilterChip(
              selected = dueDaysOffset == days,
              onClick = { dueDaysOffset = days },
              label = { Text(stringResource(labelRes)) },
            )
          }
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(160) },
          label = { Text(stringResource(R.string.debt_note_label)) },
          placeholder = { Text(stringResource(R.string.debt_note_hint)) },
          maxLines = 2,
          shape = RoundedCornerShape(14.dp),
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
          when {
            personName.isBlank() -> errorText = context.getString(R.string.debt_error_person)
            parsedAmount <= 0L -> errorText = context.getString(R.string.debt_error_amount)
            else -> onSave(
              personName.trim(),
              selectedType,
              parsedAmount,
              today.plusDays(dueDaysOffset.toLong()).toEpochDay(),
              note.trim(),
            )
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
          contentColor = if (isDarkTheme) ObsidianInk else Color.White,
        ),
        modifier = Modifier
          .height(48.dp)
          .testTag("save_debt_record_button"),
      ) {
        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
