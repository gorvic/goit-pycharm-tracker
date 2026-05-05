package tracker.settings

object TrackerConfigParser {
    fun parse(json: String): TrackerSettings {
        val result = TrackerSettings()
        result.userToken = readString(json, "userToken")
        result.event = readString(json, "event")
        result.eventType = readString(json, "eventType")
        result.endpoint = readString(json, "endpoint")
        result.fileExtensions = readString(json, "fileExtensions")
        result.tickSeconds = readInt(json, "tickSeconds")
        result.idleTimeoutSeconds = readInt(json, "idleTimeoutSeconds")
        result.flushIntervalSeconds = readInt(json, "flushIntervalSeconds")
        result.maxQueueSize = readInt(json, "maxQueueSize")
        result.requestTimeoutMs = readInt(json, "requestTimeoutMs")
        result.updateInfoUrl = readString(json, "updateInfoUrl")
        result.disableStatusBarButton = readBoolean(json, "disableStatusBarButton")
        result.activateOnStartup = readBoolean(json, "activateOnStartup")
        result.logMode = readString(json, "logMode")
        result.maxLogFileSizeMb = readInt(json, "maxLogFileSizeMb")
        result.customDataDir = readString(json, "customDataDir")
        result.activityTrackingMode = readString(json, "activityTrackingMode")
        return result
    }

    private fun readString(json: String, key: String): String {
        val pattern = Regex("\\\"$key\\\"\\s*:\\s*\\\"(.*?)\\\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: ""
    }

    private fun readInt(json: String, key: String): Int {
        val pattern = Regex("\\\"$key\\\"\\s*:\\s*(\\d+)")
        return pattern.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun readBoolean(json: String, key: String): Boolean {
        val pattern = Regex("\\\"$key\\\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        return pattern.find(json)?.groupValues?.getOrNull(1)?.equals("true", ignoreCase = true) ?: false
    }
}
