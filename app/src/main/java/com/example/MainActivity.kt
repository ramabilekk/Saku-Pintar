package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.ui.MainAppScaffold
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.SakuPintarTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent {
      SakuPintarTheme {
        AppRoot()
      }
    }
  }
}

@Composable
private fun AppRoot() {
  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val context = LocalContext.current
  val credentialManager = remember(context) { CredentialManager.create(context) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = this,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = {},
    )
  } else {
    key(user.uid) {
      MainAppScaffold(
        currentUser = user,
        onSignedOut = {},
      )
    }
  }
}
