# Hardware test matrix

No appliance is currently marked verified by this repository.

| Test | Status | Evidence to record |
|---|---|---|
| Phone IR capability detection | Code implemented; physical test pending | Phone exact model, OS and carrier ranges |
| Phone IR transmission | Pending | Exact appliance model and observed response |
| ESP32 transmitter | Firmware source; board test pending | Board, driver circuit and appliance response |
| ESP32 receiver learning | API source; board test pending | Capture quality from original remote |
| mDNS discovery | Code implemented; LAN test pending | AP/router, phone OS and discovered service |
| Profile save and replay | Code implemented; end-to-end test pending | Saved profile and command-by-command results |
| AC state control | Not implemented | Exact model serializer and state-transition tests |
| Vendor Wi-Fi controls | Not implemented | Documented local API and pairing tests |

Passing CI is not proof of appliance compatibility.
