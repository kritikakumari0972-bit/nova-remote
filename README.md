# NovaRemote

NovaRemote is an Android universal remote app built around real device transports, model-scoped profiles and explicit hardware capability checks.

Current milestone: 0.4.0 — Smart Network Appliances.

## Features

- Android built-in IR emitter detection and transmission.
- ESP32 IR bridge discovery over mDNS, manual private-IP fallback and authenticated local API.
- One-shot IR capture and reusable local remote profiles.
- Model-independent NEC and Sony SIRC 12/15/20 bit timing codecs.
- Protocol-specific repeat handling: NEC repeat frames and full-frame Sony repeats.
- JSON profile import/export through the Android document picker.
- Search saved profiles by brand, model, category or protocol; export and delete profiles.
- Android Keystore AES-GCM encryption for stored ESP32 and Home Assistant credentials.
- Home Assistant local REST API connection, authenticated health check and media-player entity listing.
- Power, playback and volume actions sent only to the selected Home Assistant media_player entity.
- Private-network address validation, redirect blocking, bounded responses and reconnect/refresh controls.
- Android and ESP32 GitHub Actions workflows.

## Smart network appliances

NovaRemote 0.4.0 connects to a user's local Home Assistant instance. The TV or media device must already be configured in Home Assistant through an integration supported by that instance. NovaRemote calls Home Assistant's documented REST API; it is not a direct vendor integration and does not claim every model or action is supported. Available controls depend on the media_player entity and the Home Assistant integration.

See docs/SMART_HOME.md for setup, token handling and limitations.

## Honest compatibility

An IR protocol decoder identifies a signal format; it does not prove that a particular television, set-top box, projector or air conditioner is compatible. The brand catalogue is a research index, not a verified support matrix. Newly captured and imported profiles remain EXPERIMENTAL. Exact-model verification requires a physical test and recorded evidence.

A phone can transmit traditional IR only if Android reports a built-in IR emitter. Phones without one need the optional ESP32 bridge. A demodulating receiver does not measure carrier frequency; unrecognised RAW captures ask the user to enter the carrier. AC stateful encoders and exact-model IR libraries remain future work.

## Android build

Open in Android Studio with JDK 17+ and Android SDK 36. Tooling: AGP 9.5.0, Kotlin 2.4.21, Gradle 9.7.0, Compose BOM 2026.06.01 and Activity Compose 1.13.0. CI runs assembleDebug and testDebugUnitTest.

## ESP32 build

Open bridge/esp32 in PlatformIO. Copy include/secrets.example.h to include/secrets.h, add local Wi-Fi credentials and a random token of at least 32 characters, then build and flash. Never commit secrets.h. The bridge advertises _novaremote._tcp; manual private-IP entry remains available if multicast discovery fails.

## Security

The ESP32 bridge uses bearer-token-authenticated plain HTTP on the local LAN. Home Assistant may also use plain HTTP on the LAN unless HTTPS is configured. Neither should be port-forwarded or exposed publicly. Both credentials are encrypted at rest with Android Keystore, but LAN observers can still see tokens sent over HTTP. Use HTTPS where available and keep services on a trusted home network.

See SECURITY.md, docs/COMPATIBILITY.md, docs/PROTOCOLS.md, docs/SMART_HOME.md and docs/ROADMAP.md.
