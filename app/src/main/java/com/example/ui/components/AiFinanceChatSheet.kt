package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AiFinanceContextSnapshot
import com.example.data.ai.FinanceAiAssistant
import com.example.data.budget.PaydayCycleManager
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ObsidianInk
import com.example.util.Formatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiFinanceChatDialog(
  aiAssistant: FinanceAiAssistant,
  snapshot: AiFinanceContextSnapshot,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val activeLocale = remember(activeLanguage) { activeLanguage.toLocale() }
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  var messages by remember { mutableStateOf<List<AiChatMessage>>(emptyList()) }
  var inputText by remember { mutableStateOf("") }
  var streamingText by remember { mutableStateOf<String?>(null) }
  var isSending by remember { mutableStateOf(false) }
  var lastFailedQuestion by remember { mutableStateOf<String?>(null) }
  var activeJob by remember { mutableStateOf<Job?>(null) }

  val listState = rememberLazyListState()

  val cycle = remember(snapshot.monthKey, snapshot.paydayStartDay) {
    PaydayCycleManager.resolveCycle(snapshot.monthKey, snapshot.paydayStartDay)
  }
  val contextPeriodLabel = remember(cycle, activeLocale) {
    if (cycle.isCustomCycle) {
      cycle.formatRangeLabel(activeLocale)
    } else {
      Formatters.formatMonthTitle(snapshot.monthKey, activeLocale)
    }
  }

  val starterPrompts = listOf(
    stringResource(R.string.ai_chat_chip_1),
    stringResource(R.string.ai_chat_chip_2),
    stringResource(R.string.ai_chat_chip_3),
    stringResource(R.string.ai_chat_chip_4),
  )

  fun sendQuestion(question: String) {
    val trimmed = question.trim()
    if (trimmed.isEmpty() || isSending) return

    val priorHistory = messages.filter { !it.isError }
    messages = messages.filterNot { it.isError } + AiChatMessage(isUser = true, text = trimmed)
    inputText = ""
    lastFailedQuestion = null
    isSending = true
    streamingText = ""

    activeJob?.cancel()
    activeJob = scope.launch {
      var latestReply = ""
      try {
        aiAssistant.streamFinanceChatReply(
          userQuestion = trimmed,
          conversationHistory = priorHistory,
          snapshot = snapshot,
          language = activeLanguage,
          currency = activeCurrency,
        ).collect { partial ->
          latestReply = partial
          streamingText = partial
        }
        val finalReply = latestReply.trim()
        if (finalReply.isNotEmpty()) {
          messages = messages + AiChatMessage(isUser = false, text = finalReply)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastFailedQuestion = trimmed
        messages = messages + AiChatMessage(
          isUser = false,
          text = e.localizedMessage ?: context.getString(R.string.error_generic),
          isError = true,
        )
      } finally {
        streamingText = null
        isSending = false
      }
    }
  }

  LaunchedEffect(messages.size, streamingText?.length) {
    val totalItems = messages.size + (if (isSending) 1 else 0) + 1
    if (totalItems > 0) {
      runCatching {
        listState.animateScrollToItem(totalItems - 1)
      }
    }
  }

  BackHandler {
    activeJob?.cancel()
    onDismiss()
  }

  DisposableEffect(context, isDarkTheme) {
    val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
      .filterIsInstance<Activity>()
      .firstOrNull()
    val window = activity?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    controller?.isAppearanceLightStatusBars = false
    onDispose {
      controller?.isAppearanceLightStatusBars = !isDarkTheme
    }
  }

  Surface(
    color = Color.Transparent,
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {}
      .testTag("ai_finance_chat_dialog"),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AppAmbientBackdrop()
      Column(
        modifier = Modifier.fillMaxSize(),
      ) {
        // Smoked Liquid Crystal Top Header
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                colors = if (isDarkTheme) {
                  listOf(
                    Color(0xFF0A1629).copy(alpha = 0.86f),
                    Color(0xFF10283E).copy(alpha = 0.82f),
                  )
                } else {
                  listOf(
                    Color(0xFF0B3534).copy(alpha = 0.88f),
                    Color(0xFF104F4A).copy(alpha = 0.84f),
                  )
                }
              )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = EmeraldSafe.copy(alpha = 0.22f),
              border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.48f)),
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  tint = GoldAccent,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.ai_chat_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = stringResource(R.string.ai_chat_context_badge, contextPeriodLabel),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (messages.isNotEmpty()) {
              IconButton(
                onClick = {
                  activeJob?.cancel()
                  messages = emptyList()
                  streamingText = null
                  isSending = false
                  lastFailedQuestion = null
                },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("ai_chat_reset_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = stringResource(R.string.ai_chat_reset_cd),
                  tint = Color.White.copy(alpha = 0.85f),
                )
              }
            }
            IconButton(
              onClick = {
                activeJob?.cancel()
                onDismiss()
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("ai_chat_close_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.ai_chat_close_cd),
                tint = Color.White,
              )
            }
          }
        }

        // Chat Feed
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Welcome & Quick Prompts when empty
          if (messages.isEmpty() && !isSending) {
            item {
              SculptedSurfaceCard(
                accentColor = EmeraldSafe,
                cornerRadius = 20,
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = stringResource(R.string.ai_chat_welcome_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = stringResource(R.string.ai_chat_welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    starterPrompts.forEachIndexed { idx, prompt ->
                      SuggestionChip(
                        onClick = { sendQuestion(prompt) },
                        label = {
                          Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp),
                          )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                          containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.14f else 0.08f),
                          labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier.testTag("ai_chat_starter_$idx"),
                      )
                    }
                  }
                }
              }
            }
          }

          items(messages, key = { it.id }) { msg ->
            ChatBubbleRow(
              message = msg,
              onRetry = if (msg.isError && lastFailedQuestion != null) {
                { lastFailedQuestion?.let { sendQuestion(it) } }
              } else null,
            )
          }

          if (isSending) {
            item(key = "streaming_bubble") {
              val partial = streamingText.orEmpty()
              if (partial.isBlank()) {
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = if (isDarkTheme) Color(0xFF16243D).copy(alpha = 0.64f) else Color.White.copy(alpha = 0.76f),
                  border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.90f),
                  ),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp,
                      color = EmeraldSafe,
                    )
                    Text(
                      text = stringResource(R.string.ai_chat_typing),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              } else {
                ChatBubbleRow(
                  message = AiChatMessage(
                    id = "streaming",
                    isUser = false,
                    text = partial,
                  ),
                )
              }
            }
          }
        }

        // Bottom Liquid Glass Input Composer
        HorizontalDivider(
          color = if (isDarkTheme) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.72f),
        )
        Surface(
          color = if (isDarkTheme) {
            Color(0xFF0E192D).copy(alpha = 0.76f)
          } else {
            Color.White.copy(alpha = 0.76f)
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it.take(400) },
              placeholder = {
                Text(
                  text = stringResource(R.string.ai_chat_input_hint),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              maxLines = 3,
              shape = RoundedCornerShape(24.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.72f),
                unfocusedContainerColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.56f),
                unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f),
              ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(
                onSend = { sendQuestion(inputText) }
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("ai_chat_input_field"),
            )

            val canSend = inputText.trim().isNotEmpty() && !isSending
            Surface(
              onClick = { sendQuestion(inputText) },
              enabled = canSend,
              shape = CircleShape,
              color = if (canSend) EmeraldSafe else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              border = BorderStroke(
                1.dp,
                if (canSend) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.25f),
              ),
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .testTag("ai_chat_send_button"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Send,
                  contentDescription = stringResource(R.string.ai_chat_send_cd),
                  tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubbleRow(
  message: AiChatMessage,
  onRetry: (() -> Unit)? = null,
) {
  val isDarkTheme = rememberIsEffectiveDarkTheme()
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (message.isUser) 20.dp else 6.dp,
        bottomEnd = if (message.isUser) 6.dp else 20.dp,
      ),
      color = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.86f)
        message.isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
        isDarkTheme -> Color(0xFF16243D).copy(alpha = 0.66f)
        else -> Color.White.copy(alpha = 0.78f)
      },
      border = if (!message.isUser && !message.isError) {
        BorderStroke(
          1.1.dp,
          if (isDarkTheme) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.92f),
        )
      } else if (message.isError) {
        BorderStroke(1.dp, CoralDanger.copy(alpha = 0.45f))
      } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
      },
      shadowElevation = if (isDarkTheme) 2.dp else 3.dp,
      modifier = Modifier.widthIn(max = 320.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (!message.isUser && !message.isError) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = EmeraldSafe,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = "SakuPintar AI",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = EmeraldSafe,
            )
          }
        }
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = when {
            message.isError -> MaterialTheme.colorScheme.onErrorContainer
            message.isUser -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
          },
        )
        if (onRetry != null) {
          TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.ai_chat_retry))
          }
        }
      }
    }
  }
}
