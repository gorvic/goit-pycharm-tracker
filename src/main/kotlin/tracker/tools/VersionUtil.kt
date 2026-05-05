package tracker.tools

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object VersionUtil {
    private const val PLUGIN_ID = "goit.pycharm.tracker"

    fun currentVersion(): String {
        return PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version ?: "1.4.0"
    }

    fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val max = maxOf(latestParts.size, currentParts.size)
        var result = false

        for (index in 0 until max) {
            val l = latestParts.getOrElse(index) { 0 }
            val c = currentParts.getOrElse(index) { 0 }
            if (l > c) {
                result = true
                break
            } else if (l < c) {
                break
            }
        }

        return result
    }
}
