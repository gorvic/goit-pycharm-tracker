package tracker.logging

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import tracker.config.TrackerPaths
import com.intellij.openapi.application.ApplicationManager
import tracker.tools.TrackerTextLog
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@Service(Service.Level.APP)
class SessionLogWriter {

    private val log = Logger.getInstance(SessionLogWriter::class.java)
    private val textLog: TrackerTextLog = ApplicationManager.getApplication().getService(TrackerTextLog::class.java)

    fun append(record: SessionRecord) {
        try {
            Files.createDirectories(TrackerPaths.dataDir)
            Files.writeString(
                TrackerPaths.sessionsFile,
                record.toJsonLine() + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
            textLog.info("Session log written: project=${record.projectName}, startedAt=${record.startedAt}, durationSeconds=${record.durationSeconds}, file=${TrackerPaths.sessionsFile}")
        } catch (e: Exception) {
            log.warn("Cannot write GoIT Tracker session log", e)
            textLog.warn("Cannot write session log: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
