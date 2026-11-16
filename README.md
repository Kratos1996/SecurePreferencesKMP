# SecurePreferencesKMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kratos1996/secure-preferences-kmp)](https://central.sonatype.com/artifact/io.github.kratos1996/secure-preferences-kmp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A SharedPreferences-style encrypted key-value storage library for Kotlin Multiplatform.

**Implemented today:** common API and Android Keystore-backed implementation. iOS, JVM Desktop, JavaScript and WebAssembly adapters are planned and are not yet available.

## Android usage (without Koin)

```kotlin
val preferences = SecurePreferencesAndroid.create(
    context = context,
    config = SecurePreferencesConfig(name = "account"),
)

preferences.putString("access_token", token)
val token = preferences.getString("access_token")
preferences.putBoolean("is_logged_in", true)
preferences.remove("access_token")
```

All reads and writes are suspending.

## Android usage with Koin

Add both artifacts to your Android app:

```kotlin
implementation("io.github.kratos1996:secure-preferences-kmp:0.1.0")
implementation("io.github.kratos1996:secure-preferences-koin:0.1.0")
```

Register the module during Koin startup. Koin's Android context is required:

```kotlin
startKoin {
    androidContext(applicationContext)
    modules(
        securePreferencesModule(
            config = SecurePreferencesConfig(name = "account"),
        ),
    )
}
```

Inject the interface into repositories or other Koin-managed components:

```kotlin
class AuthRepository(
    private val preferences: SecurePreferences,
) {
    suspend fun saveAccessToken(token: String) {
        preferences.putString("access_token", token)
    }

    suspend fun accessToken(): String? =
        preferences.getString("access_token")
}
```

### Multiple preference stores

Use a distinct Koin qualifier for each configuration:

```kotlin
val accountPreferences = named("account-preferences")
val settingsPreferences = named("settings-preferences")

startKoin {
    androidContext(applicationContext)
    modules(
        securePreferencesModule(
            config = SecurePreferencesConfig(name = "account"),
            qualifier = accountPreferences,
        ),
        securePreferencesModule(
            config = SecurePreferencesConfig(name = "settings"),
            qualifier = settingsPreferences,
        ),
    )
}
```

Resolve each store with its qualifier, for example `get<SecurePreferences>(accountPreferences)`.

The Koin integration is an optional Android artifact. The core library does not depend on Koin. Each Koin definition is a singleton; the Android factory also caches instances by application, preferences name and key alias so separately created definitions for the same store share the same in-process instance.

## Security model

- Android uses AES-256-GCM with a non-exportable Android Keystore key.
- A fresh 96-bit IV is generated for every encryption operation.
- Authenticated additional data binds the encrypted snapshot to its configured namespace.
- Only ciphertext is persisted in the private SharedPreferences file.
- The current implementation encrypts the preference map as one snapshot. It is intended for small preference values, not large files or blobs.

Android Keystore keys are Keystore-backed; hardware-backed protection depends on device capabilities. This library does not claim that every device provides a hardware-backed key.

Browser-side encryption, once implemented, will not protect against malicious JavaScript executing in the same origin (including XSS). Desktop support must use the operating system credential store; a plaintext key file is not an acceptable fallback.

## Project status

The common API and Android implementation are under active development. iOS, JVM Desktop, JavaScript and WebAssembly platform adapters, platform integration tests and release validation remain outstanding. Do not treat this project as production-ready until those platform implementations and tests are complete.

## Relationship to KMP_Security

This repository currently uses Android JCA AES/GCM with Android Keystore directly so the non-exportable Keystore key never needs to be exported into a generic crypto API. It does not yet depend on or reuse KMP_Security. Any future shared crypto integration must preserve platform-native, non-exportable key handling; signing keys must never be repurposed as AES encryption keys.

## License

Apache-2.0
