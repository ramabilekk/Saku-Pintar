package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.auth.AccountSecurityOtpManager
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.data.local.GuestToGoogleMigrationManager
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SavingsRepository
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.security.PrivacySecurityManager
import com.example.ui.LocalAccountScaffold
import com.example.ui.MainAppScaffold
import com.example.ui.auth.AuthScreen
import com.example.ui.components.AppLockGateScreen
import com.example.ui.components.ForceUpdateScreen
import com.example.ui.components.UpdateCheckingSplash
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SakuPintarTheme
import com.example.ui.theme.ThemePreferenceManager
import com.example.update.AppUpdateChecker
import com.example.widget.WidgetQuickActions
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var pendingWidgetActionState by mutableStateOf<String?>(null)

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(LanguagePreferenceManager.wrapActivityContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    pendingWidgetActionState = intent?.getStringExtra(WidgetQuickActions.EXTRA_WIDGET_ACTION)
    setContent {
      val themeMode by ThemePreferenceManager.observeThemeMode(applicationContext)
        .collectAsStateWithLifecycle()
      val activeLanguage by LanguagePreferenceManager.observeLanguage(applicationContext)
        .collectAsStateWithLifecycle()
      // Keep CurrencyPreferenceManager initialized at app startup
      CurrencyPreferenceManager.observeCurrency(applicationContext).collectAsStateWithLifecycle()

      val localizedContext = remember(this, activeLanguage) {
        LanguagePreferenceManager.wrapActivityContext(this, activeLanguage)
      }
      val isRtl = remember(activeLanguage) {
        LanguagePreferenceManager.isRtl(activeLanguage)
      }

      val systemDark = isSystemInDarkTheme()
      val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }

      SideEffect {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !useDarkTheme
        insetsController.isAppearanceLightNavigationBars = !useDarkTheme
      }

      val appLockEnabled by PrivacySecurityManager.observeAppLockEnabled(applicationContext)
        .collectAsStateWithLifecycle()
      var isAppLocked by remember {
        mutableStateOf(PrivacySecurityManager.isAppLockEnabled(applicationContext))
      }
      val activityLifecycleOwner = LocalLifecycleOwner.current

      LaunchedEffect(appLockEnabled) {
        if (!appLockEnabled) {
          isAppLocked = false
        }
      }

      DisposableEffect(activityLifecycleOwner, appLockEnabled) {
        val lockObserver = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_STOP && appLockEnabled) {
            isAppLocked = true
          }
        }
        activityLifecycleOwner.lifecycle.addObserver(lockObserver)
        onDispose {
          activityLifecycleOwner.lifecycle.removeObserver(lockObserver)
        }
      }

      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalActivityResultRegistryOwner provides this@MainActivity,
      ) {
        SakuPintarTheme(darkTheme = useDarkTheme) {
          Box(modifier = Modifier.fillMaxSize()) {
            AppRoot(
              pendingWidgetAction = pendingWidgetActionState,
              onConsumeWidgetAction = { pendingWidgetActionState = null },
            )
            if (appLockEnabled && isAppLocked) {
              AppLockGateScreen(
                onUnlocked = { isAppLocked = false },
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    configureAppCheck(this, intent)
    val widgetAction = intent.getStringExtra(WidgetQuickActions.EXTRA_WIDGET_ACTION)
    if (!widgetAction.isNullOrBlank()) {
      pendingWidgetActionState = widgetAction
    }
  }
}

@Composable
private fun AppRoot(
  pendingWidgetAction: String?,
  onConsumeWidgetAction: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  var requiredUpdate by remember {
    mutableStateOf(AppUpdateChecker.getCachedRequiredUpdate(context))
  }
  var isInitialUpdateCheckPending by remember {
    mutableStateOf(BuildConfig.IS_GITHUB_RELEASE_BUILD && requiredUpdate == null)
  }
  var isRecheckingUpdate by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (BuildConfig.IS_GITHUB_RELEASE_BUILD) {
      requiredUpdate = AppUpdateChecker.checkForUpdate(context)
    }
    isInitialUpdateCheckPending = false
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME && BuildConfig.IS_GITHUB_RELEASE_BUILD) {
        coroutineScope.launch {
          requiredUpdate = AppUpdateChecker.checkForUpdate(context)
          isInitialUpdateCheckPending = false
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  if (isInitialUpdateCheckPending) {
    UpdateCheckingSplash()
    return
  }

  val activeUpdate = requiredUpdate
  if (activeUpdate != null) {
    ForceUpdateScreen(
      updateInfo = activeUpdate,
      isRechecking = isRecheckingUpdate,
      onRecheck = {
        if (!isRecheckingUpdate) {
          isRecheckingUpdate = true
          coroutineScope.launch {
            requiredUpdate = AppUpdateChecker.checkForUpdate(context)
            isRecheckingUpdate = false
          }
        }
      },
    )
    return
  }

  val isFirebaseReady = remember(context) {
    FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
  }
  if (!isFirebaseReady) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = stringResource(R.string.firebase_missing_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
          )
          Text(
            text = stringResource(R.string.firebase_missing_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
    return
  }

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val altSession by AccountSecurityOtpManager.observeAltSession(context).collectAsStateWithLifecycle()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isMigratingGuestData by remember { mutableStateOf(false) }
  var pendingMigrationBanner by remember { mutableStateOf<String?>(null) }

  val user = currentUser
  if (user == null) {
    val activeAlt = altSession
    if (activeAlt != null) {
      key(activeAlt.userId) {
        LocalAccountScaffold(
          session = activeAlt,
          onSignedOut = {
            AccountSecurityOtpManager.clearAltSession(context)
          },
          pendingWidgetAction = pendingWidgetAction,
          onConsumeWidgetAction = onConsumeWidgetAction,
          initialStatusMessage = pendingMigrationBanner,
          onConsumeInitialStatus = { pendingMigrationBanner = null },
          isMigratingGuestData = isMigratingGuestData,
          onConnectGoogleAndMigrate = {
            if (isMigratingGuestData) return@LocalAccountScaffold
            val guestUserId = activeAlt.userId
            val appCtx = context.applicationContext
            isMigratingGuestData = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              scope = coroutineScope,
              onAuthSuccess = {
                coroutineScope.launch {
                  try {
                    val result = GuestToGoogleMigrationManager.migrateGuestToGoogle(
                      context = appCtx,
                      guestUserId = guestUserId,
                      financeRepository = FinanceRepository(appCtx),
                      savingsRepository = SavingsRepository(appCtx),
                    )
                    AccountSecurityOtpManager.clearAltSession(appCtx)
                    val localized = LanguagePreferenceManager.localizedContext(appCtx)
                    pendingMigrationBanner = when {
                      result.totalMigrated == 0 && result.failedCount == 0 ->
                        localized.getString(R.string.guest_sync_connected_empty)
                      result.failedCount > 0 ->
                        localized.getString(
                          R.string.guest_sync_partial,
                          result.totalMigrated,
                          result.failedCount,
                        )
                      else ->
                        localized.getString(R.string.guest_sync_success, result.totalMigrated)
                    }
                  } catch (e: Exception) {
                    val localized = LanguagePreferenceManager.localizedContext(appCtx)
                    pendingMigrationBanner =
                      e.localizedMessage ?: localized.getString(R.string.error_generic)
                  } finally {
                    isMigratingGuestData = false
                  }
                }
              },
              onAuthError = { errMsg ->
                isMigratingGuestData = false
                pendingMigrationBanner = errMsg
              },
              onAuthCancelled = {
                isMigratingGuestData = false
              },
            )
          },
        )
      }
    } else {
      AuthScreen(
        onAuthSuccess = {},
      )
    }
  } else {
    key(user.uid) {
      MainAppScaffold(
        currentUser = user,
        onSignedOut = {},
        pendingWidgetAction = pendingWidgetAction,
        onConsumeWidgetAction = onConsumeWidgetAction,
        initialStatusMessage = pendingMigrationBanner,
        onConsumeInitialStatus = { pendingMigrationBanner = null },
      )
    }
  }
}
