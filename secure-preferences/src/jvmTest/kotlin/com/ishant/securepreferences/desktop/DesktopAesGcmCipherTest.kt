package com.ishant.securepreferences.desktop

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopAesGcmCipherTest {
    @Test
    fun encryptsWithFreshNonceAndCanDecryptAfterCipherRecreation() = runTest {
        withTemporaryDirectory { directory ->
            val credentials = MemoryCredentialStore()
            val lock = directory.resolve("master-key.lock")
            val first = DesktopAesGcmCipher("account", "alias-v1", lock, credentials)
            val plaintext = "sensitive-value".encodeToByteArray()
            val aad = "namespace-v1".encodeToByteArray()
            val ciphertextOne = first.encrypt(plaintext, aad)
            val ciphertextTwo = first.encrypt(plaintext, aad)
            assertNotEquals(ciphertextOne.take(12), ciphertextTwo.take(12))
            val recreated = DesktopAesGcmCipher("account", "alias-v1", lock, credentials)
            assertContentEquals(plaintext, recreated.decrypt(ciphertextOne, aad))
            assertContentEquals(plaintext, recreated.decrypt(ciphertextTwo, aad))
        }
    }

    @Test
    fun rejectsTamperingAndDifferentAssociatedData() = runTest {
        withTemporaryDirectory { directory ->
            val cipher = DesktopAesGcmCipher("account", "alias-v1", directory.resolve("master-key.lock"), MemoryCredentialStore())
            val aad = "namespace-v1".encodeToByteArray()
            val ciphertext = cipher.encrypt("sensitive-value".encodeToByteArray(), aad)
            val tampered = ciphertext.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            assertSuspendingFailure { cipher.decrypt(tampered, aad) }
            assertSuspendingFailure { cipher.decrypt(ciphertext, "namespace-v2".encodeToByteArray()) }
        }
    }

    @Test
    fun keyAliasSeparatesMasterKeys() = runTest {
        withTemporaryDirectory { directory ->
            val credentials = MemoryCredentialStore()
            val first = DesktopAesGcmCipher("account", "alias-v1", directory.resolve("a.lock"), credentials)
            val second = DesktopAesGcmCipher("account", "alias-v2", directory.resolve("b.lock"), credentials)
            val aad = "namespace-v1".encodeToByteArray()
            val ciphertext = first.encrypt("value".encodeToByteArray(), aad)
            assertSuspendingFailure { second.decrypt(ciphertext, aad) }
            assertEquals(2, credentials.size(), "Each key alias must create its own credential-store entry")
            assertNotEquals(
                credentials.password("com.ishant.securepreferences.account.alias-v1", "alias-v1"),
                credentials.password("com.ishant.securepreferences.account.alias-v2", "alias-v2"),
                "Different aliases must persist different random master keys",
            )
        }
    }

    private suspend fun assertSuspendingFailure(block: suspend () -> Unit) {
        var failure: Throwable? = null
        try { block() } catch (cause: Throwable) { failure = cause }
        assertNotNull(failure, "Expected the suspend operation to fail")
    }

    private class MemoryCredentialStore : DesktopCredentialStore {
        private val values = mutableMapOf<Pair<String, String>, String>()
        override fun getPassword(service: String, account: String): String? = values[service to account]
        override fun setPassword(service: String, account: String, password: String) { values[service to account] = password }
        fun size(): Int = values.size
        fun password(service: String, account: String): String? = values[service to account]
    }

    private suspend fun withTemporaryDirectory(block: suspend (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("secure-preferences-cipher-test-")
        try {
            block(directory)
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
