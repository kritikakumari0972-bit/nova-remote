# Roadmap

## 0.1.0 — Foundation
- [x] Android app shell and built-in IR hardware detection.
- [x] Raw IR command model and bridge firmware starter.
- [x] CI workflow starters and security/compatibility policies.

## 0.2.0 — Universal IR bridge and profile engine
- [x] ESP32 mDNS service advertisement and manual-IP fallback.
- [x] Android foreground discovery for _novaremote._tcp.
- [x] Authenticated bridge information, send, learn and cancel endpoints.
- [x] One-shot IR capture with listening, ready, timed-out and cancelled states.
- [x] Save captured raw commands as local versioned JSON profiles.
- [x] Replay stored commands through phone IR or ESP32 bridge.
- [x] Encrypt stored bridge token with Android Keystore AES-GCM.
- [x] Bound raw timing length, frame duration, request size and send rate.
- [x] ESP32 firmware compile passes on GitHub Actions.
- [x] Android build and unit-test workflow passes on GitHub Actions.
- [ ] Test discovery, learn, save and replay on physical hardware.

## 0.3.0 — Protocol engine and portable profiles
- [x] NEC standard/extended frame decoder and standard encoder.
- [x] NEC dedicated hold-repeat marker handling.
- [x] Sony SIRC 12-, 15- and 20-bit decoder/encoder.
- [x] Protocol-specific repeat mode and repeat interval metadata.
- [x] JSON profile import/export using Android document picker.
- [x] Search and filter saved profiles; export and delete profiles.
- [x] Import validation and verification-status downgrade.
- [x] Android unit tests and debug APK build pass on GitHub Actions.
- [x] ESP32 firmware build passes on GitHub Actions.
- [ ] Confirm protocol timing against physical captures and appliances.
- [ ] Exact-model TV/set-top-box/projector profiles with hardware test evidence.
- [ ] AC full-state serializers, state transitions and model-specific tests.
- [ ] Profile provenance and reviewed verification records.

## 0.4.0 — Smart network appliances
- [x] Implement a documented Home Assistant local REST API adapter.
- [x] Authenticate with a user-created long-lived access token and validate the API health endpoint.
- [x] List accessible media_player entities and expose bounded power, playback and volume actions.
- [x] Encrypt the Home Assistant token at rest using Android Keystore AES-GCM.
- [x] Add disconnect, refresh, reconnect and clear-credential controls.
- [x] Restrict server addresses to private/local origins and disable HTTP redirects.
- [x] Add unit tests for address policy, token validation, entity parsing and action mapping.
- [x] Document the Home Assistant setup and security model.
- [ ] Validate on a physical Android phone with a real Home Assistant server and at least one TV integration.
- [ ] Add direct vendor modules only where local API, pairing and authentication are documented and permitted.
- [ ] Bluetooth control only for devices with documented control services.

## 0.5.0 — Multi-room and automation
- [ ] Rooms, device groups and user-approved macros.
- [ ] Backup/restore.
- [ ] Accessibility, localization and large-screen layout.

## 1.0.0 — Release criteria
- [ ] Security review, secure pairing and protected local transport.
- [ ] Reproducible Android and firmware builds.
- [ ] Model-level compatibility matrix from physical tests.
- [ ] No unverified compatibility claims.
