package tracker.tracking

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.WindowManager
import tracker.Notifier
import tracker.api.EventQueueService
import tracker.api.TrackerEvent
import tracker.logging.SessionLogWriter
import tracker.logging.SessionRecord
import tracker.model.ActivityTrackingMode
import tracker.settings.TrackerSettingsService
import tracker.status.GoitStatusBarWidget
import tracker.tools.TrackerTextLog
import java.time.Instant
import java.util.Timer
import java.util.TimerTask

@Service(Service.Level.PROJECT)
class ProjectTrackingService(private val project: Project) : Disposable {

    private val settingsService: TrackerSettingsService = ApplicationManager.getApplication()
        .getService(TrackerSettingsService::class.java)
    private val queueService: EventQueueService = ApplicationManager.getApplication()
        .getService(EventQueueService::class.java)
    private val sessionLogWriter: SessionLogWriter = ApplicationManager.getApplication()
        .getService(SessionLogWriter::class.java)
    private val textLog: TrackerTextLog = ApplicationManager.getApplication()
        .getService(TrackerTextLog::class.java)

    private val sessionManager = SessionManager()
    private var started = false
    private var trackingEnabled = false
    private var timer: Timer? = null
    private var lastSoftActivityAt: Instant? = null

    fun start() {
        if (started) {
            return
        }

        started = true
        trackingEnabled = false
        textLog.info("Project service started disabled: project=${project.name}, basePath=${project.basePath ?: ""}")
        textLog.info("Runtime paths: config=${tracker.config.TrackerPaths.configFile}, queue=${tracker.config.TrackerPaths.eventsFile}, sessions=${tracker.config.TrackerPaths.sessionsFile}, log=${tracker.config.TrackerPaths.logFile}")
        registerListeners()
        startTimer()
        ensureRuntimeFiles()
        if (settingsService.get().activateOnStartup) {
            enable()
        }
    }

    fun isEnabled(): Boolean = trackingEnabled

    fun toggle(): Boolean {
        return if (trackingEnabled) {
            disable(flushActiveSession = true)
            false
        } else {
            enable()
        }
    }

    fun enable(): Boolean {
        val settings = settingsService.get()
        var enabled = false

        if (settings.userToken.isBlank()) {
            Notifier.error(
                project,
                "GoIT Tracker is not configured",
                "User token / UID is missing. Open Settings → Tools → GoIT PyCharm Tracker and set it."
            )
        } else if (settings.event.isBlank()) {
            Notifier.error(
                project,
                "GoIT Tracker is not configured",
                "Event is missing. Open Settings → Tools → GoIT PyCharm Tracker and set it."
            )
        } else {
            trackingEnabled = true
            enabled = true
            lastSoftActivityAt = null
            textLog.info("Tracking enabled for project: ${project.name}, activityMode=${settings.activityTrackingMode.ifBlank { "HARD" }}")
            Notifier.info(project, "GoIT Tracker enabled", "Tracking is enabled for this project.")
            refreshStatusBarWidget()
        }

        return enabled
    }

    fun disableAndFlush() {
        disable(flushActiveSession = true)
    }

    fun disable(flushActiveSession: Boolean) {
        if (flushActiveSession) {
            closeSendAndLogActiveSession()
            queueService.flushOnceAsync()
        }
        trackingEnabled = false
        lastSoftActivityAt = null
        textLog.info("Tracking disabled for project: ${project.name}")
        Notifier.info(project, "GoIT Tracker disabled", "Tracking is disabled for this project.")
        refreshStatusBarWidget()
    }

    private fun ensureRuntimeFiles() {
        try {
            java.nio.file.Files.createDirectories(tracker.config.TrackerPaths.dataDir)
            if (!java.nio.file.Files.exists(tracker.config.TrackerPaths.eventsFile)) {
                java.nio.file.Files.writeString(tracker.config.TrackerPaths.eventsFile, "[]")
            }
            if (!java.nio.file.Files.exists(tracker.config.TrackerPaths.sessionsFile)) {
                java.nio.file.Files.writeString(tracker.config.TrackerPaths.sessionsFile, "")
            }
        } catch (e: Exception) {
            textLog.warn("Cannot initialize runtime files: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun registerListeners() {
        val connection = ApplicationManager.getApplication().messageBus.connect(this)

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.document)
                    handleActivity(file, "document-change")
                }
            },
            this
        )

        connection.subscribe(
            FileDocumentManagerListener.TOPIC,
            object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) {
                    val file = FileDocumentManager.getInstance().getFile(document)
                    handleActivity(file, "document-save")
                }
            }
        )

        connection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.forEach { event ->
                        val file = event.file ?: return@forEach

                        when (event) {
                            is VFileCreateEvent -> handleActivity(file, "file-created")
                            is VFileDeleteEvent -> handleActivity(file, "file-deleted")
                        }
                    }
                }
            }
        )

        EditorFactory.getInstance().eventMulticaster.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.editor.document)
                    handleSoftActivity(file, "caret")
                }
            },
            this
        )

        EditorFactory.getInstance().eventMulticaster.addVisibleAreaListener(
            object : VisibleAreaListener {
                override fun visibleAreaChanged(event: VisibleAreaEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.editor.document)
                    handleSoftActivity(file, "scroll")
                }
            },
            this
        )

        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    handleSoftActivity(event.newFile, "file-switch")
                }
            }
        )
    }

    private fun startTimer() {
        val intervalMs = (settingsService.get().tickSeconds.coerceAtLeast(1) * 1000L)
        timer = Timer("GoIT Tracker Timer - ${project.name}", true)
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                onTimerTick()
            }
        }, intervalMs, intervalMs)
    }

    private fun onTimerTick() {
        if (!trackingEnabled) {
            return
        }

        val settings = settingsService.get()
        val event = if (isSoftMode()) {
            updateSoftSessionOnHeartbeat(settings.idleTimeoutSeconds)
        } else {
            sessionManager.checkIdle(settings.idleTimeoutSeconds)
        }

        if (event != null) {
            textLog.info("Idle timeout reached. Closing session: project=${project.name}, startedAt=${event.startedAt}, durationSeconds=${event.durationSeconds}")
            sendAndLog(event)
        }
    }

    private fun updateSoftSessionOnHeartbeat(idleTimeoutSeconds: Int): TrackerEvent? {
        val lastSoftActivity = lastSoftActivityAt
        var result: TrackerEvent? = null

        if (sessionManager.isActive() && lastSoftActivity != null) {
            val now = Instant.now()
            val elapsedSeconds = now.epochSecond - lastSoftActivity.epochSecond

            result = if (elapsedSeconds >= idleTimeoutSeconds) {
                sessionManager.closeSessionAt(lastSoftActivity.plusSeconds(idleTimeoutSeconds.toLong()))
            } else {
                sessionManager.extendTo(now)
                null
            }
        }

        return result
    }

    private fun handleActivity(file: VirtualFile?, source: String) {
        if (!trackingEnabled) {
            return
        }

        if (file == null) {
            return
        }

        if (!isAllowedExtension(file)) {
            textLog.info("Activity ignored by extension filter: source=$source, project=${project.name}, file=${file.path}")
            return
        }

        if (isSoftMode()) {
            lastSoftActivityAt = Instant.now()
        }

        val wasActive = sessionManager.isActive()
        sessionManager.onActivity()
        if (!wasActive) {
            textLog.info("Tracking session started: source=$source, project=${project.name}, file=${file.path}")
        } else {
            textLog.info("Tracking activity detected: source=$source, project=${project.name}, file=${file.path}")
        }
    }

    private fun handleSoftActivity(file: VirtualFile?, source: String) {
        if (!trackingEnabled || !isSoftMode()) {
            return
        }

        handleActivity(file, source)
    }

    private fun isSoftMode(): Boolean {
        return ActivityTrackingMode.from(settingsService.get().activityTrackingMode) == ActivityTrackingMode.SOFT
    }

    private fun isAllowedExtension(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase() ?: ""
        val allowed = settingsService.get().fileExtensions
            .split("|", ",", ";")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
        return extension.isNotBlank() && allowed.contains(extension)
    }

    private fun closeSendAndLogActiveSession() {
        if (isSoftMode() && sessionManager.isActive()) {
            val lastSoftActivity = lastSoftActivityAt
            val idleTimeoutSeconds = settingsService.get().idleTimeoutSeconds
            val now = Instant.now()
            if (lastSoftActivity != null && now.epochSecond - lastSoftActivity.epochSecond < idleTimeoutSeconds) {
                sessionManager.extendTo(now)
            }
        }

        val event = sessionManager.closeSession()
        if (event != null) {
            textLog.info("Closing active session: project=${project.name}, startedAt=${event.startedAt}, durationSeconds=${event.durationSeconds}")
            sendAndLog(event)
        } else {
            textLog.info("No active session to close: project=${project.name}")
        }
        lastSoftActivityAt = null
    }

    private fun sendAndLog(event: TrackerEvent) {
        queueService.addAndFlushAsync(event)
        textLog.info("Session queued: project=${project.name}, startedAt=${event.startedAt}, durationSeconds=${event.durationSeconds}")
        sessionLogWriter.append(
            SessionRecord(
                startedAt = event.startedAt,
                endedAt = event.startedAt.plusSeconds(event.durationSeconds),
                durationSeconds = event.durationSeconds,
                projectName = project.name,
                projectPath = project.basePath ?: "",
                sent = false
            )
        )
    }

    private fun refreshStatusBarWidget() {
        ApplicationManager.getApplication().invokeLater {
            WindowManager.getInstance()
                .getStatusBar(project)
                ?.updateWidget(GoitStatusBarWidget.WIDGET_ID)
        }
    }

    override fun dispose() {
        if (trackingEnabled) {
            closeSendAndLogActiveSession()
            queueService.flushOnceAsync()
        }
        trackingEnabled = false
        lastSoftActivityAt = null
        refreshStatusBarWidget()
        timer?.cancel()
        timer = null
    }
}
