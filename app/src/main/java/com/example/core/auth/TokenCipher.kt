package com.example.core.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the session tokens before they are written to disk.
 * Kept as an interface so unit tests can use a fake (the Android Keystore is not available on the JVM).
 */
interface TokenCipher {
    fun encrypt(plain: String): String

    /** Returns null when the value cannot be decrypted (wrong key, corrupted, not produced by this cipher). */
    fun decrypt(encoded: String): String?
}

/**
 * AES-256-GCM with a non-exportable key held in the Android Keystore.
 *
 * Stored format: "v1:" + Base64(12-byte IV || ciphertext+tag).
 * If the key is lost (for example after the app data is restored onto another phone) the token simply
 * fails to decrypt and the agent is asked to sign in again. There is deliberately NO plain-text fallback.
 */
class KeystoreTokenCipher(private val alias: String = DEFAULT_ALIAS) : TokenCipher {

    companion object {
        const val DEFAULT_ALIAS = "traceharvest_session_key"
        private const val PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFIX = "v1:"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }

    private val lock = Any()

    private fun secretKey(): SecretKey {
        synchronized(lock) {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            generator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            return generator.generateKey()
        }
    }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    override fun decrypt(encoded: String): String? {
        if (!encoded.startsWith(PREFIX)) return null
        return try {
            val raw = Base64.decode(encoded.removePrefix(PREFIX), Base64.NO_WRAP)
            if (raw.size <= IV_BYTES) return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES))
            String(cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
