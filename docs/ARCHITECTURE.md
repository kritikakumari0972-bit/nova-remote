# Architecture

Compose UI
  -> domain models and profile validation
  -> local profile store / transport interface
  -> Android Consumer IR or authenticated ESP32 IR bridge
  -> physical IR appliance

Compose UI
  -> Home Assistant local API client
  -> private Home Assistant server
  -> user's configured Home Assistant integration
  -> supported TV/media-player device

The brand catalogue is descriptive metadata only. A usable IR remote comes from an exact-model profile or a vendor integration with a documented local protocol. The Home Assistant path is an integration-mediated path: device pairing and vendor protocol handling are performed by Home Assistant integrations, not by NovaRemote.

## IR profile lifecycle

1. Original remote emits an IR frame.
2. ESP32 receiver captures mark/space intervals.
3. Bridge returns bounded raw microsecond timings.
4. App validates timings and stores schema-versioned JSON.
5. User selects a saved command and a transport.
6. A new profile starts EXPERIMENTAL; importing cannot promote it to hardware-verified.

Raw learning does not identify a manufacturer protocol and does not create an AC state serializer. Air conditioners commonly transmit a complete state; dedicated state encoders belong in the model-specific protocol layer.

## Bridge discovery

ESP32 advertises _novaremote._tcp using Arduino-ESP32 ESPmDNS. Android NsdManager discovers/resolves the service. Manual private-IP entry remains available because multicast discovery may be blocked by guest Wi-Fi, client isolation or routers.

## Home Assistant connection

The app validates a private/local server origin, checks GET /api/, reads GET /api/states, and filters for media_player.* entities. User actions call the fixed POST /api/services/media_player/<service> routes with a validated entity ID and an allowlisted service. It does not send arbitrary URLs or user-supplied service paths.

The Home Assistant access token is encrypted with AES-GCM using a separate Android Keystore key alias. Reconnection is explicit; users can refresh entities, disconnect or erase the saved credentials. There is no background polling.

## Security boundary

App accepts private IPv4, localhost and .local origins for its local adapters, and disables HTTP redirects before sending bearer tokens. Tokens are encrypted at rest using Android Keystore. The ESP32 bridge and a default local Home Assistant installation may use plain HTTP; a capable LAN observer could see credentials and commands. Use trusted networks and HTTPS where available. Never port-forward either service.

The app targets API 36. Before targeting API 37+, local-network access must be updated to comply with Android's local-network permission model or a system NSD picker.
