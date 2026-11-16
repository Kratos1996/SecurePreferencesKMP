# SecurePreferencesKMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kratos1996/secure-preferences-kmp)](https://central.sonatype.com/artifact/io.github.kratos1996/secure-preferences-kmp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A SharedPreferences-style encrypted key-value storage library for Kotlin Multiplatform.

**Implemented today:** common API, Android Keystore-backed implementation, and initial iOS and JVM/Desktop adapters. JavaScript and WebAssembly adapters are not yet available. Platform integration testing remains in progress.

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

## iOS usage

The iOS entry point uses AES-256-GCM through the cryptography-kotlin Apple provider. The AES key is stored as a Keychain generic-password item with `AfterFirstUnlockThisDeviceOnly` accessibility; the encrypted snapshot is stored in the app's `NSUserDefaults` domain.

```kotlin
val preferences = SecurePreferencesIos.create(
    config = SecurePreferencesConfig(name = "account"),
)

preferences.putString("access_token", token)
```

Import `com.ishant.securepreferences.ios.SecurePreferencesIos` from the iOS source set.

## Desktop usage (JVM)

The Desktop adapter supports macOS, Windows, and Linux through the OS credential store.

```kotlin
val preferences = SecurePreferencesDesktop.create(
    config = SecurePreferencesConfig(name = "account"),
)

preferences.putString("access_token", token)
val token = preferences.getString("access_token")
```

Use the core artifact; the Desktop implementation is included in its JVM variant. The adapter uses Java Keyring to access macOS Keychain, Windows Credential Manager, or Linux GNOME Keyring/KWallet. Linux requires a running Secret Service or KWallet session. If no supported credential store is available, creation fails closed.

The encrypted snapshot is stored under `~/.securepreferences` by default. On POSIX systems the directory is owner-only (0700) and snapshot files are owner read/write (0600). On Windows, the directory inherits the current user's profile ACL. The master key is not stored in this directory. A per-store file lock serializes first-time key creation across processes.

**macOS packaging note:** Keychain access control is affected by the identity of the JVM executable. For meaningful application-level isolation, package and sign the app with its own bundled runtime; running from a shared system JDK may associate Keychain permissions with that JDK rather than the app bundle.

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

The Koin integration is an optional Android artifact. The core library does not depend on Koin.

## Security model

- Android uses AES-256-GCM with a non-exportable Android Keystore key.
- iOS uses AES-256-GCM with a 256-bit key stored in Keychain using `AfterFirstUnlockThisDeviceOnly`; the key is not stored in preferences or ordinary files.
- JVM/Desktop uses AES-256-GCM with the master key stored in the OS credential store (macOS Keychain, Windows Credential Manager, or Linux Secret Service/KWallet).
- A fresh 96-bit nonce is generated for every encryption operation.
- Authenticated additional data binds the encrypted snapshot to its configured namespace.
- The snapshot contains ciphertext only; key names and values are inside the encrypted envelope.
- The current implementation encrypts the complete preference map as one snapshot. It is intended for small preference values, not large files or blobs.

Android Keystore keys are Keystore-backed; hardware-backed protection depends on device capabilities. This library does not claim that every device provides a hardware-backed key. Browser-side encryption, once implemented, will not protect against malicious JavaScript executing in the same origin (including XSS).

## Project status

The common API, Android adapter, initial iOS adapter and initial JVM/Desktop adapter are under active development. iOS simulator/device integration tests, real OS credential-store tests on macOS/Windows/Linux, JavaScript and WebAssembly adapters, and release validation remain outstanding. Do not treat this project as production-ready until those platform implementations and tests are complete.

## Relationship to KMP_Security

This repository currently uses Android JCA AES/GCM with Android Keystore directly so the non-exportable Keystore key never needs to be exported into a generic crypto API. It does not yet depend on or reuse KMP_Security. Any future shared crypto integration must preserve platform-native, non-exportable key handling; signing keys must never be repurposed as AES encryption keys.

## License

Apache-2.0
