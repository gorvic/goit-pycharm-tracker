package tracker.settings

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import tracker.config.TrackerPaths
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.APP)
@State(
    name = "GoitPyCharmTrackerSettings",
    storages = [Storage("goit-pycharm-tracker.xml")]
)
class TrackerSettingsService : PersistentStateComponent<TrackerSettings> {

    private val log = Logger.getInstance(TrackerSettingsService::class.java)
    private var settings = TrackerSettings()
    private var defaultsLoaded = false

    override fun getState(): TrackerSettings = get()

    override fun loadState(state: TrackerSettings) {
        settings = state
        applyDefaultsIfNeeded()
    }

    fun get(): TrackerSettings {
        applyDefaultsIfNeeded()
        return settings
    }

    fun update(newSettings: TrackerSettings) {
        settings = newSettings
        writeConfigCopy()
    }

    private fun applyDefaultsIfNeeded() {
        if (defaultsLoaded) {
            return
        }

        defaultsLoaded = true
        val defaults = loadDefaultSettings()
        settings.mergeMissing(defaults)
        writeConfigCopy()
    }

    private fun loadDefaultSettings(): TrackerSettings {
        val external = readExternalDefaultConfig()
        val bundled = readBundledDefaultConfig()
        val json = external.ifBlank { bundled }
        return if (json.isBlank()) TrackerSettings() else TrackerConfigParser.parse(json)
    }

    private fun readExternalDefaultConfig(): String {
        val pluginDir = Path.of(PathManager.getPluginsDir().toString(), "goit-pycharm-tracker")
        val file = pluginDir.resolve("default-config.json")
        return readFileText(file)
    }

    private fun readBundledDefaultConfig(): String {
        return javaClass.classLoader
            .getResourceAsStream("default-config.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: ""
    }

    private fun readFileText(file: Path): String {
        var result = ""
        if (Files.exists(file)) {
            result = Files.readString(file)
        }
        return result
    }

    private fun writeConfigCopy() {
        try {
            Files.createDirectories(TrackerPaths.configDir)
            Files.writeString(TrackerPaths.configFile, settings.toJson())
        } catch (e: Exception) {
            log.warn("Cannot write GoIT Tracker config copy", e)
        }
    }
}
