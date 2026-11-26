package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.budget.PaydayCycleManager
import com.example.data.local.FullAppBackup
import com.example.data.model.Expense
import com.example.i18n.AppCurrency
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.notifications.BudgetAlertNotificationManager
import com.example.notifications.DailyReminderManager
import com.example.notifications.ReminderSettings
import com.example.security.PrivacySecurityManager
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.ThemePreferenceManager

private const val SUPPORT_DONATION_URL = "https://trakteer.id/ramabilekk/gift"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountProfileDialog(
  displayName: String,
  email: String?,
  customPhotoBase64: String?,
  googlePhotoUrl: String?,
  isSavingProfile: Boolean,
  themeMode: AppThemeMode,
  reminderSettings: ReminderSettings,
  onReminderSettingsChange: (ReminderSettings) -> Unit,
  onSaveDisplayName: (String) -> Unit,
  onPickPhotoUri: (Uri) -> Unit,
  onSyncGooglePhoto: () -> Unit,
  onStatusMessage: (String) -> Unit,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
  onTakePhotoBitmap: (Bitmap) -> Unit = {},
  isGuestMode: Boolean = false,
  guestLocalRecordCount: Int = 0,
  isMigratingGuestData: Boolean = false,
  onConnectGoogleAndMigrate: (() -> Unit)? = null,
  onOpenExportReport: (() -> Unit)? = null,
  onDeleteAccount: (() -> Unit)? = null,
  isDeletingAccount: Boolean = false,
  userId: String = "",
  onBuildBackup: (suspend () -> FullAppBackup)? = null,
  onRestoreBackup: (suspend (FullAppBackup) -> Result<Int>)? = null,
  onImportCsvExpenses: (suspend (List<Expense>) -> Result<Int>)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  val tactile = rememberTactileFeedback()

  var draftName by remember(displayName) { mutableStateOf(displayName) }
  var showLanguagePicker by remember { mutableStateOf(false) }
  var showTimePickerDialog by remember { mutableStateOf(false) }
  var showPhotoSourceDialog by remember { mutableStateOf(false) }
  var showQuickEditProfileDialog by remember { mutableStateOf(false) }
  var showClearCacheDialog by remember { mutableStateOf(false) }
  var showSignOutConfirmDialog by remember { mutableStateOf(false) }
  var showDeleteAccountDialog by remember { mutableStateOf(false) }
  var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
  var showTermsDialog by remember { mutableStateOf(false) }
  var showBackupRestoreDialog by remember { mutableStateOf(false) }
  var expandNameEditor by remember { mutableStateOf(false) }
  var expandCurrencyPicker by remember { mutableStateOf(false) }
  var expandPaydaySection by remember { mutableStateOf(false) }
  var expandPrivacySection by remember { mutableStateOf(false) }
  var showSetupPinDialog by remember { mutableStateOf(false) }
  var cacheSizeBytes by remember { mutableLongStateOf(0L) }

  LaunchedEffect(Unit) {
    cacheSizeBytes = AppCacheManager.calculateCacheSizeBytes(context)
  }

  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val paydayStartDay by PaydayCycleManager.observePaydayStartDay(context).collectAsStateWithLifecycle()
  val hideBalances by PrivacySecurityManager.observeHideBalances(context).collectAsStateWithLifecycle()
  val appLockEnabled by PrivacySecurityManager.observeAppLockEnabled(context).collectAsStateWithLifecycle()
  val autoDeductWallet by PrivacySecurityManager.observeAutoDeductWallet(context).collectAsStateWithLifecycle()
  val hapticFeedbackEnabled by PrivacySecurityManager.observeHapticFeedbackEnabled(context).collectAsStateWithLifecycle()
  val budgetAlertsEnabled by PrivacySecurityManager.observeBudgetAlertsEnabled(context).collectAsStateWithLifecycle()

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      onPickPhotoUri(uri)
    }
  }

  val cameraCaptureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview(),
  ) { bitmap ->
    if (bitmap != null) {
      onTakePhotoBitmap(bitmap)
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      runCatching {
        cameraCaptureLauncher.launch(null)
      }.onFailure { err ->
        onStatusMessage(err.localizedMessage ?: context.getString(R.string.error_generic))
      }
    } else {
      onStatusMessage(context.getString(R.string.camera_permission_denied))
    }
  }

  fun launchProfileCamera() {
    val hasCamPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    if (hasCamPermission) {
      runCatching {
        cameraCaptureLauncher.launch(null)
      }.onFailure { err ->
        onStatusMessage(err.localizedMessage ?: context.getString(R.string.error_generic))
      }
    } else {
      runCatching {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }.onFailure { err ->
        onStatusMessage(err.localizedMessage ?: context.getString(R.string.error_generic))
      }
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      val updated = DailyReminderManager.updateSettings(
        context,
        reminderSettings.copy(enabled = true),
      )
      onReminderSettingsChange(updated)
      val formattedTime = "%02d:%02d".format(updated.hour, updated.minute)
      onStatusMessage(context.getString(R.string.reminder_enabled_status, formattedTime))
    } else {
      val updated = DailyReminderManager.updateSettings(
        context,
        reminderSettings.copy(enabled = false),
      )
      onReminderSettingsChange(updated)
      onStatusMessage(context.getString(R.string.reminder_denied_status))
    }
  }

  fun handleReminderToggle(enable: Boolean) {
    if (enable) {
      val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
      if (needsPermission) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        val updated = DailyReminderManager.updateSettings(
          context,
          reminderSettings.copy(enabled = true),
        )
        onReminderSettingsChange(updated)
        val formattedTime = "%02d:%02d".format(updated.hour, updated.minute)
        onStatusMessage(context.getString(R.string.reminder_enabled_status, formattedTime))
      }
    } else {
      val updated = DailyReminderManager.updateSettings(
        context,
        reminderSettings.copy(enabled = false),
      )
      onReminderSettingsChange(updated)
      onStatusMessage(context.getString(R.string.reminder_disabled_status))
    }
  }

  val budgetAlertPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      PrivacySecurityManager.setBudgetAlertsEnabled(context, true)
      BudgetAlertNotificationManager.ensureChannel(context)
      onStatusMessage(context.getString(R.string.settings_budget_alerts_enabled_status))
    } else {
      PrivacySecurityManager.setBudgetAlertsEnabled(context, false)
      onStatusMessage(context.getString(R.string.reminder_denied_status))
    }
  }

  fun handleBudgetAlertsToggle(enable: Boolean) {
    if (enable) {
      val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
      if (needsPermission) {
        budgetAlertPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        PrivacySecurityManager.setBudgetAlertsEnabled(context, true)
        BudgetAlertNotificationManager.ensureChannel(context)
        onStatusMessage(context.getString(R.string.settings_budget_alerts_enabled_status))
      }
    } else {
      PrivacySecurityManager.setBudgetAlertsEnabled(context, false)
      onStatusMessage(context.getString(R.string.settings_budget_alerts_disabled_status))
    }
  }

  val hasCustomPhoto = !customPhotoBase64.isNullOrBlank()
  val hasGooglePhoto = !googlePhotoUrl.isNullOrBlank()
  val formattedCacheSize = remember(cacheSizeBytes) {
    AppCacheManager.formatCacheSize(cacheSizeBytes)
  }

  val expandedBg = if (isDarkTheme) {
    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  val settingsChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
      alpha = if (isDarkTheme) 0.22f else 0.14f,
    ),
    selectedLabelColor = MaterialTheme.colorScheme.primary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
  )

  BackHandler(onBack = onDismiss)

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {}
      .testTag("profile_right_drawer"),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Same subtle ambient radial wash used across Beranda, Riwayat, Rencana & Tabungan
      AppAmbientBackdrop()

      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
      ) {
          // Clean Top Header: Themed Back Chip + "Pengaturan"
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            LiquidGlassIconBadge(
              size = 48.dp,
              shape = CircleShape,
              tintColor = MaterialTheme.colorScheme.primary,
            ) {
              IconButton(
                onClick = {
                  tactile.tick()
                  onDismiss()
                },
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("profile_close_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = stringResource(R.string.action_close),
                  tint = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
            Text(
              text = stringResource(R.string.settings_screen_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Scrollable Grouped Settings Body
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
          ) {
            Column(
              modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
              // Signature Executive Profile Hero Card (matches Beranda & Tabungan Vault Card)
              DimensionalVaultCard {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = Color.White.copy(alpha = 0.12f),
                      border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.30f)),
                    ) {
                      Box(modifier = Modifier.padding(2.dp)) {
                        ProfileAvatar(
                          displayName = displayName,
                          customPhotoBase64 = customPhotoBase64,
                          googlePhotoUrl = googlePhotoUrl,
                          size = 54.dp,
                        )
                      }
                    }
                    Column(
                      modifier = Modifier.weight(1f),
                      verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                      Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = email?.takeIf { it.isNotBlank() } ?: stringResource(
                          if (isGuestMode) {
                            R.string.settings_guest_subtitle_fallback
                          } else {
                            R.string.profile_connected_google
                          }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  // Translucent Glass Pill Action Buttons on Vault Hero Card
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    SettingsHeaderPillButton(
                      icon = Icons.Outlined.Edit,
                      label = stringResource(R.string.settings_quick_edit_profile),
                      onClick = {
                        tactile.tick()
                        showQuickEditProfileDialog = true
                      },
                      modifier = Modifier
                        .weight(1f)
                        .testTag("quick_edit_profile_button"),
                    )
                    SettingsHeaderPillButton(
                      icon = Icons.Outlined.PhotoCamera,
                      label = stringResource(R.string.settings_quick_change_photo),
                      onClick = {
                        if (!isSavingProfile) {
                          tactile.tick()
                          showPhotoSourceDialog = true
                        }
                      },
                      modifier = Modifier
                        .weight(1f)
                        .testTag("pick_profile_photo_button"),
                    )
                  }
                }
              }

            // =========================================================================
            // SECTION 1: AKUN (Edit Profil, Akun Terhubung / Sinkron Google)
            // =========================================================================
            SettingsSectionLabel(text = stringResource(R.string.settings_section_account))

            SettingsGroupCard(isDarkTheme = isDarkTheme) {
              // 1. Edit Profil (Nama & Foto)
              SettingsRowItem(
                icon = Icons.Outlined.Edit,
                title = stringResource(R.string.profile_edit_name_title),
                subtitle = displayName,
                trailingIcon = if (expandNameEditor) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                onClick = {
                  tactile.tick()
                  expandNameEditor = !expandNameEditor
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("profile_name_row"),
              )

              AnimatedVisibility(
                visible = expandNameEditor,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(50) },
                    label = { Text(stringResource(R.string.profile_display_name_label)) },
                    placeholder = { Text(stringResource(R.string.profile_display_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("profile_name_input"),
                  )

                  val trimmedDraft = draftName.trim()
                  FilledTonalButton(
                    onClick = {
                      onSaveDisplayName(trimmedDraft)
                      expandNameEditor = false
                    },
                    enabled = !isSavingProfile && trimmedDraft.isNotEmpty() && trimmedDraft != displayName,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("save_profile_name_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Save,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      if (isSavingProfile) {
                        stringResource(R.string.action_saving)
                      } else {
                        stringResource(R.string.profile_save_name_button)
                      }
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = {
                        if (!isSavingProfile) {
                          photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                          )
                        }
                      },
                      enabled = !isSavingProfile,
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("edit_profile_gallery_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (hasCustomPhoto) {
                          stringResource(R.string.profile_photo_change)
                        } else {
                          stringResource(R.string.profile_photo_pick)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }

                    FilledTonalButton(
                      onClick = {
                        if (!isSavingProfile) {
                          launchProfileCamera()
                        }
                      },
                      enabled = !isSavingProfile,
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("take_profile_photo_camera_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = stringResource(R.string.profile_photo_camera),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  if (hasCustomPhoto) {
                    TextButton(
                      onClick = onSyncGooglePhoto,
                      enabled = !isSavingProfile,
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_google_photo_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        if (hasGooglePhoto) {
                          stringResource(R.string.profile_photo_use_google)
                        } else {
                          stringResource(R.string.profile_photo_remove_custom)
                        }
                      )
                    }
                  }
                }
              }

              SettingsInsetDivider()

              // 2. Akun Terhubung / Hubungkan Google (Guest)
              if (isGuestMode && onConnectGoogleAndMigrate != null) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .testTag("guest_sync_google_card"),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    SettingsIconBox(
                      icon = Icons.Rounded.CloudUpload,
                      isDarkTheme = isDarkTheme,
                      tint = MaterialTheme.colorScheme.primary,
                      containerColor = MaterialTheme.colorScheme.primary.copy(
                        alpha = if (isDarkTheme) 0.18f else 0.12f,
                      ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = stringResource(R.string.guest_sync_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = if (guestLocalRecordCount > 0) {
                          stringResource(R.string.guest_sync_card_subtitle, guestLocalRecordCount)
                        } else {
                          stringResource(R.string.guest_sync_card_subtitle_empty)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Button(
                    onClick = onConnectGoogleAndMigrate,
                    enabled = !isMigratingGuestData,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("guest_sync_google_button"),
                  ) {
                    if (isMigratingGuestData) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = stringResource(R.string.guest_sync_in_progress),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = stringResource(R.string.guest_sync_cta),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              } else {
                SettingsRowItem(
                  icon = Icons.Outlined.PeopleOutline,
                  title = stringResource(R.string.settings_connected_account_title),
                  subtitle = email?.takeIf { it.isNotBlank() } ?: stringResource(
                    if (isGuestMode) {
                      R.string.settings_guest_subtitle_fallback
                    } else {
                      R.string.profile_connected_google
                    }
                  ),
                  trailingIcon = null,
                  onClick = null,
                  isDarkTheme = isDarkTheme,
                )
              }
            }

            // =========================================================================
            // SECTION 2: UMUM (Tema, Bahasa, Mata Uang, Efek Getar)
            // =========================================================================
            SettingsSectionLabel(text = stringResource(R.string.settings_section_general))

            SettingsGroupCard(isDarkTheme = isDarkTheme) {
              // 1. Tema Warna
              SettingsRowItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.theme_section_title),
                subtitleContent = {
                  AnimatedContent(
                    targetState = isDarkTheme,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "settings_theme_label",
                  ) { dark ->
                    Text(
                      text = stringResource(if (dark) R.string.theme_mode_dark else R.string.theme_mode_light),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                },
                trailingContent = {
                  ThemeLogoToggle(
                    themeMode = themeMode,
                    onSelectThemeMode = { mode ->
                      ThemePreferenceManager.setThemeMode(context, mode)
                      val modeLabel = context.getString(mode.labelRes)
                      onStatusMessage(context.getString(R.string.theme_changed_status, modeLabel))
                    },
                    lightButtonTag = "theme_mode_LIGHT",
                    darkButtonTag = "theme_mode_DARK",
                  )
                },
                onClick = {
                  tactile.tick()
                  val nextMode = if (themeMode == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
                  ThemePreferenceManager.setThemeMode(context, nextMode)
                  val nextLabel = context.getString(nextMode.labelRes)
                  onStatusMessage(context.getString(R.string.theme_changed_status, nextLabel))
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("profile_theme_button"),
              )

              SettingsInsetDivider()

              // 2. Bahasa
              val languageValue = if (activeLanguage == AppLanguage.SYSTEM) {
                "${stringResource(R.string.language_auto)} (${activeLanguage.resolveEffectiveLanguage().shortCode})"
              } else {
                activeLanguage.nativeName
              }
              SettingsRowItem(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.language_section_title),
                subtitle = languageValue,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = {
                  tactile.tick()
                  showLanguagePicker = true
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("profile_language_button"),
              )

              SettingsInsetDivider()

              // 3. Mata Uang
              SettingsRowItem(
                icon = Icons.Outlined.AttachMoney,
                title = stringResource(R.string.currency_section_title),
                subtitle = activeCurrency.displayName,
                trailingIcon = if (expandCurrencyPicker) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                onClick = {
                  tactile.tick()
                  expandCurrencyPicker = !expandCurrencyPicker
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("profile_currency_button"),
              )

              AnimatedVisibility(
                visible = expandCurrencyPicker,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                FlowRow(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  AppCurrency.entries.forEach { currency ->
                    FilterChip(
                      selected = activeCurrency == currency,
                      onClick = {
                        CurrencyPreferenceManager.setCurrency(context, currency)
                        onStatusMessage(context.getString(R.string.currency_changed_status, currency.displayName))
                        expandCurrencyPicker = false
                      },
                      label = { Text(currency.displayName) },
                      colors = settingsChipColors,
                      border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeCurrency == currency,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                      ),
                      modifier = Modifier.testTag("currency_chip_${currency.code}"),
                    )
                  }
                }
              }

              SettingsInsetDivider()

              // 4. Efek Getar (Haptic Feedback)
              SettingsRowItem(
                icon = Icons.Outlined.Vibration,
                title = stringResource(R.string.settings_haptic_title),
                subtitle = stringResource(
                  if (hapticFeedbackEnabled) {
                    R.string.settings_haptic_subtitle_on
                  } else {
                    R.string.settings_haptic_subtitle_off
                  }
                ),
                trailingContent = {
                  Switch(
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                      PrivacySecurityManager.setHapticFeedbackEnabled(context, enabled)
                      if (enabled) tactile.tick()
                    },
                    modifier = Modifier
                      .scale(0.88f)
                      .testTag("settings_haptic_switch"),
                  )
                },
                onClick = {
                  val next = !hapticFeedbackEnabled
                  PrivacySecurityManager.setHapticFeedbackEnabled(context, next)
                  if (next) tactile.tick()
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("settings_haptic_row"),
              )
            }

            // =========================================================================
            // SECTION 2: KEUANGAN & KEAMANAN (Siklus Gajian, Pengingat, Privasi)
            // =========================================================================
            SettingsSectionLabel(text = stringResource(R.string.settings_section_finance))

            SettingsGroupCard(isDarkTheme = isDarkTheme) {
              // 1. Siklus Tanggal Gajian
              val paydaySubtitle = if (paydayStartDay <= 1) {
                stringResource(R.string.payday_default_subtitle)
              } else {
                stringResource(R.string.payday_custom_subtitle, paydayStartDay)
              }
              SettingsRowItem(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.payday_section_title),
                subtitle = paydaySubtitle,
                trailingIcon = if (expandPaydaySection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                  tactile.tick()
                  expandPaydaySection = !expandPaydaySection
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("profile_payday_button"),
              )

              AnimatedVisibility(
                visible = expandPaydaySection,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                val previewCycle = remember(paydayStartDay) {
                  val curKey = PaydayCycleManager.currentCycleMonthKey(paydayStartDay = paydayStartDay)
                  PaydayCycleManager.resolveCycle(curKey, paydayStartDay)
                }
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = stringResource(R.string.payday_cycle_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    listOf(
                      1 to stringResource(R.string.payday_preset_1),
                      25 to stringResource(R.string.payday_preset_25),
                      28 to stringResource(R.string.payday_preset_28),
                    ).forEach { (day, label) ->
                      FilterChip(
                        selected = paydayStartDay == day,
                        onClick = {
                          PaydayCycleManager.setPaydayStartDay(context, day)
                          onStatusMessage(context.getString(R.string.payday_saved_status, day))
                        },
                        label = { Text(label) },
                        colors = settingsChipColors,
                        border = FilterChipDefaults.filterChipBorder(
                          enabled = true,
                          selected = paydayStartDay == day,
                          borderColor = MaterialTheme.colorScheme.outlineVariant,
                          selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        ),
                        modifier = Modifier.testTag("payday_preset_$day"),
                      )
                    }
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = stringResource(R.string.payday_day_label, paydayStartDay),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      OutlinedButton(
                        onClick = {
                          val next = (paydayStartDay - 1).coerceIn(1, 28)
                          PaydayCycleManager.setPaydayStartDay(context, next)
                        },
                        enabled = paydayStartDay > 1,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                          .size(48.dp)
                          .testTag("payday_minus_button"),
                      ) {
                        Icon(imageVector = Icons.Rounded.Remove, contentDescription = "-")
                      }
                      OutlinedButton(
                        onClick = {
                          val next = (paydayStartDay + 1).coerceIn(1, 28)
                          PaydayCycleManager.setPaydayStartDay(context, next)
                        },
                        enabled = paydayStartDay < 28,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                          .size(48.dp)
                          .testTag("payday_plus_button"),
                      ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "+")
                      }
                    }
                  }
                  Text(
                    text = stringResource(
                      R.string.payday_preview_label,
                      previewCycle.formatRangeLabel(activeLanguage.toLocale()),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }

              SettingsInsetDivider()

              // 2. Pengingat Harian
              SettingsRowItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.reminder_title),
                subtitle = if (reminderSettings.enabled) {
                  "%02d:%02d".format(reminderSettings.hour, reminderSettings.minute)
                } else {
                  stringResource(R.string.reminder_subtitle)
                },
                trailingContent = {
                  Switch(
                    checked = reminderSettings.enabled,
                    onCheckedChange = { enable -> handleReminderToggle(enable) },
                    modifier = Modifier
                      .scale(0.88f)
                      .testTag("reminder_toggle_switch"),
                  )
                },
                onClick = {
                  tactile.tick()
                  handleReminderToggle(!reminderSettings.enabled)
                },
                accentColor = MaterialTheme.colorScheme.secondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("reminder_toggle_row"),
              )

              AnimatedVisibility(
                visible = reminderSettings.enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                FlowRow(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  val isCustomTime = reminderSettings.minute != 0 ||
                    reminderSettings.hour !in listOf(19, 20, 21)
                  val formattedCurrentTime = "%02d:%02d".format(reminderSettings.hour, reminderSettings.minute)
                  FilterChip(
                    selected = isCustomTime,
                    onClick = { showTimePickerDialog = true },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    label = {
                      Text(
                        if (isCustomTime) {
                          formattedCurrentTime
                        } else {
                          stringResource(R.string.reminder_custom_time_action)
                        }
                      )
                    },
                    colors = settingsChipColors,
                    border = FilterChipDefaults.filterChipBorder(
                      enabled = true,
                      selected = isCustomTime,
                      borderColor = MaterialTheme.colorScheme.outlineVariant,
                      selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    ),
                    modifier = Modifier.testTag("reminder_custom_time_button"),
                  )
                  listOf(19, 20, 21).forEach { hourOption ->
                    val hourSelected = reminderSettings.hour == hourOption && reminderSettings.minute == 0
                    FilterChip(
                      selected = hourSelected,
                      onClick = {
                        val updated = DailyReminderManager.updateSettings(
                          context,
                          reminderSettings.copy(hour = hourOption, minute = 0),
                        )
                        onReminderSettingsChange(updated)
                        val formattedTime = "%02d:%02d".format(updated.hour, updated.minute)
                        onStatusMessage(context.getString(R.string.reminder_time_changed, formattedTime))
                      },
                      label = { Text("%02d:00".format(hourOption)) },
                      colors = settingsChipColors,
                      border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = hourSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                      ),
                      modifier = Modifier.testTag("reminder_hour_$hourOption"),
                    )
                  }
                }
              }

              SettingsInsetDivider()

              // 2b. Notifikasi Batas Anggaran (80% & 100%)
              SettingsRowItem(
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.settings_budget_alerts_title),
                subtitle = stringResource(
                  if (budgetAlertsEnabled) {
                    R.string.settings_budget_alerts_subtitle_on
                  } else {
                    R.string.settings_budget_alerts_subtitle_off
                  }
                ),
                trailingContent = {
                  Switch(
                    checked = budgetAlertsEnabled,
                    onCheckedChange = { enable -> handleBudgetAlertsToggle(enable) },
                    modifier = Modifier
                      .scale(0.88f)
                      .testTag("settings_budget_alerts_switch"),
                  )
                },
                onClick = {
                  tactile.tick()
                  handleBudgetAlertsToggle(!budgetAlertsEnabled)
                },
                accentColor = MaterialTheme.colorScheme.secondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("settings_budget_alerts_row"),
              )

              SettingsInsetDivider()

              // 3. Privasi & Keamanan
              SettingsRowItem(
                icon = Icons.Outlined.Security,
                title = stringResource(R.string.privacy_security_section_title),
                subtitle = if (appLockEnabled) {
                  stringResource(R.string.app_lock_subtitle_on)
                } else if (hideBalances) {
                  stringResource(R.string.privacy_hide_balances_title)
                } else {
                  stringResource(R.string.app_lock_subtitle_off)
                },
                trailingIcon = if (expandPrivacySection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                onClick = {
                  tactile.tick()
                  expandPrivacySection = !expandPrivacySection
                },
                accentColor = MaterialTheme.colorScheme.secondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("drawer_col_privacy_security"),
              )

              AnimatedVisibility(
                visible = expandPrivacySection,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  // 1. Sembunyikan Saldo
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = stringResource(R.string.privacy_hide_balances_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = stringResource(R.string.privacy_hide_balances_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Switch(
                      checked = hideBalances,
                      onCheckedChange = { PrivacySecurityManager.setHideBalances(context, it) },
                      modifier = Modifier
                        .scale(0.86f)
                        .testTag("privacy_hide_balance_switch"),
                    )
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                  // 2. Kunci Aplikasi (PIN / Biometrik)
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = stringResource(R.string.app_lock_title),
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.SemiBold,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          text = stringResource(
                            if (appLockEnabled) R.string.app_lock_subtitle_on else R.string.app_lock_subtitle_off,
                          ),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { enable ->
                          if (enable) {
                            if (PrivacySecurityManager.hasPinConfigured(context)) {
                              PrivacySecurityManager.setAppLockEnabled(context, true)
                              onStatusMessage(context.getString(R.string.app_lock_pin_saved))
                            } else {
                              showSetupPinDialog = true
                            }
                          } else {
                            PrivacySecurityManager.setAppLockEnabled(context, false)
                            onStatusMessage(context.getString(R.string.app_lock_disabled_status))
                          }
                        },
                        modifier = Modifier
                          .scale(0.86f)
                          .testTag("app_lock_toggle_switch"),
                      )
                    }
                    if (appLockEnabled) {
                      TextButton(
                        onClick = { showSetupPinDialog = true },
                        modifier = Modifier.testTag("change_pin_button"),
                      ) {
                        Text(
                          text = stringResource(R.string.app_lock_change_pin),
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    }
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                  // 3. Auto-Deduct Wallet Balance
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = stringResource(R.string.wallet_auto_deduct_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = stringResource(R.string.wallet_auto_deduct_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Switch(
                      checked = autoDeductWallet,
                      onCheckedChange = { PrivacySecurityManager.setAutoDeductWallet(context, it) },
                      modifier = Modifier
                        .scale(0.86f)
                        .testTag("auto_deduct_wallet_switch"),
                    )
                  }
                }
              }

              if (onOpenExportReport != null) {
                SettingsInsetDivider()

                // 4. Ekspor Laporan Keuangan
                SettingsRowItem(
                  icon = Icons.Outlined.FileDownload,
                  title = stringResource(R.string.settings_export_title),
                  subtitle = stringResource(R.string.settings_export_subtitle),
                  trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  accentColor = MaterialTheme.colorScheme.secondary,
                  onClick = {
                    tactile.tick()
                    onOpenExportReport()
                  },
                  isDarkTheme = isDarkTheme,
                  modifier = Modifier.testTag("settings_export_report_button"),
                )
              }

              if (onBuildBackup != null && onRestoreBackup != null) {
                SettingsInsetDivider()

                // 4b. Cadangkan & Pulihkan Data (JSON)
                SettingsRowItem(
                  icon = Icons.Rounded.CloudUpload,
                  title = stringResource(R.string.settings_backup_restore_title),
                  subtitle = stringResource(R.string.settings_backup_restore_subtitle),
                  trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  accentColor = MaterialTheme.colorScheme.secondary,
                  onClick = {
                    tactile.tick()
                    showBackupRestoreDialog = true
                  },
                  isDarkTheme = isDarkTheme,
                  modifier = Modifier.testTag("settings_backup_restore_button"),
                )
              }

              SettingsInsetDivider()

              // 5. Pengaturan Cache
              SettingsRowItem(
                icon = Icons.Outlined.CleaningServices,
                title = stringResource(R.string.settings_cache_title),
                subtitle = stringResource(R.string.settings_cache_subtitle, formattedCacheSize),
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = {
                  tactile.tick()
                  showClearCacheDialog = true
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("settings_clear_cache_button"),
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dedicated Destructive Card: Keluar & Hapus Akun (matching reference layout)
            SettingsGroupCard(isDarkTheme = isDarkTheme) {
              SettingsRowItem(
                icon = Icons.AutoMirrored.Rounded.Logout,
                title = stringResource(R.string.action_sign_out),
                subtitle = stringResource(R.string.settings_sign_out_subtitle),
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                isDestructive = true,
                onClick = {
                  tactile.tick()
                  showSignOutConfirmDialog = true
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("sign_out_button"),
              )

              if (onDeleteAccount != null) {
                SettingsInsetDivider()

                SettingsRowItem(
                  icon = Icons.Outlined.DeleteOutline,
                  title = stringResource(
                    if (isGuestMode) {
                      R.string.settings_delete_account_title_guest
                    } else {
                      R.string.settings_delete_account_title
                    }
                  ),
                  subtitle = stringResource(
                    if (isGuestMode) {
                      R.string.settings_delete_account_subtitle_guest
                    } else {
                      R.string.settings_delete_account_subtitle
                    }
                  ),
                  trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  isDestructive = true,
                  onClick = {
                    tactile.tick()
                    showDeleteAccountDialog = true
                  },
                  isDarkTheme = isDarkTheme,
                  modifier = Modifier.testTag("settings_delete_account_button"),
                )
              }
            }

            // =========================================================================
            // SECTION 4: TENTANG APLIKASI (Privasi, S&K, Hubungi Kami, Dukung, Versi)
            // =========================================================================
            SettingsSectionLabel(text = stringResource(R.string.settings_section_about))

            SettingsGroupCard(isDarkTheme = isDarkTheme) {
              SettingsRowItem(
                icon = Icons.Outlined.PrivacyTip,
                title = stringResource(R.string.settings_privacy_policy_title),
                subtitle = stringResource(R.string.settings_privacy_policy_subtitle),
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = {
                  tactile.tick()
                  showPrivacyPolicyDialog = true
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("settings_privacy_policy_button"),
              )

              SettingsInsetDivider()

              SettingsRowItem(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.settings_terms_title),
                subtitle = stringResource(R.string.settings_terms_subtitle),
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = {
                  tactile.tick()
                  showTermsDialog = true
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("settings_terms_button"),
              )

              SettingsInsetDivider()

              val contactEmail = stringResource(R.string.settings_contact_email)
              val contactSubject = stringResource(R.string.settings_contact_email_subject)
              SettingsRowItem(
                icon = Icons.Outlined.MailOutline,
                title = stringResource(R.string.settings_contact_title),
                subtitle = contactEmail,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = {
                  tactile.tick()
                  launchSupportEmailOrCopy(
                    context = context,
                    email = contactEmail,
                    subject = contactSubject,
                    onCopiedMessage = onStatusMessage,
                  )
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("settings_contact_button"),
              )

              SettingsInsetDivider()

              SettingsRowItem(
                icon = Icons.Rounded.VolunteerActivism,
                title = stringResource(R.string.profile_support_title),
                subtitle = stringResource(R.string.profile_support_subtitle),
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = {
                  tactile.tick()
                  runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_DONATION_URL)).apply {
                      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                  }.onFailure {
                    onStatusMessage(context.getString(R.string.profile_support_browser_error))
                  }
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.testTag("profile_support_button"),
              )

              SettingsInsetDivider()

              SettingsRowItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_app_version_title),
                subtitle = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                trailingIcon = null,
                onClick = null,
                isDarkTheme = isDarkTheme,
              )
            }

            Spacer(modifier = Modifier.height(32.dp))
          }
        }
      }
    }
  }

  if (showSetupPinDialog) {
    SetupPinDialog(
      onPinSaved = {
        showSetupPinDialog = false
        onStatusMessage(context.getString(R.string.app_lock_pin_saved))
      },
      onDismiss = { showSetupPinDialog = false },
    )
  }

  if (showPhotoSourceDialog) {
    LiquidGlassAlertDialog(
      onDismissRequest = { showPhotoSourceDialog = false },
      title = {
        Text(
          text = stringResource(R.string.profile_photo_source_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = stringResource(R.string.profile_photo_source_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FilledTonalButton(
            onClick = {
              showPhotoSourceDialog = false
              launchProfileCamera()
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("photo_source_camera_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.PhotoCamera,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.profile_photo_camera),
              fontWeight = FontWeight.SemiBold,
            )
          }
          OutlinedButton(
            onClick = {
              showPhotoSourceDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("photo_source_gallery_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.PhotoLibrary,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (hasCustomPhoto) {
                stringResource(R.string.profile_photo_change)
              } else {
                stringResource(R.string.profile_photo_pick)
              },
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showPhotoSourceDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showTimePickerDialog) {
    val timePickerState = rememberTimePickerState(
      initialHour = reminderSettings.hour,
      initialMinute = reminderSettings.minute,
      is24Hour = true,
    )
    LiquidGlassAlertDialog(
      onDismissRequest = { showTimePickerDialog = false },
      title = {
        Text(
          text = stringResource(R.string.reminder_picker_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
      },
      text = {
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          TimePicker(state = timePickerState)
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val updated = DailyReminderManager.updateSettings(
              context,
              reminderSettings.copy(
                hour = timePickerState.hour,
                minute = timePickerState.minute,
              ),
            )
            onReminderSettingsChange(updated)
            val formattedTime = "%02d:%02d".format(updated.hour, updated.minute)
            onStatusMessage(context.getString(R.string.reminder_time_changed, formattedTime))
            showTimePickerDialog = false
          },
          modifier = Modifier.testTag("reminder_time_confirm_button"),
        ) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePickerDialog = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showLanguagePicker) {
    LanguagePickerDialog(
      selectedLanguage = activeLanguage,
      onLanguageSelected = { picked ->
        val updatedCtx = LanguagePreferenceManager.localizedContext(context, picked)
        val pickedLabel = if (picked == AppLanguage.SYSTEM) {
          updatedCtx.getString(R.string.language_auto)
        } else {
          picked.nativeName
        }
        onStatusMessage(updatedCtx.getString(R.string.language_changed_status, pickedLabel))
      },
      onDismiss = { showLanguagePicker = false },
    )
  }

  if (showQuickEditProfileDialog) {
    EditProfileQuickDialog(
      currentDisplayName = displayName,
      isSavingProfile = isSavingProfile,
      onSaveDisplayName = onSaveDisplayName,
      onChangePhotoClick = { showPhotoSourceDialog = true },
      onDismiss = { showQuickEditProfileDialog = false },
    )
  }

  if (showClearCacheDialog) {
    ClearCacheConfirmDialog(
      formattedSize = formattedCacheSize,
      onConfirmClear = {
        showClearCacheDialog = false
        scope.launch {
          val clearedBytes = AppCacheManager.clearAppCache(context)
          cacheSizeBytes = AppCacheManager.calculateCacheSizeBytes(context)
          val clearedLabel = AppCacheManager.formatCacheSize(clearedBytes)
          onStatusMessage(context.getString(R.string.settings_cache_cleared_status, clearedLabel))
        }
      },
      onDismiss = { showClearCacheDialog = false },
    )
  }

  if (showSignOutConfirmDialog) {
    SignOutConfirmDialog(
      isGuestMode = isGuestMode,
      onConfirmSignOut = {
        showSignOutConfirmDialog = false
        onSignOut()
      },
      onDismiss = { showSignOutConfirmDialog = false },
    )
  }

  if (showDeleteAccountDialog && onDeleteAccount != null) {
    DeleteAccountConfirmDialog(
      isGuestMode = isGuestMode,
      isDeleting = isDeletingAccount,
      onConfirmDelete = {
        onDeleteAccount()
      },
      onDismiss = { showDeleteAccountDialog = false },
    )
  }

  if (showPrivacyPolicyDialog) {
    LegalInfoDialog(
      title = stringResource(R.string.settings_privacy_policy_title),
      body = stringResource(R.string.settings_privacy_policy_body),
      onDismiss = { showPrivacyPolicyDialog = false },
    )
  }

  if (showTermsDialog) {
    LegalInfoDialog(
      title = stringResource(R.string.settings_terms_title),
      body = stringResource(R.string.settings_terms_body),
      onDismiss = { showTermsDialog = false },
    )
  }

  if (showBackupRestoreDialog && onBuildBackup != null && onRestoreBackup != null) {
    BackupRestoreDialog(
      userId = userId,
      onBuildBackup = onBuildBackup,
      onRestoreBackup = onRestoreBackup,
      onStatusMessage = onStatusMessage,
      onDismiss = { showBackupRestoreDialog = false },
      onImportCsvExpenses = onImportCsvExpenses,
    )
  }
}

@Composable
private fun SettingsSectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 8.dp),
  )
}

@Composable
private fun SettingsGroupCard(
  isDarkTheme: Boolean,
  content: @Composable ColumnScope.() -> Unit,
) {
  SculptedSurfaceCard(
    cornerRadius = 22,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      content = content,
    )
  }
}

@Composable
private fun SettingsHeaderPillButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedButton(
    onClick = onClick,
    shape = CircleShape,
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = Color.White.copy(alpha = 0.12f),
      contentColor = Color.White,
    ),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    modifier = modifier.height(48.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = Color.White,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SettingsIconBox(
  icon: ImageVector,
  isDarkTheme: Boolean,
  tint: Color = MaterialTheme.colorScheme.primary,
  containerColor: Color = MaterialTheme.colorScheme.primary.copy(
    alpha = if (isDarkTheme) 0.16f else 0.11f,
  ),
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    modifier = Modifier.size(40.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun SettingsRowItem(
  icon: ImageVector,
  title: String,
  isDarkTheme: Boolean,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  subtitleContent: (@Composable () -> Unit)? = null,
  trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
  trailingContent: (@Composable () -> Unit)? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  isDestructive: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  val effectiveTint = if (isDestructive) MaterialTheme.colorScheme.error else accentColor
  val iconBg = effectiveTint.copy(alpha = if (isDarkTheme) 0.16f else 0.11f)
  val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

  val rowModifier = if (onClick != null) {
    modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp)
  } else {
    modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .padding(horizontal = 14.dp, vertical = 12.dp)
  }

  Row(
    modifier = rowModifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    SettingsIconBox(
      icon = icon,
      isDarkTheme = isDarkTheme,
      tint = effectiveTint,
      containerColor = iconBg,
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = titleColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitleContent != null) {
        subtitleContent()
      } else if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (trailingContent != null) {
      trailingContent()
    } else if (trailingIcon != null) {
      Icon(
        imageVector = trailingIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun SettingsInsetDivider() {
  HorizontalDivider(
    thickness = 0.75.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
    modifier = Modifier.padding(start = 68.dp, end = 14.dp),
  )
}
