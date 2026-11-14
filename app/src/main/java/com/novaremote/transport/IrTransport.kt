package com.novaremote.transport

import com.novaremote.domain.RawIrCommand
import com.novaremote.domain.TransportKind

sealed interface IrSendResult {
    data object Sent : IrSendResult
    data class Rejected(val reason: String) : IrSendResult
}
interface IrTransport {
    val kind: TransportKind
    suspend fun transmit(command: RawIrCommand): IrSendResult
}
