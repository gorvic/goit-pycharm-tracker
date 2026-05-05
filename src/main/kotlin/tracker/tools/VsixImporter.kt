package tracker.tools

import tracker.settings.TrackerConfigParser
import tracker.settings.TrackerSettings
import java.io.File
import java.util.zip.ZipFile

object VsixImporter {

    private const val DEFAULT_EVENT = "vscode"
    private const val DEFAULT_EVENT_TYPE = "workOnFrontend"
    private const val DEFAULT_API_URL = "https://dw.api.edu.goit.global/api/v1/event/user"
    private const val DEFAULT_FILE_EXTENSIONS = "js|html|css|scss|sass|json|ts|tsx|jsx|py"

    fun importFrom(file: File): TrackerSettings? {
        var result: TrackerSettings? = null

        try {
            ZipFile(file).use { zip ->
                val entry = zip.entries().asSequence()
                    .firstOrNull { it.name.replace('\\', '/').endsWith("extension/package.json") }
                    ?: zip.entries().asSequence()
                        .firstOrNull { it.name.replace('\\', '/').endsWith("package.json") }

                if (entry != null) {
                    val packageJson = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    result = parsePackageJson(packageJson)
                }
            }
        } catch (_: Exception) {
            result = null
        }

        return result
    }

    private fun parsePackageJson(json: String): TrackerSettings {
        val settings = TrackerSettings()

        settings.event = DEFAULT_EVENT

        settings.userToken = readDefaultValueByKeySuffix(json, "USER_TOKEN")

        settings.eventType = readDefaultValueByKeySuffix(json, "EVENT_TYPE").ifBlank {
            DEFAULT_EVENT_TYPE
        }

        settings.endpoint = readDefaultValueByKeySuffix(json, "API_URL").ifBlank {
            DEFAULT_API_URL
        }

        settings.fileExtensions = readDefaultValueByKeySuffix(json, "fileExtentions").ifBlank {
            DEFAULT_FILE_EXTENSIONS
        }

        val rawConfig = readEmbeddedDefaultConfig(json)
        if (rawConfig.isNotBlank()) {
            settings.mergeMissing(TrackerConfigParser.parse(rawConfig))
        }

        return settings
    }

    private fun readDefaultValueByKeySuffix(json: String, suffix: String): String {
        val pattern = Regex(
            """"([^"]*\.\Q$suffix\E)"\s*:\s*\{[\s\S]*?"default"\s*:\s*"((?:\\.|[^"\\])*)""""
        )

        val raw = pattern.find(json)
            ?.groupValues
            ?.getOrNull(2)
            ?: ""

        return unescapeJsonString(raw)
    }

    private fun readEmbeddedDefaultConfig(json: String): String {
        val pattern = Regex(
            """"goitDefaultConfig"\s*:\s*(\{[\s\S]*?\})"""
        )

        return pattern.find(json)
            ?.groupValues
            ?.getOrNull(1)
            ?: ""
    }

    private fun unescapeJsonString(value: String): String {
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}