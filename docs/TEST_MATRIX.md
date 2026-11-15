# Hardware and software test matrix

No appliance is currently marked VERIFIED_ON_HARDWARE by this repository.

| Test | Status | Evidence to record |
|---|---|---|
| Phone IR capability detection | Code implemented; physical test pending | Phone exact model, OS and carrier ranges |
| Phone IR transmission | Pending | Exact appliance model and observed response |
| NEC encode/decode | Unit tests added; CI result pending | Captured signal and physical send response |
| NEC repeat frame | Unit test added; CI result pending | Device behaviour during a held button |
| Sony SIRC 12/15/20 | Unit tests added; CI result pending | Captured signal and physical send response |
| ESP32 transmitter | Firmware source; CI previously passed 0.2.0; 0.3.0 rerun pending | Board, driver circuit and appliance response |
| ESP32 receiver learning | API source; board test pending | Capture quality from original remote |
| mDNS discovery | Code implemented; LAN test pending | AP/router, phone OS and discovered service |
| Profile import/export | Implemented; CI result pending | JSON round-trip and document-picker test |
| Profile save and replay | Implemented; end-to-end test pending | Saved profile and command-by-command results |
| AC state control | Not implemented | Exact model serializer and state-transition tests |
| Vendor Wi-Fi controls | Not implemented | Documented local API and pairing tests |

Passing CI is not proof of appliance compatibility.
