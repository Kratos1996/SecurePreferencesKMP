package com.ishant.securepreferences.koin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.core.module.Module
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class SecurePreferencesKoinInstrumentedTest {

    @Test
    fun resolvesSingletonAndEncryptsPreferenceValues() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val suffix = System.currentTimeMillis().toString()
        val config = SecurePreferencesConfig(
            name = "koin-instrumented-$suffix",
            keyAlias = "com.ishant.securepreferences.koin.test.$suffix",
        )

        val contextModule: Module = module {
            single<Context> { context }
        }
        val app = koinApplication {
            modules(contextModule, securePreferencesModule(config))
        }

        try {
            val first = app.koin.get<SecurePreferences>()
            val second = app.koin.get<SecurePreferences>()

            assertSame("Koin must provide a singleton per definition", first, second)

            first.putString("instrumented-token", "encrypted-value")
            first.putBoolean("instrumented-flag", value = true)

            assertEquals("encrypted-value", second.getString("instrumented-token"))
            assertEquals(true, second.getBoolean("instrumented-flag"))

            val persistedValues = context.getSharedPreferences(
                config.name,
                Context.MODE_PRIVATE,
            ).all.values.joinToString(separator = "|")
            assertFalse("Plaintext preference key must not be persisted", persistedValues.contains("instrumented-token"))
            assertFalse("Plaintext preference value must not be persisted", persistedValues.contains("encrypted-value"))

            // A malformed/tampered snapshot must fail closed rather than return defaults.
            context.getSharedPreferences(config.name, Context.MODE_PRIVATE)
                .edit()
                .putString("__encrypted_snapshot_v1", "{tampered")
                .commit()

            var rejectedTamperedSnapshot = false
            try {
                first.getString("instrumented-token")
            } catch (_: SecurePreferencesException.CorruptValue) {
                rejectedTamperedSnapshot = true
            }
            assertTrue("Tampered encrypted snapshot must be rejected", rejectedTamperedSnapshot)
        } finally {
            app.close()
        }
    }
}
