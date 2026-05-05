package tracker.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import tracker.settings.TrackerSettingsService
import java.nio.file.Path
import java.nio.file.Paths

object TrackerPaths {
    private const val DIR_NAME = "goit-pycharm-tracker"

    val configDir: Path = Path.of(PathManager.getConfigPath(), DIR_NAME)
    val configFile: Path = configDir.resolve("config.json")

    val dataDir: Path
        get() = resolveDataDir()

    val eventsFile: Path
        get() = dataDir.resolve("events.json")

    val sessionsFile: Path
        get() = dataDir.resolve("sessions.jsonl")

    val logFile: Path
        get() = dataDir.resolve("tracker.log")

    private fun resolveDataDir(): Path {
        var custom = ""
        try {
            custom = ApplicationManager.getApplication()
                .getService(TrackerSettingsService::class.java)
                .get()
                .customDataDir
                .trim()
        } catch (_: Exception) {
            // Use default before settings service is available.
        }

        return if (custom.isBlank()) {
            Path.of(PathManager.getSystemPath(), DIR_NAME)
        } else {
            Paths.get(custom)
        }
    }
}
