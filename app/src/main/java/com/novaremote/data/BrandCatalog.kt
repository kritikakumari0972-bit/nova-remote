package com.novaremote.data

import com.novaremote.domain.ApplianceBrand
import com.novaremote.domain.DeviceCategory

/** Research index only; this is not a claim of model-level compatibility. */
object BrandCatalog {
    val targets = listOf(
        ApplianceBrand("Acer", setOf(DeviceCategory.TELEVISION, DeviceCategory.PROJECTOR)),
        ApplianceBrand("Airtel", setOf(DeviceCategory.SET_TOP_BOX)),
        ApplianceBrand("BenQ", setOf(DeviceCategory.PROJECTOR)),
        ApplianceBrand("Blue Star", setOf(DeviceCategory.AIR_CONDITIONER)),
        ApplianceBrand("Bose", setOf(DeviceCategory.AUDIO)),
        ApplianceBrand("Carrier", setOf(DeviceCategory.AIR_CONDITIONER)),
        ApplianceBrand("Daikin", setOf(DeviceCategory.AIR_CONDITIONER)),
        ApplianceBrand("Dish TV", setOf(DeviceCategory.SET_TOP_BOX)),
        ApplianceBrand("d2h", setOf(DeviceCategory.SET_TOP_BOX)),
        ApplianceBrand("Epson", setOf(DeviceCategory.PROJECTOR)),
        ApplianceBrand("Godrej", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION)),
        ApplianceBrand("Haier", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION)),
        ApplianceBrand("Hisense", setOf(DeviceCategory.TELEVISION, DeviceCategory.AUDIO)),
        ApplianceBrand("Hitachi", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION)),
        ApplianceBrand("JBL", setOf(DeviceCategory.AUDIO)),
        ApplianceBrand("Jio", setOf(DeviceCategory.SET_TOP_BOX)),
        ApplianceBrand("LG", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION, DeviceCategory.AUDIO)),
        ApplianceBrand("Lloyd", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION)),
        ApplianceBrand("Mitsubishi Electric", setOf(DeviceCategory.AIR_CONDITIONER)),
        ApplianceBrand("Optoma", setOf(DeviceCategory.PROJECTOR)),
        ApplianceBrand("OnePlus", setOf(DeviceCategory.TELEVISION)),
        ApplianceBrand("Panasonic", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION, DeviceCategory.AUDIO)),
        ApplianceBrand("Philips", setOf(DeviceCategory.TELEVISION, DeviceCategory.AUDIO, DeviceCategory.LIGHTING)),
        ApplianceBrand("Roku", setOf(DeviceCategory.TELEVISION, DeviceCategory.SET_TOP_BOX)),
        ApplianceBrand("Samsung", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION, DeviceCategory.AUDIO)),
        ApplianceBrand("Sony", setOf(DeviceCategory.TELEVISION, DeviceCategory.PROJECTOR, DeviceCategory.AUDIO)),
        ApplianceBrand("Tata Play", setOf(DeviceCategory.SET_TOP_BOX)),
        ApplianceBrand("TCL", setOf(DeviceCategory.TELEVISION, DeviceCategory.AUDIO)),
        ApplianceBrand("TP-Link", setOf(DeviceCategory.SMART_PLUG, DeviceCategory.OTHER_SMART_HOME)),
        ApplianceBrand("Voltas", setOf(DeviceCategory.AIR_CONDITIONER)),
        ApplianceBrand("Whirlpool", setOf(DeviceCategory.AIR_CONDITIONER)),
        ApplianceBrand("Xiaomi", setOf(DeviceCategory.AIR_CONDITIONER, DeviceCategory.TELEVISION, DeviceCategory.LIGHTING, DeviceCategory.SMART_PLUG))
    ).sortedBy { it.name.lowercase() }

    fun search(query: String) =
        if (query.isBlank()) targets else targets.filter { it.name.contains(query.trim(), ignoreCase = true) }
}
