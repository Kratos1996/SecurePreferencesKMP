package com.ishant.securepreferences.desktop

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
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
            val cipher = DesktopAesGcmCipher(
                "account",
                "alias-v1",
                directory.resolve("master-key.lock"),
                MemoryCredentialStore(),
            )
            val ciphertext = cipher.encrypt(
                "sensitive-value".encodeToByteArray(),
                "namespace-v1".encodeToByteArray(),
            )

            val tampered = ciphertext.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            assertFails {
                runTest {
                    cipher.decrypt(tampered, "namespace-v1".encodeToByteArray())
                }
            }
            assertFails {
                runTest {
                    cipher.decrypt(ciphertext, "namespace-v2".encodeToByteArray())
                }
            }
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

            assertTrue(credentials.size() == 2)
            assertFails {
                runTest { second.decrypt(ciphertext, aad) }
            }
        }
    }

    private class MemoryCredentialStore : DesktopCredentialStore {
        private val values = mutableMapOf<Pair<String, String>, String>()
        override fun getPassword(service: String, account: String): String? = values[service to account]
        override fun setPassword(service: String, account: String, password: String) {
            values[service to account] = password
        }
        fun size(): Int = values.size
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
