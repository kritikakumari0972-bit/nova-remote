package com.novaremote.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrProfileValidatorTest {
    private fun valid() = IrRemoteProfile(
        "sample-tv", "Example", "Model X", DeviceCategory.TELEVISION, "RAW", VerificationStatus.EXPERIMENTAL,
        commands = listOf(RawIrCommand("power", "Power", 38000, listOf(9000, 4500, 560, 560)))
    )

    @Test fun acceptsValidProfile() {
        assertTrue(IrProfileValidator.validate(valid()).isEmpty())
    }

    @Test fun rejectsBadFrequency() {
        val profile = valid().copy(commands = listOf(valid().commands.first().copy(frequencyHz = 100000)))
        assertFalse(IrProfileValidator.validate(profile).isEmpty())
    }

    @Test fun rejectsDuplicateIds() {
        val command = valid().commands.first()
        assertTrue(IrProfileValidator.validate(valid().copy(commands = listOf(command, command.copy(label = "Duplicate"))))
            .any { it.contains("Duplicate command id") })
    }

    @Test fun rejectsOversizedFrame() {
        val profile = valid().copy(commands = listOf(valid().commands.first().copy(timingsMicros = List(21) { 100000 })))
        assertTrue(IrProfileValidator.validate(profile).any { it.contains("two-second") })
    }
}
