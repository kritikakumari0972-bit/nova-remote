package com.novaremote.domain

enum class DeviceCategory {
    AIR_CONDITIONER, TELEVISION, SET_TOP_BOX, PROJECTOR, AUDIO, FAN, LIGHTING, SMART_PLUG, OTHER_SMART_HOME
}
enum class TransportKind { BUILT_IN_IR, ESP32_IR_BRIDGE, WIFI_VENDOR_API, BLUETOOTH }
enum class VerificationStatus { EXPERIMENTAL, PROTOCOL_COMPATIBLE, VERIFIED_ON_HARDWARE }

data class ApplianceBrand(val name: String, val categories: Set<DeviceCategory>)
data class RawIrCommand(
    val id: String,
    val label: String,
    val frequencyHz: Int,
    val timingsMicros: List<Int>,
    val repeatCount: Int = 0
)
data class IrRemoteProfile(
    val id: String,
    val brand: String,
    val model: String,
    val category: DeviceCategory,
    val protocol: String,
    val verification: VerificationStatus,
    val evidenceUrl: String? = null,
    val commands: List<RawIrCommand>
)
