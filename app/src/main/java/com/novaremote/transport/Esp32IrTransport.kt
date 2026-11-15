package com.novaremote.transport

import com.novaremote.domain.RawIrCommand
import com.novaremote.domain.TransportKind

class Esp32IrTransport(private val client: Esp32BridgeClient) : IrTransport {
    override val kind = TransportKind.ESP32_IR_BRIDGE

    override suspend fun transmit(command: RawIrCommand): IrSendResult {
        val result = client.sendRaw(command)
        return when {
            !result.isSuccess -> IrSendResult.Rejected(result.error ?: "Bridge transmission failed.")
            result.value != true -> IrSendResult.Rejected("Bridge did not confirm transmission.")
            else -> IrSendResult.Sent
        }
    }
}
