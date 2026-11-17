package com.ishant.securepreferences.android

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ishant.securepreferences.internal.AeadCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM using a non-exportable Android Keystore key.
 * The key never enters common Kotlin memory.
 *
 * Stored ciphertext layout: 12-byte IV || ciphertext || 16-byte GCM tag.
 */
internal class AndroidAesGcmCipher(
    private val alias: String,
) : AeadCipher {

    override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(associatedData)
            val encrypted = cipher.doFinal(plaintext)
            val iv = cipher.iv
            require(iv.size == IV_LENGTH) { "Android Keystore returned an invalid GCM IV" }
            iv + encrypted
        }

    override suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            require(ciphertext.size >= (IV_LENGTH + TAG_LENGTH_BYTES)) {
                "Encrypted preference snapshot is malformed"
            }
            val iv = ciphertext.copyOfRange(0, IV_LENGTH)
            val encrypted = ciphertext.copyOfRange(IV_LENGTH, ciphertext.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.updateAAD(associatedData)
            cipher.doFinal(encrypted)
        }

    private fun getOrCreateKey(): java.security.Key = synchronized(KEY_LOCK) {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return@synchronized it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8
        val KEY_LOCK = Any()
    }
}
