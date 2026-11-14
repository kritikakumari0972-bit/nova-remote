# NovaRemote ESP32 IR bridge 0.2.0

## Hardware

- ESP32 development board supported by PlatformIO esp32dev.
- Demodulating IR receiver connected to GPIO 27 by default.
- IR LED driver/transistor circuit connected to GPIO 4 by default.
- Suitable board supply and common ground for the driver circuit.

Do not connect a high-current IR LED directly to a GPIO. Confirm pin assignments and electrical limits for the exact board and circuit.

## Configure and flash

1. Open this folder in PlatformIO.
2. Copy include/secrets.example.h to include/secrets.h.
3. Set local Wi-Fi SSID/password and a random API token with at least 32 valid characters.
4. Build and flash the board.
5. Open Serial Monitor at 115200 baud. Note the private IP and mDNS hostname.
6. Connect the phone to the same trusted Wi-Fi and use Find bridge or enter the private IP manually.
7. Enter the token and test the connection.

Never commit secrets.h.

## API

All endpoints require Authorization: Bearer TOKEN.

- GET /api/v1/info
- POST /api/v1/ir/send
- POST /api/v1/ir/learn/start
- GET /api/v1/ir/learn/latest
- POST /api/v1/ir/learn/cancel

The bridge advertises DNS-SD service _novaremote._tcp on port 80. Discovery can fail on guest networks or APs with multicast/client isolation; manual private-IP entry remains available.

## Limits

- Raw frame: 2–512 timings, each 50–131070 microseconds.
- Total frame duration: at most 2 seconds.
- Carrier: 30–60 kHz.
- Learning timeout: 20 seconds.
- Send interval: at least 120 ms between accepted frames.

## Security

The bearer-token API uses plain HTTP on the trusted local LAN. It is not encrypted in transit. Never port-forward it, expose it publicly or use it on a network you do not trust. Secure pairing and encrypted local transport are release blockers.
