package com.novaremote.domain

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class IrProfileStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "ir-profiles")

    @Synchronized
    fun loadAll(): List<IrRemoteProfile> {
        if (!directory.exists()) return emptyList()
        return directory.listFiles { file -> file.isFile && file.extension.equals("json", true) }
            .orEmpty().sortedBy { it.name.lowercase() }.mapNotNull { file ->
                runCatching {
                    if (file.length() > IrProfileValidator.MAX_PROFILE_BYTES) null
                    else IrProfileJsonCodec.decode(file.readText(Charsets.UTF_8))
                }.getOrNull()
            }
    }

    @Synchronized
    fun find(id: String): IrRemoteProfile? = loadAll().firstOrNull { it.id == id }

    @Synchronized
    fun save(profile: IrRemoteProfile) {
        val errors = IrProfileValidator.validate(profile)
        require(errors.isEmpty()) { errors.joinToString(" ") }
        require(directory.exists() || directory.mkdirs()) { "Could not create profile storage." }
        val target = File(directory, profile.id + ".json")
        val temporary = File(directory, profile.id + ".json.tmp")
        val bytes = IrProfileJsonCodec.encode(profile).toByteArray(Charsets.UTF_8)
        require(bytes.size <= IrProfileValidator.MAX_PROFILE_BYTES)
        FileOutputStream(temporary).use { it.write(bytes); it.fd.sync() }
        if (target.exists() && !target.delete()) { temporary.delete(); error("Could not replace profile.") }
        if (!temporary.renameTo(target)) { temporary.delete(); error("Could not finalize profile save.") }
    }

    @Synchronized
    fun delete(id: String): Boolean {
        if (!id.matches(Regex("[A-Za-z0-9._-]{1,80}"))) return false
        val target = File(directory, id + ".json")
        return target.isFile && target.delete()
    }
}
