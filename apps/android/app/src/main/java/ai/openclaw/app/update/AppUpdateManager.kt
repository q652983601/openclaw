package ai.openclaw.app.update

import ai.openclaw.app.BuildConfig
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.resumeWithException

/**
 * Checks the GitHub releases page for this fork and triggers an in-app APK update.
 *
 * The updater compares the version tag published on GitHub against the versionName baked into
 * [BuildConfig]. When a newer release is found it downloads the matching APK through the platform
 * [DownloadManager] and launches the package-install intent using a [FileProvider].
 */
object AppUpdateManager {
  private const val REPO_OWNER = "q652983601"
  private const val REPO_NAME = "openclaw"
  private const val RELEASES_URL =
    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

  private val httpClient = OkHttpClient()
  private val json = Json { ignoreUnknownKeys = true }

  /** Result of a remote version check. */
  sealed class CheckResult {
    /** No newer release is available. */
    data object UpToDate : CheckResult()

    /** A newer release is available. */
    data class UpdateAvailable(
      val version: String,
      val htmlUrl: String,
      val asset: ReleaseAsset,
    ) : CheckResult()

    /** The check could not be completed. [reason] is already localized for display. */
    data class Error(val reason: String) : CheckResult()
  }

  /** State machine surfaced by the UI while an update is in progress. */
  sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Downloading(val progress: Int?) : UpdateState()
    data class ReadyToInstall(val file: File) : UpdateState()
    data class Error(val reason: String) : UpdateState()
  }

  /** GitHub release JSON subset. */
  @Serializable
  private data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<ReleaseAsset> = emptyList(),
  )

  /** Release asset JSON subset. */
  @Serializable
  data class ReleaseAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long,
  )

  /**
   * Queries GitHub for the latest release and compares it with the running build.
   *
   * The comparison strips a leading "v" from the tag and appends "-dev" suffix handling so debug
   * builds still compare cleanly against release tags.
   */
  suspend fun checkForUpdate(): CheckResult =
    withContext(Dispatchers.IO) {
      runCatching {
        val request = Request.Builder().url(RELEASES_URL).header("Accept", "application/vnd.github+json").build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
          return@withContext CheckResult.Error("GitHub API ${response.code}")
        }
        val body = response.body.string()
        if (body.isBlank()) return@withContext CheckResult.Error("Empty response")
        val release = json.decodeFromString(GitHubRelease.serializer(), body)
        val remoteVersion = release.tag_name.removePrefix("v")
        val localVersion = BuildConfig.VERSION_NAME.trim().ifEmpty { "dev" }
        val asset = pickAsset(release.assets)
          ?: return@withContext CheckResult.Error("No APK asset found")

        if (isNewerVersion(remoteVersion, localVersion)) {
          CheckResult.UpdateAvailable(
            version = remoteVersion,
            htmlUrl = release.html_url,
            asset = asset,
          )
        } else {
          CheckResult.UpToDate
        }
      }.getOrElse { throwable ->
        CheckResult.Error(throwable.message ?: throwable.javaClass.simpleName)
      }
    }

  /**
   * Picks the best APK asset from a release. Prefers the third-party release APK because the
   * Chinese-localized fork is distributed outside Google Play.
   */
  private fun pickAsset(assets: List<ReleaseAsset>): ReleaseAsset? {
    if (assets.isEmpty()) return null
    val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
    return apks.find { it.name.contains("thirdParty", ignoreCase = true) }
      ?: apks.find { it.name.contains("release", ignoreCase = true) }
      ?: apks.firstOrNull()
      ?: assets.firstOrNull()
  }

  /**
   * Simple semantic-ish comparison. Returns true when [remote] is greater than [local].
   *
   * Both values are normalised by dropping a trailing "-dev" suffix and then split into integer
   * numeric segments so that `2026.6.10` compares correctly against `2026.6.9`.
   */
  private fun isNewerVersion(remote: String, local: String): Boolean {
    val remoteParts = remote.lowercase().removeSuffix("-dev").split(".", "-", "_")
    val localParts = local.lowercase().removeSuffix("-dev").split(".", "-", "_")
    val maxLength = maxOf(remoteParts.size, localParts.size)
    for (i in 0 until maxLength) {
      val r = remoteParts.getOrNull(i)?.toIntOrNull() ?: Int.MAX_VALUE
      val l = localParts.getOrNull(i)?.toIntOrNull() ?: Int.MAX_VALUE
      if (r != l) return r > l
    }
    return false
  }

  /**
   * Downloads [asset] through the system [DownloadManager] and returns the local file when the
   * download completes. Progress is reported through [onStateChange] when the platform broadcasts
   * download-change events.
   */
  suspend fun downloadUpdate(
    context: Context,
    asset: ReleaseAsset,
    onStateChange: (UpdateState) -> Unit,
  ): File =
    suspendCancellableCoroutine { continuation ->
      val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
      val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
      val outputFile = File(updatesDir, asset.name)
      outputFile.delete()

      val request =
        DownloadManager.Request(Uri.parse(asset.browser_download_url))
          .setTitle("OpenClaw ${asset.name}")
          .setDescription("Downloading update…")
          .setDestinationUri(Uri.fromFile(outputFile))
          .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
          .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

      val downloadId = downloadManager.enqueue(request)
      onStateChange(UpdateState.Downloading(progress = null))

      val receiver =
        object : BroadcastReceiver() {
          override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != downloadId) return

            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (!cursor.moveToFirst()) {
              cursor.close()
              return
            }
            val statusIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusIndex)
            val reasonIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
            val reason = cursor.getInt(reasonIndex)
            cursor.close()

            when (status) {
              DownloadManager.STATUS_SUCCESSFUL -> {
                ctx.unregisterReceiver(this)
                onStateChange(UpdateState.ReadyToInstall(outputFile))
                continuation.resume(outputFile) { _, _, _ -> }
              }
              DownloadManager.STATUS_FAILED -> {
                ctx.unregisterReceiver(this)
                val error = "Download failed: $reason"
                onStateChange(UpdateState.Error(error))
                continuation.resumeWithException(RuntimeException(error))
              }
            }
          }
        }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
      } else {
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
      }

      continuation.invokeOnCancellation {
        runCatching { context.unregisterReceiver(receiver) }
        downloadManager.remove(downloadId)
      }
    }

  /**
   * Launches the platform package installer for the downloaded APK.
   *
   * Callers must ensure the [REQUEST_INSTALL_PACKAGES] permission has been granted (the platform
   * will prompt the user the first time this intent is launched).
   */
  fun installUpdate(context: Context, file: File) {
    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, file)
    val intent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
      }
    context.startActivity(intent)
  }

  /** Opens the GitHub release page in a browser as a fallback when in-app install is unavailable. */
  fun openReleasePage(context: Context, htmlUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl))
    context.startActivity(intent)
  }
}
