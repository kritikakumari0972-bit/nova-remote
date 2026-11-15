# Compatibility policy

| Status | Meaning |
|---|---|
| Not implemented | No usable integration is present. |
| Experimental | Captured/imported profile awaiting validation. |
| Protocol compatible | Protocol family and command structure are understood; exact model test may be pending. |
| Verified on hardware | Exact appliance model and commands were physically tested with a recorded procedure. |

The brand index is not a support matrix. Product families and regional models may use different IR protocols, command frames, pairing and state layouts.

## Protocol codecs

The protocol engine currently recognises NEC standard/extended frames and Sony SIRC 12-, 15- and 20-bit frames. It also distinguishes the short NEC hold-repeat marker from a full initial command frame.

These codecs identify signal formats only. They do not map arbitrary command values to brand-specific functions, discover model codes, or certify an appliance. A detected NEC or SIRC frame remains EXPERIMENTAL until its exact appliance model and action are physically tested.

## Infrared realities

- Phone IR works only when Android reports a built-in emitter.
- A phone without an emitter needs the external ESP32 bridge.
- A raw capture can be replayed but is not necessarily decoded.
- AC units often transmit complete state (power, mode, temperature, fan, swing and flags), not independent key codes. Learning one or two buttons does not produce a full AC remote.
- A demodulating receiver does not measure carrier frequency; unrecognised captures require user-supplied carrier frequency.
- NEC has a distinct short repeat marker; Sony SIRC repeats the whole frame. Repeat handling is part of the command profile.

## Network realities

Wi-Fi control requires a documented supported local API and any vendor pairing. Bluetooth hardware on both devices does not imply a generic remote-control service.
