# Architecture

Compose UI
  -> domain models and profile validation
  -> local profile store / transport interface
  -> Android Consumer IR or authenticated ESP32 bridge
  -> physical appliance

The brand catalogue is descriptive metadata only. A usable remote comes from an exact-model profile or a vendor integration with a documented local protocol.

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

## Security boundary

App accepts private IPv4, localhost and .local bridge origins, and disables HTTP redirects before sending bearer tokens. Tokens are encrypted at rest using Android Keystore. Bridge API currently uses plain HTTP on the LAN; a capable LAN observer may see credentials and commands. Development only; never expose outside a trusted network.

The app targets API 36. Before targeting API 37+, local-network access must be updated to comply with Android's local-network permission model or a system NSD picker.
