# Hardware and software test matrix

No appliance is currently marked VERIFIED_ON_HARDWARE by this repository.

| Test | Status | Evidence to record |
|---|---|---|
| Phone IR capability detection | Code implemented; physical test pending | Phone exact model, OS and carrier ranges |
| Phone IR transmission | Pending | Exact appliance model and observed response |
| NEC encode/decode | Unit tests added; Android CI passes | Captured signal and physical send response |
| NEC repeat frame | Unit test added; Android CI passes | Device behaviour during a held button |
| Sony SIRC 12/15/20 | Unit tests added; Android CI passes | Captured signal and physical send response |
| ESP32 transmitter | Firmware source; firmware CI passes; board test pending | Board, driver circuit and appliance response |
| ESP32 receiver learning | API source; board test pending | Capture quality from original remote |
| ESP32 mDNS discovery | Code implemented; LAN test pending | AP/router, phone OS and discovered service |
| Profile import/export | Implemented; Android CI passes | JSON round-trip and document-picker test |
| Profile save and replay | Implemented; end-to-end test pending | Saved profile and command-by-command results |
| Home Assistant server validation | Unit tests added; Android CI pending for this change | Local server, valid/invalid token outcomes |
| Home Assistant media-player parsing | Unit tests added; Android CI pending for this change | Example entity state response |
| Home Assistant service mapping | Unit tests added; Android CI pending for this change | Device-specific supported actions |
| Home Assistant TV control | Not yet hardware-tested | Phone, Home Assistant version, integration, TV model and action results |
| AC state control | Not implemented | Exact model serializer and state-transition tests |
| Direct vendor Wi-Fi controls | Not implemented | Documented local API and pairing tests |

Passing CI is not proof of appliance compatibility. The Home Assistant connector is an API client; successful authentication does not prove a specific TV integration supports every button.
