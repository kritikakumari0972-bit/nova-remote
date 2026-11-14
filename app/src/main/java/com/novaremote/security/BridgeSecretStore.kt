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

/** Encrypts bridge credentials at rest with an AES-GCM key held by Android Keystore. */
class BridgeSecretStore(context: Context) {
    private val preferences = context.getSharedPreferences("bridge_secrets", Context.MODE_PRIVATE)

    fun save(address: String, token: String) {
        require(token.matches(Regex("[A-Za-z0-9_-]{32,128}"))) { "Invalid token format." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString("encrypted_token", encoded).putString("bridge_address", address).commit())
    }

    fun loadAddress(): String = preferences.getString("bridge_address", "").orEmpty()

    fun loadToken(): String? {
        val encoded = preferences.getString("encrypted_token", null) ?: return null
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
        preferences.edit().clear().commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "novaremote.bridge.token.v1"
    }
}
