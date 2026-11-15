# IR profile JSON schema version 1

Profiles are UTF-8 JSON documents with schemaVersion = 1. Each profile stores an ID, brand, model, category, protocol label, verification status and a bounded commands array.

Each command stores:
- id and label.
- frequencyHz (30,000–60,000).
- timingsMicros (2–512 alternating mark/space values, each 50–131,070 microseconds; total frame at most two seconds).
- repeatCount (0–10).
- repeatMode: FULL_FRAME or NEC_REPEAT_FRAME.
- repeatIntervalMillis (20–1,000 ms).

The app limits each profile to 512 KiB and 128 commands. Old schema-v1 commands without repeat metadata default to full-frame repeats, zero repeats and a 100 ms interval.

Import always downgrades the claimed verification status to EXPERIMENTAL. The source file cannot self-certify that a device has been physically tested. Imported content is validated before it is saved.
