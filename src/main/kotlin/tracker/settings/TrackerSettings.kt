package tracker.settings

class TrackerSettings {
    var userToken: String = ""
    var event: String = ""
    var eventType: String = ""
    var endpoint: String = ""
    var fileExtensions: String = ""
    var tickSeconds: Int = 0
    var idleTimeoutSeconds: Int = 0
    var flushIntervalSeconds: Int = 0
    var maxQueueSize: Int = 0
    var requestTimeoutMs: Int = 0
    var updateInfoUrl: String = ""

    var disableStatusBarButton: Boolean = false
    var activateOnStartup: Boolean = false
    var logMode: String = "Normal"
    var maxLogFileSizeMb: Int = 5
    var customDataDir: String = ""
    var activityTrackingMode: String = "HARD"

    fun copy(): TrackerSettings {
        val result = TrackerSettings()
        result.userToken = userToken
        result.event = event
        result.eventType = eventType
        result.endpoint = endpoint
        result.fileExtensions = fileExtensions
        result.tickSeconds = tickSeconds
        result.idleTimeoutSeconds = idleTimeoutSeconds
        result.flushIntervalSeconds = flushIntervalSeconds
        result.maxQueueSize = maxQueueSize
        result.requestTimeoutMs = requestTimeoutMs
        result.updateInfoUrl = updateInfoUrl
        result.disableStatusBarButton = disableStatusBarButton
        result.activateOnStartup = activateOnStartup
        result.logMode = logMode
        result.maxLogFileSizeMb = maxLogFileSizeMb
        result.customDataDir = customDataDir
        result.activityTrackingMode = activityTrackingMode
        return result
    }

    fun mergeMissing(defaults: TrackerSettings) {
        if (userToken.isBlank()) userToken = defaults.userToken
        if (event.isBlank()) event = defaults.event
        if (eventType.isBlank()) eventType = defaults.eventType
        if (endpoint.isBlank()) endpoint = defaults.endpoint
        if (fileExtensions.isBlank()) fileExtensions = defaults.fileExtensions
        if (tickSeconds <= 0) tickSeconds = defaults.tickSeconds
        if (idleTimeoutSeconds <= 0) idleTimeoutSeconds = defaults.idleTimeoutSeconds
        if (flushIntervalSeconds <= 0) flushIntervalSeconds = defaults.flushIntervalSeconds
        if (maxQueueSize <= 0) maxQueueSize = defaults.maxQueueSize
        if (requestTimeoutMs <= 0) requestTimeoutMs = defaults.requestTimeoutMs
        if (updateInfoUrl.isBlank()) updateInfoUrl = defaults.updateInfoUrl
        if (logMode.isBlank()) logMode = defaults.logMode.ifBlank { "Normal" }
        if (maxLogFileSizeMb <= 0) maxLogFileSizeMb = defaults.maxLogFileSizeMb.takeIf { it > 0 } ?: 5
        if (customDataDir.isBlank()) customDataDir = defaults.customDataDir
        if (activityTrackingMode.isBlank()) activityTrackingMode = defaults.activityTrackingMode.ifBlank { "HARD" }
    }

    fun toJson(): String = """
        {
          "userToken": "${escape(userToken)}",
          "event": "${escape(event)}",
          "eventType": "${escape(eventType)}",
          "endpoint": "${escape(endpoint)}",
          "fileExtensions": "${escape(fileExtensions)}",
          "tickSeconds": $tickSeconds,
          "idleTimeoutSeconds": $idleTimeoutSeconds,
          "flushIntervalSeconds": $flushIntervalSeconds,
          "maxQueueSize": $maxQueueSize,
          "requestTimeoutMs": $requestTimeoutMs,
          "updateInfoUrl": "${escape(updateInfoUrl)}",
          "disableStatusBarButton": $disableStatusBarButton,
          "activateOnStartup": $activateOnStartup,
          "logMode": "${escape(logMode)}",
          "maxLogFileSizeMb": $maxLogFileSizeMb,
          "customDataDir": "${escape(customDataDir)}",
          "activityTrackingMode": "${escape(activityTrackingMode)}"
        }
    """.trimIndent()

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}
