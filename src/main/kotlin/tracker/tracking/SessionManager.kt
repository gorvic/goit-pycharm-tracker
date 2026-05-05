package tracker.tracking

import tracker.api.TrackerEvent
import java.time.Instant

class SessionManager {
    private var sessionStart: Instant? = null
    private var lastActivity: Instant? = null

    fun isActive(): Boolean = sessionStart != null

    fun onActivity() {
        val now = Instant.now()
        if (sessionStart == null) {
            sessionStart = now
        }
        lastActivity = now
    }

    fun extendTo(value: Instant) {
        if (sessionStart != null) {
            lastActivity = value
        }
    }

    fun checkIdle(idleTimeoutSeconds: Int): TrackerEvent? {
        val last = lastActivity
        val now = Instant.now()
        var result: TrackerEvent? = null

        if (last != null && now.epochSecond - last.epochSecond >= idleTimeoutSeconds) {
            result = closeSession()
        }

        return result
    }

    fun closeSession(): TrackerEvent? {
        val start = sessionStart
        val end = lastActivity
        var result: TrackerEvent? = null

        if (start != null && end != null && end.epochSecond > start.epochSecond) {
            result = TrackerEvent(start, end.epochSecond - start.epochSecond)
        }

        sessionStart = null
        lastActivity = null
        return result
    }

    fun closeSessionAt(end: Instant): TrackerEvent? {
        val start = sessionStart
        var result: TrackerEvent? = null

        if (start != null && end.epochSecond > start.epochSecond) {
            result = TrackerEvent(start, end.epochSecond - start.epochSecond)
        }

        sessionStart = null
        lastActivity = null
        return result
    }
}
