package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.signOutUser
import com.example.data.model.Expense
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SavingsRepository
import com.example.notifications.DailyReminderManager
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.ExpenseEditorSheet
import com.example.ui.components.ExportReportSheet
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.ProfileAvatar
import com.example.ui.screens.AutoBudgetScreen
import com.example.ui.screens.DailyExpensesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.theme.ThemePreferenceManager
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SavingsUiData
import com.example.ui.viewmodel.SavingsViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_EXPENSES = "expenses"
const val ROUTE_BUDGET = "budget"
const val ROUTE_SAVINGS = "savings"

private data class NavTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val navTabs = listOf(
  NavTab(
    route = ROUTE_DASHBOARD,
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Rounded.Dashboard,
    unselectedIcon = Icons.Outlined.Dashboard,
    testTag = "nav_tab_dashboard",
  ),
  NavTab(
    route = ROUTE_EXPENSES,
    labelRes = R.string.nav_expenses,
    selectedIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    testTag = "nav_tab_expenses",
  ),
  NavTab(
    route = ROUTE_SAVINGS,
    labelRes = R.string.nav_savings,
    selectedIcon = Icons.Rounded.Savings,
    unselectedIcon = Icons.Outlined.Savings,
    testTag = "nav_tab_savings",
  ),
  NavTab(
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
  var editingExpense by remember { mutableStateOf<Expense?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var showExportSheet by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val themeMode by ThemePreferenceManager.observeThemeMode(context).collectAsStateWithLifecycle()
  var reminderSettings by remember(context) {
    mutableStateOf(DailyReminderManager.getSettings(context))
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

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.height(16.dp))
          navTabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            val label = stringResource(tab.labelRes)
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(tab.route) },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = label,
                )
              },
              label = { Text(label) },
              modifier = Modifier.testTag(tab.testTag),
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
              containerColor = MaterialTheme.colorScheme.background,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            },
            actions = {
              MonthSelectorBar(
                monthKey = selectedMonthKey,
                onPreviousMonth = { viewModel.shiftMonth(-1L) },
                onNextMonth = { viewModel.shiftMonth(1L) },
              )
              val profileDesc = stringResource(R.string.profile_cd, resolvedDisplayName)
              IconButton(
                onClick = { showAccountDialog = true },
                modifier = Modifier
                  .size(48.dp)
                  .semantics { contentDescription = profileDesc }
                  .testTag("account_menu_button"),
              ) {
                ProfileAvatar(
                  displayName = resolvedDisplayName,
                  customPhotoBase64 = customPhotoBase64,
                  googlePhotoUrl = googlePhotoUrl,
                  size = 34.dp,
                )
              }
            },
          )
        },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 3.dp,
            ) {
              navTabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                val label = stringResource(tab.labelRes)
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(tab.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = label,
                    )
                  },
                  label = { Text(label) },
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_EXPENSES) {
            ExtendedFloatingActionButton(
              onClick = {
                editingExpense = null
                showExpenseSheet = true
              },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              shape = RoundedCornerShape(18.dp),
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
              modifier = Modifier.testTag("add_expense_fab"),
            )
          }
        },
      ) { innerPadding ->
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
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            ) {
              composable(ROUTE_DASHBOARD) {
                DashboardScreen(
                  data = state.data,
                  displayName = resolvedDisplayName,
                  customPhotoBase64 = customPhotoBase64,
                  googlePhotoUrl = googlePhotoUrl,
                  totalSavedAmount = savingsSuccess.totalSaved,
                  totalWalletBalance = savingsSuccess.totalWalletBalance,
                  aiInsightState = aiInsightState,
                  onRequestAiInsight = viewModel::generateMonthlyAiInsight,
                  onOpenProfile = { showAccountDialog = true },
                  onOpenExportReport = { showExportSheet = true },
                  onNavigateToExpenses = { navigateToTab(ROUTE_EXPENSES) },
                  onNavigateToAutoBudget = { navigateToTab(ROUTE_BUDGET) },
                  onNavigateToSavings = { navigateToTab(ROUTE_SAVINGS) },
                  onEditExpense = { expense ->
                    editingExpense = expense
                    showExpenseSheet = true
                  },
                )
              }
              composable(ROUTE_EXPENSES) {
                DailyExpensesScreen(
                  data = state.data,
                  onSearchQueryChange = viewModel::setSearchQuery,
                  onCategoryFilterSelect = viewModel::setCategoryFilter,
                  onEditExpense = { expense ->
                    editingExpense = expense
                    showExpenseSheet = true
                  },
                  onDeleteExpense = viewModel::deleteExpense,
                  onLogRecurringTemplate = viewModel::logRecurringTemplate,
                  onAddRecurringTemplate = viewModel::addRecurringTemplate,
                  onDeleteRecurringTemplate = viewModel::deleteRecurringTemplate,
                  onOpenExportReport = { showExportSheet = true },
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
                      onDepositGoal = savingsViewModel::depositToGoal,
                      onWithdrawGoal = savingsViewModel::withdrawFromGoal,
                      onDeleteGoal = savingsViewModel::deleteGoal,
                      onSetWalletBalance = savingsViewModel::setWalletBalance,
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
      aiParseState = aiParseState,
      onParseWithAi = viewModel::parseExpenseWithAi,
      onScanReceiptUri = { uri ->
        viewModel.parseReceiptUriWithAi(context.applicationContext, uri)
      },
      onClearAiParse = viewModel::resetAiExpenseParser,
      onDismiss = {
        viewModel.resetAiExpenseParser()
        showExpenseSheet = false
        editingExpense = null
      },
      onSave = { title, amount, category, method, epochDay, note ->
        val currentEdit = editingExpense
        if (currentEdit == null) {
          viewModel.addExpense(title, amount, category, method, epochDay, note)
        } else {
          viewModel.updateExpense(currentEdit, title, amount, category, method, epochDay, note)
        }
        viewModel.resetAiExpenseParser()
        showExpenseSheet = false
        editingExpense = null
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
}
