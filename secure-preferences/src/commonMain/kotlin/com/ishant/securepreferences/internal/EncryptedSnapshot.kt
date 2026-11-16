package com.ishant.securepreferences.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class EncryptedSnapshot(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val ciphertext: ByteArray,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }

    override fun equals(other: Any?): Boolean =
        other is EncryptedSnapshot &&
            formatVersion == other.formatVersion &&
            ciphertext.contentEquals(other.ciphertext)

    override fun hashCode(): Int = 31 * formatVersion + ciphertext.contentHashCode()
}
