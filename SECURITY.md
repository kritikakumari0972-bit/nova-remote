# Security Policy

NovaRemote is early-development software. The ESP32 bridge uses a bearer token over plain HTTP on the local network. The Home Assistant connector uses the user's long-lived access token; local Home Assistant deployments may also use plain HTTP unless HTTPS is configured. Authentication is not transport confidentiality.

- Never expose or port-forward the ESP32 bridge or Home Assistant API to the public internet.
- Use only on a trusted, password-protected LAN.
- Prefer HTTPS for Home Assistant where it is configured.
- Never commit Wi-Fi credentials, secrets.h, API tokens or private captures.
- App bridge and Home Assistant origins are restricted to private IPv4, localhost or .local hosts; redirects are disabled before sending bearer tokens.
- Saved ESP32 and Home Assistant tokens are encrypted at rest with separate Android Keystore AES-GCM keys.
- Home Assistant service calls use fixed media_player service names and validate entity IDs; arbitrary request paths are not exposed.
- Imported profiles are size-bounded and always downgraded to EXPERIMENTAL.

Not yet implemented: TLS/secure local transport for the ESP32 bridge, app-managed vendor pairing, token rotation/revocation, replay protection, signed profile provenance and secure OTA.

Report vulnerabilities privately through GitHub's security reporting feature when enabled. Do not publish tokens, IPs or exploit details in public issues.
