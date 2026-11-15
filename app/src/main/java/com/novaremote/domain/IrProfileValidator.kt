package com.novaremote.domain

object IrProfileValidator {
    const val MAX_PROFILE_BYTES = 512 * 1024
    const val MAX_COMMANDS = 128
    const val MAX_TIMINGS_PER_COMMAND = 512

    fun validate(profile: IrRemoteProfile): List<String> = buildList {
        if (!profile.id.matches(Regex("[A-Za-z0-9._-]{1,80}"))) add("Invalid profile id.")
        if (profile.brand.isBlank() || profile.brand.length > 64) add("Brand is required (max 64 characters).")
        if (profile.model.isBlank() || profile.model.length > 128) add("Model is required (max 128 characters).")
        if (profile.protocol.isBlank() || profile.protocol.length > 64) add("Protocol is required (max 64 characters).")
        if (profile.evidenceUrl != null && (profile.evidenceUrl.length > 512 || !profile.evidenceUrl.startsWith("https://"))) add("Evidence URL must use HTTPS.")
        if (profile.commands.isEmpty() || profile.commands.size > MAX_COMMANDS) add("Profile must contain 1–128 commands.")
        val ids = mutableSetOf<String>()
        profile.commands.forEachIndexed { index, command ->
            val label = "Command " + (index + 1)
            if (!command.id.matches(Regex("[A-Za-z0-9._-]{1,80}"))) add(label + " has an invalid id.")
            if (!ids.add(command.id)) add("Duplicate command id: " + command.id)
            if (command.label.isBlank() || command.label.length > 64) add(label + " needs a label (max 64 characters).")
            if (command.frequencyHz !in 30000..60000) add(label + " frequency must be 30–60 kHz.")
            if (command.timingsMicros.size !in 2..MAX_TIMINGS_PER_COMMAND) add(label + " must contain 2–512 timings.")
            if (command.timingsMicros.any { it !in 50..131070 }) add(label + " has an invalid timing.")
            if (command.timingsMicros.sumOf { it.toLong() } > 2000000L) add(label + " exceeds the two-second frame limit.")
            if (command.repeatCount !in 0..10) add(label + " repeat count must be 0–10.")
            if (command.repeatIntervalMillis !in 20..1000) add(label + " repeat interval must be 20–1000 ms.")
        }
    }
}
