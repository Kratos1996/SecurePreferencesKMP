package com.ishant.securepreferences.koin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
            first.putBoolean("instrumented-flag", true)

            assertEquals("encrypted-value", second.getString("instrumented-token"))
            assertEquals(true, second.getBoolean("instrumented-flag"))
        } finally {
            app.close()
        }
    }
}
