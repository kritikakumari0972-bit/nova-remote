package com.novaremote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IrProfileJsonCodecTest {
    private fun sample(status: VerificationStatus = VerificationStatus.EXPERIMENTAL) = IrRemoteProfile(
        "sample-model", "Sample", "Model 1", DeviceCategory.TELEVISION, "NEC", status,
        commands = listOf(RawIrCommand(
            "power", "Power", 38000, listOf(9000, 4500, 560, 560),
            repeatCount = 2,
            repeatMode = IrRepeatMode.NEC_REPEAT_FRAME,
            repeatIntervalMillis = 108
        ))
    )

    @Test fun roundTripPreservesProtocolRepeatMetadata() {
        assertEquals(sample(), IrProfileJsonCodec.decode(IrProfileJsonCodec.encode(sample())))
    }

    @Test fun importerDoesNotTrustVerifiedFlag() {
        val imported = IrProfileJsonCodec.decode(IrProfileJsonCodec.encode(sample(VerificationStatus.VERIFIED_ON_HARDWARE)))
        assertEquals(VerificationStatus.EXPERIMENTAL, imported.verification)
    }

    @Test fun rejectsUnsupportedSchema() {
        assertThrows(ProfileFormatException::class.java) {
            IrProfileJsonCodec.decode("""{"schemaVersion":99}""")
        }
    }
}
