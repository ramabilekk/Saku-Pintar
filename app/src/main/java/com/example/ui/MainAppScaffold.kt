package com.example.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

internal fun navRouteOrder(route: String?): Int = when (route) {
  ROUTE_DASHBOARD -> 0
  ROUTE_EXPENSES -> 1
  ROUTE_SAVINGS -> 2
  ROUTE_BUDGET -> 3
  else -> 0
}

private val PageSlideEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

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
  var prefillEpochDay by remember { mutableStateOf<Long?>(null) }
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
                Surface(
                  shape = CircleShape,
                  color = Color.Transparent,
                  border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                ) {
                  Box(modifier = Modifier.padding(2.dp)) {
                    ProfileAvatar(
                      displayName = resolvedDisplayName,
                      customPhotoBase64 = customPhotoBase64,
                      googlePhotoUrl = googlePhotoUrl,
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
                navTabs.forEach { tab ->
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
                    prefillEpochDay = null
                    showExpenseSheet = true
                  },
                  onPreviousMonth = { viewModel.shiftMonth(-1L) },
                  onNextMonth = { viewModel.shiftMonth(1L) },
                  onSelectMonth = viewModel::selectMonth,
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
                    showExpenseSheet = true
                  },
                  onDeleteExpense = viewModel::deleteExpense,
                  onLogRecurringTemplate = viewModel::logRecurringTemplate,
                  onAddRecurringTemplate = viewModel::addRecurringTemplate,
                  onDeleteRecurringTemplate = viewModel::deleteRecurringTemplate,
                  onOpenExportReport = { showExportSheet = true },
                  onPreviousMonth = { viewModel.shiftMonth(-1L) },
                  onNextMonth = { viewModel.shiftMonth(1L) },
                  onSelectMonth = viewModel::selectMonth,
                  onAddExpenseOnDate = { epochDay ->
                    editingExpense = null
                    prefillEpochDay = epochDay
                    showExpenseSheet = true
                  },
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
                      monthKey = selectedMonthKey,
                      onPreviousMonth = { viewModel.shiftMonth(-1L) },
                      onNextMonth = { viewModel.shiftMonth(1L) },
                      onSelectMonth = viewModel::selectMonth,
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
    }
  }

  if (showExpenseSheet) {
    ExpenseEditorSheet(
      existingExpense = editingExpense,
      initialEpochDay = prefillEpochDay,
      aiParseState = aiParseState,
      onParseWithAi = viewModel::parseExpenseWithAi,
      onScanReceiptUri = { uri ->
        viewModel.parseReceiptUriWithAi(context.applicationContext, uri)
      },
      onScanReceiptBase64 = viewModel::parseReceiptBase64WithAi,
      onClearAiParse = viewModel::resetAiExpenseParser,
      onDismiss = {
        viewModel.resetAiExpenseParser()
        showExpenseSheet = false
        editingExpense = null
        prefillEpochDay = null
      },
      onSave = { title, amount, category, method, epochDay, note, receiptPhotoBase64 ->
        val currentEdit = editingExpense
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
        }
        viewModel.resetAiExpenseParser()
        showExpenseSheet = false
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
