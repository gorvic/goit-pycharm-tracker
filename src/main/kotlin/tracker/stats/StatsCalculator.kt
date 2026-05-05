package tracker.stats

import tracker.config.TrackerPaths
import java.nio.file.Files

class StatsCalculator {
    fun load(): StatsResult {
        val sessions = loadSessions()
        val byDay = sessions.groupBy { it.day }.mapValues { entry -> entry.value.sumOf { it.durationSeconds } }.toSortedMap()
        val byProject = sessions.groupBy { it.projectName }.mapValues { entry -> entry.value.sumOf { it.durationSeconds } }.toSortedMap()
        val total = sessions.sumOf { it.durationSeconds }
        val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val todayTotal = byDay[today] ?: 0L
        return StatsResult(total, todayTotal, byDay, byProject)
    }

    private fun loadSessions(): List<SessionItem> {
        val file = TrackerPaths.sessionsFile
        if (!Files.exists(file)) {
            return emptyList()
        }

        return Files.readAllLines(file).mapNotNull { parseLine(it) }
    }

    private fun parseLine(line: String): SessionItem? {
        val startedAt = readString(line, "startedAt")
        val projectName = readString(line, "projectName").ifBlank { "Unknown" }
        val durationSeconds = readLong(line, "durationSeconds")
        return if (startedAt.isBlank() || durationSeconds <= 0) {
            null
        } else {
            SessionItem(
                day = startedAt.take(10),
                projectName = projectName,
                durationSeconds = durationSeconds
            )
        }
    }

    private fun readString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
        return regex.find(json)?.groupValues?.getOrNull(1) ?: ""
    }

    private fun readLong(json: String, key: String): Long {
        val regex = Regex("\\\"$key\\\"\\s*:\\s*(\\d+)")
        return regex.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    }

    companion object {
        fun formatDuration(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${secs}s"
                else -> "${secs}s"
            }
        }
    }
}

class StatsResult(
    val totalSeconds: Long,
    val todaySeconds: Long,
    val byDay: Map<String, Long>,
    val byProject: Map<String, Long>
)

class SessionItem(
    val day: String,
    val projectName: String,
    val durationSeconds: Long
)
