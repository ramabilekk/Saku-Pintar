package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AppCheckInit"

// App Check contract keys. These strings are defined by the Firebase SDK and
// the runtime harness, so do not rename them.
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val APPCHECK_TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

private val providerInstalled = AtomicBoolean(false)

fun configureAppCheck(context: Context, intent: Intent?) {
  try {
    val appContext = context.applicationContext ?: context
    if (FirebaseApp.getApps(appContext).isEmpty()) {
      FirebaseApp.initializeApp(appContext)
    }

    val persistenceKey = FirebaseApp.getInstance().persistenceKey
    val debugPrefs = appContext.getSharedPreferences(
      "$DEBUG_STORE_PREFIX$persistenceKey",
      Context.MODE_PRIVATE,
    )

    if (BuildConfig.DEBUG && !BuildConfig.IS_GITHUB_RELEASE_BUILD) {
      // AI Studio emulator / local debug flow: strictly follow Firebase AI Logic App Check contract.
      val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
      if (!debugToken.isNullOrEmpty()) {
        // CRITICAL: synchronous commit() so the secret is written before DebugAppCheckProviderFactory reads it.
        debugPrefs.edit()
          .putString(DEBUG_SECRET_KEY, debugToken)
          .commit()
        Log.d(TAG, "App Check debug secret configured from launch intent.")
        installDebugProvider()
      } else if (providerInstalled.compareAndSet(false, true)) {
        Log.w(TAG, "No debug token found in Intent extra; installing DebugAppCheckProviderFactory.")
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance(),
        )
        Log.d(TAG, "DebugAppCheckProviderFactory installed.")
      }
      return
    }

    // Sideloaded GitHub Actions APK or Release build:
    // 1) If a registered Firebase App Check debug token was provided via GitHub Secrets, use it.
    val configuredGithubDebugToken = BuildConfig.GITHUB_APPCHECK_DEBUG_TOKEN.trim()
    if (BuildConfig.DEBUG && configuredGithubDebugToken.isNotEmpty()) {
      val previousToken = debugPrefs.getString(DEBUG_SECRET_KEY, null)
      if (previousToken != configuredGithubDebugToken) {
        debugPrefs.edit()
          .putString(DEBUG_SECRET_KEY, configuredGithubDebugToken)
          .commit()
        appContext.getSharedPreferences(
          "$APPCHECK_TOKEN_STORE_PREFIX$persistenceKey",
          Context.MODE_PRIVATE,
        ).edit().clear().commit()
      }
      if (providerInstalled.compareAndSet(false, true)) {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance(),
        )
        Log.d(TAG, "DebugAppCheckProviderFactory installed with configured GitHub debug token.")
      }
      return
    }

    // 2) Otherwise clear any stale unregistered debug token and use reCAPTCHA Enterprise.
    if (debugPrefs.contains(DEBUG_SECRET_KEY)) {
      debugPrefs.edit().remove(DEBUG_SECRET_KEY).commit()
      appContext.getSharedPreferences(
        "$APPCHECK_TOKEN_STORE_PREFIX$persistenceKey",
        Context.MODE_PRIVATE,
      ).edit().clear().commit()
      Log.d(TAG, "Cleared stale unregistered debug App Check token.")
    }

    if (providerInstalled.compareAndSet(false, true)) {
      installRecaptchaAppCheck(appContext)
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed to initialize Firebase App Check", e)
  }
}

private fun installDebugProvider() {
  providerInstalled.set(true)
  FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
    DebugAppCheckProviderFactory.getInstance(),
  )
  Log.d(TAG, "DebugAppCheckProviderFactory installed.")
}

private fun installRecaptchaAppCheck(context: Context) {
  val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
  if (resId != 0) {
    val siteKey = context.getString(resId)
    if (siteKey.isNotBlank()) {
      FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey),
      )
      Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
    } else {
      Log.e(TAG, "recaptcha_site_key resource string is blank.")
    }
  } else {
    Log.e(TAG, "recaptcha_site_key string resource not found in package.")
  }
}
