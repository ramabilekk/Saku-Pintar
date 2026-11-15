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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.components.CreateSavingsGoalDialog
import com.example.ui.components.SavingsTransactionDialog
import com.example.ui.components.WalletBalanceDialog
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.viewmodel.SavingsUiData
import com.example.util.Formatters
import java.util.Locale

@Composable
fun SavingsScreen(
  data: SavingsUiData,
  onCreateGoal: (
    title: String,
    targetAmount: Long,
    initialSavedAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) -> Unit,
  onDepositGoal: (SavingsGoal, Long) -> Unit,
  onWithdrawGoal: (SavingsGoal, Long) -> Unit,
  onDeleteGoal: (String) -> Unit,
  onSetWalletBalance: (PaymentMethod, Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }

  var showCreateDialog by remember { mutableStateOf(false) }
  var transactionTarget by remember { mutableStateOf<Pair<SavingsGoal, Boolean>?>(null) }
  var pendingDeleteGoal by remember { mutableStateOf<SavingsGoal?>(null) }
  var editingWallet by remember { mutableStateOf<PaymentMethod?>(null) }

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
      // 1. Hero Total Saved & Wallet Overview
      item {
        SavingsHeroCard(
          data = data,
          currency = activeCurrency,
          locale = activeLocale,
        )
      }

      // 2. Wallet Balances Section
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.savings_wallets_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          val methods = PaymentMethod.entries
          methods.chunked(2).forEach { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              pair.forEach { method ->
                val balance = data.walletBalances[method] ?: 0L
                WalletCard(
                  paymentMethod = method,
                  balance = balance,
                  currency = activeCurrency,
                  onClick = { editingWallet = method },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // 3. Savings Goals Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.savings_goals_title),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = stringResource(R.string.savings_goals_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          FilledTonalButton(
            onClick = { showCreateDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("add_savings_goal_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.savings_new_goal_button))
          }
        }
      }

      // 4. Savings Goals List or Empty State
      if (data.goals.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
              Text(
                text = stringResource(R.string.savings_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = stringResource(R.string.savings_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("empty_create_goal_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.savings_empty_cta))
              }
            }
          }
        }
      } else {
        items(data.goals, key = { it.id }) { goal ->
          SavingsGoalCard(
            goal = goal,
            currency = activeCurrency,
            locale = activeLocale,
            onDeposit = { transactionTarget = goal to true },
            onWithdraw = { transactionTarget = goal to false },
            onDelete = { pendingDeleteGoal = goal },
          )
        }
      }

      // 5. Reassuring info notice
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = stringResource(R.string.savings_disclaimer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateSavingsGoalDialog(
      onConfirm = { title, target, initial, cat, note ->
        onCreateGoal(title, target, initial, cat, note)
        showCreateDialog = false
      },
      onDismiss = { showCreateDialog = false },
    )
  }

  transactionTarget?.let { (goal, isDeposit) ->
    SavingsTransactionDialog(
      goal = goal,
      isDeposit = isDeposit,
      onConfirm = { amount ->
        if (isDeposit) {
          onDepositGoal(goal, amount)
        } else {
          onWithdrawGoal(goal, amount)
        }
        transactionTarget = null
      },
      onDismiss = { transactionTarget = null },
    )
  }

  editingWallet?.let { method ->
    WalletBalanceDialog(
      paymentMethod = method,
      currentBalance = data.walletBalances[method] ?: 0L,
      onConfirm = { newBalance ->
        onSetWalletBalance(method, newBalance)
        editingWallet = null
      },
      onDismiss = { editingWallet = null },
    )
  }

  pendingDeleteGoal?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteGoal = null },
      title = { Text(stringResource(R.string.savings_delete_title)) },
      text = {
        Text(
          stringResource(
            R.string.savings_delete_message,
            target.title,
            Formatters.formatMoney(target.savedAmount, activeCurrency),
          ),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteGoal(target.id)
            pendingDeleteGoal = null
          },
        ) {
          Text(stringResource(R.string.action_delete), color = CoralDanger)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteGoal = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun SavingsHeroCard(
  data: SavingsUiData,
  currency: AppCurrency,
  locale: Locale,
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val animatedProgress by animateFloatAsState(
    targetValue = data.overallProgressFraction,
    animationSpec = tween(durationMillis = 450),
    label = "savings_total_progress",
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(26.dp))
      .background(Brush.linearGradient(listOf(primary, secondary)))
      .padding(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.savings_hero_total),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = Formatters.formatMoney(data.totalSaved, currency),
            style = MaterialTheme.typography.displaySmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
        Surface(
          color = Color.White.copy(alpha = 0.16f),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Savings,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier
              .padding(10.dp)
              .size(28.dp),
          )
        }
      }

      if (data.totalTarget > 0L) {
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
          color = GoldAccent,
          trackColor = Color.White.copy(alpha = 0.22f),
          strokeCap = StrokeCap.Round,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.savings_hero_pct, (data.overallProgressFraction * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
          )
          Text(
            text = stringResource(
              R.string.savings_hero_target,
              Formatters.formatMoneyCompact(data.totalTarget, currency, locale),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }

      Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AccountBalanceWallet,
              contentDescription = null,
              tint = SkyPositiveAccent,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = stringResource(R.string.savings_hero_wallet_total),
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.9f),
            )
          }
          Text(
            text = Formatters.formatMoney(data.totalWalletBalance, currency),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
      }
    }
  }
}

@Composable
private fun WalletCard(
  paymentMethod: PaymentMethod,
  balance: Long,
  currency: AppCurrency,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val methodLabel = stringResource(paymentMethod.labelRes)
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .clickable(onClick = onClick)
      .testTag("wallet_card_${paymentMethod.name}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
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
            imageVector = paymentMethod.walletIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = methodLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = "${stringResource(R.string.action_edit)} $methodLabel",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = Formatters.formatMoney(balance, currency),
        style = MaterialTheme.typography.titleSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SavingsGoalCard(
  goal: SavingsGoal,
  currency: AppCurrency,
  locale: Locale,
  onDeposit: () -> Unit,
  onWithdraw: () -> Unit,
  onDelete: () -> Unit,
) {
  val category = goal.categoryEnum
  val categoryLabel = stringResource(category.labelRes)
  val animatedProgress by animateFloatAsState(
    targetValue = goal.progressFraction,
    animationSpec = tween(durationMillis = 400),
    label = "goal_progress_${goal.id}",
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("savings_goal_card_${goal.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = category.accentColor.copy(alpha = 0.14f),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = category.iconVector(),
              contentDescription = null,
              tint = category.accentColor,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = goal.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (goal.isCompleted) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = SkyPositiveAccent,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Text(
            text = if (goal.note.isNotBlank()) "$categoryLabel • ${goal.note}" else categoryLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(48.dp)
            .testTag("delete_goal_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "${stringResource(R.string.action_delete)} ${goal.title}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = Formatters.formatMoney(goal.savedAmount, currency),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (goal.isCompleted) SkyPositiveAccent else MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(
              R.string.savings_card_target,
              Formatters.formatMoneyCompact(goal.targetAmount, currency, locale),
              (goal.progressFraction * 100).toInt(),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
          color = if (goal.isCompleted) SkyPositiveAccent else category.accentColor,
          trackColor = category.accentColor.copy(alpha = 0.14f),
          strokeCap = StrokeCap.Round,
        )
        if (!goal.isCompleted) {
          Text(
            text = stringResource(
              R.string.savings_card_remaining,
              Formatters.formatMoney(goal.remainingAmount, currency),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = onDeposit,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("deposit_goal_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.savings_deposit))
        }
        OutlinedButton(
          onClick = onWithdraw,
          enabled = goal.savedAmount > 0L,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("withdraw_goal_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.savings_withdraw))
        }
      }
    }
  }
}

private fun PaymentMethod.walletIcon(): ImageVector = when (this) {
  PaymentMethod.QRIS -> Icons.Rounded.QrCode2
  PaymentMethod.CASH -> Icons.Rounded.Payments
  PaymentMethod.TRANSFER -> Icons.Rounded.AccountBalance
  PaymentMethod.CARD -> Icons.Rounded.CreditCard
}

private fun SavingsGoalCategory.iconVector(): ImageVector = when (this) {
  SavingsGoalCategory.EMERGENCY -> Icons.Rounded.Shield
  SavingsGoalCategory.GADGET -> Icons.Rounded.LaptopMac
  SavingsGoalCategory.TRAVEL -> Icons.Rounded.FlightTakeoff
  SavingsGoalCategory.VEHICLE -> Icons.Rounded.DirectionsCar
  SavingsGoalCategory.EDUCATION -> Icons.Rounded.School
  SavingsGoalCategory.HOME -> Icons.Rounded.Home
  SavingsGoalCategory.OTHER -> Icons.Rounded.Savings
}
