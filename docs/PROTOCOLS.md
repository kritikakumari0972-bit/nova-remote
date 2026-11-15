# IR protocol codecs

NovaRemote 0.3.0 adds model-independent timing codecs for two widely used formats:

- NEC: 32-bit LSB-first frame with a 9 ms / 4.5 ms header, 560 microsecond bit marks, 560 or 1690 microsecond bit spaces, and a final mark. The standard 8-bit address form is encoded; the decoder also recognizes 16-bit extended addresses when the command byte complement is valid.
- Sony SIRC: 12-, 15- and 20-bit variants, LSB-first, with a 2.4 ms / 600 microsecond header and 600/1200 microsecond data marks.

The decoder uses timing tolerances because receiver captures vary. It rejects frames outside supported lengths or invalid byte/timing structures. A NEC repeat marker is identified separately; it is not a standalone button command.

Repeat behaviour is part of each command profile:
- NEC sends its initial frame, then dedicated NEC repeat markers.
- Sony SIRC re-sends the complete frame at the profile interval.

The codecs identify a protocol family, not an exact appliance model or manufacturer. A compatible-looking frame is not proof a television, set-top box, projector or air conditioner will respond. AC stateful protocols are not included in this milestone.

Reference implementations and protocol API descriptions:
- IRremoteESP8266 IRsend declarations: https://github.com/crankyoldgit/IRremoteESP8266/blob/master/src/IRsend.h
- IRremoteESP8266 IRsend API reference: https://crankyoldgit.github.io/IRremoteESP8266/doxygen/html/classIRsend.html
