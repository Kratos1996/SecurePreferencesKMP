package com.ishant.securepreferences.koin

import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.android.SecurePreferencesAndroid
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module

/**
 * Creates a Koin module that provides an application-scoped [SecurePreferences]
 * instance backed by the Android Keystore implementation.
 *
 * Koin's Android context must be registered before this module is resolved:
 *
 * ```kotlin
 * startKoin {
 *     androidContext(applicationContext)
 *     modules(securePreferencesModule())
 * }
 * ```
 *
 * For multiple configurations, register one module per configuration and use
 * a distinct qualifier for each registration.
 */
fun securePreferencesModule(
    config: SecurePreferencesConfig = SecurePreferencesConfig(),
    qualifier: Qualifier? = null,
): Module = module {
    single<SecurePreferences>(qualifier = qualifier) {
        SecurePreferencesAndroid.create(
            context = get(),
            config = config,
        )
    }
}
