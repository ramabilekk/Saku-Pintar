package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateCheckerTest {

  @Test
  fun parseVersionTuple_parsesGitHubTagsCleanly() {
    assertEquals(listOf(1, 0, 12), AppUpdateChecker.parseVersionTuple("v1.0.12"))
    assertEquals(listOf(1, 0, 9), AppUpdateChecker.parseVersionTuple("V1.0.9"))
    assertEquals(listOf(2, 1, 0), AppUpdateChecker.parseVersionTuple("2.1.0-beta"))
    assertNull(AppUpdateChecker.parseVersionTuple("release-notes"))
  }

  @Test
  fun isRemoteVersionNewer_detectsOutdatedVersionsAccurately() {
    assertTrue(AppUpdateChecker.isRemoteVersionNewer("v1.0.10", 9, "1.0.9"))
    assertTrue(AppUpdateChecker.isRemoteVersionNewer("v1.1.0", 10, "1.0.10"))
    assertTrue(AppUpdateChecker.isRemoteVersionNewer("v2.0.0", 99, "1.0.99"))

    assertFalse(AppUpdateChecker.isRemoteVersionNewer("v1.0.9", 9, "1.0.9"))
    assertFalse(AppUpdateChecker.isRemoteVersionNewer("v1.0.8", 9, "1.0.9"))
    assertFalse(AppUpdateChecker.isRemoteVersionNewer("v1.0.1", 1, "1.0"))
    assertTrue(AppUpdateChecker.isRemoteVersionNewer("v1.0.8", 1, "1.0"))
    assertTrue(AppUpdateChecker.isRemoteVersionNewer("v2.0.18", 17, "1.0.17"))
    assertFalse(AppUpdateChecker.isRemoteVersionNewer("v2.0.18", 18, "2.0.18"))
    assertTrue(AppUpdateChecker.isRemoteVersionNewer("v2.0.19", 18, "2.0.18"))
  }

  @Test
  fun parseGithubReleaseJson_prefersApkAssetDownloadUrl() {
    val json = """
      {
        "tag_name": "v1.0.15",
        "draft": false,
        "prerelease": false,
        "html_url": "https://github.com/ramabilekk/Saku-Pintar/releases/tag/v1.0.15",
        "body": "Build APK otomatis untuk SakuPintar (v1.0.15).\n- Versi Aplikasi: v1.0.15 (Build 15)\n- SHA-1 signing: AB:CD",
        "assets": [
          {
            "name": "SakuPintar-v1.0.15.apk",
            "browser_download_url": "https://github.com/ramabilekk/Saku-Pintar/releases/download/v1.0.15/SakuPintar-v1.0.15.apk"
          }
        ]
      }
    """.trimIndent()

    val parsed = AppUpdateChecker.parseGithubReleaseJson(
      jsonText = json,
      fallbackReleasePageUrl = "https://github.com/ramabilekk/Saku-Pintar/releases/latest",
    )

    assertNotNull(parsed)
    assertEquals("v1.0.15", parsed!!.tagName)
    assertEquals(
      "https://github.com/ramabilekk/Saku-Pintar/releases/download/v1.0.15/SakuPintar-v1.0.15.apk",
      parsed.downloadUrl,
    )
    assertEquals(
      "https://github.com/ramabilekk/Saku-Pintar/releases/tag/v1.0.15",
      parsed.releasePageUrl,
    )
    assertFalse(parsed.releaseNotes.contains("SHA-1"))
  }

  @Test
  fun parseGithubReleaseJson_ignoresDraftsAndExtractsRedirectTag() {
    val draftJson = """
      {
        "tag_name": "v1.0.16",
        "draft": true,
        "prerelease": false
      }
    """.trimIndent()
    assertNull(
      AppUpdateChecker.parseGithubReleaseJson(
        jsonText = draftJson,
        fallbackReleasePageUrl = "https://github.com/ramabilekk/Saku-Pintar/releases/latest",
      ),
    )

    assertEquals(
      "v1.0.15",
      AppUpdateChecker.extractTagFromRedirectLocation(
        "https://github.com/ramabilekk/Saku-Pintar/releases/tag/v1.0.15",
      ),
    )
  }
}
