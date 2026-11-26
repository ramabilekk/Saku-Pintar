package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.signOutUser
import com.example.data.ai.AiFinanceContextSnapshot
import com.example.data.local.CashflowLedgerManager
import com.example.data.local.CustomCategoryManager
import com.example.data.local.CustomWalletManager
import com.example.data.local.DebtLedgerManager
import com.example.data.local.FullAppBackup
import com.example.data.local.SavingsHistoryManager
import com.example.data.model.Expense
import com.example.data.model.SavingsGoalCategory
import com.example.data.repository.ExtrasCloudSyncRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SavingsRepository
import com.example.i18n.CurrencyPreferenceManager
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
import com.example.ui.components.LiquidGlassIconBadge
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.rememberIsEffectiveDarkTheme
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactilePressScale
import com.example.widget.WidgetQuickActions
import com.example.widget.WidgetSnapshotStore
import com.example.ui.screens.AutoBudgetScreen
import com.example.ui.screens.DailyExpensesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.ThemePreferenceManager
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SavingsUiData
import com.example.ui.viewmodel.SavingsViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.Formatters
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_EXPENSES = "expenses"
const val ROUTE_BUDGET = "budget"
const val ROUTE_SAVINGS = "savings"

internal fun navRouteOrder(route: String?): Int = when (route) {
  ROUTE_DASHBOARD -> 0
  ROUTE_EXPENSES -> 1
  ROUTE_SAVINGS -> 2
  ROUTE_BUDGET -> 3
  else -> 0
}

private val PageSlideEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private val navTabs = listOf(
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
fun MainAppScaffold(
  currentUser: FirebaseUser,
  onSignedOut: () -> Unit,
  pendingWidgetAction: String? = null,
  onConsumeWidgetAction: () -> Unit = {},
  initialStatusMessage: String? = null,
  onConsumeInitialStatus: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val currentUserId = currentUser.uid
  val viewModel: FinanceViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        FinanceViewModel(
          repository = FinanceRepository(db),
          currentUserId = currentUserId,
          appContext = app.applicationContext,
        )
      }
    },
  )

  val savingsViewModel: SavingsViewModel = viewModel(
    key = "savings_$currentUserId",
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        SavingsViewModel(
          repository = SavingsRepository(db),
          currentUserId = currentUserId,
          appContext = app.applicationContext,
        )
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val savingsUiState by savingsViewModel.uiState.collectAsStateWithLifecycle()
  val savingsActionStatus by savingsViewModel.actionStatus.collectAsStateWithLifecycle()
  val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
  val actionStatus by viewModel.actionStatus.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
  val aiInsightState by viewModel.aiInsightState.collectAsStateWithLifecycle()
  val aiParseState by viewModel.aiParseState.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

  val defaultUserName = stringResource(R.string.default_user_name)
  val resolvedDisplayName = remember(userProfile?.displayName, currentUser.displayName, defaultUserName) {
    userProfile?.displayName?.trim()?.takeIf { it.isNotEmpty() }
      ?: currentUser.displayName?.trim()?.takeIf { it.isNotEmpty() }
      ?: defaultUserName
  }
  val customPhotoBase64 = userProfile?.photoBase64?.takeIf { it.isNotBlank() }
  val googlePhotoUrl = remember(currentUser.photoUrl) {
    currentUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination
  val currentRoute = currentDestination?.route ?: ROUTE_DASHBOARD

  val snackbarHostState = remember { SnackbarHostState() }
  var showExpenseSheet by remember { mutableStateOf(false) }
  var autoLaunchReceiptScan by remember { mutableStateOf(false) }
  var editingExpense by remember { mutableStateOf<Expense?>(null) }
  var prefillEpochDay by remember { mutableStateOf<Long?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var showExportSheet by remember { mutableStateOf(false) }
  var showAiChatDialog by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val themeMode by ThemePreferenceManager.observeThemeMode(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val cashflowEntries by CashflowLedgerManager.observeEntries(context, currentUserId).collectAsStateWithLifecycle()
  val customWallets by CustomWalletManager.observeWallets(context, currentUserId).collectAsStateWithLifecycle()
  val debtRecords by DebtLedgerManager.observeRecords(context, currentUserId).collectAsStateWithLifecycle()
  val billSchedules by BillDueReminderManager.observeSchedules(context).collectAsStateWithLifecycle()
  val extrasSyncRepo = remember(context) { ExtrasCloudSyncRepository(context) }
  val financeRepoForBackup = remember(context) { FinanceRepository(context) }
  val savingsRepoForBackup = remember(context) { SavingsRepository(context) }
  var reminderSettings by remember(context) {
    mutableStateOf(DailyReminderManager.getSettings(context))
  }

  LaunchedEffect(Unit) {
    BillDueReminderManager.notifyDueBillsIfAny(context)
  }

  // Auto-log due recurring bills once per month when autoLogEnabled == true
  LaunchedEffect(uiState, billSchedules) {
    val success = (uiState as? UiState.Success)?.data ?: return@LaunchedEffect
    val today = LocalDate.now()
    val currentMonth = Formatters.currentMonthKey(today)
    val dueTemplates = BillDueReminderManager.findDueAutoLogTemplates(
      templates = success.recurringTemplates,
      schedules = billSchedules,
      today = today,
    )
    if (dueTemplates.isNotEmpty()) {
      dueTemplates.forEach { (tpl, _) ->
        val marked = BillDueReminderManager.markAutoLogged(context, tpl.id, currentMonth)
        viewModel.logRecurringTemplate(tpl)
        if (marked != null) {
          extrasSyncRepo.upsertBillSchedule(marked)
        }
      }
      snackbarHostState.showSnackbar(
        context.getString(R.string.bill_autolog_triggered_status, dueTemplates.size)
      )
    }
  }

  DisposableEffect(currentUserId) {
    val jobs = extrasSyncRepo.startRealtimeSync(scope, context, currentUserId)
    onDispose {
      jobs.forEach { it.cancel() }
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

  LaunchedEffect(uiState, activeCurrency) {
    val success = (uiState as? UiState.Success)?.data ?: return@LaunchedEffect
    WidgetSnapshotStore.updateSnapshot(
      context = context,
      dailySafeLimit = success.dailyInsight.dailySafeLimit,
      todaySpent = success.dailyInsight.todaySpent,
      remainingBudget = success.dailyInsight.remainingBudget,
      hasBudget = success.budget != null,
      monthKey = success.monthKey,
    )
  }

  LaunchedEffect(actionStatus) {
    actionStatus?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearActionStatus()
    }
  }

  LaunchedEffect(savingsActionStatus) {
    savingsActionStatus?.let { message ->
      snackbarHostState.showSnackbar(message)
      savingsViewModel.clearActionStatus()
    }
  }

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

  fun openQuickReceiptScanner() {
    tactile.confirm()
    editingExpense = null
    prefillEpochDay = null
    autoLaunchReceiptScan = true
    showExpenseSheet = true
  }

  fun handleDeleteExpenseWithUndo(expense: Expense) {
    val autoDeduct = PrivacySecurityManager.isAutoDeductWalletEnabled(context)
    viewModel.deleteExpense(expense.id, silentStatus = true)
    if (autoDeduct) {
      savingsViewModel.adjustWalletBalance(expense.paymentMethodEnum, +expense.amount)
    }
    scope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      val result = snackbarHostState.showSnackbar(
        message = context.getString(R.string.vm_expense_deleted),
        actionLabel = context.getString(R.string.action_undo),
        duration = SnackbarDuration.Long,
      )
      if (result == SnackbarResult.ActionPerformed) {
        viewModel.restoreExpense(expense)
        if (autoDeduct) {
          savingsViewModel.adjustWalletBalance(expense.paymentMethodEnum, -expense.amount)
        }
      }
    }
  }

  fun handleLogRecurringTemplate(template: com.example.data.model.RecurringTemplate) {
    viewModel.logRecurringTemplate(template)
    if (PrivacySecurityManager.isAutoDeductWalletEnabled(context)) {
      savingsViewModel.adjustWalletBalance(template.paymentMethodEnum, -template.amount)
    }
  }

  fun handleDuplicateExpense(expense: Expense) {
    viewModel.duplicateExpense(expense)
    if (PrivacySecurityManager.isAutoDeductWalletEnabled(context)) {
      savingsViewModel.adjustWalletBalance(expense.paymentMethodEnum, -expense.amount)
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
  AppAmbientBackdrop()
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          TopAppBar(
            modifier = Modifier.background(
              Brush.verticalGradient(
                colors = if (isDarkTheme) {
                  listOf(
                    Color(0xFF0A1426).copy(alpha = 0.72f),
                    Color(0xFF0A1426).copy(alpha = 0.34f),
                    Color.Transparent,
                  )
                } else {
                  listOf(
                    Color.White.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.30f),
                    Color.Transparent,
                  )
                },
              ),
            ),
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
                    slideInHorizontally(tween(340, easing = PageSlideEasing)) { fullWidth ->
                      if (forward) fullWidth / 4 else -fullWidth / 4
                    })
                    .togetherWith(
                      fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(300, easing = PageSlideEasing)) { fullWidth ->
                          if (forward) -fullWidth / 5 else fullWidth / 5
                        }
                    )
                },
                label = "topbar_header_anim",
              ) { route ->
                when (route) {
                  ROUTE_EXPENSES -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      LiquidGlassIconBadge(
                        size = 40.dp,
                        shape = RoundedCornerShape(14.dp),
                        tintColor = MaterialTheme.colorScheme.primary,
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(20.dp),
                        )
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
                      LiquidGlassIconBadge(
                        size = 40.dp,
                        shape = RoundedCornerShape(14.dp),
                        tintColor = MaterialTheme.colorScheme.tertiary,
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.AutoAwesome,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.tertiary,
                          modifier = Modifier.size(20.dp),
                        )
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
                      LiquidGlassIconBadge(
                        size = 40.dp,
                        shape = RoundedCornerShape(14.dp),
                        tintColor = MaterialTheme.colorScheme.secondary,
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Savings,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.secondary,
                          modifier = Modifier.size(20.dp),
                        )
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
                      LiquidGlassIconBadge(
                        size = 40.dp,
                        shape = RoundedCornerShape(14.dp),
                        tintColor = EmeraldSafe,
                      ) {
                        Image(
                          painter = painterResource(id = R.drawable.img_sakupintar_logo),
                          contentDescription = stringResource(R.string.app_name),
                          contentScale = ContentScale.Crop,
                          modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(13.dp)),
                        )
                      }
                      Column {
                        Text(
                          text = stringResource(R.string.dashboard_greeting, resolvedDisplayName),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.onBackground,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.testTag("dashboard_greeting_name"),
                        )
                        Text(
                          text = stringResource(R.string.dashboard_subtitle),
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
                LiquidGlassIconBadge(
                  size = 38.dp,
                  shape = CircleShape,
                  tintColor = MaterialTheme.colorScheme.primary,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Forum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              val profileDesc = stringResource(R.string.profile_cd, resolvedDisplayName)
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
                LiquidGlassIconBadge(
                  size = 38.dp,
                  shape = CircleShape,
                  tintColor = EmeraldSafe,
                ) {
                  ProfileAvatar(
                    displayName = resolvedDisplayName,
                    customPhotoBase64 = customPhotoBase64,
                    googlePhotoUrl = googlePhotoUrl,
                    size = 34.dp,
                  )
                }
              }
            },
          )
        },
        bottomBar = {
          FloatingPillBottomBar(
            tabs = navTabs,
            currentRoute = currentRoute,
            onSelectTab = ::navigateToTab,
            onQuickScanClick = ::openQuickReceiptScanner,
          )
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
              containerColor = if (isDarkTheme) Color(0xFF0E9F77).copy(alpha = 0.88f) else ObsidianInk.copy(alpha = 0.88f),
              contentColor = Color.White,
              shape = CircleShape,
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
          when (val state = uiState) {
            UiState.Loading -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = stringResource(R.string.loading_finance_data),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          is UiState.Error -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(20.dp)) {
                  Text(
                    text = stringResource(R.string.connection_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }

          is UiState.Success -> {
            val savingsSuccess = (savingsUiState as? UiState.Success)?.data ?: SavingsUiData()
            NavHost(
              navController = navController,
              startDestination = ROUTE_DASHBOARD,
              enterTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideInHorizontally(
                  animationSpec = tween(durationMillis = 380, easing = PageSlideEasing),
                ) { fullWidth ->
                  if (forward) fullWidth else -fullWidth
                } + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
              },
              exitTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideOutHorizontally(
                  animationSpec = tween(durationMillis = 360, easing = PageSlideEasing),
                ) { fullWidth ->
                  if (forward) -fullWidth / 3 else fullWidth / 3
                } + fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
              },
              popEnterTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideInHorizontally(
                  animationSpec = tween(durationMillis = 380, easing = PageSlideEasing),
                ) { fullWidth ->
                  if (forward) fullWidth else -fullWidth
                } + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
              },
              popExitTransition = {
                val forward = navRouteOrder(targetState.destination.route) >= navRouteOrder(initialState.destination.route)
                slideOutHorizontally(
                  animationSpec = tween(durationMillis = 360, easing = PageSlideEasing),
                ) { fullWidth ->
                  if (forward) -fullWidth / 3 else fullWidth / 3
                } + fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
              },
              modifier = Modifier
                .fillMaxSize()
                .padding(
                  top = innerPadding.calculateTopPadding(),
                  start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                  end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                  bottom = 0.dp,
                ),
            ) {
              composable(ROUTE_DASHBOARD) {
                DashboardScreen(
                  data = state.data,
                  displayName = resolvedDisplayName,
                  customPhotoBase64 = customPhotoBase64,
                  googlePhotoUrl = googlePhotoUrl,
                  totalSavedAmount = savingsSuccess.totalSaved,
                  totalWalletBalance = savingsSuccess.totalWalletBalance + customWallets.sumOf { it.balance },
                  aiInsightState = aiInsightState,
                  onRequestAiInsight = viewModel::generateMonthlyAiInsight,
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
                  onPreviousMonth = { viewModel.shiftMonth(-1L) },
                  onNextMonth = { viewModel.shiftMonth(1L) },
                  onSelectMonth = viewModel::selectMonth,
                  onQuickScanReceipt = ::openQuickReceiptScanner,
                  onLogRecurringTemplate = ::handleLogRecurringTemplate,
                  userId = currentUserId,
                  onOpenAiChat = { showAiChatDialog = true },
                )
              }
              composable(ROUTE_EXPENSES) {
                DailyExpensesScreen(
                  data = state.data,
                  onSearchQueryChange = viewModel::setSearchQuery,
                  onCategoryFilterSelect = viewModel::setCategoryFilter,
                  onEditExpense = { expense ->
                    editingExpense = expense
                    prefillEpochDay = null
                    autoLaunchReceiptScan = false
                    showExpenseSheet = true
                  },
                  onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                  onDeleteExpenseWithUndo = ::handleDeleteExpenseWithUndo,
                  onDuplicateExpense = ::handleDuplicateExpense,
                  onLogRecurringTemplate = ::handleLogRecurringTemplate,
                  onAddRecurringTemplate = viewModel::addRecurringTemplate,
                  onDeleteRecurringTemplate = viewModel::deleteRecurringTemplate,
                  onBillScheduleChanged = { templateId, schedule ->
                    scope.launch {
                      if (schedule != null) {
                        extrasSyncRepo.upsertBillSchedule(schedule)
                      } else {
                        extrasSyncRepo.deleteBillSchedule(templateId)
                      }
                    }
                  },
                  onOpenExportReport = { showExportSheet = true },
                  onPreviousMonth = { viewModel.shiftMonth(-1L) },
                  onNextMonth = { viewModel.shiftMonth(1L) },
                  onSelectMonth = viewModel::selectMonth,
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
                when (val savState = savingsUiState) {
                  UiState.Loading -> {
                    Box(
                      modifier = Modifier.fillMaxSize(),
                      contentAlignment = Alignment.Center,
                    ) {
                      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                  }
                  is UiState.Error -> {
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                          Text(
                            text = stringResource(R.string.savings_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                          )
                          Spacer(modifier = Modifier.height(6.dp))
                          Text(
                            text = savState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                          )
                        }
                      }
                    }
                  }
                  is UiState.Success -> {
                    SavingsScreen(
                      data = savState.data,
                      onCreateGoal = savingsViewModel::createGoal,
                      onUpdateGoal = savingsViewModel::updateGoal,
                      onDepositGoal = savingsViewModel::depositToGoal,
                      onWithdrawGoal = savingsViewModel::withdrawFromGoal,
                      onDeleteGoal = savingsViewModel::deleteGoal,
                      onSetWalletBalance = savingsViewModel::setWalletBalance,
                      userId = currentUserId,
                      onRecordIncome = savingsViewModel::recordIncomeToWallet,
                      onTransferWallet = savingsViewModel::transferBetweenWallets,
                      onDeleteCashflowEntry = savingsViewModel::deleteCashflowEntry,
                      monthKey = selectedMonthKey,
                      onPreviousMonth = { viewModel.shiftMonth(-1L) },
                      onNextMonth = { viewModel.shiftMonth(1L) },
                      onSelectMonth = viewModel::selectMonth,
                      onUpsertCustomWallet = { wallet ->
                        scope.launch { extrasSyncRepo.upsertCustomWallet(wallet) }
                      },
                      onDeleteCustomWallet = { id ->
                        scope.launch { extrasSyncRepo.deleteCustomWallet(id) }
                      },
                      onUpsertDebtRecord = { debt ->
                        scope.launch { extrasSyncRepo.upsertDebtRecord(debt) }
                      },
                      onDeleteDebtRecord = { id ->
                        scope.launch { extrasSyncRepo.deleteDebtRecord(id) }
                      },
                      onUpsertSubscription = { sub ->
                        scope.launch { extrasSyncRepo.upsertSubscription(sub) }
                      },
                      onDeleteSubscription = { id ->
                        scope.launch { extrasSyncRepo.deleteSubscription(id) }
                      },
                      onLogSubscriptionAsExpense = { sub ->
                        viewModel.addExpense(
                          title = sub.name,
                          amount = sub.amount,
                          category = sub.category,
                          paymentMethod = sub.paymentMethod,
                          dateEpochDay = LocalDate.now().toEpochDay(),
                          note = sub.note.ifBlank { "#langganan" },
                        )
                        viewModel.showStatusMessage(
                          context.getString(R.string.sub_logged_expense_status, sub.name)
                        )
                      },
                      onUpsertNetWorthSnapshot = { snapshot ->
                        scope.launch { extrasSyncRepo.upsertNetWorthSnapshot(snapshot) }
                      },
                      onStatusMessage = viewModel::showStatusMessage,
                    )
                  }
                }
              }
              composable(ROUTE_BUDGET) {
                AutoBudgetScreen(
                  data = state.data,
                  currentUserId = currentUserId,
                  isSaving = isSaving,
                  onSaveBudget = viewModel::saveComposedBudget,
                  onPreviousMonth = { viewModel.shiftMonth(-1L) },
                  onNextMonth = { viewModel.shiftMonth(1L) },
                  onSelectMonth = viewModel::selectMonth,
                )
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
      onParseWithAi = viewModel::parseExpenseWithAi,
      onScanReceiptUri = { uri ->
        viewModel.parseReceiptUriWithAi(context.applicationContext, uri)
      },
      onScanReceiptBase64 = viewModel::parseReceiptBase64WithAi,
      onClearAiParse = viewModel::resetAiExpenseParser,
      onCustomCategoryAdded = { cat ->
        scope.launch { extrasSyncRepo.upsertCustomCategory(cat) }
      },
      onCustomCategoryDeleted = { id ->
        scope.launch { extrasSyncRepo.deleteCustomCategory(id) }
      },
      customWallets = customWallets,
      onDeductCustomWallet = { walletId, amount ->
        val updatedWallet = CustomWalletManager.adjustBalance(context, currentUserId, walletId, -amount)
        if (updatedWallet != null) {
          scope.launch { extrasSyncRepo.upsertCustomWallet(updatedWallet) }
        }
      },
      onDismiss = {
        viewModel.resetAiExpenseParser()
        showExpenseSheet = false
        autoLaunchReceiptScan = false
        editingExpense = null
        prefillEpochDay = null
      },
      onSave = { title, amount, category, method, epochDay, note, receiptPhotoBase64 ->
        val currentEdit = editingExpense
        val autoDeduct = PrivacySecurityManager.isAutoDeductWalletEnabled(context)
        if (currentEdit == null) {
          viewModel.addExpense(
            title = title,
            amount = amount,
            category = category,
            paymentMethod = method,
            dateEpochDay = epochDay,
            note = note,
            receiptPhotoBase64 = receiptPhotoBase64,
          )
          if (autoDeduct) {
            savingsViewModel.adjustWalletBalance(method, -amount)
          }
        } else {
          viewModel.updateExpense(
            existing = currentEdit,
            title = title,
            amount = amount,
            category = category,
            paymentMethod = method,
            dateEpochDay = epochDay,
            note = note,
            receiptPhotoBase64 = receiptPhotoBase64,
          )
          if (autoDeduct) {
            if (currentEdit.paymentMethodEnum == method) {
              val diff = currentEdit.amount - amount
              if (diff != 0L) savingsViewModel.adjustWalletBalance(method, diff)
            } else {
              savingsViewModel.adjustWalletBalance(currentEdit.paymentMethodEnum, +currentEdit.amount)
              savingsViewModel.adjustWalletBalance(method, -amount)
            }
          }
        }
        viewModel.resetAiExpenseParser()
        showExpenseSheet = false
        autoLaunchReceiptScan = false
        editingExpense = null
        prefillEpochDay = null
      },
    )
  }

  if (showAccountDialog) {
    AccountProfileDialog(
      displayName = resolvedDisplayName,
      email = currentUser.email,
      customPhotoBase64 = customPhotoBase64,
      googlePhotoUrl = googlePhotoUrl,
      isSavingProfile = isSaving,
      themeMode = themeMode,
      reminderSettings = reminderSettings,
      onReminderSettingsChange = { reminderSettings = it },
      onSaveDisplayName = viewModel::saveDisplayName,
      onPickPhotoUri = { uri ->
        viewModel.saveProfilePhotoFromUri(context.applicationContext, uri, resolvedDisplayName)
      },
      onTakePhotoBitmap = { bitmap ->
        viewModel.saveProfilePhotoFromBitmap(bitmap, resolvedDisplayName)
      },
      onSyncGooglePhoto = {
        viewModel.clearCustomProfilePhoto(resolvedDisplayName)
      },
      onStatusMessage = viewModel::showStatusMessage,
      onSignOut = {
        showAccountDialog = false
        signOutUser(
          credentialManager = credentialManager,
          scope = scope,
          onSignOutComplete = onSignedOut,
        )
      },
      onDismiss = { showAccountDialog = false },
      onOpenExportReport = {
        showAccountDialog = false
        showExportSheet = true
      },
      onDeleteAccount = {
        viewModel.deleteAccountAndData {
          showAccountDialog = false
          signOutUser(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = onSignedOut,
          )
        }
      },
      isDeletingAccount = isSaving,
      userId = currentUserId,
      onBuildBackup = {
        val dash = (uiState as? UiState.Success)?.data
        val sav = (savingsUiState as? UiState.Success)?.data ?: SavingsUiData()
        FullAppBackup(
          expenses = dash?.allHistoricalExpenses.orEmpty(),
          budgets = financeRepoForBackup.getAllBudgets(),
          recurringTemplates = dash?.recurringTemplates.orEmpty(),
          savingsGoals = sav.goals,
          walletBalances = sav.walletBalances,
          cashflowEntries = CashflowLedgerManager.observeEntries(context, currentUserId).value,
          customCategories = CustomCategoryManager.observeCategories(context).value,
          savingsHistory = SavingsHistoryManager.observeEntries(context, currentUserId).value,
          debtRecords = DebtLedgerManager.observeRecords(context, currentUserId).value,
          customWallets = CustomWalletManager.observeWallets(context, currentUserId).value,
        )
      },
      onRestoreBackup = { backup ->
        runCatching {
          var restored = 0
          backup.expenses.forEach { e ->
            financeRepoForBackup.addExpense(
              title = e.title,
              amount = e.amount,
              category = e.category,
              paymentMethod = e.paymentMethod,
              dateEpochDay = e.dateEpochDay,
              monthKey = e.monthKey,
              note = e.note,
              receiptPhotoBase64 = e.receiptPhotoBase64,
            )
            restored++
          }
          backup.budgets.forEach { b ->
            financeRepoForBackup.saveMonthlyBudget(b.copy(userId = currentUserId))
            restored++
          }
          backup.recurringTemplates.forEach { r ->
            financeRepoForBackup.addRecurringTemplate(
              title = r.title,
              amount = r.amount,
              category = r.category,
              paymentMethod = r.paymentMethod,
              note = r.note,
            )
            restored++
          }
          val goalIdMap = mutableMapOf<String, String>()
          backup.savingsGoals.forEach { g ->
            val newId = savingsRepoForBackup.createSavingsGoal(
              title = g.title,
              targetAmount = g.targetAmount,
              initialSavedAmount = g.savedAmount,
              category = SavingsGoalCategory.fromKey(g.category),
              note = g.note,
            )
            if (g.id.isNotBlank()) goalIdMap[g.id] = newId
            restored++
          }
          backup.walletBalances.forEach { (method, bal) ->
            if (bal > 0L) {
              savingsRepoForBackup.setWalletBalance(method, bal)
              restored++
            }
          }
          if (backup.cashflowEntries.isNotEmpty()) {
            CashflowLedgerManager.replaceAll(context, currentUserId, backup.cashflowEntries)
            backup.cashflowEntries.forEach { entry ->
              extrasSyncRepo.upsertCashflowEntry(entry.copy(userId = currentUserId))
              restored++
            }
          }
          if (backup.customCategories.isNotEmpty()) {
            CustomCategoryManager.replaceAll(context, backup.customCategories)
            backup.customCategories.forEach { cat ->
              extrasSyncRepo.upsertCustomCategory(cat)
              restored++
            }
          }
          if (backup.savingsHistory.isNotEmpty()) {
            val mappedHistory = backup.savingsHistory.map { h ->
              h.copy(goalId = goalIdMap[h.goalId] ?: h.goalId)
            }
            SavingsHistoryManager.replaceAll(context, currentUserId, mappedHistory)
            mappedHistory.forEach { h ->
              extrasSyncRepo.addSavingsHistoryEntry(h)
              restored++
            }
          }
          if (backup.debtRecords.isNotEmpty()) {
            DebtLedgerManager.replaceAll(context, currentUserId, backup.debtRecords)
            backup.debtRecords.forEach { d ->
              extrasSyncRepo.upsertDebtRecord(d.copy(userId = currentUserId))
              restored++
            }
          }
          if (backup.customWallets.isNotEmpty()) {
            CustomWalletManager.replaceAll(context, currentUserId, backup.customWallets)
            backup.customWallets.forEach { w ->
              extrasSyncRepo.upsertCustomWallet(w.copy(userId = currentUserId))
              restored++
            }
          }
          restored
        }
      },
      onImportCsvExpenses = { csvExpenses ->
        runCatching {
          var imported = 0
          csvExpenses.forEach { e ->
            financeRepoForBackup.addExpense(
              title = e.title,
              amount = e.amount,
              category = e.category,
              paymentMethod = e.paymentMethod,
              dateEpochDay = e.dateEpochDay,
              monthKey = e.monthKey,
              note = e.note,
              receiptPhotoBase64 = "",
            )
            imported++
          }
          imported
        }
      },
    )
  }

  val successData = (uiState as? UiState.Success)?.data
  if (showExportSheet && successData != null) {
    ExportReportSheet(
      data = successData,
      onStatusMessage = viewModel::showStatusMessage,
      onDismiss = { showExportSheet = false },
    )
  }

  if (showAiChatDialog && successData != null) {
    val savingsForChat = (savingsUiState as? UiState.Success)?.data ?: SavingsUiData()
    AiFinanceChatDialog(
      aiAssistant = viewModel.aiAssistant,
      snapshot = AiFinanceContextSnapshot(
        monthKey = successData.monthKey,
        paydayStartDay = successData.paydayStartDay,
        periodExpenses = successData.expenses,
        allHistoricalExpenses = successData.allHistoricalExpenses,
        budget = successData.budget,
        dailyInsight = successData.dailyInsight,
        walletBalances = savingsForChat.walletBalances,
        savingsGoals = savingsForChat.goals,
        cashflowEntries = cashflowEntries,
        customWallets = customWallets,
        debtRecords = debtRecords,
      ),
      onDismiss = { showAiChatDialog = false },
    )
  }
  }
}

@Composable
internal fun CenterScanNavButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scanTitle = stringResource(R.string.ai_scan_quick_title)
  val scanShortLabel = stringResource(R.string.widget_action_scan)

  Box(
    modifier = modifier
      .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
      .clip(RoundedCornerShape(18.dp))
      .tactilePressScale(
        interactionSource = interactionSource,
        pressedScale = 0.90f,
        enablePressHaptic = false,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(color = EmeraldSafe),
        onClick = onClick,
      )
      .semantics { contentDescription = scanTitle }
      .testTag("nav_center_scan_button"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
      modifier = Modifier.padding(vertical = 4.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(15.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        modifier = Modifier.size(44.dp),
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.linearGradient(
                colors = listOf(
                  EmeraldSafe,
                  Color(0xFF059669),
                )
              )
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.DocumentScanner,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Text(
        text = scanShortLabel,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = EmeraldSafe,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
