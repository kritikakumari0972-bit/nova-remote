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
- [ ] Android CI passes on GitHub Actions.
- [ ] Test discovery, learn, save and replay on physical hardware.

## 0.3.0 — Protocol engine and portable profiles
- [x] NEC standard/extended frame decoder and standard encoder.
- [x] NEC dedicated hold-repeat marker handling.
- [x] Sony SIRC 12-, 15- and 20-bit decoder/encoder.
- [x] Protocol-specific repeat mode and repeat interval metadata.
- [x] JSON profile import/export using Android document picker.
- [x] Search and filter saved profiles; export and delete profiles.
- [x] Import validation and verification-status downgrade.
- [ ] Android unit tests and debug APK build pass.
- [ ] Confirm protocol timing against physical captures and appliances.
- [ ] Exact-model TV/set-top-box/projector profiles with hardware test evidence.
- [ ] AC full-state serializers, state transitions and model-specific tests.
- [ ] Profile provenance and reviewed verification records.

## 0.4.0 — Smart network appliances
- [ ] Research documented local APIs for selected TV models.
- [ ] Add vendor modules only where pairing/authentication and local control are documented.
- [ ] Connection state, reconnection and offline handling.
- [ ] Investigate Bluetooth only for devices with documented control services.

## 0.5.0 — Multi-room and automation
- [ ] Rooms, device groups and user-approved macros.
- [ ] Backup/restore.
- [ ] Accessibility, localization and large-screen layout.

## 1.0.0 — Release criteria
- [ ] Security review, secure pairing and protected local transport.
- [ ] Reproducible Android and firmware builds.
- [ ] Model-level compatibility matrix from physical tests.
- [ ] No unverified compatibility claims.
