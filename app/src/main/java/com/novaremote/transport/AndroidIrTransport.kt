package com.novaremote.transport

import android.content.Context
import android.hardware.ConsumerIrManager
import com.novaremote.domain.RawIrCommand
import com.novaremote.domain.TransportKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class IrHardwareStatus(val hasEmitter: Boolean, val supportedFrequencyRangesHz: List<IntRange>)

class AndroidIrTransport(context: Context) : IrTransport {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    override val kind = TransportKind.BUILT_IN_IR

    fun status(): IrHardwareStatus {
        val ir = manager ?: return IrHardwareStatus(false, emptyList())
        if (!ir.hasIrEmitter()) return IrHardwareStatus(false, emptyList())
        val ranges = runCatching {
            ir.carrierFrequencies?.map { it.minFrequency..it.maxFrequency }.orEmpty()
        }.getOrDefault(emptyList())
        return IrHardwareStatus(true, ranges)
    }

    override suspend fun transmit(command: RawIrCommand): IrSendResult = withContext(Dispatchers.IO) {
        val ir = manager ?: return@withContext IrSendResult.Rejected("This phone does not expose an IR emitter.")
        if (!ir.hasIrEmitter()) return@withContext IrSendResult.Rejected("No built-in IR emitter is available.")
        if (status().supportedFrequencyRangesHz.none { command.frequencyHz in it }) {
            return@withContext IrSendResult.Rejected("Phone does not report support for " + command.frequencyHz + " Hz.")
        }
        if (command.timingsMicros.size !in 2..1024 ||
            command.timingsMicros.any { it !in 50..131070 } ||
            command.timingsMicros.sumOf { it.toLong() } > 2000000L ||
            command.repeatCount !in 0..10) {
            return@withContext IrSendResult.Rejected("IR timing data failed validation.")
        }
        try {
            repeat(command.repeatCount + 1) { index ->
                ir.transmit(command.frequencyHz, command.timingsMicros.toIntArray())
                if (index < command.repeatCount) delay(100)
            }
            IrSendResult.Sent
        } catch (exception: Exception) {
            IrSendResult.Rejected(exception.message ?: "Android IR transmission failed.")
        }
    }
}
