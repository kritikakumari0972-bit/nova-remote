# NovaRemote

Android universal remote app based on real device transports and exact-model profiles.

Current milestone: 0.2.0 — Universal IR Bridge & Remote Profile Engine.

Implemented in this milestone: built-in Android IR capability detection and transmission; ESP32 bridge discovery over mDNS with manual private-IP fallback; authenticated bridge information, send, learn and cancel APIs; one-shot IR capture; local versioned JSON profiles; replay via built-in IR or ESP32; Android Keystore AES-GCM token storage; bounded input validation and CI workflows.

The manufacturer catalogue is a research index, not a verified compatibility matrix. Profiles begin as EXPERIMENTAL. Verification requires physical testing on the exact model. Traditional IR requires an IR emitter in the phone or the optional ESP32 bridge. Wi-Fi and Bluetooth control will be added only for devices with documented and tested local APIs.

## Android build

Open in Android Studio with JDK 17+ and Android SDK 36. Tooling: AGP 9.5.0, Kotlin 2.4.21, Gradle 9.7.0, Compose BOM 2026.09.00, Activity Compose 1.13.0. CI runs assembleDebug and testDebugUnitTest.

## ESP32 build

Open bridge/esp32 in PlatformIO. Copy include/secrets.example.h to include/secrets.h, add local Wi-Fi credentials and a random token of at least 32 characters, then build and flash. Never commit secrets.h. The bridge advertises _novaremote._tcp; manual private-IP entry is available if multicast discovery fails.

## Security

The bridge uses bearer-token-authenticated plain HTTP on the local LAN. This is not end-to-end encryption. Keep it on a trusted home network; never port-forward or expose it publicly. The app encrypts the saved token at rest, but LAN observers can still see HTTP traffic.

See SECURITY.md, docs/COMPATIBILITY.md and docs/ROADMAP.md.
