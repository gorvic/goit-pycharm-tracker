package tracker.tools

import com.intellij.openapi.application.ApplicationManager
import tracker.config.TrackerPaths
import java.awt.Desktop
import java.nio.file.Files

object LogOpener {
    fun openTrackerLog() {
        openFile(TrackerPaths.logFile)
    }

    fun openSessionsLog() {
        openFile(TrackerPaths.sessionsFile)
    }

    fun clearLogs() {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                Files.createDirectories(TrackerPaths.dataDir)
                Files.writeString(TrackerPaths.logFile, "")
            } catch (_: Exception) {
                // Ignore.
            }
        }
    }

    private fun openFile(path: java.nio.file.Path) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                Files.createDirectories(TrackerPaths.dataDir)
                if (!Files.exists(path)) {
                    Files.writeString(path, "")
                }

                val file = path.toFile()
                if (System.getProperty("os.name").lowercase().contains("win")) {
                    ProcessBuilder("notepad.exe", file.absolutePath).start()
                } else if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                }
            } catch (_: Exception) {
                // Opening logs is a convenience feature; ignore failures here.
            }
        }
    }
}
