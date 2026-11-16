package com.ishant.securepreferences

import com.ishant.securepreferences.internal.AeadCipher
import com.ishant.securepreferences.internal.EncryptedSnapshotStorage
import com.ishant.securepreferences.internal.SecurePreferencesImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurePreferencesImplTest {

    @Test
    fun storesAndReadsTypedValues() = runTest {
        val prefs = createPreferences()
        prefs.putString("token", "secret")
        prefs.putInt("count", 42)
        prefs.putBoolean("enabled", true)
        prefs.putLong("timestamp", 123456789L)
        prefs.putFloat("ratio", 1.5f)
        prefs.putDouble("amount", 9.25)

        assertEquals("secret", prefs.getString("token"))
        assertEquals(42, prefs.getInt("count"))
        assertTrue(prefs.getBoolean("enabled"))
        assertEquals(123456789L, prefs.getLong("timestamp"))
        assertEquals(1.5f, prefs.getFloat("ratio"))
        assertEquals(9.25, prefs.getDouble("amount"))
    }

    @Test
    fun missingAndRemovedValuesUseDefaults() = runTest {
        val prefs = createPreferences()
        assertEquals("fallback", prefs.getString("missing", "fallback"))
        prefs.putString("token", "value")
        prefs.remove("token")
        assertEquals(null, prefs.getString("token"))
        assertFalse(prefs.contains("token"))
    }

    @Test
    fun clearRemovesAllValues() = runTest {
        val prefs = createPreferences()
        prefs.putString("a", "one")
        prefs.putInt("b", 2)
        prefs.clear()
        assertTrue(prefs.getAllKeys().isEmpty())
    }

    private fun createPreferences(): SecurePreferences {
        val storage = MemoryStorage()
        val cipher = TestCipher()
        return SecurePreferencesImpl(
            SecurePreferencesConfig(name = "test"),
            cipher,
            storage,
        )
    }

    private class MemoryStorage : EncryptedSnapshotStorage {
        private var value: String? = null
        override suspend fun read(): String? = value
        override suspend fun write(value: String) { this.value = value }
        override suspend fun delete() { value = null }
    }

    /** Test-only reversible adapter; never used in production. */
    private class TestCipher : AeadCipher {
        override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
            plaintext.copyOf()
        override suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
            ciphertext.copyOf()
    }
}
