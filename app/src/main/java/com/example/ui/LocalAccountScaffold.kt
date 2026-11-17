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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dashboard
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.data.ai.FinanceAiAssistant
import com.example.data.budget.BudgetAutoComposer
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.notifications.DailyReminderManager
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.AppAmbientBackdrop
import com.example.ui.components.ExpenseEditorSheet
import com.example.ui.components.ExportReportSheet
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.rememberTactileFeedback
import com.example.ui.components.tactilePressScale
import com.example.ui.screens.AutoBudgetScreen
import com.example.ui.screens.DailyExpensesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.theme.ThemePreferenceManager
import com.example.ui.viewmodel.AiExpenseParseState
import com.example.ui.viewmodel.AiInsightUiState
import com.example.ui.viewmodel.CategoryProgress
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.ui.viewmodel.SavingsUiData
import com.example.util.Formatters
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

private val LocalPageSlideEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private data class LocalNavTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val localNavTabs = listOf(
  LocalNavTab(
    route = ROUTE_DASHBOARD,
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Rounded.Dashboard,
    unselectedIcon = Icons.Outlined.Dashboard,
    testTag = "nav_tab_dashboard",
  ),
  LocalNavTab(
    route = ROUTE_EXPENSES,
    labelRes = R.string.nav_expenses,
    selectedIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
    testTag = "nav_tab_expenses",
  ),
  LocalNavTab(
    route = ROUTE_SAVINGS,
    labelRes = R.string.nav_savings,
    selectedIcon = Icons.Rounded.Savings,
    unselectedIcon = Icons.Outlined.Savings,
    testTag = "nav_tab_savings",
  ),
  LocalNavTab(
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
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val aiAssistant = remember { FinanceAiAssistant() }

  val themeMode by ThemePreferenceManager.observeThemeMode(context).collectAsStateWithLifecycle()
  var reminderSettings by remember(context) {
    mutableStateOf(DailyReminderManager.getSettings(context))
  }

  var selectedMonthKey by remember { mutableStateOf(Formatters.currentMonthKey()) }
  var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val isGuestVisitor = session.method == AuthMethodType.GUEST

  var allExpenses by remember(session.userId, isGuestVisitor) {
    mutableStateOf(
      if (isGuestVisitor) emptyList() else LocalVaultStorage.loadExpenses(context, session.userId)
    )
  }
  var budgetsByMonth by remember(session.userId, isGuestVisitor) {
    mutableStateOf(
      if (isGuestVisitor) emptyMap() else LocalVaultStorage.loadBudgets(context, session.userId)
    )
  }
  var recurringTemplates by remember(session.userId, isGuestVisitor) {
    mutableStateOf(
      if (isGuestVisitor) emptyList() else LocalVaultStorage.loadRecurring(context, session.userId)
    )
  }
  var savingsGoals by remember(session.userId, isGuestVisitor) {
    mutableStateOf(
      if (isGuestVisitor) emptyList() else LocalVaultStorage.loadGoals(context, session.userId)
    )
  }
  var walletBalances by remember(session.userId, isGuestVisitor) {
    mutableStateOf(
      if (isGuestVisitor) emptyMap() else LocalVaultStorage.loadWallets(context, session.userId)
    )
  }

  var aiInsightState by remember { mutableStateOf<AiInsightUiState>(AiInsightUiState.Idle) }
  var aiParseState by remember { mutableStateOf<AiExpenseParseState>(AiExpenseParseState.Idle) }
  var isSaving by remember { mutableStateOf(false) }

  var showExpenseSheet by remember { mutableStateOf(false) }
  var editingExpense by remember { mutableStateOf<Expense?>(null) }
  var prefillEpochDay by remember { mutableStateOf<Long?>(null) }
  var showAccountDialog by remember { mutableStateOf(false) }
  var showExportSheet by remember { mutableStateOf(false) }

  fun showToastMessage(msg: String) {
    scope.launch {
      snackbarHostState.showSnackbar(msg)
    }
  }

  val monthExpenses = remember(allExpenses, selectedMonthKey) {
    allExpenses.filter { it.monthKey == selectedMonthKey }
      .sortedByDescending { it.dateEpochDay }
  }
  val activeBudget = budgetsByMonth[selectedMonthKey]
  val dailyInsight = remember(activeBudget, monthExpenses, selectedMonthKey) {
    BudgetAutoComposer.calculateDailyInsight(
      budget = activeBudget,
      monthExpenses = monthExpenses,
      monthKey = selectedMonthKey,
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

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination
  val currentRoute = currentDestination?.route ?: ROUTE_DASHBOARD
  val tactile = rememberTactileFeedback()

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

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.height(16.dp))
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
                targetState = currentRoute ?: ROUTE_DASHBOARD,
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier.size(38.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Rounded.Dashboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                          )
                        }
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
                  color = Color.Transparent,
                  border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
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
            Column {
              HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
              )
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
              ) {
                localNavTabs.forEach { tab ->
                  val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                  val label = stringResource(tab.labelRes)
                  val tabInteractionSource = remember { MutableInteractionSource() }
                  val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1.0f,
                    animationSpec = spring(
                      dampingRatio = Spring.DampingRatioMediumBouncy,
                      stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "nav_icon_scale_${tab.route}",
                  )
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateToTab(tab.route) },
                    interactionSource = tabInteractionSource,
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
                    label = {
                      Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                      )
                    },
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier
                      .tactilePressScale(
                        interactionSource = tabInteractionSource,
                        pressedScale = 0.92f,
                        enablePressHaptic = false,
                      )
                      .testTag(tab.testTag),
                  )
                }
              }
            }
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
                showExpenseSheet = true
              },
              interactionSource = fabInteractionSource,
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
              .padding(innerPadding),
          ) {
            if (isGuestVisitor) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                border = BorderStroke(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f),
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
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = stringResource(R.string.guest_visitor_banner),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                  showExpenseSheet = true
                },
                onPreviousMonth = { shiftMonth(-1L) },
                onNextMonth = { shiftMonth(1L) },
                onSelectMonth = { selectedMonthKey = it },
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
                  showExpenseSheet = true
                },
                onDeleteExpense = { id ->
                  allExpenses = allExpenses.filterNot { it.id == id }
                  LocalVaultStorage.saveExpenses(context, session.userId, allExpenses)
                  showToastMessage(context.getString(R.string.vm_expense_deleted))
                },
                onLogRecurringTemplate = { template ->
                  val today = LocalDate.now()
                  val monthKey = Formatters.currentMonthKey(today)
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
                  LocalVaultStorage.saveExpenses(context, session.userId, allExpenses)
                  showToastMessage(context.getString(R.string.vm_expense_added, template.title))
                },
                onAddRecurringTemplate = { title, amount, category, method, note ->
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
                  LocalVaultStorage.saveRecurring(context, session.userId, recurringTemplates)
                  showToastMessage(context.getString(R.string.vm_recurring_added))
                },
                onDeleteRecurringTemplate = { id ->
                  recurringTemplates = recurringTemplates.filterNot { it.id == id }
                  LocalVaultStorage.saveRecurring(context, session.userId, recurringTemplates)
                  showToastMessage(context.getString(R.string.vm_recurring_deleted))
                },
                onOpenExportReport = { showExportSheet = true },
                onPreviousMonth = { shiftMonth(-1L) },
                onNextMonth = { shiftMonth(1L) },
                onSelectMonth = { selectedMonthKey = it },
                onAddExpenseOnDate = { epochDay ->
                  editingExpense = null
                  prefillEpochDay = epochDay
                  showExpenseSheet = true
                },
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
                  LocalVaultStorage.saveGoals(context, session.userId, savingsGoals)
                  showToastMessage(context.getString(R.string.vm_goal_created, goal.title))
                },
                onDepositGoal = { goal, amount ->
                  val updatedAmount = (goal.savedAmount + amount).coerceAtLeast(0L)
                  savingsGoals = savingsGoals.map {
                    if (it.id == goal.id) it.copy(savedAmount = updatedAmount) else it
                  }
                  LocalVaultStorage.saveGoals(context, session.userId, savingsGoals)
                  showToastMessage(context.getString(R.string.vm_goal_deposited, amount.toString(), goal.title))
                },
                onWithdrawGoal = { goal, amount ->
                  val updatedAmount = (goal.savedAmount - amount).coerceAtLeast(0L)
                  savingsGoals = savingsGoals.map {
                    if (it.id == goal.id) it.copy(savedAmount = updatedAmount) else it
                  }
                  LocalVaultStorage.saveGoals(context, session.userId, savingsGoals)
                  showToastMessage(context.getString(R.string.vm_goal_withdrawn, amount.toString(), goal.title))
                },
                onDeleteGoal = { id ->
                  savingsGoals = savingsGoals.filterNot { it.id == id }
                  LocalVaultStorage.saveGoals(context, session.userId, savingsGoals)
                  showToastMessage(context.getString(R.string.vm_goal_deleted))
                },
                onSetWalletBalance = { method, balance ->
                  walletBalances = walletBalances + (method to balance.coerceAtLeast(0L))
                  LocalVaultStorage.saveWallets(context, session.userId, walletBalances)
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
                  LocalVaultStorage.saveBudgets(context, session.userId, budgetsByMonth)
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
        editingExpense = null
        prefillEpochDay = null
      },
      onSave = { title, amount, category, method, epochDay, note, receiptPhotoBase64 ->
        val monthKey = Formatters.currentMonthKey(LocalDate.ofEpochDay(epochDay))
        val currentEdit = editingExpense
        if (currentEdit == null) {
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
          LocalVaultStorage.saveExpenses(context, session.userId, allExpenses)
          showToastMessage(context.getString(R.string.vm_expense_added, added.title))
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
          LocalVaultStorage.saveExpenses(context, session.userId, allExpenses)
          showToastMessage(context.getString(R.string.vm_expense_updated))
        }
        aiParseState = AiExpenseParseState.Idle
        showExpenseSheet = false
        editingExpense = null
        prefillEpochDay = null
      },
    )
  }

  if (showAccountDialog) {
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
    )
  }

  if (showExportSheet) {
    ExportReportSheet(
      data = dashboardData,
      onStatusMessage = ::showToastMessage,
      onDismiss = { showExportSheet = false },
    )
  }
}

private object LocalVaultStorage {
  private const val PREF_PREFIX = "sakupintar_local_vault_"

  private fun isGuestVisitor(userId: String): Boolean =
    userId.startsWith("guest_")

  private fun prefs(context: Context, userId: String) =
    context.applicationContext.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)

  fun loadExpenses(context: Context, userId: String): List<Expense> {
    if (isGuestVisitor(userId)) return emptyList()
    val raw = prefs(context, userId).getString("expenses", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            Expense(
              id = obj.optString("id"),
              userId = userId,
              title = obj.optString("title"),
              amount = obj.optLong("amount", 0L),
              category = obj.optString("category", ExpenseCategory.FOOD.name),
              paymentMethod = obj.optString("paymentMethod", PaymentMethod.QRIS.name),
              dateEpochDay = obj.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
              monthKey = obj.optString("monthKey", Formatters.currentMonthKey()),
              note = obj.optString("note", ""),
              receiptPhotoBase64 = obj.optString("receiptPhotoBase64", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveExpenses(context: Context, userId: String, list: List<Expense>) {
    if (isGuestVisitor(userId)) return
    val arr = JSONArray()
    list.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("amount", item.amount)
          put("category", item.category)
          put("paymentMethod", item.paymentMethod)
          put("dateEpochDay", item.dateEpochDay)
          put("monthKey", item.monthKey)
          put("note", item.note)
          put("receiptPhotoBase64", item.receiptPhotoBase64)
        }
      )
    }
    prefs(context, userId).edit().putString("expenses", arr.toString()).apply()
  }

  fun loadBudgets(context: Context, userId: String): Map<String, MonthlyBudget> {
    if (isGuestVisitor(userId)) return emptyMap()
    val raw = prefs(context, userId).getString("budgets", null) ?: return emptyMap()
    return runCatching {
      val arr = JSONArray(raw)
      buildMap {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val monthKey = obj.optString("monthKey")
          val catObj = obj.optJSONObject("categoryBudgets")
          val catMap = mutableMapOf<String, Long>()
          if (catObj != null) {
            catObj.keys().forEach { k -> catMap[k] = catObj.optLong(k, 0L) }
          }
          put(
            monthKey,
            MonthlyBudget(
              userId = userId,
              monthKey = monthKey,
              monthlyIncome = obj.optLong("monthlyIncome", 0L),
              strategy = obj.optString("strategy", "BALANCED_50_30_20"),
              savingsTarget = obj.optLong("savingsTarget", 0L),
              needsPercent = obj.optLong("needsPercent", 50L),
              wantsPercent = obj.optLong("wantsPercent", 30L),
              savingsPercent = obj.optLong("savingsPercent", 20L),
              categoryBudgets = catMap,
            )
          )
        }
      }
    }.getOrDefault(emptyMap())
  }

  fun saveBudgets(context: Context, userId: String, map: Map<String, MonthlyBudget>) {
    if (isGuestVisitor(userId)) return
    val arr = JSONArray()
    map.values.forEach { b ->
      val catObj = JSONObject()
      b.categoryBudgets.forEach { (k, v) -> catObj.put(k, v) }
      arr.put(
        JSONObject().apply {
          put("monthKey", b.monthKey)
          put("monthlyIncome", b.monthlyIncome)
          put("strategy", b.strategy)
          put("savingsTarget", b.savingsTarget)
          put("needsPercent", b.needsPercent)
          put("wantsPercent", b.wantsPercent)
          put("savingsPercent", b.savingsPercent)
          put("categoryBudgets", catObj)
        }
      )
    }
    prefs(context, userId).edit().putString("budgets", arr.toString()).apply()
  }

  fun loadRecurring(context: Context, userId: String): List<RecurringTemplate> {
    if (isGuestVisitor(userId)) return emptyList()
    val raw = prefs(context, userId).getString("recurring", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            RecurringTemplate(
              id = obj.optString("id"),
              userId = userId,
              title = obj.optString("title"),
              amount = obj.optLong("amount", 0L),
              category = obj.optString("category", ExpenseCategory.BILLS.name),
              paymentMethod = obj.optString("paymentMethod", PaymentMethod.TRANSFER.name),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveRecurring(context: Context, userId: String, list: List<RecurringTemplate>) {
    if (isGuestVisitor(userId)) return
    val arr = JSONArray()
    list.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("amount", item.amount)
          put("category", item.category)
          put("paymentMethod", item.paymentMethod)
          put("note", item.note)
        }
      )
    }
    prefs(context, userId).edit().putString("recurring", arr.toString()).apply()
  }

  fun loadGoals(context: Context, userId: String): List<SavingsGoal> {
    if (isGuestVisitor(userId)) return emptyList()
    val raw = prefs(context, userId).getString("goals", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            SavingsGoal(
              id = obj.optString("id"),
              userId = userId,
              title = obj.optString("title"),
              targetAmount = obj.optLong("targetAmount", 0L),
              savedAmount = obj.optLong("savedAmount", 0L),
              category = obj.optString("category", SavingsGoalCategory.EMERGENCY.name),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveGoals(context: Context, userId: String, list: List<SavingsGoal>) {
    if (isGuestVisitor(userId)) return
    val arr = JSONArray()
    list.forEach { g ->
      arr.put(
        JSONObject().apply {
          put("id", g.id)
          put("title", g.title)
          put("targetAmount", g.targetAmount)
          put("savedAmount", g.savedAmount)
          put("category", g.category)
          put("note", g.note)
        }
      )
    }
    prefs(context, userId).edit().putString("goals", arr.toString()).apply()
  }

  fun loadWallets(context: Context, userId: String): Map<PaymentMethod, Long> {
    if (isGuestVisitor(userId)) return emptyMap()
    val raw = prefs(context, userId).getString("wallets", null) ?: return emptyMap()
    return runCatching {
      val obj = JSONObject(raw)
      PaymentMethod.entries.associateWith { m -> obj.optLong(m.name, 0L) }
    }.getOrDefault(emptyMap())
  }

  fun saveWallets(context: Context, userId: String, map: Map<PaymentMethod, Long>) {
    if (isGuestVisitor(userId)) return
    val obj = JSONObject()
    map.forEach { (m, b) -> obj.put(m.name, b) }
    prefs(context, userId).edit().putString("wallets", obj.toString()).apply()
  }
}
