package tracker.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import tracker.Notifier
import tracker.settings.TrackerSettingsService
import tracker.tools.TrackerTextLog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

@Service(Service.Level.APP)
class ApiClient {

    private val log = Logger.getInstance(ApiClient::class.java)
    private val settingsService: TrackerSettingsService = ApplicationManager.getApplication()
        .getService(TrackerSettingsService::class.java)
    private val textLog: TrackerTextLog = ApplicationManager.getApplication()
        .getService(TrackerTextLog::class.java)

    fun send(event: TrackerEvent): ApiSendResult {
        val settings = settingsService.get()
        var result: ApiSendResult = ApiSendResult.RetryLater

        if (settings.userToken.isBlank()) {
            textLog.warn("API send skipped: userToken is blank")
            result = ApiSendResult.InvalidCredentials
        } else if (settings.event.isBlank() || settings.endpoint.isBlank()) {
            textLog.warn("API send skipped: event or endpoint is blank")
        } else {
            result = executeRequest(event)
        }

        return result
    }

    private fun executeRequest(event: TrackerEvent): ApiSendResult {
        val settings = settingsService.get()
        var connection: HttpURLConnection? = null
        var result: ApiSendResult = ApiSendResult.RetryLater

        try {
            val payload = event.toApiJson(settings.event, settings.eventType)
            textLog.info("API request prepared: POST ${settings.endpoint}, event=${settings.event}, eventType=${settings.eventType}, startedAt=${event.startedAt}, durationSeconds=${event.durationSeconds}")
            textLog.info("API request body: $payload")

            connection = URI(settings.endpoint).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = settings.requestTimeoutMs
            connection.readTimeout = settings.requestTimeoutMs
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "PersonalToken ${settings.userToken}")

            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val body = readResponseBody(connection, code)
            result = parseResponse(code, body)

            when (result) {
                ApiSendResult.Success -> {
                    textLog.info("API accepted event. HTTP code: $code, body=$body")
                }

                ApiSendResult.InvalidCredentials -> {
                    log.warn("GoIT Tracker API authentication failed")
                    textLog.warn("API authentication failed. HTTP code: $code, body=$body")
                    Notifier.error(
                        null,
                        "GoIT Tracker login error",
                        "Invalid user token. Open Settings → Tools → GoIT PyCharm Tracker and update your token. Queued tracking events were kept for retry."
                    )
                }

                ApiSendResult.RetryLater -> {
                    log.warn("GoIT Tracker API rejected event. HTTP code: $code")
                    textLog.warn("API rejected event. Will retry later. HTTP code: $code, body=$body")
                }
            }
        } catch (e: Exception) {
            log.warn("GoIT Tracker API send failed", e)
            textLog.warn("API send failed. Will retry later: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            connection?.disconnect()
        }

        return result
    }

    private fun parseResponse(code: Int, body: String): ApiSendResult {
        val compact = body
            .replace(" ", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")

        var result: ApiSendResult = ApiSendResult.RetryLater

        if (code == 200 && compact.contains("\"success\":true") && compact.contains("\"error\":\"ok\"")) {
            result = ApiSendResult.Success
        } else if (code == 401 && compact.contains("\"success\":false") && compact.contains("\"error\":\"invalidCredentials\"")) {
            result = ApiSendResult.InvalidCredentials
        }

        return result
    }

    private fun readResponseBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        var result = ""
        if (stream != null) {
            result = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }

        return result
    }
}

sealed class ApiSendResult {
    data object Success : ApiSendResult()
    data object InvalidCredentials : ApiSendResult()
    data object RetryLater : ApiSendResult()
}
