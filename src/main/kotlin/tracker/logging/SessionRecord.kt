package tracker.logging

import java.time.Instant

class SessionRecord(
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long,
    val projectName: String,
    val projectPath: String,
    val sent: Boolean
) {
    fun toJsonLine(): String = """
        {"startedAt":"${startedAt}","endedAt":"${endedAt}","durationSeconds":$durationSeconds,"projectName":"${escape(projectName)}","projectPath":"${escape(projectPath)}","sent":$sent}
    """.trimIndent()

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}
