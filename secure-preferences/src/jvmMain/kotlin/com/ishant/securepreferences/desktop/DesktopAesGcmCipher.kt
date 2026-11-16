package com.ishant.securepreferences.desktop

import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import com.ishant.securepreferences.internal.AeadCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM/Desktop AES-256-GCM backed by the operating system credential store.
 *
 * The keyring stores only the 256-bit master key. It is never written to the
 * encrypted snapshot directory. A file lock serializes first-time key creation
 * across processes belonging to this application.
 */
internal class DesktopAesGcmCipher(
    private val configName: String,
    private val keyAlias: String,
    private val lockFile: Path,
) : AeadCipher {
    private val keyring: Keyring = try {
        Keyring.create()
    } catch (cause: Throwable) {
        throw IllegalStateException(
            "No supported OS credential store is available. Configure macOS Keychain, " +
                "Windows Credential Manager, or Linux Secret Service before using Desktop SecurePreferences.",
            cause,
        )
    }

    @Volatile
    private var cachedKey: SecretKeySpec? = null

    override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            val nonce = ByteArray(GCM_NONCE_BYTES).also(random::nextBytes)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(associatedData)
            nonce + cipher.doFinal(plaintext)
        }

    override suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            require(ciphertext.size >= GCM_NONCE_BYTES + GCM_TAG_BYTES) {
                "Encrypted preference snapshot is malformed"
            }
            val nonce = ciphertext.copyOfRange(0, GCM_NONCE_BYTES)
            val encrypted = ciphertext.copyOfRange(GCM_NONCE_BYTES, ciphertext.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(associatedData)
            cipher.doFinal(encrypted)
        }

    private fun getOrCreateKey(): SecretKeySpec {
        cachedKey?.let { return it }
        synchronized(KEY_LOCK) {
            cachedKey?.let { return it }
            Files.createDirectories(lockFile.parent)
            FileChannel.open(
                lockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            ).use { channel ->
                channel.lock().use {
                    val stored = try {
                        keyring.getPassword(SERVICE_PREFIX + configName, keyAlias)
                    } catch (_: PasswordAccessException) {
                        null
                    }
                    val encoded = stored ?: createAndStoreKey()
                    val raw = try {
                        Base64.getDecoder().decode(encoded)
                    } catch (cause: IllegalArgumentException) {
                        throw IllegalStateException("The Desktop master key in the OS credential store is malformed", cause)
                    }
                    try {
                        require(raw.size == AES_256_KEY_BYTES) {
                            "The Desktop master key in the OS credential store has an invalid length"
                        }
                        return SecretKeySpec(raw.copyOf(), AES_ALGORITHM).also { cachedKey = it }
                    } finally {
                        raw.fill(0)
                    }
                }
            }
        }
    }

    private fun createAndStoreKey(): String {
        val raw = ByteArray(AES_256_KEY_BYTES).also(random::nextBytes)
        return try {
            val encoded = Base64.getEncoder().encodeToString(raw)
            keyring.setPassword(SERVICE_PREFIX + configName, keyAlias, encoded)
            // Re-read to ensure the credential store accepted and retained the value.
            keyring.getPassword(SERVICE_PREFIX + configName, keyAlias)
                ?: throw IllegalStateException("OS credential store did not return the stored master key")
        } catch (cause: Exception) {
            throw IllegalStateException("Unable to create or persist the Desktop master key in the OS credential store", cause)
        } finally {
            raw.fill(0)
        }
    }

    private companion object {
        const val SERVICE_PREFIX = "com.ishant.securepreferences."
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_ALGORITHM = "AES"
        const val AES_256_KEY_BYTES = 32
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        val random = SecureRandom()
        val KEY_LOCK = Any()
    }
}
