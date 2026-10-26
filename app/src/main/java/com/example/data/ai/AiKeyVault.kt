package com.example.data.ai

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the provider API key at rest with a hardware-backed Android Keystore
 * key (AES-256-GCM). The ciphertext is useless on another device because the
 * Keystore key never leaves this one.
 *
 * Unlike the base85 "obfuscation" in the original Python engine, this is real
 * authenticated encryption: a tampered payload fails to decrypt instead of
 * silently producing garbage.
 */
open class AiKeyVault(private val alias: String = DEFAULT_ALIAS) {

    open fun encrypt(plaintext: String): String? = runCatching {
        if (plaintext.isBlank()) return@runCatching null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivPart = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataPart = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        "$PAYLOAD_VERSION:$ivPart:$dataPart"
    }.getOrNull()

    open fun decrypt(payload: String?): String? = runCatching {
        if (payload.isNullOrBlank()) return@runCatching null
        val parts = payload.split(":")
        if (parts.size != 3 || parts[0] != PAYLOAD_VERSION) return@runCatching null
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private const val DEFAULT_ALIAS = "scrapmaster_ai_key_v1"
        private const val PAYLOAD_VERSION = "v1"
    }
}
