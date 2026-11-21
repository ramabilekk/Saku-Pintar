package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.budget.PaydayCycleManager
import com.example.i18n.AppCurrency
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.notifications.DailyReminderManager
import com.example.notifications.ReminderSettings
import com.example.security.PrivacySecurityManager
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SkyPositiveAccent
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
) {
  val context = LocalContext.current
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  var draftName by remember(displayName) { mutableStateOf(displayName) }
  var showLanguagePicker by remember { mutableStateOf(false) }
  var showTimePickerDialog by remember { mutableStateOf(false) }
  var showPhotoSourceDialog by remember { mutableStateOf(false) }
  var expandNameEditor by remember { mutableStateOf(false) }
  var expandCurrencyPicker by remember { mutableStateOf(false) }
  var expandPaydaySection by remember { mutableStateOf(false) }
  var expandPrivacySection by remember { mutableStateOf(false) }
  var showSetupPinDialog by remember { mutableStateOf(false) }

  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val paydayStartDay by PaydayCycleManager.observePaydayStartDay(context).collectAsStateWithLifecycle()
  val hideBalances by PrivacySecurityManager.observeHideBalances(context).collectAsStateWithLifecycle()
  val appLockEnabled by PrivacySecurityManager.observeAppLockEnabled(context).collectAsStateWithLifecycle()
  val autoDeductWallet by PrivacySecurityManager.observeAutoDeductWallet(context).collectAsStateWithLifecycle()

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

  val hasCustomPhoto = !customPhotoBase64.isNullOrBlank()
  val hasGooglePhoto = !googlePhotoUrl.isNullOrBlank()

  val drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    val tactile = rememberTactileFeedback()
    val drawerScrimAlpha by animateFloatAsState(
      targetValue = if (isDarkTheme) 0.62f else 0.44f,
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_scrim_alpha",
    )
    val drawerBorderTop by animateColorAsState(
      targetValue = MaterialTheme.colorScheme.outlineVariant,
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_border_top",
    )
    val drawerBorderMid by animateColorAsState(
      targetValue = MaterialTheme.colorScheme.outlineVariant,
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_border_mid",
    )
    val headerGradStart by animateColorAsState(
      targetValue = if (isDarkTheme) Color(0xFF0B1120) else Color(0xFF0F2C29),
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_header_grad_start",
    )
    val headerGradMid by animateColorAsState(
      targetValue = if (isDarkTheme) Color(0xFF141E33) else Color(0xFF14403A),
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_header_grad_mid",
    )
    val headerGradEnd by animateColorAsState(
      targetValue = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF0D282C),
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_header_grad_end",
    )
    val headerDividerColor by animateColorAsState(
      targetValue = MaterialTheme.colorScheme.outlineVariant,
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_header_divider",
    )
    val expandedSectionBgColor by animateColorAsState(
      targetValue = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (isDarkTheme) 0.32f else 0.45f,
      ),
      animationSpec = tween(420, easing = FastOutSlowInEasing),
      label = "drawer_expanded_section_bg",
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = drawerScrimAlpha))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss,
        ),
    ) {
      // Right-anchored slide-over drawer panel right at the profile button side
      Surface(
        shape = drawerShape,
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .fillMaxHeight()
          .fillMaxWidth(0.86f)
          .widthIn(min = 290.dp, max = 390.dp)
          .clip(drawerShape)
          .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
              colors = listOf(
                drawerBorderTop,
                drawerBorderMid,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
              ),
            ),
            shape = drawerShape,
          )
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          )
          .testTag("profile_right_drawer"),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          // Match Login & Dashboard multi-layered atmospheric backdrop
          AppAmbientBackdrop()

          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .navigationBarsPadding(),
          ) {
            // Top Dimensional Profile Identity Header
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  Brush.linearGradient(
                    colors = listOf(headerGradStart, headerGradMid, headerGradEnd),
                    start = Offset(0f, 0f),
                    end = Offset(700f, 420f),
                  )
                ),
            ) {
              Canvas(modifier = Modifier.matchParentSize()) {
                if (size.width <= 1f || size.height <= 1f) return@Canvas
                val r1 = (size.height * 0.85f).coerceAtLeast(1f)
                drawCircle(
                  brush = Brush.radialGradient(
                    colors = listOf(
                      SkyPositiveAccent.copy(alpha = 0.26f),
                      Color.Transparent,
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.22f),
                    radius = r1,
                  ),
                  radius = r1,
                  center = Offset(size.width * 0.88f, size.height * 0.22f),
                )
                val r2 = (size.height * 0.65f).coerceAtLeast(1f)
                drawCircle(
                  brush = Brush.radialGradient(
                    colors = listOf(
                      GoldAccent.copy(alpha = 0.18f),
                      Color.Transparent,
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.90f),
                    radius = r2,
                  ),
                  radius = r2,
                  center = Offset(size.width * 0.15f, size.height * 0.90f),
                )
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .statusBarsPadding()
                  .padding(start = 18.dp, end = 10.dp, top = 18.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                // Avatar with quick camera/gallery badge
                Box(contentAlignment = Alignment.BottomEnd) {
                  Surface(
                    onClick = {
                      if (!isSavingProfile) {
                        showPhotoSourceDialog = true
                      }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(2.dp),
                  ) {
                    Box(modifier = Modifier.padding(3.dp)) {
                      ProfileAvatar(
                        displayName = displayName,
                        customPhotoBase64 = customPhotoBase64,
                        googlePhotoUrl = googlePhotoUrl,
                        size = 54.dp,
                      )
                    }
                  }
                  Surface(
                    onClick = {
                      if (!isSavingProfile) {
                        showPhotoSourceDialog = true
                      }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier
                      .size(24.dp)
                      .testTag("pick_profile_photo_button"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = stringResource(R.string.profile_photo_title),
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                      )
                    }
                  }
                }

                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = email ?: stringResource(R.string.profile_connected_google),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("profile_close_button"),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(36.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                }
              }
            }

            DrawerFullBleedDivider(
              color = headerDividerColor,
              thickness = 1.dp,
            )

            // Optional Guest-to-Google Cloud Migration Card
            if (isGuestMode && onConnectGoogleAndMigrate != null) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDarkTheme) 0.45f else 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 14.dp)
                  .testTag("guest_sync_google_card"),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(36.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Rounded.CloudUpload,
                          contentDescription = null,
                          tint = Color.White,
                          modifier = Modifier.size(19.dp),
                        )
                      }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = stringResource(R.string.guest_sync_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
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
                      contentColor = Color.White,
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
                        color = Color.White,
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
              }

              DrawerFullBleedDivider(
                color = headerDividerColor,
                thickness = 1.dp,
              )
            }

            // =========================================================================
            // COLUMN 1: EDIT PROFIL (Name + Gallery & Camera Photo Upload)
            // =========================================================================
            DrawerColumnRow(
              icon = Icons.Outlined.Edit,
              badgeColors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5)),
              title = stringResource(R.string.profile_edit_name_title),
              subtitle = displayName,
              trailingIcon = if (expandNameEditor) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              onClick = { expandNameEditor = !expandNameEditor },
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
                  .background(expandedSectionBgColor)
                  .padding(horizontal = 18.dp, vertical = 14.dp),
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

                // Side-by-side Gallery & Camera Profile Photo Upload Buttons
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

            DrawerFullBleedDivider()

            // =========================================================================
            // COLUMN 2: BAHASA
            // =========================================================================
            val languageValue = if (activeLanguage == AppLanguage.SYSTEM) {
              "${stringResource(R.string.language_auto)} (${activeLanguage.resolveEffectiveLanguage().shortCode})"
            } else {
              activeLanguage.nativeName
            }
            DrawerColumnRow(
              icon = Icons.Outlined.Language,
              badgeColors = listOf(Color(0xFF0284C7), Color(0xFF06B6D4)),
              title = stringResource(R.string.language_section_title),
              subtitle = languageValue,
              trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              onClick = { showLanguagePicker = true },
              modifier = Modifier.testTag("profile_language_button"),
            )

            DrawerFullBleedDivider()

            // =========================================================================
            // COLUMN 3: MATA UANG
            // =========================================================================
            DrawerColumnRow(
              icon = Icons.Outlined.AttachMoney,
              badgeColors = listOf(Color(0xFF059669), Color(0xFF10B981)),
              title = stringResource(R.string.currency_section_title),
              subtitle = activeCurrency.displayName,
              trailingIcon = if (expandCurrencyPicker) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              onClick = { expandCurrencyPicker = !expandCurrencyPicker },
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
                  .background(expandedSectionBgColor)
                  .padding(horizontal = 18.dp, vertical = 12.dp),
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
                    modifier = Modifier.testTag("currency_chip_${currency.code}"),
                  )
                }
              }
            }

            DrawerFullBleedDivider()

            // =========================================================================
            // COLUMN 3B: SIKLUS TANGGAL GAJIAN (PAYDAY CYCLE)
            // =========================================================================
            val paydaySubtitle = if (paydayStartDay <= 1) {
              stringResource(R.string.payday_default_subtitle)
            } else {
              stringResource(R.string.payday_custom_subtitle, paydayStartDay)
            }
            DrawerColumnRow(
              icon = Icons.Outlined.CalendarMonth,
              badgeColors = listOf(Color(0xFF0D9488), Color(0xFF14B8A6)),
              title = stringResource(R.string.payday_section_title),
              subtitle = paydaySubtitle,
              trailingIcon = if (expandPaydaySection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              onClick = { expandPaydaySection = !expandPaydaySection },
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
                  .background(expandedSectionBgColor)
                  .padding(horizontal = 18.dp, vertical = 12.dp),
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
                        .size(44.dp)
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
                        .size(44.dp)
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

            DrawerFullBleedDivider()

            // =========================================================================
            // COLUMN 4: TEMA
            // =========================================================================
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable {
                  tactile.tick()
                  val nextMode = if (themeMode == AppThemeMode.DARK) {
                    AppThemeMode.LIGHT
                  } else {
                    AppThemeMode.DARK
                  }
                  ThemePreferenceManager.setThemeMode(context, nextMode)
                  val nextLabel = context.getString(nextMode.labelRes)
                  onStatusMessage(context.getString(R.string.theme_changed_status, nextLabel))
                }
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("profile_theme_button"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              SculptedDrawerBadge(
                icon = Icons.Outlined.Palette,
                gradientColors = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
              )
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = stringResource(R.string.theme_section_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                AnimatedContent(
                  targetState = isDarkTheme,
                  transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                  },
                  label = "drawer_theme_mode_label",
                ) { dark ->
                  Text(
                    text = stringResource(
                      if (dark) R.string.theme_mode_dark else R.string.theme_mode_light
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
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
            }

            DrawerFullBleedDivider()

            // =========================================================================
            // COLUMN 5: REMINDER / PENGINGAT HARIAN
            // =========================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 72.dp)
                  .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                SculptedDrawerBadge(
                  icon = Icons.Outlined.Notifications,
                  gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF9333EA)),
                )
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = stringResource(R.string.reminder_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = if (reminderSettings.enabled) {
                      "%02d:%02d".format(reminderSettings.hour, reminderSettings.minute)
                    } else {
                      stringResource(R.string.reminder_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Switch(
                  checked = reminderSettings.enabled,
                  onCheckedChange = { enable ->
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
                  },
                  modifier = Modifier
                    .scale(0.9f)
                    .testTag("reminder_toggle_switch"),
                )
              }

              AnimatedVisibility(
                visible = reminderSettings.enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                FlowRow(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedSectionBgColor)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
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
                    modifier = Modifier.testTag("reminder_custom_time_button"),
                  )
                  listOf(19, 20, 21).forEach { hourOption ->
                    FilterChip(
                      selected = reminderSettings.hour == hourOption && reminderSettings.minute == 0,
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
                      modifier = Modifier.testTag("reminder_hour_$hourOption"),
                    )
                  }
                }
              }
            }

            DrawerFullBleedDivider()

            // =========================================================================
            // COLUMN 6: PRIVASI & KEAMANAN (Hide Balances, App Lock, Auto-Deduct Wallet)
            // =========================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
              DrawerColumnRow(
                icon = Icons.Outlined.Security,
                badgeColors = listOf(Color(0xFF0F766E), Color(0xFF14B8A6)),
                title = stringResource(R.string.privacy_security_section_title),
                subtitle = if (appLockEnabled) {
                  stringResource(R.string.app_lock_subtitle_on)
                } else if (hideBalances) {
                  stringResource(R.string.privacy_hide_balances_title)
                } else {
                  stringResource(R.string.app_lock_subtitle_off)
                },
                trailingIcon = if (expandPrivacySection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                modifier = Modifier.testTag("drawer_col_privacy_security"),
                onClick = { expandPrivacySection = !expandPrivacySection },
              )

              AnimatedVisibility(
                visible = expandPrivacySection,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(expandedSectionBgColor)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
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
                        .scale(0.88f)
                        .testTag("privacy_hide_balance_switch"),
                    )
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

                  // 2. Kunci Aplikasi (PIN / Biometrik)
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                          .scale(0.88f)
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

                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

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
                        .scale(0.88f)
                        .testTag("auto_deduct_wallet_switch"),
                    )
                  }
                }
              }
            }

            DrawerFullBleedDivider()

            Spacer(modifier = Modifier.height(18.dp))

            // Footer: Support & Sign Out inside sculpted container
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              SculptedSurfaceCard(
                accentColor = CoralDanger,
                cornerRadius = 18,
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  SculptedDrawerBadge(
                    icon = Icons.Rounded.VolunteerActivism,
                    gradientColors = listOf(Color(0xFFE11D48), Color(0xFFF43F5E)),
                    sizeDp = 38,
                  )
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    Text(
                      text = stringResource(R.string.profile_support_title),
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = stringResource(R.string.profile_support_subtitle),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  FilledTonalButton(
                    onClick = {
                      runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_DONATION_URL)).apply {
                          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                      }.onFailure {
                        onStatusMessage(context.getString(R.string.profile_support_browser_error))
                      }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                      .height(48.dp)
                      .testTag("profile_support_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Favorite,
                      contentDescription = null,
                      tint = CoralDanger,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = stringResource(R.string.profile_support_button),
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                    )
                  }
                }
              }

              TextButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("sign_out_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Logout,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.action_sign_out),
                  color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))
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

  // Quick Profile Photo Source Chooser Dialog (Camera vs Gallery)
  if (showPhotoSourceDialog) {
    AlertDialog(
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
    AlertDialog(
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
}

@Composable
private fun SculptedDrawerBadge(
  icon: ImageVector,
  gradientColors: List<Color>,
  sizeDp: Int = 44,
) {
  val shape = RoundedCornerShape(14.dp)
  Surface(
    shape = shape,
    color = Color.Transparent,
    shadowElevation = 4.dp,
    modifier = Modifier.size(sizeDp.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            colors = gradientColors,
            start = Offset(0f, 0f),
            end = Offset(120f, 120f),
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.45f),
              Color.White.copy(alpha = 0.10f),
            )
          ),
          shape = shape,
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size((sizeDp * 0.5f).dp),
      )
    }
  }
}

@Composable
private fun DrawerColumnRow(
  icon: ImageVector,
  badgeColors: List<Color>,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailingIcon: ImageVector? = null,
  enabled: Boolean = true,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 72.dp)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    SculptedDrawerBadge(
      icon = icon,
      gradientColors = badgeColors,
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (trailingIcon != null) {
      Icon(
        imageVector = trailingIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun DrawerFullBleedDivider(
  color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
  thickness: androidx.compose.ui.unit.Dp = 0.8.dp,
) {
  HorizontalDivider(
    thickness = thickness,
    color = color,
    modifier = Modifier.fillMaxWidth(),
  )
}
