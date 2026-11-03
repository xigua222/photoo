package com.voicelike.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GiteeRelease(
    val id: Int,
    val tag_name: String,
    val target_commitish: String,
    val name: String,
    val body: String,
    val created_at: String,
    val assets: List<GiteeAsset>
)

data class GiteeAsset(
    val name: String,
    val browser_download_url: String
)

data class UpdateCheckResult(
    val release: GiteeRelease? = null,
    val errorMessage: String? = null,
    val channel: UpdateChannel? = null,
    val latestRelease: GiteeRelease? = null
)

enum class UpdateChannel {
    Gitee,
    GitHub
}

object UpdateManager {
    private const val GITEE_REPO_OWNER = "superwatermelon"
    private const val GITEE_REPO_NAME = "pho"
    private const val GITEE_API_URL =
        "https://gitee.com/api/v5/repos/$GITEE_REPO_OWNER/$GITEE_REPO_NAME/releases/latest"
    private const val GITHUB_REPO_OWNER = "xigua222"
    private const val GITHUB_REPO_NAME = "photoo"
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
    private const val UPDATE_CHECK_USER_AGENT = "Photoo-Android-UpdateChecker"

    private fun resolveChannel(language: AppLanguage): UpdateChannel {
        return if (language == AppLanguage.SimplifiedChinese) {
            UpdateChannel.Gitee
        } else {
            UpdateChannel.GitHub
        }
    }

    private fun apiUrlForChannel(channel: UpdateChannel): String {
        return when (channel) {
            UpdateChannel.Gitee -> GITEE_API_URL
            UpdateChannel.GitHub -> GITHUB_API_URL
        }
    }

    suspend fun checkForUpdate(currentVersion: String, language: AppLanguage): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            val channel = resolveChannel(language)
            try {
                val url = URL(apiUrlForChannel(channel))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", UPDATE_CHECK_USER_AGENT)
                when (channel) {
                    UpdateChannel.Gitee -> {
                        connection.setRequestProperty("Accept", "application/json")
                    }
                    UpdateChannel.GitHub -> {
                        connection.setRequestProperty("Accept", "application/vnd.github+json")
                        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    }
                }

                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val suffix = if (detail.isBlank()) "" else " - $detail"
                    return@withContext UpdateCheckResult(
                        errorMessage = "Update API failed (${channel.name}): HTTP $code$suffix",
                        channel = channel
                    )
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val release = parseRelease(response)
                    ?: return@withContext UpdateCheckResult(
                        errorMessage = "Invalid release payload (${channel.name})",
                        channel = channel
                    )
                val remoteVersion = release.tag_name.removePrefix("v")
                val localVersion = currentVersion.removePrefix("v")

                if (compareVersions(remoteVersion, localVersion) > 0) {
                    return@withContext UpdateCheckResult(release = release, channel = channel)
                }

                return@withContext UpdateCheckResult(
                    channel = channel,
                    latestRelease = release
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext UpdateCheckResult(
                    errorMessage = e.message ?: "Unknown network error",
                    channel = channel
                )
            }
        }
    }

    private fun parseRelease(raw: String): GiteeRelease? {
        return runCatching {
            val root = JSONObject(raw)
            val assetsJson = root.optJSONArray("assets") ?: JSONArray()
            val assets = buildList {
                for (i in 0 until assetsJson.length()) {
                    val item = assetsJson.optJSONObject(i) ?: continue
                    val name = item.optString("name", "")
                    val url = item.optString("browser_download_url", "")
                    if (name.isNotBlank() && url.isNotBlank()) {
                        add(
                            GiteeAsset(
                                name = name,
                                browser_download_url = url
                            )
                        )
                    }
                }
            }
            GiteeRelease(
                id = root.optInt("id", 0),
                tag_name = root.optString("tag_name", ""),
                target_commitish = root.optString("target_commitish", ""),
                name = root.optString("name", ""),
                body = root.optString("body", ""),
                created_at = root.optString("created_at", ""),
                assets = assets
            )
        }.getOrNull()
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(parts1.size, parts2.size)

        for (i in 0 until length) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            if (p1 > p2) return 1
            if (p1 < p2) return -1
        }
        return 0
    }

    suspend fun downloadApk(urlStr: String, destinationFile: File, onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext false
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(destinationFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                var lastProgress = -1
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt().coerceIn(0, 100)
                        if (progress != lastProgress) {
                            lastProgress = progress
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.write(data, 0, count)
                }
                if (fileLength > 0 && lastProgress < 100) {
                    withContext(Dispatchers.Main) {
                        onProgress(100)
                    }
                }
                output.close()
                input.close()
                return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    fun installApk(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
