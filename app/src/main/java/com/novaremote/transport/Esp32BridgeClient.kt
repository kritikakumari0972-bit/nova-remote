package com.novaremote.transport

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.novaremote.domain.IrRepeatMode
import com.novaremote.domain.RawIrCommand

data class BridgeInfo(
    val bridgeId: String,
    val chip: String,
    val firmware: String,
    val apiVersion: Int,
    val hasIrTransmitter: Boolean,
    val hasIrReceiver: Boolean
)
data class BridgeCapture(val ready: Boolean, val learning: Boolean, val timedOut: Boolean, val timingsMicros: List<Int>)
data class BridgeResult<T>(val value: T? = null, val error: String? = null) {
    val isSuccess: Boolean get() = error == null && value != null
}

class Esp32BridgeClient(private val baseAddress: String, private val bearerToken: String) {
    suspend fun getInfo(): BridgeResult<BridgeInfo> = withContext(Dispatchers.IO) {
        request("GET", "/api/v1/info", null) { json ->
            BridgeInfo(
                json.optString("bridgeId", "unknown"),
                json.optString("chip", "ESP32"),
                json.optString("firmware", "unknown"),
                json.optInt("apiVersion", 1),
                json.optBoolean("irTx", false),
                json.optBoolean("irRx", false)
            )
        }
    }

    suspend fun sendRaw(command: RawIrCommand): BridgeResult<Boolean> = withContext(Dispatchers.IO) {
        if (command.frequencyHz !in 30000..60000) return@withContext BridgeResult(error = "Carrier must be 30–60 kHz.")
        if (command.timingsMicros.size !in 2..512 ||
            command.timingsMicros.any { it !in 50..131070 } ||
            command.timingsMicros.sumOf { it.toLong() } > 2000000L) {
            return@withContext BridgeResult(error = "IR timings failed validation.")
        }
        if (command.repeatCount !in 0..10 || command.repeatIntervalMillis !in 20..1000) {
            return@withContext BridgeResult(error = "IR repeat data failed validation.")
        }
        val body = JSONObject()
            .put("frequencyHz", command.frequencyHz)
            .put("rawMicros", JSONArray(command.timingsMicros))
            .put("repeatCount", command.repeatCount)
            .put("repeatMode", command.repeatMode.name)
            .put("repeatIntervalMs", command.repeatIntervalMillis)
        request("POST", "/api/v1/ir/send", body) { it.optBoolean("sent", false) }
    }

    suspend fun startLearning(): BridgeResult<Boolean> = withContext(Dispatchers.IO) {
        request("POST", "/api/v1/ir/learn/start", JSONObject()) { it.optBoolean("learning", false) }
    }

    suspend fun getLatestCapture(): BridgeResult<BridgeCapture> = withContext(Dispatchers.IO) {
        request("GET", "/api/v1/ir/learn/latest", null) { json ->
            val values = json.optJSONArray("rawMicros") ?: JSONArray()
            if (values.length() > 512) throw IllegalArgumentException("Capture exceeds timing limit.")
            val timings = buildList {
                for (index in 0 until values.length()) {
                    val value = values.optLong(index, -1L)
                    if (value !in 50L..131070L) throw IllegalArgumentException("Invalid captured timing.")
                    add(value.toInt())
                }
            }
            BridgeCapture(
                json.optBoolean("ready", false),
                json.optBoolean("learning", false),
                json.optBoolean("timedOut", false),
                timings
            )
        }
    }

    suspend fun cancelLearning(): BridgeResult<Boolean> = withContext(Dispatchers.IO) {
        request("POST", "/api/v1/ir/learn/cancel", JSONObject()) { it.optBoolean("cancelled", false) }
    }

    private fun <T> request(
        method: String,
        path: String,
        payload: JSONObject?,
        decode: (JSONObject) -> T
    ): BridgeResult<T> {
        if (!bearerToken.matches(Regex("[A-Za-z0-9_-]{32,128}"))) {
            return BridgeResult(error = "Bridge token must be 32–128 valid characters.")
        }
        BridgeAddressPolicy.validate(baseAddress)?.let { return BridgeResult(error = it) }
        var connection: HttpURLConnection? = null
        return try {
            val active = (URL(baseAddress.trim().trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 3000
                readTimeout = 15000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer " + bearerToken)
                if (payload != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                }
            }
            connection = active
            val code = active.responseCode
            val stream = if (code in 200..299) active.inputStream else active.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { readBounded(it, 65536) }.orEmpty()
            if (code !in 200..299) {
                BridgeResult(error = "Bridge returned HTTP " + code + ": " + body.take(240))
            } else {
                val json = JSONObject(body)
                if (!json.optBoolean("ok", false)) BridgeResult(error = json.optString("error", "Bridge rejected request."))
                else BridgeResult(value = decode(json))
            }
        } catch (exception: Exception) {
            BridgeResult(error = exception.message ?: exception.javaClass.simpleName)
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
            if (output.length + count > maxChars) throw java.io.IOException("Bridge response is too large.")
            output.append(buffer, 0, count)
        }
        return output.toString()
    }
}

internal object BridgeAddressPolicy {
    fun validate(rawAddress: String): String? {
        val uri = try {
            URI(rawAddress.trim())
        } catch (_: Exception) {
            return "Enter a valid local bridge address."
        }
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return "Bridge address must use HTTP or HTTPS."
        }
        if (uri.port !in -1..65535 || uri.port == 0 || uri.userInfo != null ||
            uri.query != null || uri.fragment != null || uri.path !in listOf("", "/")) {
            return "Enter only the bridge origin (scheme, local host and optional port)."
        }
        val host = uri.host.lowercase()
        if (host == "localhost" || host.endsWith(".local")) return null
        val ip = host.split('.')
        if (ip.size != 4) return "Only private IPv4, localhost or .local hosts are accepted."
        val parts = ip.map { it.toIntOrNull() ?: return "Invalid IPv4 address." }
        if (parts.any { it !in 0..255 }) return "Invalid IPv4 address."
        val private = parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31) ||
            (parts[0] == 169 && parts[1] == 254) ||
            (parts[0] == 127 && parts[1] == 0 && parts[2] == 0 && parts[3] == 1)
        return if (private) null else "Only private IPv4, localhost or .local hosts are accepted."
    }
}
