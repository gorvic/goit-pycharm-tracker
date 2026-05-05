package tracker.tools

import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class UpdateChecker(private val timeoutMs: Int) {
    fun check(updateInfoUrl: String): UpdateInfo? {
        var result: UpdateInfo? = null
        var connection: HttpURLConnection? = null

        try {
            connection = URL(updateInfoUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            val code = connection.responseCode
            if (code in 200..299) {
                result = UpdateInfo.fromJson(connection.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (_: Exception) {
            result = null
        } finally {
            connection?.disconnect()
        }

        return result
    }

    fun openDownloadPage(url: String) {
        if (url.isBlank() || !Desktop.isDesktopSupported()) {
            return
        }
        Desktop.getDesktop().browse(URI(url))
    }
}

class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
) {
    companion object {
        fun fromJson(json: String): UpdateInfo {
            return UpdateInfo(
                version = readString(json, "version"),
                downloadUrl = readString(json, "downloadUrl"),
                changelog = readString(json, "changelog")
            )
        }

        private fun readString(json: String, key: String): String {
            val pattern = Regex("\\\"$key\\\"\\s*:\\s*\\\"(.*?)\\\"", RegexOption.DOT_MATCHES_ALL)
            return pattern.find(json)?.groupValues?.getOrNull(1)
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?: ""
        }
    }
}
