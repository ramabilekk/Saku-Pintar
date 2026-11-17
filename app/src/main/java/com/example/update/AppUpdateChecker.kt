package com.example.update

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
  val latestVersionTag: String,
  val currentVersionLabel: String,
  val downloadUrl: String,
  val releasePageUrl: String,
  val releaseNotes: String,
)

data class ParsedGithubRelease(
  val tagName: String,
  val downloadUrl: String,
  val releasePageUrl: String,
  val releaseNotes: String,
)

object AppUpdateChecker {
  private const val TAG = "AppUpdateChecker"
  private const val PREFS_NAME = "sakupintar_update_gate"
  private const val KEY_LATEST_TAG = "cached_latest_tag"
  private const val KEY_DOWNLOAD_URL = "cached_download_url"
  private const val KEY_RELEASE_PAGE_URL = "cached_release_page_url"
  private const val KEY_RELEASE_NOTES = "cached_release_notes"
  private const val CONNECT_TIMEOUT_MS = 3500
  private const val READ_TIMEOUT_MS = 3500

  fun currentVersionLabel(
    versionName: String = BuildConfig.VERSION_NAME,
    versionCode: Int = BuildConfig.VERSION_CODE,
  ): String {
    val normalized = normalizeTag(versionName, versionCode)
    return if (normalized.startsWith("v", ignoreCase = true)) normalized else "v$normalized"
  }

  fun getCachedRequiredUpdate(context: Context): AppUpdateInfo? {
    if (!BuildConfig.IS_GITHUB_RELEASE_BUILD) return null
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val cachedTag = prefs.getString(KEY_LATEST_TAG, null)?.takeIf { it.isNotBlank() } ?: return null

    if (!isRemoteVersionNewer(cachedTag, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)) {
      clearCachedUpdate(context)
      return null
    }

    val fallbackPage = BuildConfig.GITHUB_RELEASES_PAGE_URL
    val releasePageUrl = prefs.getString(KEY_RELEASE_PAGE_URL, fallbackPage)
      ?.takeIf { it.startsWith("https://") }
      ?: fallbackPage
    val downloadUrl = prefs.getString(KEY_DOWNLOAD_URL, releasePageUrl)
      ?.takeIf { it.startsWith("https://") }
      ?: releasePageUrl
    val releaseNotes = prefs.getString(KEY_RELEASE_NOTES, "") ?: ""

    return AppUpdateInfo(
      latestVersionTag = formatDisplayTag(cachedTag),
      currentVersionLabel = currentVersionLabel(),
      downloadUrl = downloadUrl,
      releasePageUrl = releasePageUrl,
      releaseNotes = releaseNotes,
    )
  }

  suspend fun checkForUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
    if (!BuildConfig.IS_GITHUB_RELEASE_BUILD) {
      return@withContext null
    }

    val remoteRelease = fetchLatestGithubRelease(
      apiUrl = BuildConfig.GITHUB_RELEASES_API_URL,
      pageUrl = BuildConfig.GITHUB_RELEASES_PAGE_URL,
    ) ?: return@withContext getCachedRequiredUpdate(context)

    if (!isRemoteVersionNewer(remoteRelease.tagName, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)) {
      clearCachedUpdate(context)
      return@withContext null
    }

    val info = AppUpdateInfo(
      latestVersionTag = formatDisplayTag(remoteRelease.tagName),
      currentVersionLabel = currentVersionLabel(),
      downloadUrl = remoteRelease.downloadUrl,
      releasePageUrl = remoteRelease.releasePageUrl,
      releaseNotes = remoteRelease.releaseNotes,
    )
    saveCachedUpdate(context, remoteRelease.tagName, info)
    info
  }

  private fun saveCachedUpdate(context: Context, rawTag: String, info: AppUpdateInfo) {
    context.applicationContext
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_LATEST_TAG, rawTag)
      .putString(KEY_DOWNLOAD_URL, info.downloadUrl)
      .putString(KEY_RELEASE_PAGE_URL, info.releasePageUrl)
      .putString(KEY_RELEASE_NOTES, info.releaseNotes)
      .apply()
  }

  private fun clearCachedUpdate(context: Context) {
    context.applicationContext
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .apply()
  }

  private fun fetchLatestGithubRelease(
    apiUrl: String,
    pageUrl: String,
  ): ParsedGithubRelease? {
    val apiResult = runCatching {
      val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "SakuPintar-Android/${BuildConfig.VERSION_NAME}")
      }
      try {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
          val body = conn.inputStream.bufferedReader().use { it.readText() }
          parseGithubReleaseJson(body, pageUrl)
        } else {
          Log.w(TAG, "GitHub API returned HTTP ${conn.responseCode}")
          null
        }
      } finally {
        conn.disconnect()
      }
    }.getOrNull()

    if (apiResult != null) return apiResult

    // Fallback via redirect Location header if GitHub REST API hits rate limiting
    return runCatching {
      val conn = (URL(pageUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        instanceFollowRedirects = false
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        setRequestProperty("User-Agent", "SakuPintar-Android/${BuildConfig.VERSION_NAME}")
      }
      try {
        val code = conn.responseCode
        if (code in 300..399) {
          val location = conn.getHeaderField("Location")
          val tag = extractTagFromRedirectLocation(location)
          if (!tag.isNullOrBlank()) {
            val resolvedPage = if (location != null && location.startsWith("https://")) {
              location
            } else {
              pageUrl
            }
            ParsedGithubRelease(
              tagName = tag,
              downloadUrl = resolvedPage,
              releasePageUrl = resolvedPage,
              releaseNotes = "",
            )
          } else {
            null
          }
        } else {
          null
        }
      } finally {
        conn.disconnect()
      }
    }.getOrNull()
  }

  internal fun parseGithubReleaseJson(
    jsonText: String,
    fallbackReleasePageUrl: String,
  ): ParsedGithubRelease? {
    if (jsonText.isBlank()) return null
    val json = JSONObject(jsonText)
    if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
      return null
    }

    val tagName = json.optString("tag_name", "").trim()
    if (tagName.isBlank()) return null

    val htmlUrl = json.optString("html_url", "")
      .trim()
      .takeIf { it.startsWith("https://") }
      ?: fallbackReleasePageUrl

    val assets = json.optJSONArray("assets")
    var apkUrl: String? = null
    if (assets != null) {
      for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        val name = asset.optString("name", "")
        val browserUrl = asset.optString("browser_download_url", "").trim()
        if (name.endsWith(".apk", ignoreCase = true) && browserUrl.startsWith("https://")) {
          apkUrl = browserUrl
          break
        }
      }
    }

    val notes = json.optString("body", "")
      .lines()
      .map { it.trim() }
      .filter { it.isNotBlank() && !it.startsWith("- SHA-1", ignoreCase = true) }
      .take(4)
      .joinToString("\n")

    return ParsedGithubRelease(
      tagName = tagName,
      downloadUrl = apkUrl ?: htmlUrl,
      releasePageUrl = htmlUrl,
      releaseNotes = notes,
    )
  }

  internal fun extractTagFromRedirectLocation(locationHeader: String?): String? {
    if (locationHeader.isNullOrBlank()) return null
    val marker = "/releases/tag/"
    val index = locationHeader.indexOf(marker)
    if (index < 0) return null
    return locationHeader
      .substring(index + marker.length)
      .substringBefore('?')
      .substringBefore('#')
      .trim('/')
      .takeIf { it.isNotBlank() }
  }

  internal fun parseVersionTuple(raw: String): List<Int>? {
    val cleaned = raw.trim()
      .removePrefix("v")
      .removePrefix("V")
      .substringBefore('+')
      .substringBefore(' ')
      .trim()
    if (cleaned.isBlank()) return null

    val parts = cleaned.split('.', '-')
      .mapNotNull { segment ->
        val digitsOnly = segment.takeWhile { it.isDigit() }
        digitsOnly.toIntOrNull()
      }
    return parts.takeIf { it.isNotEmpty() }
  }

  internal fun isRemoteVersionNewer(
    remoteTag: String,
    currentVersionCode: Int,
    currentVersionName: String,
  ): Boolean {
    val remoteTuple = parseVersionTuple(remoteTag) ?: return false
    val effectiveCurrent = normalizeTag(currentVersionName, currentVersionCode)
    val currentTuple = parseVersionTuple(effectiveCurrent) ?: return false

    val maxLen = maxOf(remoteTuple.size, currentTuple.size)
    for (i in 0 until maxLen) {
      val remotePart = remoteTuple.getOrElse(i) { 0 }
      val currentPart = currentTuple.getOrElse(i) { 0 }
      if (remotePart != currentPart) {
        return remotePart > currentPart
      }
    }
    return false
  }

  private fun normalizeTag(versionName: String, versionCode: Int): String {
    val trimmed = versionName.trim()
    return when {
      trimmed.isBlank() && versionCode >= 1 -> "2.0.$versionCode"
      (trimmed == "1.0" || trimmed == "2.0") && versionCode >= 1 -> "$trimmed.$versionCode"
      else -> trimmed
    }
  }

  private fun formatDisplayTag(tag: String): String {
    val trimmed = tag.trim()
    return if (trimmed.startsWith("v", ignoreCase = true)) trimmed else "v$trimmed"
  }
}
