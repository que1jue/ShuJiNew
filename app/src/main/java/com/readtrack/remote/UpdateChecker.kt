package com.readtrack.remote

import com.readtrack.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateChecker {

    // 更新检查指向你自己的仓库
    private const val GITHUB_OWNER = "que1jue"
    private const val GITHUB_REPO = "ShuJiNew"

    private const val GITHUB_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdate(client: OkHttpClient): UpdateResult = checkGitHub(client)

    private fun checkGitHub(client: OkHttpClient): UpdateResult {
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw RuntimeException("GitHub API 请求失败: ${response.code}")

            val release = json.decodeFromString(GitHubRelease.serializer(), body)
            val tagVersion = release.tagName.removePrefix("v")
            val hasUpdate = compareVersions(tagVersion, BuildConfig.VERSION_NAME) > 0

            val downloadUrl = release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl
                ?: release.htmlUrl

            return UpdateResult(
                hasUpdate = hasUpdate,
                latestVersion = tagVersion,
                currentVersion = BuildConfig.VERSION_NAME,
                releaseNotes = release.body ?: "",
                downloadUrl = downloadUrl,
                releasePageUrl = release.htmlUrl
            )
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
