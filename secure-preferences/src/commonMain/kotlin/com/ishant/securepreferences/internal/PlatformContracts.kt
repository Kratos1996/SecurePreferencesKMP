package com.ishant.securepreferences.internal

/**
 * Authenticated encryption adapter. Implementations must use AES-256-GCM,
 * a fresh 96-bit nonce per encryption, and authenticate the supplied AAD.
 * The returned ciphertext format is adapter-owned and must be versioned.
 */
interface AeadCipher {
    suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray
    suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray
}

/**
 * Stores one encrypted snapshot. Implementations must provide atomic replace
 * semantics and must never write plaintext preference data.
 */
interface EncryptedSnapshotStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
    suspend fun delete()
}
