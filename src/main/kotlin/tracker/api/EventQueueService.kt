package tracker.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import tracker.config.TrackerPaths
import tracker.settings.TrackerSettingsService
import tracker.tools.TrackerTextLog
import java.nio.file.Files
import java.util.ArrayDeque
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class EventQueueService {

    private val log = Logger.getInstance(EventQueueService::class.java)
    private val events = ArrayDeque<TrackerEvent>()
    private val flushing = AtomicBoolean(false)
    private val apiClient: ApiClient = ApplicationManager.getApplication().getService(ApiClient::class.java)
    private val settingsService: TrackerSettingsService = ApplicationManager.getApplication()
        .getService(TrackerSettingsService::class.java)
    private val textLog: TrackerTextLog = ApplicationManager.getApplication()
        .getService(TrackerTextLog::class.java)
    private val timer = Timer("GoIT Tracker Queue Sender", true)

    init {
        textLog.info("Queue service started. eventsFile=${TrackerPaths.eventsFile}")
        ensureQueueFileExists()
        loadQueue()
        startBackgroundSender()
        flushAsync()
    }

    @Synchronized
    fun add(event: TrackerEvent) {
        val maxQueueSize = settingsService.get().maxQueueSize
        events.addLast(event)
        textLog.info("Event added to persistent queue: startedAt=${event.startedAt}, durationSeconds=${event.durationSeconds}, queueSize=${events.size}, file=${TrackerPaths.eventsFile}")
        while (maxQueueSize > 0 && events.size > maxQueueSize) {
            events.removeFirst()
        }
        saveQueue()
    }

    fun addAndFlushAsync(event: TrackerEvent) {
        add(event)
        flushAsync()
    }

    @Synchronized
    fun pendingCount(): Int {
        return events.size
    }

    @Synchronized
    fun clearQueue() {
        events.clear()
        saveQueue()
        textLog.warn("Persistent queue manually cleared")
    }

    fun flushAsync() {
        if (!flushing.compareAndSet(false, true)) {
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                flushAllBatches()
            } finally {
                flushing.set(false)
            }
        }
    }

    /**
     * Makes one short background delivery attempt and returns immediately after that attempt.
     * The caller should never use this as a blocking shutdown guarantee: local persistence is the guarantee.
     */
    fun flushOnceAsync() {
        flushAsync()
    }

    private fun flushAllBatches() {
        var keepSending = true

        while (keepSending) {
            val event = peekFirst()
            keepSending = if (event == null) {
                false
            } else {
                when (apiClient.send(event)) {
                    ApiSendResult.Success -> {
                        removeFirst(1)
                        textLog.info("Successfully sent and removed 1 event from queue")
                        true
                    }

                    ApiSendResult.InvalidCredentials -> {
                        textLog.warn("Queue flush stopped: invalid credentials. ${queueSize()} event(s) remain in ${TrackerPaths.eventsFile}")
                        false
                    }

                    ApiSendResult.RetryLater -> {
                        textLog.warn("Queue flush failed. ${queueSize()} event(s) remain in ${TrackerPaths.eventsFile}")
                        false
                    }
                }
            }
        }
    }

    @Synchronized
    private fun peekFirst(): TrackerEvent? {
        return events.firstOrNull()
    }

    @Synchronized
    private fun queueSize(): Int {
        return events.size
    }

    @Synchronized
    private fun removeFirst(count: Int) {
        repeat(count.coerceAtMost(events.size)) {
            events.removeFirst()
        }
        saveQueue()
    }

    private fun startBackgroundSender() {
        val intervalSeconds = settingsService.get().flushIntervalSeconds.coerceAtLeast(30)
        val intervalMs = intervalSeconds * 1000L

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                flushAsync()
            }
        }, intervalMs, intervalMs)
    }

    @Synchronized
    private fun ensureQueueFileExists() {
        try {
            Files.createDirectories(TrackerPaths.dataDir)
            if (!Files.exists(TrackerPaths.eventsFile)) {
                Files.writeString(TrackerPaths.eventsFile, "[]")
            }
        } catch (e: Exception) {
            log.warn("Cannot initialize GoIT Tracker queue file", e)
            textLog.warn("Cannot initialize queue file: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    @Synchronized
    private fun loadQueue() {
        try {
            if (!Files.exists(TrackerPaths.eventsFile)) {
                return
            }

            val text = Files.readString(TrackerPaths.eventsFile)
            TrackerEvent.fromQueueJsonArray(text).forEach { events.addLast(it) }
            log.info("GoIT Tracker loaded ${events.size} queued events")
            textLog.info("Loaded ${events.size} queued event(s) from disk")
        } catch (e: Exception) {
            log.warn("Cannot load GoIT Tracker queue", e)
        }
    }

    @Synchronized
    private fun saveQueue() {
        try {
            Files.createDirectories(TrackerPaths.dataDir)
            val text = events.joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]") { it.toQueueJson() }
            Files.writeString(TrackerPaths.eventsFile, text)
            textLog.info("Queue saved. queueSize=${events.size}, file=${TrackerPaths.eventsFile}")
        } catch (e: Exception) {
            log.warn("Cannot save GoIT Tracker queue", e)
            textLog.warn("Cannot save queue: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
