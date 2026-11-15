package com.novaremote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IrProtocolEngineTest {
    @Test fun necEncodeDecodeRoundTrip() {
        val encoded = IrProtocolEngine.encodeNec(address = 0x12, command = 0x34)
        val decoded = IrProtocolEngine.decode(encoded.timingsMicros)
        assertNotNull(decoded)
        assertEquals("NEC", decoded!!.protocol)
        assertEquals(0x12, decoded.address)
        assertEquals(0x34, decoded.command)
        assertEquals(IrRepeatMode.NEC_REPEAT_FRAME, encoded.repeatMode)
    }

    @Test fun necRepeatMarkerIsRecognizedSeparately() {
        val decoded = IrProtocolEngine.decode(IrProtocolEngine.encodeNecRepeatFrame())
        assertNotNull(decoded)
        assertEquals("NEC_REPEAT", decoded!!.protocol)
        assertEquals(0, decoded.bitCount)
    }

    @Test fun sonySirc12RoundTrip() {
        val encoded = IrProtocolEngine.encodeSony(command = 0x45, address = 0x12, bitCount = 12)
        val decoded = IrProtocolEngine.decode(encoded.timingsMicros)
        assertEquals("SONY_SIRC_12", decoded?.protocol)
        assertEquals(0x45, decoded?.command)
        assertEquals(0x12, decoded?.address)
        assertEquals(45, encoded.repeatIntervalMillis)
    }

    @Test fun sonySirc15RoundTrip() {
        val encoded = IrProtocolEngine.encodeSony(command = 0x23, address = 0xA5, bitCount = 15)
        val decoded = IrProtocolEngine.decode(encoded.timingsMicros)
        assertEquals("SONY_SIRC_15", decoded?.protocol)
        assertEquals(0x23, decoded?.command)
        assertEquals(0xA5, decoded?.address)
    }

    @Test fun sonySirc20RoundTrip() {
        val encoded = IrProtocolEngine.encodeSony(command = 0x31, address = 0x0A, bitCount = 20, extendedAddress = 0xCD)
        val decoded = IrProtocolEngine.decode(encoded.timingsMicros)
        assertEquals("SONY_SIRC_20", decoded?.protocol)
        assertEquals(0x31, decoded?.command)
        assertEquals(0x0A, decoded?.address)
        assertEquals(0xCD, decoded?.extendedAddress)
    }

    @Test fun rejectsMalformedTimingFrame() {
        val encoded = IrProtocolEngine.encodeNec(0x12, 0x34)
        val malformed = encoded.timingsMicros.toMutableList().also { it[3] = 999 }
        assertNull(IrProtocolEngine.decode(malformed))
    }

    @Test fun validatesProtocolFieldRanges() {
        assertThrows(IllegalArgumentException::class.java) {
            IrProtocolEngine.encodeNec(256, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IrProtocolEngine.encodeSony(128, 1, 12)
        }
    }
}
