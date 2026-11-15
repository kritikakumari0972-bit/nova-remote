package com.novaremote.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class HomeAssistantResult<T>(val value: T? = null, val error: String? = null) {
    val isSuccess: Boolean get() = error == null && value != null
}

data class HomeAssistantMediaPlayer(
    val entityId: String,
    val friendlyName: String,
    val state: String,
    val manufacturer: String?,
    val model: String?,
    val mediaTitle: String?,
    val volumeLevel: Double?
) {
    val isAvailable: Boolean get() = state != "unavailable" && state != "unknown"
}

enum class HomeAssistantMediaAction(val service: String, val label: String) {
    POWER_ON("turn_on", "Power on"),
    POWER_OFF("turn_off", "Power off"),
    PLAY_PAUSE("media_play_pause", "Play / pause"),
    VOLUME_DOWN("volume_down", "Volume −"),
    VOLUME_UP("volume_up", "Volume +"),
    MUTE("volume_mute", "Mute")
}

/**
 * Home Assistant's documented local REST API adapter.
 * It targets media_player entities already configured by the user's Home Assistant instance;
 * it does not claim direct support for every TV vendor or model.
 */
class HomeAssistantClient(
    private val baseAddress: String,
    private val accessToken: String
) {
    suspend fun testConnection(): HomeAssistantResult<String> = withContext(Dispatchers.IO) {
        val response = request("GET", "/api/", null)
        if (!response.isSuccess) return@withContext HomeAssistantResult(error = response.error)
        val message = runCatching { JSONObject(response.value!!).optString("message") }.getOrNull()
        if (message == "API running.") HomeAssistantResult(value = message)
        else HomeAssistantResult(error = "The address responded, but it is not a Home Assistant API endpoint.")
    }

    suspend fun getMediaPlayers(): HomeAssistantResult<List<HomeAssistantMediaPlayer>> =
        withContext(Dispatchers.IO) {
            val response = request("GET", "/api/states", null)
            if (!response.isSuccess) return@withContext HomeAssistantResult(error = response.error)
            runCatching {
                parseHomeAssistantMediaPlayers(JSONArray(response.value!!))
            }.fold(
                onSuccess = { HomeAssistantResult(value = it) },
                onFailure = { HomeAssistantResult(error = "Could not parse Home Assistant entities: " + (it.message ?: "invalid response")) }
            )
        }

    suspend fun sendMediaPlayerAction(
        entityId: String,
        action: HomeAssistantMediaAction
    ): HomeAssistantResult<Boolean> = withContext(Dispatchers.IO) {
        if (!entityId.matches(Regex("media_player\\.[A-Za-z0-9_]+"))) {
            return@withContext HomeAssistantResult(error = "Invalid media-player entity ID.")
        }
        val payload = JSONObject().put("entity_id", entityId)
        val response = request("POST", "/api/services/media_player/" + action.service, payload)
        if (response.isSuccess) HomeAssistantResult(value = true)
        else HomeAssistantResult(error = response.error)
    }

    private fun request(method: String, path: String, payload: JSONObject?): HomeAssistantResult<String> {
        HomeAssistantAddressPolicy.validate(baseAddress)?.let {
            return HomeAssistantResult(error = it)
        }
        if (!isValidHomeAssistantToken(accessToken)) {
            return HomeAssistantResult(error = "Enter a valid Home Assistant access token.")
        }

        var connection: HttpURLConnection? = null
        return try {
            val origin = baseAddress.trim().trimEnd('/')
            val active = (URL(origin + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 4000
                readTimeout = 7000
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer " + accessToken)
                if (payload != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                }
            }
            connection = active
            val code = active.responseCode
            val stream = if (code in 200..299) active.inputStream else active.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { readBounded(it, MAX_RESPONSE_CHARS) }.orEmpty()
            if (code !in 200..299) {
                val message = when (code) {
                    401 -> "Home Assistant rejected the access token (HTTP 401). Check the token."
                    403 -> "Home Assistant denied this account's permissions (HTTP 403)."
                    404 -> "Home Assistant endpoint not found (HTTP 404). Check the server URL."
                    503 -> "Home Assistant is unavailable or shutting down (HTTP 503)."
                    else -> "Home Assistant returned HTTP " + code + ": " + body.take(240)
                }
                HomeAssistantResult(error = message)
            } else {
                HomeAssistantResult(value = body)
            }
        } catch (_: Exception) {
            HomeAssistantResult(error = "Could not reach Home Assistant. Check the Wi-Fi connection, local address and server status.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBounded(reader: java.io.BufferedReader, maxChars: Int): String {
        val output = StringBuilder()
        val buffer = CharArray(4096)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            if (output.length + count > maxChars) throw java.io.IOException("Home Assistant response is too large.")
            output.append(buffer, 0, count)
        }
        return output.toString()
    }

    private companion object {
        const val MAX_RESPONSE_CHARS = 4 * 1024 * 1024
    }
}

internal fun isValidHomeAssistantToken(token: String): Boolean =
    token.length in 16..4096 &&
        token.all { it.code in 33..126 } &&
        token.matches(Regex("[A-Za-z0-9._~+/=-]{16,4096}"))

internal object HomeAssistantAddressPolicy {
    fun validate(rawAddress: String): String? {
        val uri = try {
            URI(rawAddress.trim())
        } catch (_: Exception) {
            return "Enter a valid local Home Assistant address."
        }
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return "Home Assistant address must use HTTP or HTTPS."
        }
        if (uri.port !in -1..65535 || uri.port == 0 || uri.userInfo != null ||
            uri.query != null || uri.fragment != null || uri.path !in listOf("", "/")) {
            return "Enter only the Home Assistant server origin, with an optional port."
        }
        val host = uri.host.lowercase()
        if (host == "localhost" || host.endsWith(".local")) return null
        val octets = host.split('.')
        if (octets.size != 4) return "Only private IPv4, localhost or .local addresses are accepted."
        val parts = octets.map { it.toIntOrNull() ?: return "Invalid IPv4 address." }
        if (parts.any { it !in 0..255 }) return "Invalid IPv4 address."
        val privateAddress = parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31) ||
            (parts[0] == 169 && parts[1] == 254) ||
            parts[0] == 127
        return if (privateAddress) null
        else "Only private IPv4, localhost or .local addresses are accepted."
    }
}

internal fun parseHomeAssistantMediaPlayers(states: JSONArray): List<HomeAssistantMediaPlayer> =
    buildList {
        for (index in 0 until states.length()) {
            val state = states.optJSONObject(index) ?: continue
            val entityId = state.optString("entity_id")
            if (!entityId.startsWith("media_player.")) continue
            val attributes = state.optJSONObject("attributes") ?: JSONObject()
            val friendlyName = attributes.optString("friendly_name").takeIf { it.isNotBlank() }
                ?: entityId
            val manufacturer = attributes.optString("manufacturer").takeIf { it.isNotBlank() }
            val model = attributes.optString("model").takeIf { it.isNotBlank() }
            val mediaTitle = attributes.optString("media_title").takeIf { it.isNotBlank() }
            val volume = attributes.optDouble("volume_level", Double.NaN).takeIf { it.isFinite() }
            add(
                HomeAssistantMediaPlayer(
                    entityId = entityId,
                    friendlyName = friendlyName,
                    state = state.optString("state", "unknown"),
                    manufacturer = manufacturer,
                    model = model,
                    mediaTitle = mediaTitle,
                    volumeLevel = volume
                )
            )
        }
    }.sortedBy { it.friendlyName.lowercase() }
