package com.novaremote.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores a Home Assistant URL and an AES-GCM encrypted long-lived access token. */
class HomeAssistantSecretStore(context: Context) {
    private val preferences = context.getSharedPreferences("home_assistant_secrets", Context.MODE_PRIVATE)

    fun save(address: String, token: String) {
        require(address.startsWith("http://") || address.startsWith("https://")) {
            "Enter a valid Home Assistant address."
        }
        require(token.matches(Regex("[A-Za-z0-9._~+/=-]{16,4096}"))) {
            "Invalid Home Assistant access token."
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(
            preferences.edit()
                .putString("server_address", address.trim().trimEnd('/'))
                .putString("encrypted_access_token", encoded)
                .commit()
        )
    }

    fun loadAddress(): String = preferences.getString("server_address", "").orEmpty()

    fun loadToken(): String? {
        val encoded = preferences.getString("encrypted_access_token", null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size > 28)
            val iv = bytes.copyOfRange(0, 12)
            val encrypted = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        check(preferences.edit().clear().commit()) { "Could not clear saved Home Assistant credentials." }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "novaremote.homeassistant.token.v1"
    }
}
