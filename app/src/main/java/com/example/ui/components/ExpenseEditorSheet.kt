package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.CustomCategory
import com.example.data.local.CustomCategoryManager
import com.example.data.local.CustomWallet
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.SkyPositiveAccent
import com.example.ui.viewmodel.AiExpenseParseState
import com.example.util.EventTagHelper
import com.example.util.Formatters
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditorSheet(
  existingExpense: Expense?,
  aiParseState: AiExpenseParseState,
  onParseWithAi: (String) -> Unit,
  onScanReceiptUri: (Uri) -> Unit,
  onClearAiParse: () -> Unit,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    dateEpochDay: Long,
    note: String,
    receiptPhotoBase64: String,
  ) -> Unit,
  initialEpochDay: Long? = null,
  onScanReceiptBase64: (String) -> Unit = {},
  autoLaunchReceiptScan: Boolean = false,
  onCustomCategoryAdded: (CustomCategory) -> Unit = {},
  onCustomCategoryDeleted: (String) -> Unit = {},
  customWallets: List<CustomWallet> = emptyList(),
  onDeductCustomWallet: ((walletId: String, amount: Long) -> Unit)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val customCategories by CustomCategoryManager.observeCategories(context).collectAsStateWithLifecycle()
  val activeLocale = activeLanguage.toLocale()
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  var selectedCustomWalletId by remember(existingExpense?.id) { mutableStateOf<String?>(null) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val today = remember { LocalDate.now() }

  var title by remember(existingExpense) { mutableStateOf(existingExpense?.title ?: "") }
  var amountText by remember(existingExpense) {
    mutableStateOf(
      if (existingExpense != null && existingExpense.amount > 0L) {
        existingExpense.amount.toString()
      } else {
        ""
      }
    )
  }
  var selectedCategory by remember(existingExpense) {
    mutableStateOf(existingExpense?.categoryEnum ?: ExpenseCategory.FOOD)
  }
  var selectedMethod by remember(existingExpense) {
    mutableStateOf(existingExpense?.paymentMethodEnum ?: PaymentMethod.QRIS)
  }
  var selectedEpochDay by remember(existingExpense, initialEpochDay) {
    mutableLongStateOf(existingExpense?.dateEpochDay ?: initialEpochDay ?: today.toEpochDay())
  }
  var note by remember(existingExpense) { mutableStateOf(existingExpense?.note ?: "") }
  var receiptPhotoBase64 by remember(existingExpense) {
    mutableStateOf(existingExpense?.receiptPhotoBase64 ?: "")
  }
  var isEncodingReceipt by remember { mutableStateOf(false) }
  var showFullReceiptViewer by remember { mutableStateOf(false) }
  var aiPromptText by remember(existingExpense) { mutableStateOf("") }
  var showDatePicker by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var aiScanSuccessDetectedDate by remember { mutableStateOf<Long?>(null) }
  var showAiScanSuccessBanner by remember { mutableStateOf(false) }
  var showQuickScanChooser by remember(autoLaunchReceiptScan) { mutableStateOf(autoLaunchReceiptScan) }
  var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraTriggersAi by remember { mutableStateOf(false) }

  fun createTempReceiptCameraUri(): Uri? = runCatching {
    val camDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val photoFile = File(camDir, "receipt_${System.currentTimeMillis()}.jpg")
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
  }.getOrNull()

  // 1. Launcher for AI Scan + Auto Attach Receipt from Gallery (Zero-permission Photo Picker)
  val aiReceiptScannerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      onScanReceiptUri(uri)
    }
  }

  // 1b. Zero-permission Voice-to-Expense AI via system Speech Recognizer Intent
  var voiceErrorText by remember { mutableStateOf<String?>(null) }
  val voiceRecognizerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull()
        ?.trim()
      if (!spoken.isNullOrBlank()) {
        voiceErrorText = null
        aiPromptText = spoken.take(160)
        onParseWithAi(spoken.take(160))
      }
    }
  }

  fun launchVoiceExpenseAi() {
    voiceErrorText = null
    if (aiParseState is AiExpenseParseState.Error) onClearAiParse()
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLocale.toLanguageTag())
      putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_ai_prompt))
    }
    runCatching {
      voiceRecognizerLauncher.launch(intent)
    }.onFailure {
      voiceErrorText = context.getString(R.string.voice_ai_unavailable)
    }
  }

  // 2. Launcher for Attaching Receipt Photo from Gallery (without auto AI scan)
  val attachGalleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isEncodingReceipt = true
        try {
          receiptPhotoBase64 = ImageUtils.encodeReceiptBase64(context.applicationContext, uri)
          validationError = null
        } catch (e: Exception) {
          validationError = e.localizedMessage
        } finally {
          isEncodingReceipt = false
        }
      }
    }
  }

  // 3. Full-HD Camera Capture via FileProvider (for sharp Vision AI OCR & receipt archiving)
  val fullHdCameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val capturedUri = pendingCameraUri
    val shouldScanWithAi = pendingCameraTriggersAi
    pendingCameraUri = null
    pendingCameraTriggersAi = false
    if (success && capturedUri != null) {
      if (shouldScanWithAi) {
        onScanReceiptUri(capturedUri)
      } else {
        scope.launch {
          isEncodingReceipt = true
          try {
            receiptPhotoBase64 = ImageUtils.encodeReceiptBase64(context.applicationContext, capturedUri)
            validationError = null
          } catch (e: Exception) {
            validationError = e.localizedMessage
          } finally {
            isEncodingReceipt = false
          }
        }
      }
    }
  }

  fun startFullHdCameraCapture(triggerAiScan: Boolean) {
    val uri = createTempReceiptCameraUri()
    if (uri == null) {
      validationError = context.getString(R.string.error_generic)
      return
    }
    pendingCameraUri = uri
    pendingCameraTriggersAi = triggerAiScan
    runCatching {
      fullHdCameraLauncher.launch(uri)
    }.onFailure { err ->
      pendingCameraUri = null
      pendingCameraTriggersAi = false
      validationError = err.localizedMessage
    }
  }

  val receiptCameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      startFullHdCameraCapture(triggerAiScan = pendingCameraTriggersAi)
    } else {
      pendingCameraTriggersAi = false
      validationError = context.getString(R.string.camera_permission_denied)
    }
  }

  fun launchReceiptCamera(triggerAiScan: Boolean = false) {
    pendingCameraTriggersAi = triggerAiScan
    val hasCamPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    if (hasCamPermission) {
      startFullHdCameraCapture(triggerAiScan = triggerAiScan)
    } else {
      runCatching {
        receiptCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }.onFailure { err ->
        pendingCameraTriggersAi = false
        validationError = err.localizedMessage
      }
    }
  }

  LaunchedEffect(aiParseState) {
    if (aiParseState is AiExpenseParseState.Success) {
      val parsed = aiParseState.parsed
      title = parsed.title
      amountText = parsed.amount.toString()
      selectedCategory = parsed.category
      selectedMethod = parsed.paymentMethod
      if (parsed.detectedEpochDay != null) {
        selectedEpochDay = parsed.detectedEpochDay
      }
      if (parsed.note.isNotBlank()) {
        note = parsed.note
      }
      if (!aiParseState.receiptPhotoBase64.isNullOrBlank()) {
        receiptPhotoBase64 = aiParseState.receiptPhotoBase64
      }
      aiScanSuccessDetectedDate = parsed.detectedEpochDay
      showAiScanSuccessBanner = true
      validationError = null
      onClearAiParse()
    }
  }

  val decodedReceiptBitmap = remember(receiptPhotoBase64) {
    ImageUtils.decodeAvatarBitmap(receiptPhotoBase64)
  }

  val currentAmount = amountText.toLongOrNull() ?: 0L

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = rememberLiquidGlassSheetContainerColor(),
    tonalElevation = 0.dp,
    dragHandle = { LiquidGlassSheetDragHandle() },
  ) {
    LiquidGlassWindowBlurEffect()
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      AppAmbientBackdrop()

      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Dimensional Live Preview Header Card
        DimensionalVaultCard {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (existingExpense == null) {
                    stringResource(R.string.editor_new_title)
                  } else {
                    stringResource(R.string.editor_edit_title)
                  },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                )
                Text(
                  text = title.ifBlank { stringResource(R.string.editor_name_hint) },
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.78f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = Color.White.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                  modifier = Modifier.size(34.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.Close,
                      contentDescription = stringResource(R.string.action_cancel),
                      tint = Color.White,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }

            // Live Nominal Display + Metadata Pills
            AnimatedContent(
              targetState = currentAmount,
              transitionSpec = {
                fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
              },
              label = "live_amount_preview",
            ) { amt ->
              Text(
                text = Formatters.formatMoney(amt, activeCurrency),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
              )
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(
                    imageVector = selectedCategory.iconVector(),
                    contentDescription = null,
                    tint = SkyPositiveAccent,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = stringResource(selectedCategory.shortLabelRes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(
                    imageVector = selectedMethod.iconVector(),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "${stringResource(selectedMethod.labelRes)} • ${Formatters.formatShortDate(selectedEpochDay, activeLocale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                  )
                }
              }

              if (receiptPhotoBase64.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = EmeraldSafe.copy(alpha = 0.26f),
                  border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.65f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = null,
                      tint = Color(0xFF6EE7B7),
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = stringResource(R.string.receipt_attached_badge),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                    )
                  }
                }
              }
            }
          }
        }

        // Section 1: AI Smart Fill & Receipt Scanner
        SculptedSurfaceCard(
          accentColor = SkyPositiveAccent,
          cornerRadius = 20,
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.size(34.dp),
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.linearGradient(
                        colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED)),
                        start = Offset(0f, 0f),
                        end = Offset(90f, 90f),
                      )
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.ai_quick_fill_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            // Primary AI Receipt Scanner Row: Full-HD Camera AI + Gallery AI
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  if (aiParseState is AiExpenseParseState.Error) onClearAiParse()
                  launchReceiptCamera(triggerAiScan = true)
                },
                enabled = aiParseState !is AiExpenseParseState.Loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
                  contentColor = if (isDarkTheme) ObsidianInk else Color.White,
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("ai_scan_camera_button"),
              ) {
                if (aiParseState is AiExpenseParseState.Loading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isDarkTheme) ObsidianInk else Color.White,
                  )
                } else {
                  Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.ai_scan_camera_action),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              OutlinedButton(
                onClick = {
                  if (aiParseState is AiExpenseParseState.Error) onClearAiParse()
                  aiReceiptScannerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                  )
                },
                enabled = aiParseState !is AiExpenseParseState.Loading,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("ai_scan_receipt_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DocumentScanner,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.ai_scan_gallery_action),
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            AnimatedVisibility(
              visible = showAiScanSuccessBanner,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut(),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldSafe.copy(alpha = if (isDarkTheme) 0.18f else 0.12f),
                border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.40f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("ai_scan_success_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(18.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = stringResource(R.string.ai_scan_success_banner),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    aiScanSuccessDetectedDate?.let { epoch ->
                      Text(
                        text = stringResource(
                          R.string.ai_scan_date_detected,
                          Formatters.formatShortDate(epoch, activeLocale),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSafe,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = aiPromptText,
                onValueChange = {
                  aiPromptText = it.take(160)
                  voiceErrorText = null
                  if (aiParseState is AiExpenseParseState.Error) onClearAiParse()
                },
                placeholder = { Text(stringResource(R.string.ai_quick_fill_hint)) },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("ai_quick_fill_input"),
              )
              FilledTonalIconButton(
                onClick = { launchVoiceExpenseAi() },
                enabled = aiParseState !is AiExpenseParseState.Loading,
                modifier = Modifier
                  .size(52.dp)
                  .testTag("voice_ai_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Mic,
                  contentDescription = stringResource(R.string.voice_ai_button_cd),
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }

            AnimatedVisibility(
              visible = aiParseState is AiExpenseParseState.Error || !voiceErrorText.isNullOrBlank(),
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut(),
            ) {
              val errMsg = (aiParseState as? AiExpenseParseState.Error)?.message ?: voiceErrorText
              if (!errMsg.isNullOrBlank()) {
                Text(
                  text = errMsg,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }

            FilledTonalButton(
              onClick = { onParseWithAi(aiPromptText) },
              enabled = aiParseState !is AiExpenseParseState.Loading && aiPromptText.isNotBlank(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ai_quick_fill_button"),
            ) {
              if (aiParseState is AiExpenseParseState.Loading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.ai_quick_fill_loading),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.ai_quick_fill_action),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        // Section 2: Amount & Title Card
        SculptedSurfaceCard(
          accentColor = MaterialTheme.colorScheme.primary,
          cornerRadius = 20,
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            // Amount Input + Quick Nominal Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                  val digits = input.filter { it.isDigit() }.take(10)
                  amountText = digits
                  validationError = null
                },
                label = { Text(stringResource(R.string.editor_amount_label, activeCurrency.symbol)) },
                prefix = {
                  Text(
                    text = activeCurrency.prefixWithSpace,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                  )
                },
                placeholder = { Text(activeCurrency.budgetStepAmount.toString()) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("expense_amount_input"),
              )

              AnimatedVisibility(
                visible = currentAmount > 0L,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
              ) {
                Text(
                  text = stringResource(
                    R.string.editor_parsed_preview,
                    Formatters.formatMoney(currentAmount, activeCurrency),
                  ),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.SemiBold,
                )
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                activeCurrency.expenseQuickDeltas.forEach { (delta, label) ->
                  SuggestionChip(
                    onClick = {
                      val next = (amountText.toLongOrNull() ?: 0L) + delta
                      amountText = next.toString()
                      validationError = null
                    },
                    label = { Text(label, fontFamily = JetBrainsMonoFontFamily) },
                  )
                }
              }
            }

            // Title Input + Quick Title Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = title,
                onValueChange = {
                  title = it.take(80)
                  validationError = null
                },
                label = { Text(stringResource(R.string.editor_name_label)) },
                placeholder = { Text(stringResource(R.string.editor_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("expense_title_input"),
              )

              val presetOptions = listOf(
                stringResource(R.string.preset_lunch) to ExpenseCategory.FOOD,
                stringResource(R.string.preset_coffee) to ExpenseCategory.FOOD,
                stringResource(R.string.preset_transport) to ExpenseCategory.TRANSPORT,
                stringResource(R.string.preset_groceries) to ExpenseCategory.SHOPPING,
                stringResource(R.string.preset_utilities) to ExpenseCategory.BILLS,
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                presetOptions.forEach { (preset, cat) ->
                  SuggestionChip(
                    onClick = {
                      title = preset
                      selectedCategory = cat
                      validationError = null
                    },
                    label = { Text(preset) },
                  )
                }
              }
            }
          }
        }

        // Section 3: Receipt / Invoice Photo Attachment Card
        SculptedSurfaceCard(
          accentColor = EmeraldSafe,
          cornerRadius = 20,
          modifier = Modifier.testTag("receipt_photo_section"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color.Transparent,
                  modifier = Modifier.size(40.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(
                        Brush.linearGradient(
                          colors = listOf(Color(0xFF059669), Color(0xFF10B981)),
                          start = Offset(0f, 0f),
                          end = Offset(100f, 100f),
                        )
                      ),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Receipt,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = stringResource(R.string.receipt_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = stringResource(R.string.receipt_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = stringResource(R.string.receipt_included_in_recap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSafe,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }

              if (isEncodingReceipt) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                )
              }
            }

            if (receiptPhotoBase64.isNotBlank() && decodedReceiptBitmap != null) {
              // Attached Receipt Preview + Actions
              SculptedSurfaceCard(
                accentColor = EmeraldSafe,
                cornerRadius = 18,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("receipt_photo_preview_card"),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(164.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surfaceContainerLow)
                      .clickable { showFullReceiptViewer = true }
                      .testTag("view_receipt_fullscreen_button"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Image(
                      bitmap = decodedReceiptBitmap.asImageBitmap(),
                      contentDescription = stringResource(R.string.receipt_viewer_title),
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxWidth(),
                    )
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.68f),
                      modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.ZoomIn,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.inverseOnSurface,
                          modifier = Modifier.size(14.dp),
                        )
                        Text(
                          text = stringResource(R.string.receipt_view_full),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.inverseOnSurface,
                          fontWeight = FontWeight.SemiBold,
                        )
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    FilledTonalButton(
                      onClick = { onScanReceiptBase64(receiptPhotoBase64) },
                      enabled = aiParseState !is AiExpenseParseState.Loading,
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("scan_attached_receipt_ai_button"),
                    ) {
                      if (aiParseState is AiExpenseParseState.Loading) {
                        CircularProgressIndicator(
                          modifier = Modifier.size(16.dp),
                          strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = stringResource(R.string.ai_quick_fill_loading),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          style = MaterialTheme.typography.labelMedium,
                        )
                      } else {
                        Icon(
                          imageVector = Icons.Rounded.AutoAwesome,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = stringResource(R.string.receipt_scan_ai_from_photo),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          style = MaterialTheme.typography.labelMedium,
                        )
                      }
                    }

                    OutlinedButton(
                      onClick = {
                        attachGalleryLauncher.launch(
                          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .height(48.dp)
                        .testTag("change_receipt_photo_button"),
                    ) {
                      Text(
                        text = stringResource(R.string.receipt_change_photo),
                        style = MaterialTheme.typography.labelMedium,
                      )
                    }

                    IconButton(
                      onClick = { receiptPhotoBase64 = "" },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("remove_receipt_photo_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.receipt_remove_photo),
                        tint = CoralDanger,
                      )
                    }
                  }
                }
              }
            } else {
              // Empty Receipt Picker Buttons (Gallery & Camera)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                FilledTonalButton(
                  onClick = {
                    attachGalleryLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("attach_receipt_gallery_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.receipt_pick_gallery),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                OutlinedButton(
                  onClick = { launchReceiptCamera(triggerAiScan = false) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("attach_receipt_camera_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.receipt_take_camera),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        // Section 4: Category, Date & Payment Method Card
        SculptedSurfaceCard(
          accentColor = selectedCategory.accentColor,
          cornerRadius = 20,
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = stringResource(R.string.label_category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ExpenseCategory.entries.forEach { category ->
                  val selected = selectedCategory == category
                  val animatedBg by animateColorAsState(
                    targetValue = if (selected) category.accentColor.copy(alpha = 0.18f) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "cat_chip_bg",
                  )
                  FilterChip(
                    selected = selected,
                    onClick = { selectedCategory = category },
                    label = { Text(stringResource(category.labelRes)) },
                    leadingIcon = {
                      Icon(
                        imageVector = category.iconVector(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = animatedBg,
                      selectedLabelColor = category.accentColor,
                      selectedLeadingIconColor = category.accentColor,
                    ),
                    modifier = Modifier.testTag("category_chip_${category.name}"),
                  )
                }
              }

              // Custom Categories Row (user-defined labels mapped cleanly to core budget groups)
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = stringResource(R.string.custom_cat_section_title),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                  onClick = { showAddCustomCategoryDialog = true },
                  modifier = Modifier
                    .height(40.dp)
                    .testTag("add_custom_category_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = stringResource(R.string.custom_cat_add_action),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              if (customCategories.isEmpty()) {
                Text(
                  text = stringResource(R.string.custom_cat_empty_hint),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              } else {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  customCategories.forEach { customCat ->
                    val isApplied = note.contains(customCat.noteTag, ignoreCase = true)
                    FilterChip(
                      selected = isApplied,
                      onClick = {
                        selectedCategory = customCat.parentCategory
                        if (title.isBlank()) {
                          title = customCat.name
                        }
                        note = CustomCategoryManager.applyTagToNote(note, customCat)
                      },
                      label = { Text(customCat.name) },
                      leadingIcon = {
                        Icon(
                          imageVector = Icons.Rounded.Label,
                          contentDescription = null,
                          tint = customCat.accentColor,
                          modifier = Modifier.size(16.dp),
                        )
                      },
                      trailingIcon = {
                        Icon(
                          imageVector = Icons.Rounded.Close,
                          contentDescription = stringResource(R.string.action_delete),
                          modifier = Modifier
                            .size(15.dp)
                            .clickable {
                              CustomCategoryManager.deleteCategory(context, customCat.id)
                              onCustomCategoryDeleted(customCat.id)
                            },
                        )
                      },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = customCat.accentColor.copy(alpha = 0.16f),
                        selectedLabelColor = customCat.accentColor,
                      ),
                      modifier = Modifier.testTag("custom_cat_chip_${customCat.id}"),
                    )
                  }
                }
              }
            }

            // Date Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = stringResource(R.string.label_date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                val todayEpoch = today.toEpochDay()
                val yesterdayEpoch = today.minusDays(1).toEpochDay()
                FilterChip(
                  selected = selectedEpochDay == todayEpoch,
                  onClick = { selectedEpochDay = todayEpoch },
                  label = { Text(stringResource(R.string.label_today)) },
                )
                FilterChip(
                  selected = selectedEpochDay == yesterdayEpoch,
                  onClick = { selectedEpochDay = yesterdayEpoch },
                  label = { Text(stringResource(R.string.label_yesterday)) },
                )
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.56f else 0.72f),
                  border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.80f),
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true }
                    .testTag("pick_date_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CalendarMonth,
                      contentDescription = stringResource(R.string.label_date),
                      modifier = Modifier.size(18.dp),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = Formatters.formatShortDate(selectedEpochDay, activeLocale),
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }

            // Payment Method
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = stringResource(R.string.label_payment_method),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                PaymentMethod.entries.forEach { method ->
                  FilterChip(
                    selected = selectedMethod == method,
                    onClick = { selectedMethod = method },
                    label = { Text(stringResource(method.labelRes)) },
                    leadingIcon = {
                      Icon(
                        imageVector = method.iconVector(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                    },
                  )
                }
              }

              if (existingExpense == null && customWallets.isNotEmpty() && onDeductCustomWallet != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = stringResource(R.string.editor_custom_wallet_deduct_label),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  customWallets.forEach { wallet ->
                    val isSelected = selectedCustomWalletId == wallet.id
                    FilterChip(
                      selected = isSelected,
                      onClick = {
                        if (isSelected) {
                          selectedCustomWalletId = null
                        } else {
                          selectedCustomWalletId = wallet.id
                          selectedMethod = wallet.walletType.defaultPaymentMethod()
                        }
                      },
                      label = {
                        Text(
                          text = "${wallet.name} (${Formatters.formatMoney(wallet.balance, activeCurrency)})",
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      },
                      leadingIcon = {
                        Box(
                          modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(wallet.accentColor),
                        )
                      },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = wallet.accentColor.copy(alpha = 0.16f),
                        selectedLabelColor = wallet.accentColor,
                      ),
                      modifier = Modifier.testTag("editor_custom_wallet_${wallet.id}"),
                    )
                  }
                }
              }
            }
          }
        }

        // Event / Project #Tags Quick Picker
        var customEventTagDraft by remember { mutableStateOf("") }
        val activeNoteTags = remember(note) { EventTagHelper.extractTags(note) }
        val suggestedEventTags = remember(activeNoteTags) {
          (EventTagHelper.presetEventTags + activeNoteTags).distinct()
        }
        SculptedSurfaceCard(
          accentColor = SkyPositiveAccent,
          cornerRadius = 18,
          modifier = Modifier.testTag("event_tags_editor_section"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Tag,
                contentDescription = null,
                tint = SkyPositiveAccent,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = stringResource(R.string.event_tags_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = stringResource(R.string.event_tags_section_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              suggestedEventTags.forEach { tag ->
                val isTagged = EventTagHelper.hasTag(note, tag)
                FilterChip(
                  selected = isTagged,
                  onClick = { note = EventTagHelper.toggleTag(note, tag) },
                  label = { Text(tag) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyPositiveAccent.copy(alpha = 0.18f),
                    selectedLabelColor = SkyPositiveAccent,
                  ),
                  modifier = Modifier.testTag("editor_event_tag_$tag"),
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = customEventTagDraft,
                onValueChange = { customEventTagDraft = it.take(25) },
                placeholder = { Text(stringResource(R.string.event_tags_custom_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("custom_event_tag_input"),
              )
              FilledTonalIconButton(
                onClick = {
                  val normalized = EventTagHelper.normalizeTag(customEventTagDraft)
                  if (normalized != null) {
                    if (!EventTagHelper.hasTag(note, normalized)) {
                      note = EventTagHelper.toggleTag(note, normalized)
                    }
                    customEventTagDraft = ""
                  }
                },
                enabled = EventTagHelper.normalizeTag(customEventTagDraft) != null,
                modifier = Modifier
                  .size(48.dp)
                  .testTag("add_custom_event_tag_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = stringResource(R.string.event_tags_add_cd),
                )
              }
            }
          }
        }

        // Optional Note
        OutlinedTextField(
          value = note,
          onValueChange = { note = it.take(200) },
          label = { Text(stringResource(R.string.label_optional_note)) },
          placeholder = { Text(stringResource(R.string.label_additional_details)) },
          maxLines = 2,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(
          visible = validationError != null,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          if (validationError != null) {
            Text(
              text = validationError!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .weight(1f)
              .height(52.dp),
          ) {
            Text(
              text = stringResource(R.string.action_cancel),
              fontWeight = FontWeight.SemiBold,
            )
          }
          Button(
            onClick = {
              val parsedAmount = amountText.toLongOrNull() ?: 0L
              when {
                parsedAmount <= 0L -> validationError = context.getString(R.string.editor_error_amount)
                title.isBlank() -> validationError = context.getString(R.string.editor_error_title)
                else -> {
                  if (existingExpense == null && selectedCustomWalletId != null) {
                    onDeductCustomWallet?.invoke(selectedCustomWalletId!!, parsedAmount)
                  }
                  onSave(
                    title.trim(),
                    parsedAmount,
                    selectedCategory,
                    selectedMethod,
                    selectedEpochDay,
                    note.trim(),
                    receiptPhotoBase64,
                  )
                }
              }
            },
            enabled = !isEncodingReceipt,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
              contentColor = if (isDarkTheme) ObsidianInk else Color.White,
            ),
            modifier = Modifier
              .weight(1.4f)
              .height(52.dp)
              .testTag("save_expense_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (existingExpense == null) {
                stringResource(R.string.action_save)
              } else {
                stringResource(R.string.action_update)
              },
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }

  if (showFullReceiptViewer && receiptPhotoBase64.isNotBlank()) {
    val parsedAmount = amountText.toLongOrNull() ?: 0L
    ReceiptPhotoViewerDialog(
      receiptPhotoBase64 = receiptPhotoBase64,
      title = title.ifBlank { stringResource(R.string.receipt_viewer_title) },
      subtitle = "${Formatters.formatShortDate(selectedEpochDay, activeLocale)} • ${stringResource(selectedMethod.labelRes)}",
      amountFormatted = if (parsedAmount > 0L) Formatters.formatMoney(parsedAmount, activeCurrency) else "",
      onDismiss = { showFullReceiptViewer = false },
    )
  }

  if (showDatePicker) {
    val initialMillis = LocalDate.ofEpochDay(selectedEpochDay)
      .atStartOfDay(ZoneOffset.UTC)
      .toInstant()
      .toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      shape = RoundedCornerShape(28.dp),
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
              selectedEpochDay = picked.toEpochDay()
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_select))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    ) {
      LiquidGlassWindowBlurEffect()
      DatePicker(state = datePickerState)
    }
  }

  if (showQuickScanChooser) {
    LiquidGlassAlertDialog(
      onDismissRequest = { showQuickScanChooser = false },
      title = { Text(stringResource(R.string.ai_scan_chooser_title)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = stringResource(R.string.ai_scan_chooser_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = {
              showQuickScanChooser = false
              launchReceiptCamera(triggerAiScan = true)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isDarkTheme) Color(0xFFF8FAFC) else ObsidianInk,
              contentColor = if (isDarkTheme) ObsidianInk else Color.White,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("chooser_camera_ai_button"),
          ) {
            Icon(imageVector = Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_scan_camera_action), fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = {
              showQuickScanChooser = false
              aiReceiptScannerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("chooser_gallery_ai_button"),
          ) {
            Icon(imageVector = Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_scan_gallery_action), fontWeight = FontWeight.SemiBold)
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showQuickScanChooser = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showAddCustomCategoryDialog) {
    AddCustomCategoryDialog(
      onDismiss = { showAddCustomCategoryDialog = false },
      onCreate = { name, parent, colorHex ->
        val created = CustomCategoryManager.addCategory(context, name, parent, colorHex)
        if (created != null) {
          onCustomCategoryAdded(created)
          selectedCategory = created.parentCategory
          if (title.isBlank()) title = created.name
          note = CustomCategoryManager.applyTagToNote(note, created)
        }
        showAddCustomCategoryDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomCategoryDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, parent: ExpenseCategory, colorHex: Long) -> Unit,
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf("") }
  var selectedParent by remember { mutableStateOf(ExpenseCategory.FOOD) }
  var selectedColor by remember { mutableLongStateOf(CustomCategoryManager.presetColorPalette.first()) }
  var errorText by remember { mutableStateOf<String?>(null) }

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.custom_cat_dialog_title)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it.take(24)
            errorText = null
          },
          label = { Text(stringResource(R.string.custom_cat_name_label)) },
          placeholder = { Text(stringResource(R.string.custom_cat_name_hint)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_cat_name_input"),
        )
        Text(
          text = stringResource(R.string.custom_cat_parent_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ExpenseCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedParent == cat,
              onClick = { selectedParent = cat },
              label = { Text(stringResource(cat.shortLabelRes)) },
            )
          }
        }
        Text(
          text = stringResource(R.string.custom_cat_color_label),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
          CustomCategoryManager.presetColorPalette.forEach { hex ->
            val isChosen = selectedColor == hex
            Surface(
              onClick = { selectedColor = hex },
              shape = CircleShape,
              color = Color(hex),
              border = if (isChosen) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
              modifier = Modifier.size(32.dp),
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
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            errorText = context.getString(R.string.custom_cat_error_name)
          } else {
            onCreate(name.trim(), selectedParent, selectedColor)
          }
        },
        modifier = Modifier
          .height(48.dp)
          .testTag("save_custom_cat_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
