package com.novaremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.novaremote.security.HomeAssistantSecretStore
import com.novaremote.transport.HomeAssistantClient
import com.novaremote.transport.HomeAssistantMediaAction
import com.novaremote.transport.HomeAssistantMediaPlayer
import kotlinx.coroutines.launch

@Composable
internal fun HomeAssistantSection(secretStore: HomeAssistantSecretStore) {
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf(secretStore.loadAddress().ifBlank { "http://homeassistant.local:8123" }) }
    var token by remember { mutableStateOf(secretStore.loadToken().orEmpty()) }
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Not connected. Add your local Home Assistant address and access token.") }
    var players by remember { mutableStateOf<List<HomeAssistantMediaPlayer>>(emptyList()) }

    fun refreshPlayers() {
        scope.launch {
            busy = true
            val result = HomeAssistantClient(address, token).getMediaPlayers()
            busy = false
            if (result.isSuccess) {
                players = result.value.orEmpty()
                connected = true
                status = "Connected • " + players.size + " media-player entities found."
            } else {
                connected = false
                status = result.error ?: "Could not refresh Home Assistant."
            }
        }
    }

    fun sendAction(entityId: String, action: HomeAssistantMediaAction) {
        scope.launch {
            busy = true
            val result = HomeAssistantClient(address, token).sendMediaPlayerAction(entityId, action)
            busy = false
            status = if (result.isSuccess) {
                action.label + " sent to " + entityId + "."
            } else {
                result.error ?: "Home Assistant rejected the command."
            }
            if (!result.isSuccess && (result.error.orEmpty().contains("HTTP 401") ||
                    result.error.orEmpty().contains("Could not reach"))) {
                connected = false
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Smart home • Home Assistant", style = MaterialTheme.typography.titleMedium)
            Text(
                "Connect to your own local Home Assistant server. NovaRemote lists its media_player entities and sends supported TV/media actions through the integrations you have configured.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    connected = false
                    players = emptyList()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Local server URL (for example http://192.168.1.20:8123)") },
                singleLine = true
            )
            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it
                    connected = false
                    players = emptyList()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Home Assistant long-lived access token") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Text(
                "Create the token in your Home Assistant user profile under Security. It is stored encrypted with Android Keystore on this phone.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !busy && address.isNotBlank() && token.isNotBlank(),
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Authenticating with Home Assistant…"
                            val client = HomeAssistantClient(address, token)
                            val health = client.testConnection()
                            if (!health.isSuccess) {
                                connected = false
                                players = emptyList()
                                status = health.error ?: "Could not connect."
                            } else {
                                val result = client.getMediaPlayers()
                                if (result.isSuccess) {
                                    players = result.value.orEmpty()
                                    connected = true
                                    status = "Connected • " + players.size + " media-player entities found."
                                    runCatching { secretStore.save(address, token) }
                                        .onFailure { status = "Connected, but credentials could not be saved: " + it.message }
                                } else {
                                    connected = false
                                    players = emptyList()
                                    status = result.error ?: "Could not load entities."
                                }
                            }
                            busy = false
                        }
                    }
                ) {
                    Text(if (busy) "Connecting…" else "Connect & load TVs")
                }
                OutlinedButton(
                    enabled = !busy && connected,
                    onClick = {
                        connected = false
                        players = emptyList()
                        status = "Disconnected from Home Assistant."
                    }
                ) { Text("Disconnect") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy && connected, onClick = { refreshPlayers() }) {
                    Text("Refresh devices")
                }
                OutlinedButton(enabled = !busy, onClick = {
                    runCatching { secretStore.clear() }
                    token = ""
                    connected = false
                    players = emptyList()
                    status = "Saved Home Assistant token forgotten."
                }) { Text("Forget token") }
            }
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (busy) CircularProgressIndicator()
            if (connected && players.isEmpty()) {
                Text("No media_player entities are visible to this Home Assistant account.", style = MaterialTheme.typography.bodySmall)
            }
            players.forEach { player ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(player.friendlyName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            player.entityId + " • " + player.state +
                                (player.model?.let { " • " + it } ?: ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                        player.mediaTitle?.let {
                            Text("Playing: " + it, style = MaterialTheme.typography.bodySmall)
                        }
                        val firstRow = listOf(
                            HomeAssistantMediaAction.POWER_ON,
                            HomeAssistantMediaAction.POWER_OFF,
                            HomeAssistantMediaAction.PLAY_PAUSE
                        )
                        val secondRow = listOf(
                            HomeAssistantMediaAction.VOLUME_DOWN,
                            HomeAssistantMediaAction.VOLUME_UP,
                            HomeAssistantMediaAction.MUTE
                        )
                        listOf(firstRow, secondRow).forEach { rowActions ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowActions.forEach { action ->
                                    OutlinedButton(
                                        enabled = connected && !busy && player.isAvailable,
                                        onClick = { sendAction(player.entityId, action) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(action.label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                "Home Assistant controls only the devices and actions exposed by its configured integrations. Use HTTPS where available; plain HTTP exposes the token to other devices on the LAN.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
