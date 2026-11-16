# SecurePreferencesKMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kratos1996/secure-preferences-kmp)](https://central.sonatype.com/artifact/io.github.kratos1996/secure-preferences-kmp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A SharedPreferences-style encrypted key-value storage library for Kotlin Multiplatform.

**Targets planned:** Android · iOS · JVM Desktop · JavaScript · WebAssembly

## API

```kotlin
val preferences = SecurePreferences.create(
    config = SecurePreferencesConfig(name = "account")
)

preferences.putString("access_token", token)
val token = preferences.getString("access_token")
preferences.putBoolean("is_logged_in", true)
preferences.remove("access_token")
```

All reads and writes are suspending to support asynchronous platform crypto and storage without blocking UI threads.

## Security goals

- AES-256-GCM authenticated encryption.
- Fresh cryptographically secure nonce for every encryption.
- Associated data binds encrypted values to the preference namespace and key.
- Android Keystore-backed encryption keys.
- iOS Keychain-protected key material.
- Desktop OS credential storage.
- Browser Web Crypto non-extractable keys stored in IndexedDB.
- Versioned encrypted record format and explicit authentication errors.
- No plaintext secrets or encryption keys in persistent storage or logs.

Browser-side encryption does not protect against malicious JavaScript executing in the same origin (including XSS). Desktop support must use the operating system credential store; a plaintext key file is not an acceptable fallback.

## Project status

This repository is at the initial scaffold stage. Common API and target declarations are being established first. Platform key providers, encrypted record persistence, factory APIs, integration tests, and CI validation must be completed before a release is considered production-ready.

## Relationship to KMP_Security

The implementation is designed to reuse the cryptographic primitives from [KMP_Security](https://github.com/Kratos1996/KMP_Security), rather than creating a second independent encryption engine. The integration must preserve platform-native, non-exportable key handling; existing device identity signing keys must never be repurposed as AES encryption keys.

## License

Apache-2.0
