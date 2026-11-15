# Smart home connection — Home Assistant

NovaRemote 0.4.0 can use a local Home Assistant server as a network-control gateway for TVs and media players. The device must first be added and configured in Home Assistant using an integration that supports it. NovaRemote does not implement every vendor protocol and does not pair directly with each TV.

## Requirements

- A running Home Assistant instance reachable from the same trusted Wi-Fi/LAN as the Android phone.
- A TV or media device configured in Home Assistant and exposed as a media_player entity.
- A long-lived access token created from the user's Home Assistant profile.

Home Assistant serves its REST API on the same port as its frontend, 8123 by default. If the installation uses another port or HTTPS reverse proxy, enter that local server origin in NovaRemote.

## Connect

1. Open the Home Assistant user profile and its Security section.
2. Create a long-lived access token. Treat it like a password; do not share it or paste it into an issue.
3. In NovaRemote, enter a local URL such as http://192.168.1.20:8123 or http://homeassistant.local:8123.
4. Paste the token and tap Connect & load TVs.
5. NovaRemote calls GET /api/ to verify the API, then GET /api/states and lists the media_player entities visible to that account.
6. Select a listed entity and use its power, playback and volume buttons. Home Assistant returns an error if that integration or device does not implement a requested action.

The connection controls allow the user to refresh the entity list, disconnect locally, or forget the saved token. Reconnection is user-initiated. The app does not continuously poll Home Assistant in the background.

## What is supported

The initial adapter supports:
- Listing visible media_player entities and showing basic state/friendly-name metadata.
- Calling the media_player turn_on, turn_off, media_play_pause, volume_down, volume_up and volume_mute services.
- Validating media-player entity IDs and selecting service names from a fixed allowlist.

Actual support varies by integration and physical device. A Home Assistant entity may be unavailable, expose only a subset of actions, or represent a receiver/streaming player rather than a TV. A successful HTTP response confirms that Home Assistant accepted the service call; it does not prove the TV physically changed state.

## Security and privacy

- The token is encrypted at rest with AES-GCM using Android Keystore.
- NovaRemote accepts private IPv4, localhost and .local server origins only. Public internet hosts, URL credentials, paths, query strings and fragments are rejected.
- HTTP redirects are disabled so the bearer token is not forwarded to a different host.
- Prefer HTTPS when available. With plain HTTP, the bearer token and commands are visible to devices able to observe traffic on the LAN.
- Do not port-forward Home Assistant or expose its API to the public internet for NovaRemote.
- Create a separate non-administrator Home Assistant user for this purpose where practical, and revoke the token from Home Assistant if the phone is lost or the token is no longer needed.

## Troubleshooting

- Connection failed: confirm the phone and Home Assistant are on the same network, the server is running, and the entered IP/hostname and port are correct.
- HTTP 401: create a new token and paste it without spaces or line breaks.
- HTTP 403: the Home Assistant account lacks permission to read or control the requested entity.
- No media players listed: configure the TV/device integration in Home Assistant, then refresh.
- A button returns an error: that device integration may not support the service. Check the entity's available actions in Home Assistant.

## API reference

This adapter follows Home Assistant's documented REST endpoints: GET /api/, GET /api/states, and POST /api/services/<domain>/<service>. See the [Home Assistant REST API documentation](https://developers.home-assistant.io/docs/api/rest).
