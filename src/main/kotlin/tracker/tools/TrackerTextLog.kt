package tracker.tools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import tracker.config.TrackerPaths
import tracker.settings.TrackerSettingsService
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant

@Service(Service.Level.APP)
class TrackerTextLog {
    private val settingsService: TrackerSettingsService = ApplicationManager.getApplication()
        .getService(TrackerSettingsService::class.java)

    @Synchronized
    fun info(message: String) {
        val mode = settingsService.get().logMode.trim().lowercase()
        if (mode != "errors only") {
            append("INFO", message)
        }
    }

    @Synchronized
    fun debug(message: String) {
        val mode = settingsService.get().logMode.trim().lowercase()
        if (mode == "debug") {
            append("DEBUG", message)
        }
    }

    @Synchronized
    fun warn(message: String) {
        append("WARN", message)
    }

    @Synchronized
    fun error(message: String) {
        append("ERROR", message)
    }

    @Synchronized
    fun clear() {
        try {
            Files.createDirectories(TrackerPaths.dataDir)
            Files.writeString(TrackerPaths.logFile, "")
        } catch (_: Exception) {
            // File logging must never affect IDE/plugin behavior.
        }
    }

    @Synchronized
    private fun append(level: String, message: String) {
        try {
            Files.createDirectories(TrackerPaths.dataDir)
            clearIfTooLarge()
            Files.writeString(
                TrackerPaths.logFile,
                "${Instant.now()} [$level] $message${System.lineSeparator()}",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        } catch (_: Exception) {
            // File logging must never affect IDE/plugin behavior.
        }
    }

    private fun clearIfTooLarge() {
        val maxMb = settingsService.get().maxLogFileSizeMb
        if (maxMb <= 0 || !Files.exists(TrackerPaths.logFile)) {
            return
        }

        val maxBytes = maxMb.toLong() * 1024L * 1024L
        if (Files.size(TrackerPaths.logFile) > maxBytes) {
            Files.writeString(TrackerPaths.logFile, "")
        }
    }
}
