# NovaRemote

NovaRemote is an Android universal remote app built around real device transports, model-scoped profiles and explicit hardware capability checks.

Current milestone: 0.3.0 — Protocol Engine & Portable Remote Profiles.

## 0.3.0 features

- Android built-in IR emitter detection and transmission.
- ESP32 IR bridge discovery over mDNS, manual private-IP fallback and authenticated local API.
- One-shot IR capture and reusable local remote profiles.
- Model-independent NEC and Sony SIRC 12/15/20 bit timing codecs.
- Protocol-specific repeat handling: NEC repeat frames and full-frame Sony repeats.
- JSON profile import/export through the Android document picker.
- Search saved profiles by brand, model, category or protocol; export and delete profiles.
- Android Keystore AES-GCM encryption for the stored bridge token.
- Bounded input validation and Android/ESP32 GitHub Actions workflows.

## Honest compatibility

A protocol decoder identifies a signal format; it does not prove that a particular television, set-top box, projector or air conditioner is compatible. The brand catalogue is a research index, not a verified support matrix. Newly captured and imported profiles remain EXPERIMENTAL. Exact-model verification requires a physical test and recorded evidence.

A phone can transmit traditional IR only if Android reports a built-in IR emitter. Phones without one need the optional ESP32 bridge. A demodulating receiver does not measure carrier frequency; unrecognised RAW captures ask the user to enter the carrier. AC stateful encoders and exact-model libraries remain future work.

## Android build

Open in Android Studio with JDK 17+ and Android SDK 36. Tooling: AGP 9.5.0, Kotlin 2.4.21, Gradle 9.7.0, Compose BOM 2026.07.00 and Activity Compose 1.13.0. CI runs assembleDebug and testDebugUnitTest.

## ESP32 build

Open bridge/esp32 in PlatformIO. Copy include/secrets.example.h to include/secrets.h, add local Wi-Fi credentials and a random token of at least 32 characters, then build and flash. Never commit secrets.h. The bridge advertises _novaremote._tcp; manual private-IP entry remains available if multicast discovery fails.

## Security

The bridge currently uses bearer-token-authenticated plain HTTP on the local LAN. This is not end-to-end encryption. Keep it on a trusted home network; never port-forward or expose it publicly. The app encrypts the saved token at rest, but LAN observers can still see HTTP traffic.

See SECURITY.md, docs/COMPATIBILITY.md, docs/PROTOCOLS.md and docs/ROADMAP.md.
