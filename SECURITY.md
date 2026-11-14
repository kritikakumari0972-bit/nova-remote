# Security Policy

NovaRemote is early-development software. The ESP32 bridge uses a bearer token over plain HTTP on the local network. This provides request authentication, not transport confidentiality.

- Never expose or port-forward the bridge to the public internet.
- Use only on a trusted, password-protected LAN.
- Never commit Wi-Fi credentials, secrets.h, API tokens or private captures.
- App bridge origins are restricted to private IPv4, localhost or .local hosts; redirects are disabled before sending the token.
- Saved tokens are encrypted at rest with Android Keystore AES-GCM.
- Imported profiles are size-bounded and always downgraded to EXPERIMENTAL.

Not yet implemented: TLS/secure local transport, secure pairing, token rotation/revocation, replay protection, signed profile provenance and secure OTA.

Report vulnerabilities privately through GitHub's security reporting feature when enabled. Do not publish tokens, IPs or exploit details in public issues.
