package com.novaremote.domain

import org.json.JSONArray
import org.json.JSONObject

class ProfileFormatException(message: String) : IllegalArgumentException(message)

object IrProfileJsonCodec {
    const val SCHEMA_VERSION = 1

    fun encode(profile: IrRemoteProfile): String {
        val errors = IrProfileValidator.validate(profile)
        if (errors.isNotEmpty()) throw ProfileFormatException(errors.joinToString(" "))
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("id", profile.id)
            .put("brand", profile.brand)
            .put("model", profile.model)
            .put("category", profile.category.name)
            .put("protocol", profile.protocol)
            .put("verification", profile.verification.name)
        profile.evidenceUrl?.let { root.put("evidenceUrl", it) }
        val commands = JSONArray()
        profile.commands.forEach { command ->
            commands.put(JSONObject()
                .put("id", command.id)
                .put("label", command.label)
                .put("frequencyHz", command.frequencyHz)
                .put("timingsMicros", JSONArray(command.timingsMicros))
                .put("repeatCount", command.repeatCount))
        }
        return root.put("commands", commands).toString()
    }

    fun decode(json: String): IrRemoteProfile {
        if (json.toByteArray(Charsets.UTF_8).size > IrProfileValidator.MAX_PROFILE_BYTES) throw ProfileFormatException("Profile exceeds 512 KiB.")
        val root = try { JSONObject(json) } catch (_: Exception) { throw ProfileFormatException("Invalid JSON.") }
        if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION) throw ProfileFormatException("Unsupported schema version.")
        val category = runCatching { DeviceCategory.valueOf(root.getString("category")) }
            .getOrElse { throw ProfileFormatException("Unknown category.") }
        val array = root.optJSONArray("commands") ?: throw ProfileFormatException("Missing commands.")
        if (array.length() !in 1..IrProfileValidator.MAX_COMMANDS) throw ProfileFormatException("Invalid command count.")
        val commands = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: throw ProfileFormatException("Command is not an object.")
                val values = item.optJSONArray("timingsMicros") ?: throw ProfileFormatException("Missing timings.")
                if (values.length() !in 2..IrProfileValidator.MAX_TIMINGS_PER_COMMAND) throw ProfileFormatException("Invalid timing count.")
                val timings = buildList {
                    for (j in 0 until values.length()) {
                        val value = values.optLong(j, -1L)
                        if (value !in 50L..131070L) throw ProfileFormatException("Timing is out of range.")
                        add(value.toInt())
                    }
                }
                add(RawIrCommand(
                    item.optString("id"),
                    item.optString("label"),
                    item.optInt("frequencyHz", -1),
                    timings,
                    item.optInt("repeatCount", 0)
                ))
            }
        }
        val profile = IrRemoteProfile(
            id = root.optString("id"),
            brand = root.optString("brand"),
            model = root.optString("model"),
            category = category,
            protocol = root.optString("protocol"),
            // Imported data cannot self-assert hardware verification.
            verification = VerificationStatus.EXPERIMENTAL,
            evidenceUrl = root.optString("evidenceUrl").takeIf { it.isNotBlank() },
            commands = commands
        )
        val errors = IrProfileValidator.validate(profile)
        if (errors.isNotEmpty()) throw ProfileFormatException(errors.joinToString(" "))
        return profile
    }
}
