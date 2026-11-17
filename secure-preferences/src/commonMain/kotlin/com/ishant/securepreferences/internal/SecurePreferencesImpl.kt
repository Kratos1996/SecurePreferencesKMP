package com.ishant.securepreferences.internal

import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Common implementation encrypting the complete preference snapshot.
 *
 * Encrypting the complete map prevents preference names and keys from leaking
 * through the underlying platform preference database. A write replaces the
 * snapshot atomically. This trades write amplification for a small and simple
 * encrypted storage format suitable for preferences (not large files).
 */
class SecurePreferencesImpl internal constructor(
    private val config: SecurePreferencesConfig,
    private val cipher: AeadCipher,
    private val storage: EncryptedSnapshotStorage,
    private val json: Json = Json { encodeDefaults = true; ignoreUnknownKeys = false },
) : SecurePreferences {

    private val mutex = Mutex()

    override suspend fun putString(key: String, value: String) =
        put(key, Entry(Type.STRING, value))

    override suspend fun getString(key: String, defaultValue: String?): String? =
        get(key, Type.STRING)?.value ?: defaultValue

    override suspend fun putInt(key: String, value: Int) = put(key, Entry(Type.INT, value.toString()))
    override suspend fun getInt(key: String, defaultValue: Int): Int =
        get(key, Type.INT)?.value?.toIntOrNull() ?: defaultValue

    override suspend fun putLong(key: String, value: Long) = put(key, Entry(Type.LONG, value.toString()))
    override suspend fun getLong(key: String, defaultValue: Long): Long =
        get(key, Type.LONG)?.value?.toLongOrNull() ?: defaultValue

    override suspend fun putFloat(key: String, value: Float) {
        require(value.isFinite()) { "Float preference must be finite" }
        put(key, Entry(Type.FLOAT, value.toString()))
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float =
        get(key, Type.FLOAT)?.value?.toFloatOrNull() ?: defaultValue

    override suspend fun putDouble(key: String, value: Double) {
        require(value.isFinite()) { "Double preference must be finite" }
        put(key, Entry(Type.DOUBLE, value.toString()))
    }

    override suspend fun getDouble(key: String, defaultValue: Double): Double =
        get(key, Type.DOUBLE)?.value?.toDoubleOrNull() ?: defaultValue

    override suspend fun putBoolean(key: String, value: Boolean) =
        put(key, Entry(Type.BOOLEAN, value.toString()))

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        get(key, Type.BOOLEAN)?.value?.toBooleanStrictOrNull() ?: defaultValue

    override suspend fun contains(key: String): Boolean = mutex.withLock {
        validateKey(key)
        readSnapshot().containsKey(key)
    }

    override suspend fun remove(key: String) = mutex.withLock {
        validateKey(key)
        val entries = readSnapshot().toMutableMap()
        if (entries.remove(key) != null) writeSnapshot(entries)
    }

    override suspend fun clear() = mutex.withLock { storage.delete() }

    override suspend fun getAllKeys(): Set<String> = mutex.withLock {
        readSnapshot().keys.toSet()
    }

    private suspend fun put(key: String, entry: Entry) = mutex.withLock {
        validateKey(key)
        val entries = readSnapshot().toMutableMap()
        entries[key] = entry
        writeSnapshot(entries)
    }

    private suspend fun get(key: String, expected: Type): Entry? = mutex.withLock {
        validateKey(key)
        val entry = readSnapshot()[key] ?: return@withLock null
        if (entry.type != expected) throw SecurePreferencesException.TypeMismatch
        entry
    }

    private suspend fun readSnapshot(): Map<String, Entry> {
        val stored = storage.read() ?: return emptyMap()
        val envelope = try {
            json.decodeFromString<EncryptedSnapshot>(stored)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Throwable) {
            throw SecurePreferencesException.CorruptValue(cause)
        }
        if (envelope.formatVersion != EncryptedSnapshot.CURRENT_FORMAT_VERSION) {
            throw SecurePreferencesException.CorruptValue(
                IllegalStateException("Unsupported preferences format: ${envelope.formatVersion}")
            )
        }
        val plaintext = try {
            cipher.decrypt(envelope.ciphertext, associatedData())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Throwable) {
            throw SecurePreferencesException.CorruptValue(cause)
        }
        return try {
            json.decodeFromString<Map<String, Entry>>(plaintext.decodeToString())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Throwable) {
            throw SecurePreferencesException.CorruptValue(cause)
        } finally {
            plaintext.fill(0)
        }
    }

    private suspend fun writeSnapshot(entries: Map<String, Entry>) {
        if (entries.isEmpty()) {
            storage.delete()
            return
        }
        val plaintext = json.encodeToString(entries).encodeToByteArray()
        try {
            val encrypted = cipher.encrypt(plaintext, associatedData())
            try {
                val envelope = EncryptedSnapshot(ciphertext = encrypted)
                storage.write(json.encodeToString(envelope))
            } finally {
                encrypted.fill(0)
            }
        } finally {
            plaintext.fill(0)
        }
    }

    private fun associatedData(): ByteArray =
        "com.ishant.securepreferences|v1|${config.name}".encodeToByteArray()

    private fun validateKey(key: String) {
        if (key.isBlank()) throw SecurePreferencesException.InvalidKey
    }

    @Serializable
    private data class Entry(val type: Type, val value: String)

    @Serializable
    private enum class Type { STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN }
}
