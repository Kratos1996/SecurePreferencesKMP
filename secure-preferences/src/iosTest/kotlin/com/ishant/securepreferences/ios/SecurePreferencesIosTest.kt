package com.ishant.securepreferences.ios

import com.ishant.securepreferences.SecurePreferencesConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurePreferencesIosTest {
    @Test
    fun encryptsAndPersistsTypedPreferences() = runTest {
        val config = SecurePreferencesConfig(
            name = "ios-instrumentation-test",
            keyAlias = "com.ishant.securepreferences.ios.test.v1",
        )
        val preferences = SecurePreferencesIos.create(config)

        preferences.clear()
        preferences.putString("message", "secret-value")
        preferences.putBoolean("enabled", true)
        preferences.putInt("launch-count", 42)

        assertEquals("secret-value", preferences.getString("message"))
        assertTrue(preferences.getBoolean("enabled"))
        assertEquals(42, preferences.getInt("launch-count"))
        assertTrue(preferences.contains("message"))

        val secondReference = SecurePreferencesIos.create(config)
        assertEquals("secret-value", secondReference.getString("message"))

        preferences.remove("message")
        assertFalse(preferences.contains("message"))
        preferences.clear()
    }
}
