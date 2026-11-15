package com.novaremote.domain

/**
 * Model-independent timing codec for common pulse-distance IR formats.
 * A successful decode identifies a signal format, not an appliance model.
 */
data class DecodedIrFrame(
    val protocol: String,
    val bitCount: Int,
    val data: Long,
    val address: Int?,
    val command: Int?,
    val extendedAddress: Int?,
    val carrierFrequencyHz: Int,
    val repeatMode: IrRepeatMode,
    val recommendedRepeatCount: Int,
    val repeatIntervalMillis: Int
) {
    fun toRawCommand(
        id: String,
        label: String,
        timingsMicros: List<Int>,
        frequencyOverrideHz: Int? = null
    ): RawIrCommand = RawIrCommand(
        id = id,
        label = label,
        frequencyHz = frequencyOverrideHz ?: carrierFrequencyHz,
        timingsMicros = timingsMicros,
        repeatCount = recommendedRepeatCount,
        repeatMode = repeatMode,
        repeatIntervalMillis = repeatIntervalMillis
    )
}

object IrProtocolEngine {
    private const val NEC_HEADER_MARK = 9000
    private const val NEC_HEADER_SPACE = 4500
    private const val NEC_BIT_MARK = 560
    private const val NEC_ZERO_SPACE = 560
    private const val NEC_ONE_SPACE = 1690
    private const val NEC_FOOTER_MARK = 560
    private const val SONY_HEADER_MARK = 2400
    private const val SONY_HEADER_SPACE = 600
    private const val SONY_ZERO_MARK = 600
    private const val SONY_ONE_MARK = 1200
    private const val SONY_BIT_SPACE = 600

    fun encodeNec(address: Int, command: Int, id: String = "nec-command", label: String = "Command"): RawIrCommand {
        require(address in 0..255) { "Standard NEC address must be 0–255." }
        require(command in 0..255) { "NEC command must be 0–255." }
        val bytes = intArrayOf(address, address xor 0xFF, command, command xor 0xFF)
        val timings = mutableListOf(NEC_HEADER_MARK, NEC_HEADER_SPACE)
        bytes.forEach { value ->
            for (bit in 0 until 8) {
                timings += NEC_BIT_MARK
                timings += if ((value and (1 shl bit)) != 0) NEC_ONE_SPACE else NEC_ZERO_SPACE
            }
        }
        timings += NEC_FOOTER_MARK
        return RawIrCommand(id, label, 38000, timings, 2, IrRepeatMode.NEC_REPEAT_FRAME, 108)
    }

    fun encodeSony(
        command: Int,
        address: Int,
        bitCount: Int = 12,
        extendedAddress: Int = 0,
        id: String = "sony-command",
        label: String = "Command"
    ): RawIrCommand {
        require(bitCount == 12 || bitCount == 15 || bitCount == 20) { "Sony SIRC supports 12, 15 or 20 bits." }
        require(command in 0..0x7F) { "Sony SIRC command must be 7 bits." }
        val addressBits = if (bitCount == 15) 8 else 5
        require(address in 0 until (1 shl addressBits)) { "Sony SIRC address is out of range." }
        if (bitCount == 20) require(extendedAddress in 0..255) { "Sony SIRC extended address must be 0–255." }
        val data = command.toLong() or (address.toLong() shl 7) or
            (if (bitCount == 20) extendedAddress.toLong() shl 12 else 0L)
        val timings = mutableListOf(SONY_HEADER_MARK, SONY_HEADER_SPACE)
        for (bit in 0 until bitCount) {
            timings += if ((data and (1L shl bit)) != 0L) SONY_ONE_MARK else SONY_ZERO_MARK
            timings += SONY_BIT_SPACE
        }
        return RawIrCommand(id, label, 40000, timings, 2, IrRepeatMode.FULL_FRAME, 45)
    }

    fun encodeNecRepeatFrame(): List<Int> = listOf(9000, 2250, 560)

    fun decode(timingsMicros: List<Int>): DecodedIrFrame? {
        if (timingsMicros.size < 3 || timingsMicros.any { it !in 50..131070 }) return null
        return decodeNec(timingsMicros) ?: decodeSony(timingsMicros)
    }

    private fun decodeNec(values: List<Int>): DecodedIrFrame? {
        if (values.size in 3..4 && near(values[0], 9000) && near(values[1], 2250) && near(values[2], 560)) {
            return DecodedIrFrame(
                "NEC_REPEAT", 0, 0, null, null, null, 38000,
                IrRepeatMode.NEC_REPEAT_FRAME, 0, 108
            )
        }

        val hasTrailingGap = values.size == 68 && values.last() >= 5000
        if (values.size != 66 && values.size != 67 && !hasTrailingGap) return null
        if (!near(values[0], NEC_HEADER_MARK) || !near(values[1], NEC_HEADER_SPACE)) return null

        var data = 0L
        for (bit in 0 until 32) {
            val mark = values[2 + bit * 2]
            val space = values[3 + bit * 2]
            if (!near(mark, NEC_BIT_MARK)) return null
            val bitValue = when {
                near(space, NEC_ZERO_SPACE) -> 0
                near(space, NEC_ONE_SPACE) -> 1
                else -> return null
            }
            if (bitValue == 1) data = data or (1L shl bit)
        }
        if (values.size >= 67 && !near(values[66], NEC_FOOTER_MARK)) return null

        val addressLow = (data and 0xFF).toInt()
        val addressHigh = ((data ushr 8) and 0xFF).toInt()
        val command = ((data ushr 16) and 0xFF).toInt()
        val commandInverse = ((data ushr 24) and 0xFF).toInt()
        if ((command xor commandInverse) != 0xFF) return null

        val standardAddress = (addressLow xor addressHigh) == 0xFF
        val address = if (standardAddress) addressLow else addressLow or (addressHigh shl 8)
        return DecodedIrFrame(
            if (standardAddress) "NEC" else "NEC_EXTENDED",
            32,
            data,
            address,
            command,
            null,
            38000,
            IrRepeatMode.NEC_REPEAT_FRAME,
            2,
            108
        )
    }

    private fun decodeSony(values: List<Int>): DecodedIrFrame? {
        val bitCount = when (values.size) {
            26 -> 12
            32 -> 15
            42 -> 20
            27 -> if (values.last() >= 5000) 12 else return null
            33 -> if (values.last() >= 5000) 15 else return null
            43 -> if (values.last() >= 5000) 20 else return null
            else -> return null
        }
        val effectiveSize = 2 + bitCount * 2
        if (!near(values[0], SONY_HEADER_MARK) || !near(values[1], SONY_HEADER_SPACE)) return null
        var data = 0L
        for (bit in 0 until bitCount) {
            val mark = values[2 + bit * 2]
            val space = values[3 + bit * 2]
            val bitValue = when {
                near(mark, SONY_ZERO_MARK) -> 0
                near(mark, SONY_ONE_MARK) -> 1
                else -> return null
            }
            if (!near(space, SONY_BIT_SPACE)) return null
            if (bitValue == 1) data = data or (1L shl bit)
        }
        if (values.size > effectiveSize && values[effectiveSize] < 5000) return null

        val addressBits = if (bitCount == 15) 8 else 5
        val address = ((data ushr 7) and ((1L shl addressBits) - 1)).toInt()
        val extended = if (bitCount == 20) ((data ushr 12) and 0xFF).toInt() else null
        return DecodedIrFrame(
            "SONY_SIRC_$bitCount",
            bitCount,
            data,
            address,
            (data and 0x7F).toInt(),
            extended,
            40000,
            IrRepeatMode.FULL_FRAME,
            2,
            45
        )
    }

    private fun near(actual: Int, expected: Int): Boolean {
        return kotlin.math.abs(actual - expected).toDouble() <= expected * 0.28
    }
}
