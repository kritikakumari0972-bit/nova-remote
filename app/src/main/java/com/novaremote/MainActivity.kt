package com.novaremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.novaremote.data.BrandCatalog
import com.novaremote.domain.*
import com.novaremote.security.BridgeSecretStore
import com.novaremote.transport.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val irTransport = AndroidIrTransport(applicationContext)
        val profileStore = IrProfileStore(applicationContext)
        val secretStore = BridgeSecretStore(applicationContext)
        val discovery = Esp32BridgeDiscovery(applicationContext)

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var address by remember { mutableStateOf(secretStore.loadAddress()) }
                var token by remember { mutableStateOf(secretStore.loadToken().orEmpty()) }
                var connected by remember { mutableStateOf(false) }
                var status by remember { mutableStateOf("Bridge not connected") }
                var details by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var scanning by remember { mutableStateOf(false) }
                var learning by remember { mutableStateOf(false) }
                var capture by remember { mutableStateOf<List<Int>>(emptyList()) }
                var profiles by remember { mutableStateOf(profileStore.loadAll()) }
                var brand by remember { mutableStateOf("Unknown") }
                var model by remember { mutableStateOf("") }
                var commandLabel by remember { mutableStateOf("Power") }
                var frequency by remember { mutableStateOf("38000") }
                var searchQuery by remember { mutableStateOf("") }
                var category by remember { mutableStateOf(DeviceCategory.TELEVISION) }
                var categoryMenu by remember { mutableStateOf(false) }
                var candidates by remember { mutableStateOf<List<BridgeCandidate>>(emptyList()) }
                var profileToExport by remember { mutableStateOf<IrRemoteProfile?>(null) }
                val phoneIr = remember { irTransport.status() }
                val decodedCapture = remember(capture) { IrProtocolEngine.decode(capture) }
                val visibleProfiles = profiles.filter {
                    it.brand.contains(searchQuery, true) ||
                        it.model.contains(searchQuery, true) ||
                        it.protocol.contains(searchQuery, true) ||
                        it.category.name.contains(searchQuery.replace(' ', '_'), true)
                }

                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    val profile = profileToExport
                    if (uri != null && profile != null) {
                        runCatching {
                            val stream = contentResolver.openOutputStream(uri) ?: error("Could not open the destination file.")
                            stream.bufferedWriter(Charsets.UTF_8).use { it.write(IrProfileJsonCodec.encode(profile)) }
                        }.onSuccess {
                            status = "Exported " + profile.id + ".json"
                        }.onFailure {
                            status = "Export failed: " + it.message
                        }
                    }
                    profileToExport = null
                }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            val stream = contentResolver.openInputStream(uri) ?: error("Could not open the selected profile.")
                            val imported = stream.bufferedReader(Charsets.UTF_8).use {
                                IrProfileJsonCodec.decode(it.readText())
                            }
                            val stored = if (profileStore.find(imported.id) == null) {
                                imported
                            } else {
                                imported.copy(id = (imported.id + "-imported-" + System.currentTimeMillis().toString(36)).take(80))
                            }
                            profileStore.save(stored)
                            profiles = profileStore.loadAll()
                            stored
                        }.onSuccess {
                            status = "Imported as experimental profile " + it.id
                        }.onFailure {
                            status = "Import failed: " + it.message
                        }
                    }
                }

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("NovaRemote", style = MaterialTheme.typography.headlineLarge)
                    Text("Universal remote • model-specific profiles • real hardware transports")

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Phone infrared", style = MaterialTheme.typography.titleMedium)
                            Text(if (phoneIr.hasEmitter) "IR emitter detected" else "No built-in IR emitter")
                            Text(
                                if (phoneIr.hasEmitter) "Carrier ranges: " + phoneIr.supportedFrequencyRangesHz.joinToString {
                                    it.first.toString() + "–" + it.last + " Hz"
                                } else "Use an ESP32 NovaRemote bridge for traditional infrared appliances.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ESP32 IR bridge", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(enabled = !scanning && !busy && !learning, onClick = {
                                scanning = true
                                scope.launch {
                                    status = "Searching for NovaRemote bridges…"
                                    candidates = runCatching { discovery.discover() }.getOrElse {
                                        status = it.message ?: "Discovery failed."
                                        emptyList()
                                    }
                                    status = if (candidates.isEmpty()) {
                                        "No bridge discovered. Use manual private-IP entry."
                                    } else {
                                        "Found " + candidates.size + " bridge(s). Select one below."
                                    }
                                    scanning = false
                                }
                            }) { Text(if (scanning) "Searching…" else "Find bridge on Wi-Fi") }

                            candidates.forEach { candidate ->
                                OutlinedButton(onClick = {
                                    address = candidate.address
                                    connected = false
                                    status = "Selected " + candidate.name + ". Test connection to continue."
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(candidate.name + " • " + candidate.address)
                                }
                            }
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it; connected = false },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Bridge address (private IP or discovered host)") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = token,
                                onValueChange = { token = it; connected = false },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Bridge access token") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = !busy && !learning && !scanning, onClick = {
                                    busy = true
                                    status = "Testing bridge…"
                                    scope.launch {
                                        val result = Esp32BridgeClient(address, token).getInfo()
                                        busy = false
                                        if (result.isSuccess) {
                                            val info = result.value!!
                                            connected = true
                                            details = info.bridgeId + " • " + info.chip + " • FW " + info.firmware +
                                                " • API " + info.apiVersion + " • TX " + info.hasIrTransmitter +
                                                " • RX " + info.hasIrReceiver
                                            status = "Connected"
                                            runCatching { secretStore.save(address, token) }
                                                .onFailure { status = "Connected; encrypted token save failed: " + it.message }
                                        } else {
                                            connected = false
                                            status = "Connection failed: " + result.error
                                        }
                                    }
                                }) { Text(if (busy) "Testing…" else "Test & save") }

                                OutlinedButton(enabled = !busy && !learning, onClick = {
                                    secretStore.clear()
                                    token = ""
                                    connected = false
                                    status = "Saved bridge token cleared."
                                }) { Text("Forget token") }
                            }
                            Text(status, style = MaterialTheme.typography.bodyMedium)
                            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Bridge HTTP is authenticated but not encrypted in transit. Use only on a trusted home LAN; never port-forward it.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Learn an original remote button", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Aim the original remote at the ESP32 receiver and press a button once. NovaRemote attempts NEC and Sony SIRC decoding; other captures remain RAW. A protocol match is not proof of exact-model compatibility.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(enabled = connected && !busy && !learning, onClick = {
                                learning = true
                                capture = emptyList()
                                scope.launch {
                                    val client = Esp32BridgeClient(address, token)
                                    val start = client.startLearning()
                                    if (!start.isSuccess || start.value != true) {
                                        status = start.error ?: "Could not start learning."
                                    } else {
                                        status = "Listening for up to 20 seconds…"
                                        var finished = false
                                        repeat(44) {
                                            if (!finished) {
                                                delay(500)
                                                val result = client.getLatestCapture()
                                                when {
                                                    !result.isSuccess -> {
                                                        status = result.error ?: "Capture failed."
                                                        finished = true
                                                    }
                                                    result.value!!.ready -> {
                                                        capture = result.value!!.timingsMicros
                                                        val decoded = IrProtocolEngine.decode(capture)
                                                        status = if (decoded == null) {
                                                            "Captured " + capture.size + " timings. Protocol not recognised; save as RAW."
                                                        } else {
                                                            "Detected " + decoded.protocol + " (" + decoded.bitCount + " bits). Verify the exact appliance before relying on it."
                                                        }
                                                        finished = true
                                                    }
                                                    result.value!!.timedOut -> {
                                                        status = "Learning timed out. Check receiver alignment and wiring."
                                                        finished = true
                                                    }
                                                }
                                            }
                                        }
                                        if (!finished) {
                                            client.cancelLearning()
                                            status = "No capture received before app timeout."
                                        }
                                    }
                                    learning = false
                                }
                            }) {
                                if (learning) CircularProgressIndicator() else Text("Start learning")
                            }
                            if (learning) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        Esp32BridgeClient(address, token).cancelLearning()
                                        learning = false
                                        status = "Learning cancelled."
                                    }
                                }) { Text("Cancel learning") }
                            }

                            if (capture.isNotEmpty()) {
                                if (decodedCapture?.protocol == "NEC_REPEAT") {
                                    Text(
                                        "This is only a NEC hold-repeat marker. Learn the button's initial full frame instead.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else if (decodedCapture != null) {
                                    Text(
                                        "Recognised " + decodedCapture.protocol + ". Nominal carrier: " +
                                            decodedCapture.carrierFrequencyHz + " Hz. This is not exact-model verification.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedTextField(brand, { brand = it }, Modifier.fillMaxWidth(), label = { Text("Brand") }, singleLine = true)
                                OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Exact model (or leave unspecified)") }, singleLine = true)
                                OutlinedTextField(commandLabel, { commandLabel = it }, Modifier.fillMaxWidth(), label = { Text("Button label") }, singleLine = true)
                                OutlinedTextField(
                                    frequency,
                                    { frequency = it.filter(Char::isDigit).take(6) },
                                    Modifier.fillMaxWidth(),
                                    label = { Text("Carrier frequency for RAW captures (Hz)") },
                                    singleLine = true
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box {
                                        OutlinedButton(onClick = { categoryMenu = true }) {
                                            Text(category.name.replace('_', ' '))
                                        }
                                        DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                            DeviceCategory.entries.forEach { item ->
                                                DropdownMenuItem(
                                                    text = { Text(item.name.replace('_', ' ')) },
                                                    onClick = { category = item; categoryMenu = false }
                                                )
                                            }
                                        }
                                    }
                                    Button(enabled = decodedCapture?.protocol != "NEC_REPEAT", onClick = {
                                        val carrierInput = frequency.toIntOrNull()
                                        if ((decodedCapture == null && (carrierInput == null || carrierInput !in 30000..60000)) || commandLabel.isBlank()) {
                                            status = "Enter a button label and a RAW carrier between 30000 and 60000 Hz."
                                        } else {
                                            val profileId = slug(brand) + "-" + slug(model.ifBlank { "unspecified-model" })
                                            val commandId = slug(commandLabel)
                                            val oldProfile = profileStore.find(profileId)
                                            val protocol = decodedCapture?.protocol ?: "RAW"
                                            val command = decodedCapture?.let { frame ->
                                                frame.toRawCommand(commandId, commandLabel.trim(), capture, frame.carrierFrequencyHz)
                                            } ?: RawIrCommand(commandId, commandLabel.trim(), carrierInput!!, capture)
                                            val newProfile = IrRemoteProfile(
                                                id = profileId,
                                                brand = brand.ifBlank { "Unknown" },
                                                model = model.ifBlank { "Unspecified" },
                                                category = category,
                                                protocol = protocol,
                                                verification = VerificationStatus.EXPERIMENTAL,
                                                commands = oldProfile?.commands.orEmpty().filterNot { it.id == commandId } + command
                                            )
                                            runCatching {
                                                profileStore.save(newProfile)
                                                profiles = profileStore.loadAll()
                                                capture = emptyList()
                                                status = "Saved " + protocol + " command in experimental profile " + profileId
                                            }.onFailure {
                                                status = "Profile save failed: " + it.message
                                            }
                                        }
                                    }) { Text("Save button") }
                                }
                            }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Remote profiles", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) }) {
                                    Text("Import JSON")
                                }
                                Text(profiles.size.toString() + " saved", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search brand, model, category or protocol") },
                                singleLine = true
                            )
                            if (visibleProfiles.isEmpty()) Text("No matching profiles. Learn a remote or import a JSON profile.")
                            visibleProfiles.forEach { profile ->
                                Text(profile.brand + " • " + profile.model, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    profile.category.name.replace('_', ' ') + " • " + profile.protocol +
                                        " • " + profile.commands.size + " commands • " + profile.verification.name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        profileToExport = profile
                                        exportLauncher.launch(profile.id + ".json")
                                    }) { Text("Export") }
                                    OutlinedButton(onClick = {
                                        if (profileStore.delete(profile.id)) {
                                            profiles = profileStore.loadAll()
                                            status = "Deleted profile " + profile.id
                                        } else {
                                            status = "Could not delete profile."
                                        }
                                    }) { Text("Delete") }
                                }
                                profile.commands.forEach { command ->
                                    OutlinedButton(enabled = !busy && !learning, onClick = {
                                        busy = true
                                        scope.launch {
                                            val transport: IrTransport = if (connected) {
                                                Esp32IrTransport(Esp32BridgeClient(address, token))
                                            } else {
                                                irTransport
                                            }
                                            status = when (val sent = transport.transmit(command)) {
                                                IrSendResult.Sent -> "Sent " + command.label + " via " + transport.kind
                                                is IrSendResult.Rejected -> "Not sent: " + sent.reason
                                            }
                                            busy = false
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Send " + command.label)
                                    }
                                }
                            }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Brand research index", style = MaterialTheme.typography.titleMedium)
                            Text(BrandCatalog.targets.size.toString() + " manufacturers indexed. Listing is not proof of model support.")
                            Text(BrandCatalog.targets.joinToString(" • ") { it.name }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("NovaRemote 0.3.0 • Protocol match is not a hardware-verified model profile.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    private fun slug(input: String): String =
        input.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48).ifBlank { "custom" }
}
