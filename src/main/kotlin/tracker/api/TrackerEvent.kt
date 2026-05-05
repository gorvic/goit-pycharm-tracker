package tracker.api

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TrackerEvent(
    val startedAt: Instant,
    val durationSeconds: Long
) {
    fun toApiJson(event: String, eventType: String): String = """
        {
          "event": "${escape(event)}",
          "params": {
            "type": "${escape(eventType)}",
            "startedAt": "${formatStartedAt(startedAt)}",
            "durationSeconds": $durationSeconds
          }
        }
    """.trimIndent()

    fun toQueueJson(): String = """
        {
          "startedAt": "${startedAt}",
          "durationSeconds": $durationSeconds
        }
    """.trimIndent()

    private fun formatStartedAt(value: Instant): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC)
            .format(value)
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    companion object {
        private val objectRegex = Regex("\\{[^{}]*}", RegexOption.DOT_MATCHES_ALL)
        private val startedAtRegex = Regex("\"startedAt\"\\s*:\\s*\"([^\"]+)\"")
        private val durationRegex = Regex("\"durationSeconds\"\\s*:\\s*(\\d+)")

        fun fromQueueJsonArray(json: String): List<TrackerEvent> {
            val result = mutableListOf<TrackerEvent>()

            for (match in objectRegex.findAll(json)) {
                val raw = match.value
                val startedAt = startedAtRegex.find(raw)?.groupValues?.getOrNull(1)
                val duration = durationRegex.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull()

                if (startedAt != null && duration != null) {
                    result.add(
                        TrackerEvent(
                            startedAt = Instant.parse(startedAt),
                            durationSeconds = duration
                        )
                    )
                }
            }

            return result
        }
    }
}
