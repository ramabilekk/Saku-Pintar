package com.example.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.security.PrivacySecurityManager
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.JetBrainsMonoFontFamily

/**
 * Full-screen obsidian lock overlay supporting 4-digit PIN entry and Android Device Credential
 * (Biometric / Fingerprint / Face / Screen Lock) unlock.
 */
@Composable
fun AppLockGateScreen(
  onUnlocked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val tactile = rememberTactileFeedback()
  var enteredPin by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  val hasDeviceCredential = remember(context) {
    PrivacySecurityManager.isDeviceCredentialAvailable(context)
  }

  val deviceCredentialLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      tactile.confirm()
      onUnlocked()
    }
  }

  fun launchDeviceCredential() {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
    @Suppress("DEPRECATION")
    val intent = km.createConfirmDeviceCredentialIntent(
      context.getString(R.string.app_lock_screen_title),
      context.getString(R.string.app_lock_screen_subtitle),
    )
    if (intent != null) {
      runCatching { deviceCredentialLauncher.launch(intent) }
    }
  }

  fun appendDigit(digit: String) {
    if (enteredPin.length >= 4) return
    tactile.tick()
    errorText = null
    val next = enteredPin + digit
    enteredPin = next
    if (next.length == 4) {
      if (PrivacySecurityManager.verifyPin(context, next)) {
        tactile.confirm()
        onUnlocked()
      } else {
        errorText = context.getString(R.string.app_lock_wrong_pin)
        enteredPin = ""
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF0A1020),
            Color(0xFF0F172A),
            Color(0xFF0B1324),
          )
        )
      )
      .safeDrawingPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp)
      .testTag("app_lock_gate_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 360.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF141E33),
        border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.45f)),
        shadowElevation = 10.dp,
        modifier = Modifier.size(68.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_sakupintar_logo),
          contentDescription = stringResource(R.string.app_name),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = EmeraldSafe,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.app_lock_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
          )
        }
        Text(
          text = stringResource(R.string.app_lock_screen_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.72f),
          textAlign = TextAlign.Center,
        )
      }

      // 4 PIN indicator dots
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp),
      ) {
        repeat(4) { index ->
          val filled = index < enteredPin.length
          Surface(
            shape = CircleShape,
            color = if (filled) EmeraldSafe else Color.White.copy(alpha = 0.14f),
            border = BorderStroke(
              width = 1.5.dp,
              color = if (filled) EmeraldSafe else Color.White.copy(alpha = 0.30f),
            ),
            modifier = Modifier.size(16.dp),
          ) {}
        }
      }

      AnimatedVisibility(
        visible = errorText != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Text(
          text = errorText.orEmpty(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = CoralDanger,
          textAlign = TextAlign.Center,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Numeric Keypad 3x4
      val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
      )
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        rows.forEach { rowDigits ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            rowDigits.forEach { digit ->
              PinKeyButton(
                label = digit,
                onClick = { appendDigit(digit) },
                modifier = Modifier.testTag("pin_key_$digit"),
              )
            }
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (hasDeviceCredential) {
            Surface(
              onClick = { launchDeviceCredential() },
              shape = CircleShape,
              color = EmeraldSafe.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.45f)),
              modifier = Modifier
                .size(66.dp)
                .testTag("pin_biometric_button"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Fingerprint,
                  contentDescription = stringResource(R.string.app_lock_use_biometric),
                  tint = EmeraldSafe,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.size(66.dp))
          }

          PinKeyButton(
            label = "0",
            onClick = { appendDigit("0") },
            modifier = Modifier.testTag("pin_key_0"),
          )

          Surface(
            onClick = {
              if (enteredPin.isNotEmpty()) {
                tactile.tick()
                enteredPin = enteredPin.dropLast(1)
              }
            },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            modifier = Modifier
              .size(66.dp)
              .testTag("pin_backspace_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }

      if (hasDeviceCredential) {
        OutlinedButton(
          onClick = { launchDeviceCredential() },
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.50f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSafe),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("app_lock_biometric_cta"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.app_lock_use_biometric),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun PinKeyButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = Color.White.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    modifier = modifier.size(66.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
    }
  }
}

/**
 * Dialog for configuring or updating the 4-digit App Lock PIN from Settings.
 */
@Composable
fun SetupPinDialog(
  onPinSaved: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var pin1 by remember { mutableStateOf("") }
  var pin2 by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.app_lock_setup_title),
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.app_lock_setup_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = pin1,
          onValueChange = { raw ->
            pin1 = raw.filter { it.isDigit() }.take(4)
            errorMsg = null
          },
          label = { Text("PIN (4 digit)") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("setup_pin_input_1"),
        )
        OutlinedTextField(
          value = pin2,
          onValueChange = { raw ->
            pin2 = raw.filter { it.isDigit() }.take(4)
            errorMsg = null
          },
          label = { Text(stringResource(R.string.app_lock_setup_confirm)) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("setup_pin_input_2"),
        )
        if (errorMsg != null) {
          Text(
            text = errorMsg.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = CoralDanger,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (pin1.length != 4 || pin1 != pin2) {
            errorMsg = context.getString(R.string.app_lock_pin_mismatch)
          } else if (PrivacySecurityManager.savePinAndEnableLock(context, pin1)) {
            onPinSaved()
          }
        },
        enabled = pin1.length == 4 && pin2.length == 4,
        modifier = Modifier.testTag("setup_pin_save_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
