package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.ui.Alignment
import com.example.R
import com.example.auth.AccountSecurityOtpManager
import com.example.auth.AltAuthSession
import com.example.auth.AuthMethodType
import com.example.data.ai.AiFinanceContextSnapshot
import com.example.data.ai.FinanceAiAssistant
import com.example.data.budget.BudgetAutoComposer
import com.example.data.budget.PaydayCycleManager
import com.example.data.local.CashflowLedgerManager
import com.example.data.local.LocalAccountVault
import com.example.data.local.SavingsHistoryManager
import com.example.data.local.SavingsHistoryType
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.notifications.BillDueReminderManager
import com.example.notifications.DailyReminderManager
import com.example.security.PrivacySecurityManager
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.AiFinanceChatDialog
import com.example.ui.components.AppAmbientBackdrop
import com.example.ui.components.ExpenseEditorSheet
import com.example.ui.components.ExportReportSheet
import com.example.ui.components.FloatingNavItem
import com.example.ui.components.FloatingPillBottomBar
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.rememberIsEffectiveDarkTheme
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactilePressScale
import com.example.ui.screens.AutoBudgetScreen
import com.example.ui.screens.DailyExpensesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.ThemePreferenceManager
import com.example.ui.viewmodel.AiExpenseParseState
import com.example.ui.viewmodel.AiInsightUiState
import com.example.ui.viewmodel.CategoryProgress
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.ui.viewmodel.SavingsUiData
import com.example.util.Formatters
import com.example.util.ImageUtils
import com.example.widget.WidgetQuickActions
import com.example.widget.WidgetSnapshotStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

private val LocalPageSlideEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private val localNavTabs = listOf(
  FloatingNavItem(
    route = ROUTE_DASHBOARD,
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Rounded.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_tab_dashboard",
  ),
  FloatingNavItem(
    route = ROUTE_EXPENSES,
    labelRes = R.string.nav_expenses,
    selectedIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    testTag = "nav_tab_expenses",
  ),
  FloatingNavItem(
    route = ROUTE_SAVINGS,
    labelRes = R.string.nav_savings,
    selectedIcon = Icons.Rounded.Savings,
    unselectedIcon = Icons.Outlined.Savings,
    testTag = "nav_tab_savings",
  ),
  FloatingNavItem(
    route = ROUTE_BUDGET,
    labelRes = R.string.nav_budget,
    selectedIcon = Icons.Rounded.AutoAwesome,
    unselectedIcon = Icons.Outlined.AutoAwesome,
    testTag = "nav_tab_budget",
  ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAccountScaffold(
  session: AltAuthSession,
  onSignedOut: () -> Unit,
  pendingWidgetAction: String? = null,
  onConsumeWidgetAction: () -> Unit = {},
  initialStatusMessage: String? = null,
  onConsumeInitialStatus: () -> Unit = {},
  isMigratingGuestData: Boolean = false,
  onConnectGoogleAndMigrate: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val aiAssistant = remember { FinanceAiAssistant() }

  val themeMode by ThemePreferenceManager.observeThemeMode(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val paydayStartDay by PaydayCycleManager.observePaydayStartDay(context).collectAsStateWithLifecycle()
  val cashflowEntries by CashflowLedgerManager.observeEntries(context, session.userId).collectAsStateWithLifecycle()
  var reminderSettings by remember(context) {
    mutableStateOf(DailyReminderManager.getSettings(context))
  }

  var selectedMonthKey by remember {
    mutableStateOf(
      PaydayCycleManager.currentCycleMonthKey(
        paydayStartDay = paydayStartDay,
      ),
    )
  }
  var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val isGuestVisitor = session.method == AuthMethodType.GUEST

  var allExpenses by remember(session.userId) {
    mutableStateOf(LocalAccountVault.loadExpenses(context, session.userId))
  }
  var budgetsByMonth by remember(session.userId) {
    mutableStateOf(LocalAccountVault.loadBudgets(context, session.userId))
  }
  var recurringTemplates by remember(session.userId) {
    mutableStateOf(LocalAccountVault.loadRecurring(context, session.userId))
  }
  var savingsGoals by remember(session.userId) {
    mutableStateOf(LocalAccountVault.loadGoals(context, session.userId))
  }
  var walletBalances by remember(session.userId) {
    mutableStateOf(LocalAccountVault.loadWallets(context, session.userId))
  }

  var aiInsightState by remember { mutableStateOf<AiInsightUiState>(AiInsightUiState.Idle) }
  var aiParseState by remember { mutableStateOf<AiExpenseParseState>(AiExpenseParseState.Idle) }
  var isSaving by remember { mutableStateOf(false) }

  var showExpenseSheet by remember { mutableStateOf(false) }
  var autoLaunchReceiptScan by remember { mutableStateOf(false) }
  var editingExpense by remember { mutableStateOf<Expense?>(null) }
  var prefillEpochDay by remember { mutableStateOf<Long?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var showExportSheet by remember { mutableStateOf(false) }
  var showAiChatDialog by remember { mutableStateOf(false) }

  fun showToastMessage(msg: String) {
    scope.launch {
      snackbarHostState.showSnackbar(msg)
    }
  }

  fun adjustLocalWallet(method: PaymentMethod, delta: Long) {
    if (delta == 0L) return
    val current = walletBalances[method] ?: 0L
    if (delta < 0L && current == 0L) return
    val next = (current + delta).coerceAtLeast(0L)
    walletBalances = walletBalances + (method to next)
    LocalAccountVault.saveWallets(context, session.userId, walletBalances)
  }

  fun computeLocalBudgetAlert(category: ExpenseCategory, addedAmount: Long, monthKey: String): String? {
    val budget = budgetsByMonth[monthKey] ?: return null
    val limit = budget.limitFor(category)
    if (limit <= 0L) return null
    val spentSoFar = allExpenses
      .filter { it.monthKey == monthKey && it.categoryEnum == category }
      .sumOf { it.amount }
    val newSpent = spentSoFar + addedAmount
    val pct = ((newSpent * 100L) / limit).toInt()
    val catLabel = category.label(context)
    return when {
      newSpent >= limit -> context.getString(R.string.budget_alert_over, catLabel, pct)
      pct >= 80 -> context.getString(R.string.budget_alert_caution, catLabel, pct)
      else -> null
    }
  }

  fun logRecurringTemplateItem(template: RecurringTemplate) {
    val today = LocalDate.now()
    val monthKey = Formatters.currentMonthKey(today)
    val alertMsg = computeLocalBudgetAlert(template.categoryEnum, template.amount, monthKey)
    val newExpense = Expense(
      id = UUID.randomUUID().toString(),
      userId = session.userId,
      title = template.title,
      amount = template.amount,
      category = template.category,
      paymentMethod = template.paymentMethod,
      dateEpochDay = today.toEpochDay(),
      monthKey = monthKey,
      note = template.note,
    )
    allExpenses = listOf(newExpense) + allExpenses
    selectedMonthKey = monthKey
    LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
    if (PrivacySecurityManager.isAutoDeductWalletEnabled(context)) {
      adjustLocalWallet(template.paymentMethodEnum, -template.amount)
    }
    showToastMessage(alertMsg ?: context.getString(R.string.vm_expense_added))
  }

  fun handleLocalDuplicateExpense(expense: Expense) {
    val today = LocalDate.now()
    val monthKey = Formatters.currentMonthKey(today)
    val alertMsg = computeLocalBudgetAlert(expense.categoryEnum, expense.amount, monthKey)
    val copy = expense.copy(
      id = UUID.randomUUID().toString(),
      dateEpochDay = today.toEpochDay(),
      monthKey = monthKey,
      receiptPhotoBase64 = "",
    )
    allExpenses = listOf(copy) + allExpenses
    selectedMonthKey = monthKey
    LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
    if (PrivacySecurityManager.isAutoDeductWalletEnabled(context)) {
      adjustLocalWallet(expense.paymentMethodEnum, -expense.amount)
    }
    showToastMessage(
      alertMsg ?: context.getString(R.string.expense_duplicated_status, expense.title),
    )
  }

  fun handleLocalDeleteWithUndo(expense: Expense) {
    val autoDeduct = PrivacySecurityManager.isAutoDeductWalletEnabled(context)
    allExpenses = allExpenses.filterNot { it.id == expense.id }
    LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
    if (autoDeduct) {
      adjustLocalWallet(expense.paymentMethodEnum, +expense.amount)
    }
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      val result = snackbarHostState.showSnackbar(
        message = context.getString(R.string.vm_expense_deleted),
        actionLabel = context.getString(R.string.action_undo),
        duration = SnackbarDuration.Long,
      )
      if (result == SnackbarResult.ActionPerformed) {
        allExpenses = listOf(expense) + allExpenses
        LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
        if (autoDeduct) {
          adjustLocalWallet(expense.paymentMethodEnum, -expense.amount)
        }
        showToastMessage(context.getString(R.string.expense_restored_status))
      }
    }
  }

  LaunchedEffect(initialStatusMessage) {
    initialStatusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      onConsumeInitialStatus()
    }
  }

  LaunchedEffect(pendingWidgetAction) {
    when (pendingWidgetAction) {
      WidgetQuickActions.ACTION_QUICK_ADD -> {
        editingExpense = null
        prefillEpochDay = null
        autoLaunchReceiptScan = false
        showExpenseSheet = true
        onConsumeWidgetAction()
      }
      WidgetQuickActions.ACTION_QUICK_SCAN -> {
        editingExpense = null
        prefillEpochDay = null
        autoLaunchReceiptScan = true
        showExpenseSheet = true
        onConsumeWidgetAction()
      }
    }
  }

  val monthExpenses = remember(allExpenses, selectedMonthKey, paydayStartDay) {
    if (paydayStartDay <= 1) {
      allExpenses.filter { it.monthKey == selectedMonthKey }
        .sortedByDescending { it.dateEpochDay }
    } else {
      val cycle = PaydayCycleManager.resolveCycle(selectedMonthKey, paydayStartDay)
      allExpenses.filter { cycle.contains(it.dateEpochDay) }
        .sortedByDescending { it.dateEpochDay }
    }
  }
  val activeBudget = budgetsByMonth[selectedMonthKey]
  val dailyInsight = remember(activeBudget, monthExpenses, selectedMonthKey, paydayStartDay) {
    BudgetAutoComposer.calculateDailyInsight(
      budget = activeBudget,
      monthExpenses = monthExpenses,
      monthKey = selectedMonthKey,
      paydayStartDay = paydayStartDay,
    )
  }
  val categoryBreakdown = remember(monthExpenses, activeBudget) {
    val spentByCat = monthExpenses.groupBy { it.categoryEnum }
      .mapValues { (_, list) -> list.sumOf { it.amount } }
    ExpenseCategory.entries.map { cat ->
      CategoryProgress(
        category = cat,
        allocated = activeBudget?.limitFor(cat) ?: 0L,
        spent = spentByCat[cat] ?: 0L,
      )
    }.sortedByDescending { maxOf(it.spent, it.allocated) }
  }
  val filteredExpenses = remember(monthExpenses, selectedCategoryFilter, searchQuery, context) {
    val q = searchQuery.trim().lowercase()
    monthExpenses.filter { item ->
      val catMatch = selectedCategoryFilter == null || item.categoryEnum == selectedCategoryFilter
      val queryMatch = q.isEmpty() ||
        item.title.lowercase().contains(q) ||
        item.note.lowercase().contains(q) ||
        item.categoryEnum.label(context).lowercase().contains(q)
      catMatch && queryMatch
    }
  }

  val dashboardData = remember(
    selectedMonthKey,
    paydayStartDay,
    monthExpenses,
    filteredExpenses,
    allExpenses,
    recurringTemplates,
    activeBudget,
    dailyInsight,
    categoryBreakdown,
    selectedCategoryFilter,
    searchQuery,
  ) {
    FinanceDashboardData(
      monthKey = selectedMonthKey,
      paydayStartDay = paydayStartDay,
      expenses = monthExpenses,
      filteredExpenses = filteredExpenses,
      allHistoricalExpenses = allExpenses,
      recurringTemplates = recurringTemplates,
      budget = activeBudget,
      dailyInsight = dailyInsight,
      categoryBreakdown = categoryBreakdown,
      selectedCategoryFilter = selectedCategoryFilter,
      searchQuery = searchQuery,
    )
  }

  val savingsData = remember(savingsGoals, walletBalances) {
    SavingsUiData(
      goals = savingsGoals,
      walletBalances = walletBalances,
    )
  }

  LaunchedEffect(dashboardData, activeCurrency) {
    WidgetSnapshotStore.updateSnapshot(
      context = context,
      dailySafeLimit = dashboardData.dailyInsight.dailySafeLimit,
      todaySpent = dashboardData.dailyInsight.todaySpent,
      remainingBudget = dashboardData.dailyInsight.remainingBudget,
      hasBudget = dashboardData.budget != null,
      monthKey = dashboardData.monthKey,
    )
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination
  val currentRoute = currentDestination?.route ?: ROUTE_DASHBOARD
  val tactile = rememberTactileFeedback()
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  fun navigateToTab(route: String) {
    tactile.navigation()
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun shiftMonth(offsetMonths: Long) {
    tactile.tick()
    selectedMonthKey = Formatters.shiftMonthKey(selectedMonthKey, offsetMonths)
  }

  fun openQuickReceiptScanner() {
    tactile.confirm()
    editingExpense = null
    prefillEpochDay = null
    autoLaunchReceiptScan = true
    showExpenseSheet = true
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          header = {
            Spacer(modifier = Modifier.height(8.dp))
            CenterScanRailButton(
              onClick = ::openQuickReceiptScanner,
            )
          },
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.height(12.dp))
          localNavTabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            val label = stringResource(tab.labelRes)
            val railInteractionSource = remember { MutableInteractionSource() }
            val iconScale by animateFloatAsState(
              targetValue = if (selected) 1.12f else 1.0f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
              ),
              label = "rail_icon_scale_${tab.route}",
            )
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(tab.route) },
              interactionSource = railInteractionSource,
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = label,
                  modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                  },
                )
              },
              label = { Text(label) },
              modifier = Modifier
                .tactilePressScale(
                  interactionSource = railInteractionSource,
                  pressedScale = 0.92f,
                  enablePressHaptic = false,
                )
                .testTag(tab.testTag),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            title = {
              AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                  val forward = navRouteOrder(targetState) >= navRouteOrder(initialState)
                  (fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(340, easing = LocalPageSlideEasing)) { fullWidth ->
                      if (forward) fullWidth / 4 else -fullWidth / 4
                    })
                    .togetherWith(
                      fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(300, easing = LocalPageSlideEasing)) { fullWidth ->
                          if (forward) -fullWidth / 5 else fullWidth / 5
                        }
                    )
                },
                label = "local_topbar_header_anim",
              ) { route ->
                when (route) {
                  ROUTE_EXPENSES -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier.size(38.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                          )
                        }
                      }
                      Column {
                        Text(
                          text = stringResource(R.string.topbar_expenses_title),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onBackground,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = stringResource(R.string.topbar_expenses_subtitle),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                  ROUTE_BUDGET -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)),
                        modifier = Modifier.size(38.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                          )
                        }
                      }
                      Column {
                        Text(
                          text = stringResource(R.string.topbar_budget_title),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onBackground,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = stringResource(R.string.topbar_budget_subtitle),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                  ROUTE_SAVINGS -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
                        modifier = Modifier.size(38.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                          )
                        }
                      }
                      Column {
                        Text(
                          text = stringResource(R.string.topbar_savings_title),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onBackground,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = stringResource(R.string.topbar_savings_subtitle),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                  else -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(38.dp),
                      ) {
                        Image(
                          painter = painterResource(id = R.drawable.img_sakupintar_logo),
                          contentDescription = stringResource(R.string.app_name),
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize(),
                        )
                      }
                      Column {
                        Text(
                          text = stringResource(R.string.dashboard_greeting, session.displayName),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onBackground,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.testTag("dashboard_greeting_name"),
                        )
                        Text(
                          text = session.identifierLabel,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                }
              }
            },
            actions = {
              val aiChatDesc = stringResource(R.string.ai_chat_open_cd)
              val aiChatInteractionSource = remember { MutableInteractionSource() }
              IconButton(
                onClick = {
                  tactile.tick()
                  showAiChatDialog = true
                },
                interactionSource = aiChatInteractionSource,
                modifier = Modifier
                  .size(48.dp)
                  .tactilePressScale(
                    interactionSource = aiChatInteractionSource,
                    pressedScale = 0.90f,
                    enablePressHaptic = false,
                  )
                  .semantics { contentDescription = aiChatDesc }
                  .testTag("topbar_ai_chat_button"),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                ) {
                  Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Forum,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
              val profileDesc = stringResource(R.string.profile_cd, session.displayName)
              val profileInteractionSource = remember { MutableInteractionSource() }
              IconButton(
                onClick = {
                  tactile.tick()
                  showAccountDialog = true
                },
                interactionSource = profileInteractionSource,
                modifier = Modifier
                  .size(48.dp)
                  .tactilePressScale(
                    interactionSource = profileInteractionSource,
                    pressedScale = 0.90f,
                    enablePressHaptic = false,
                  )
                  .semantics { contentDescription = profileDesc }
                  .testTag("account_menu_button"),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                  Box(modifier = Modifier.padding(2.dp)) {
                    ProfileAvatar(
                      displayName = session.displayName,
                      customPhotoBase64 = session.customPhotoBase64,
                      googlePhotoUrl = null,
                      size = 34.dp,
                    )
                  }
                }
              }
            },
          )
        },
        bottomBar = {
          if (!useNavigationRail) {
            FloatingPillBottomBar(
              tabs = localNavTabs,
              currentRoute = currentRoute,
              onSelectTab = ::navigateToTab,
              onQuickScanClick = ::openQuickReceiptScanner,
            )
          }
        },
        floatingActionButton = {
          AnimatedVisibility(
            visible = currentRoute == ROUTE_EXPENSES,
            enter = fadeIn(tween(220)) + scaleIn(
              initialScale = 0.85f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
              ),
            ),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.85f, animationSpec = tween(160)),
          ) {
            val fabInteractionSource = remember { MutableInteractionSource() }
            ExtendedFloatingActionButton(
              onClick = {
                tactile.confirm()
                editingExpense = null
                prefillEpochDay = null
                autoLaunchReceiptScan = false
                showExpenseSheet = true
              },
              interactionSource = fabInteractionSource,
              containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
              contentColor = if (isDarkTheme) ObsidianInk else Color.White,
              shape = RoundedCornerShape(16.dp),
              icon = {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                )
              },
              text = {
                Text(
                  text = stringResource(R.string.action_log_short),
                  fontWeight = FontWeight.SemiBold,
                )
              },
              modifier = Modifier
                .tactilePressScale(
                  interactionSource = fabInteractionSource,
                  pressedScale = 0.94f,
                  enablePressHaptic = false,
                )
                .testTag("add_expense_fab"),
            )
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
          AppAmbientBackdrop()
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                bottom = if (useNavigationRail) innerPadding.calculateBottomPadding() else 0.dp,
              ),
          ) {
            if (isGuestVisitor) {
              Surface(
                onClick = { showAccountDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 6.dp)
                  .testTag("guest_visitor_mode_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = stringResource(R.string.guest_visitor_banner),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            NavHost(
              navController = navController,
              startDestination = ROUTE_DASHBOARD,
              enterTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideInHorizontally(
                  animationSpec = tween(durationMillis = 380, easing = LocalPageSlideEasing),
                ) { fullWidth ->
                  if (forward) fullWidth else -fullWidth
                } + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
              },
              exitTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideOutHorizontally(
                  animationSpec = tween(durationMillis = 360, easing = LocalPageSlideEasing),
                ) { fullWidth ->
                  if (forward) -fullWidth / 3 else fullWidth / 3
                } + fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
              },
              popEnterTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideInHorizontally(
                  animationSpec = tween(durationMillis = 380, easing = LocalPageSlideEasing),
                ) { fullWidth ->
                  if (forward) fullWidth else -fullWidth
                } + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
              },
              popExitTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideOutHorizontally(
                  animationSpec = tween(durationMillis = 360, easing = LocalPageSlideEasing),
                ) { fullWidth ->
                  if (forward) -fullWidth / 3 else fullWidth / 3
                } + fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
              },
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            ) {
            composable(ROUTE_DASHBOARD) {
              DashboardScreen(
                data = dashboardData,
                displayName = session.displayName,
                customPhotoBase64 = session.customPhotoBase64,
                googlePhotoUrl = null,
                totalSavedAmount = savingsData.totalSaved,
                totalWalletBalance = savingsData.totalWalletBalance,
                aiInsightState = aiInsightState,
                onRequestAiInsight = {
                  scope.launch {
                    aiInsightState = AiInsightUiState.Loading
                    try {
                      val insight = aiAssistant.analyzeMonthlyFinances(
                        monthKey = dashboardData.monthKey,
                        expenses = dashboardData.expenses,
                        budget = dashboardData.budget,
                        dailyInsight = dashboardData.dailyInsight,
                        language = LanguagePreferenceManager.currentLanguage,
                        currency = CurrencyPreferenceManager.currentCurrency,
                      )
                      aiInsightState = AiInsightUiState.Success(
                        insight = insight,
                        monthKey = dashboardData.monthKey,
                      )
                    } catch (e: Exception) {
                      aiInsightState = AiInsightUiState.Error(
                        e.localizedMessage ?: context.getString(R.string.error_generic),
                      )
                    }
                  }
                },
                onOpenProfile = { showAccountDialog = true },
                onOpenExportReport = { showExportSheet = true },
                onNavigateToExpenses = { navigateToTab(ROUTE_EXPENSES) },
                onNavigateToAutoBudget = { navigateToTab(ROUTE_BUDGET) },
                onNavigateToSavings = { navigateToTab(ROUTE_SAVINGS) },
                onEditExpense = { expense ->
                  editingExpense = expense
                  prefillEpochDay = null
                  autoLaunchReceiptScan = false
                  showExpenseSheet = true
                },
                onPreviousMonth = { shiftMonth(-1L) },
                onNextMonth = { shiftMonth(1L) },
                onSelectMonth = { selectedMonthKey = it },
                onQuickScanReceipt = ::openQuickReceiptScanner,
                onLogRecurringTemplate = ::logRecurringTemplateItem,
                userId = session.userId,
                onOpenAiChat = { showAiChatDialog = true },
              )
            }
            composable(ROUTE_EXPENSES) {
              DailyExpensesScreen(
                data = dashboardData,
                onSearchQueryChange = { searchQuery = it },
                onCategoryFilterSelect = { selectedCategoryFilter = it },
                onEditExpense = { expense ->
                  editingExpense = expense
                  prefillEpochDay = null
                  autoLaunchReceiptScan = false
                  showExpenseSheet = true
                },
                onDeleteExpense = { id ->
                  allExpenses = allExpenses.filterNot { it.id == id }
                  LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
                  showToastMessage(context.getString(R.string.vm_expense_deleted))
                },
                onDeleteExpenseWithUndo = ::handleLocalDeleteWithUndo,
                onDuplicateExpense = ::handleLocalDuplicateExpense,
                onLogRecurringTemplate = ::logRecurringTemplateItem,
                onAddRecurringTemplate = { title, amount, category, method, note, dueDayOfMonth ->
                  val item = RecurringTemplate(
                    id = UUID.randomUUID().toString(),
                    userId = session.userId,
                    title = title.trim().take(80),
                    amount = amount,
                    category = category.name,
                    paymentMethod = method.name,
                    note = note.trim().take(200),
                  )
                  recurringTemplates = recurringTemplates + item
                  LocalAccountVault.saveRecurring(context, session.userId, recurringTemplates)
                  if (dueDayOfMonth != null) {
                    BillDueReminderManager.setDueSchedule(
                      context = context,
                      templateId = item.id,
                      title = item.title,
                      dueDayOfMonth = dueDayOfMonth,
                    )
                  }
                  showToastMessage(context.getString(R.string.vm_recurring_added))
                },
                onDeleteRecurringTemplate = { id ->
                  recurringTemplates = recurringTemplates.filterNot { it.id == id }
                  LocalAccountVault.saveRecurring(context, session.userId, recurringTemplates)
                  BillDueReminderManager.removeDueSchedule(context, id)
                  showToastMessage(context.getString(R.string.vm_recurring_deleted))
                },
                onOpenExportReport = { showExportSheet = true },
                onPreviousMonth = { shiftMonth(-1L) },
                onNextMonth = { shiftMonth(1L) },
                onSelectMonth = { selectedMonthKey = it },
                onAddExpenseOnDate = { epochDay ->
                  editingExpense = null
                  prefillEpochDay = epochDay
                  autoLaunchReceiptScan = false
                  showExpenseSheet = true
                },
                onQuickScanReceipt = ::openQuickReceiptScanner,
              )
            }
            composable(ROUTE_SAVINGS) {
              SavingsScreen(
                data = savingsData,
                onCreateGoal = { title, target, initialSaved, cat, note ->
                  val goal = SavingsGoal(
                    id = UUID.randomUUID().toString(),
                    userId = session.userId,
                    title = title.trim().take(60),
                    targetAmount = target,
                    savedAmount = initialSaved.coerceAtLeast(0L),
                    category = cat.name,
                    note = note.trim().take(120),
                  )
                  savingsGoals = savingsGoals + goal
                  LocalAccountVault.saveGoals(context, session.userId, savingsGoals)
                  if (goal.savedAmount > 0L) {
                    SavingsHistoryManager.recordEvent(
                      context = context,
                      userId = session.userId,
                      goalId = goal.id,
                      type = SavingsHistoryType.INITIAL,
                      amount = goal.savedAmount,
                      balanceAfter = goal.savedAmount,
                    )
                  }
                  showToastMessage(context.getString(R.string.vm_goal_created, goal.title))
                },
                onUpdateGoal = { goalId, title, targetAmount, cat, note ->
                  savingsGoals = savingsGoals.map { existing ->
                    if (existing.id == goalId) {
                      existing.copy(
                        title = title.trim().take(60),
                        targetAmount = targetAmount.coerceAtLeast(1L),
                        category = cat.name,
                        note = note.trim().take(120),
                      )
                    } else {
                      existing
                    }
                  }
                  LocalAccountVault.saveGoals(context, session.userId, savingsGoals)
                  showToastMessage(context.getString(R.string.savings_goal_updated))
                },
                onDepositGoal = { goal, amount ->
                  val updatedAmount = (goal.savedAmount + amount).coerceAtLeast(0L)
                  savingsGoals = savingsGoals.map {
                    if (it.id == goal.id) it.copy(savedAmount = updatedAmount) else it
                  }
                  LocalAccountVault.saveGoals(context, session.userId, savingsGoals)
                  SavingsHistoryManager.recordEvent(
                    context = context,
                    userId = session.userId,
                    goalId = goal.id,
                    type = SavingsHistoryType.DEPOSIT,
                    amount = amount,
                    balanceAfter = updatedAmount,
                  )
                  val msg = if (goal.targetAmount > 0L && updatedAmount >= goal.targetAmount) {
                    context.getString(R.string.vm_goal_reached, goal.title)
                  } else {
                    val formatted = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
                    context.getString(R.string.vm_goal_deposited, formatted, goal.title)
                  }
                  showToastMessage(msg)
                },
                onWithdrawGoal = { goal, amount ->
                  val updatedAmount = (goal.savedAmount - amount).coerceAtLeast(0L)
                  savingsGoals = savingsGoals.map {
                    if (it.id == goal.id) it.copy(savedAmount = updatedAmount) else it
                  }
                  LocalAccountVault.saveGoals(context, session.userId, savingsGoals)
                  SavingsHistoryManager.recordEvent(
                    context = context,
                    userId = session.userId,
                    goalId = goal.id,
                    type = SavingsHistoryType.WITHDRAW,
                    amount = amount,
                    balanceAfter = updatedAmount,
                  )
                  val formatted = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
                  showToastMessage(context.getString(R.string.vm_goal_withdrawn, formatted, goal.title))
                },
                onDeleteGoal = { id ->
                  savingsGoals = savingsGoals.filterNot { it.id == id }
                  LocalAccountVault.saveGoals(context, session.userId, savingsGoals)
                  SavingsHistoryManager.deleteHistoryForGoal(context, session.userId, id)
                  showToastMessage(context.getString(R.string.vm_goal_deleted))
                },
                onSetWalletBalance = { method, balance ->
                  val cleanBalance = balance.coerceAtLeast(0L)
                  walletBalances = walletBalances + (method to cleanBalance)
                  LocalAccountVault.saveWallets(context, session.userId, walletBalances)
                  val formatted = Formatters.formatMoney(cleanBalance, CurrencyPreferenceManager.currentCurrency)
                  showToastMessage(context.getString(R.string.vm_wallet_updated, method.label(context), formatted))
                },
                userId = session.userId,
                onRecordIncome = { title, amount, targetWallet, note ->
                  val nextBal = (walletBalances[targetWallet] ?: 0L) + amount
                  walletBalances = walletBalances + (targetWallet to nextBal)
                  LocalAccountVault.saveWallets(context, session.userId, walletBalances)
                  CashflowLedgerManager.recordIncome(
                    context = context,
                    userId = session.userId,
                    title = title,
                    amount = amount,
                    targetWallet = targetWallet,
                    note = note,
                  )
                  val formatted = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
                  showToastMessage(context.getString(R.string.cashflow_income_saved, formatted, targetWallet.label(context)))
                },
                onTransferWallet = { from, to, amount, note ->
                  if (from == to) {
                    showToastMessage(context.getString(R.string.cashflow_transfer_same_wallet_error))
                  } else {
                    val fromBal = walletBalances[from] ?: 0L
                    val toBal = walletBalances[to] ?: 0L
                    if (amount > fromBal) {
                      showToastMessage(context.getString(R.string.cashflow_transfer_insufficient, from.label(context)))
                    } else {
                      walletBalances = walletBalances +
                        (from to (fromBal - amount).coerceAtLeast(0L)) +
                        (to to (toBal + amount))
                      LocalAccountVault.saveWallets(context, session.userId, walletBalances)
                      CashflowLedgerManager.recordTransfer(
                        context = context,
                        userId = session.userId,
                        fromWallet = from,
                        toWallet = to,
                        amount = amount,
                        note = note,
                      )
                      val formatted = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
                      showToastMessage(
                        context.getString(
                          R.string.cashflow_transfer_saved,
                          formatted,
                          from.label(context),
                          to.label(context),
                        ),
                      )
                    }
                  }
                },
                onDeleteCashflowEntry = { entryId ->
                  CashflowLedgerManager.deleteEntry(context, session.userId, entryId)
                  showToastMessage(context.getString(R.string.cashflow_entry_deleted))
                },
                monthKey = selectedMonthKey,
                onPreviousMonth = { shiftMonth(-1L) },
                onNextMonth = { shiftMonth(1L) },
                onSelectMonth = { selectedMonthKey = it },
              )
            }
            composable(ROUTE_BUDGET) {
              AutoBudgetScreen(
                data = dashboardData,
                currentUserId = session.userId,
                isSaving = isSaving,
                onSaveBudget = { budget ->
                  val updated = budget.copy(userId = session.userId, monthKey = selectedMonthKey)
                  budgetsByMonth = budgetsByMonth + (selectedMonthKey to updated)
                  LocalAccountVault.saveBudgets(context, session.userId, budgetsByMonth)
                  showToastMessage(context.getString(R.string.vm_budget_saved))
                },
                onPreviousMonth = { shiftMonth(-1L) },
                onNextMonth = { shiftMonth(1L) },
                onSelectMonth = { selectedMonthKey = it },
              )
            }
            }
          }
        }
      }
    }
  }

  if (showExpenseSheet) {
    ExpenseEditorSheet(
      existingExpense = editingExpense,
      initialEpochDay = prefillEpochDay,
      autoLaunchReceiptScan = autoLaunchReceiptScan,
      aiParseState = aiParseState,
      onParseWithAi = { raw ->
        scope.launch {
          aiParseState = AiExpenseParseState.Loading
          try {
            val parsed = aiAssistant.parseNaturalExpense(
              rawInput = raw,
              language = LanguagePreferenceManager.currentLanguage,
              currency = CurrencyPreferenceManager.currentCurrency,
            )
            aiParseState = AiExpenseParseState.Success(parsed)
          } catch (e: Exception) {
            aiParseState = AiExpenseParseState.Error(
              e.localizedMessage ?: context.getString(R.string.error_generic),
            )
          }
        }
      },
      onScanReceiptUri = { uri: Uri ->
        scope.launch {
          aiParseState = AiExpenseParseState.Loading
          try {
            val bitmap = ImageUtils.decodeScaledBitmap(context.applicationContext, uri)
            val encoded = runCatching { ImageUtils.encodeReceiptBitmapBase64(bitmap) }.getOrNull()
            val parsed = aiAssistant.parseReceiptImage(
              bitmap = bitmap,
              language = LanguagePreferenceManager.currentLanguage,
              currency = CurrencyPreferenceManager.currentCurrency,
            )
            aiParseState = AiExpenseParseState.Success(parsed, encoded)
          } catch (e: Exception) {
            aiParseState = AiExpenseParseState.Error(
              e.localizedMessage ?: context.getString(R.string.error_generic),
            )
          }
        }
      },
      onScanReceiptBase64 = { base64 ->
        scope.launch {
          aiParseState = AiExpenseParseState.Loading
          try {
            val bitmap = ImageUtils.decodeAvatarBitmap(base64)
              ?: error(context.getString(R.string.error_generic))
            val parsed = aiAssistant.parseReceiptImage(
              bitmap = bitmap,
              language = LanguagePreferenceManager.currentLanguage,
              currency = CurrencyPreferenceManager.currentCurrency,
            )
            aiParseState = AiExpenseParseState.Success(parsed, base64)
          } catch (e: Exception) {
            aiParseState = AiExpenseParseState.Error(
              e.localizedMessage ?: context.getString(R.string.error_generic),
            )
          }
        }
      },
      onClearAiParse = { aiParseState = AiExpenseParseState.Idle },
      onDismiss = {
        aiParseState = AiExpenseParseState.Idle
        showExpenseSheet = false
        autoLaunchReceiptScan = false
        editingExpense = null
        prefillEpochDay = null
      },
      onSave = { title, amount, category, method, epochDay, note, receiptPhotoBase64 ->
        val monthKey = Formatters.currentMonthKey(LocalDate.ofEpochDay(epochDay))
        val currentEdit = editingExpense
        val autoDeduct = PrivacySecurityManager.isAutoDeductWalletEnabled(context)
        if (currentEdit == null) {
          val alertMsg = computeLocalBudgetAlert(category, amount, monthKey)
          val added = Expense(
            id = UUID.randomUUID().toString(),
            userId = session.userId,
            title = title.trim().take(80),
            amount = amount,
            category = category.name,
            paymentMethod = method.name,
            dateEpochDay = epochDay,
            monthKey = monthKey,
            note = note.trim().take(200),
            receiptPhotoBase64 = receiptPhotoBase64.take(200_000),
          )
          allExpenses = listOf(added) + allExpenses
          selectedMonthKey = monthKey
          LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
          if (autoDeduct) {
            adjustLocalWallet(method, -amount)
          }
          showToastMessage(alertMsg ?: context.getString(R.string.vm_expense_added))
        } else {
          val updated = currentEdit.copy(
            title = title.trim().take(80),
            amount = amount,
            category = category.name,
            paymentMethod = method.name,
            dateEpochDay = epochDay,
            monthKey = monthKey,
            note = note.trim().take(200),
            receiptPhotoBase64 = receiptPhotoBase64.take(200_000),
          )
          allExpenses = allExpenses.map { if (it.id == updated.id) updated else it }
          selectedMonthKey = monthKey
          LocalAccountVault.saveExpenses(context, session.userId, allExpenses)
          if (autoDeduct) {
            if (currentEdit.paymentMethodEnum == method) {
              val diff = currentEdit.amount - amount
              if (diff != 0L) adjustLocalWallet(method, diff)
            } else {
              adjustLocalWallet(currentEdit.paymentMethodEnum, +currentEdit.amount)
              adjustLocalWallet(method, -amount)
            }
          }
          showToastMessage(context.getString(R.string.vm_expense_updated))
        }
        aiParseState = AiExpenseParseState.Idle
        showExpenseSheet = false
        autoLaunchReceiptScan = false
        editingExpense = null
        prefillEpochDay = null
      },
    )
  }

  if (showAccountDialog) {
    val guestLocalRecordCount = remember(
      allExpenses,
      budgetsByMonth,
      recurringTemplates,
      savingsGoals,
      walletBalances,
    ) {
      allExpenses.size +
        budgetsByMonth.size +
        recurringTemplates.size +
        savingsGoals.size +
        walletBalances.values.count { it > 0L }
    }

    AccountProfileDialog(
      displayName = session.displayName,
      email = session.identifierLabel,
      customPhotoBase64 = session.customPhotoBase64,
      googlePhotoUrl = null,
      isSavingProfile = isSaving,
      themeMode = themeMode,
      reminderSettings = reminderSettings,
      onReminderSettingsChange = { reminderSettings = it },
      onSaveDisplayName = { newName ->
        AccountSecurityOtpManager.updateAltSessionProfile(context, displayName = newName)
        showToastMessage(context.getString(R.string.vm_profile_name_saved, newName))
      },
      onPickPhotoUri = { uri ->
        scope.launch {
          isSaving = true
          try {
            val encoded = ImageUtils.encodeAvatarBase64(context.applicationContext, uri)
            AccountSecurityOtpManager.updateAltSessionProfile(context, customPhotoBase64 = encoded)
            showToastMessage(context.getString(R.string.vm_profile_photo_saved))
          } catch (e: Exception) {
            showToastMessage(e.localizedMessage ?: context.getString(R.string.error_generic))
          } finally {
            isSaving = false
          }
        }
      },
      onTakePhotoBitmap = { bitmap: Bitmap ->
        scope.launch {
          isSaving = true
          try {
            val encoded = withContext(Dispatchers.IO) {
              ImageUtils.encodeAvatarBitmapBase64(bitmap)
            }
            AccountSecurityOtpManager.updateAltSessionProfile(context, customPhotoBase64 = encoded)
            showToastMessage(context.getString(R.string.vm_profile_photo_saved))
          } catch (e: Exception) {
            showToastMessage(e.localizedMessage ?: context.getString(R.string.error_generic))
          } finally {
            isSaving = false
          }
        }
      },
      onSyncGooglePhoto = {
        AccountSecurityOtpManager.updateAltSessionProfile(context, customPhotoBase64 = "")
      },
      onStatusMessage = ::showToastMessage,
      onSignOut = {
        showAccountDialog = false
        AccountSecurityOtpManager.clearAltSession(context)
        onSignedOut()
      },
      onDismiss = { showAccountDialog = false },
      isGuestMode = isGuestVisitor,
      guestLocalRecordCount = guestLocalRecordCount,
      isMigratingGuestData = isMigratingGuestData,
      onConnectGoogleAndMigrate = onConnectGoogleAndMigrate,
    )
  }

  if (showExportSheet) {
    ExportReportSheet(
      data = dashboardData,
      onStatusMessage = ::showToastMessage,
      onDismiss = { showExportSheet = false },
    )
  }

  if (showAiChatDialog) {
    AiFinanceChatDialog(
      aiAssistant = aiAssistant,
      snapshot = AiFinanceContextSnapshot(
        monthKey = dashboardData.monthKey,
        paydayStartDay = dashboardData.paydayStartDay,
        periodExpenses = dashboardData.expenses,
        allHistoricalExpenses = dashboardData.allHistoricalExpenses,
        budget = dashboardData.budget,
        dailyInsight = dashboardData.dailyInsight,
        walletBalances = walletBalances,
        savingsGoals = savingsGoals,
        cashflowEntries = cashflowEntries,
      ),
      onDismiss = { showAiChatDialog = false },
    )
  }
}
