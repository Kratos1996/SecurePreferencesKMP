# SecurePreferencesKMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kratos1996/secure-preferences-kmp)](https://central.sonatype.com/artifact/io.github.kratos1996/secure-preferences-kmp)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A lightweight, thread-safe, SharedPreferences-style **encrypted key-value preference storage library** for Kotlin Multiplatform (KMP).

It transparently encrypts all key-value entries using **AES-256-GCM** authenticated encryption, storing the master encryption key inside platform-native secure hardware/OS key stores (Android Keystore, Apple Keychain, OS Credential Managers).

---

## 🌟 Supported Platforms

| Platform | Master Key Protection | Snapshot Storage |
| :--- | :--- | :--- |
| 🤖 **Android** | Android Keystore (non-exportable AES-256-GCM key) | Private `SharedPreferences` |
| 🍏 **iOS** | Apple Keychain (`AfterFirstUnlockThisDeviceOnly`) | `NSUserDefaults` |
| 🖥️ **Desktop (JVM)** | macOS Keychain / Windows Credential Manager / Linux Secret Service (via Java Keyring) | POSIX-secured user file (`~/.securepreferences/`) |
| 🌐 **Extensible** | `SecurePreferencesFactory` for custom crypto & storage adapters | Custom implementation |

---

## 🛠️ What We Are Using Under The Hood

* **Cryptography**: AES-256-GCM with a fresh 96-bit Initialization Vector (IV/nonce) generated for every write operation.
* **Coroutines & Concurrency**: Fully suspending API (`suspend`) with thread-safe in-process mutation locks.
* **Serialization**: `kotlinx.serialization` for typed JSON preference payload snapshotting.
* **Java Keyring**: Native credential store integration for macOS, Windows, and Linux on JVM.
* **Apple Security Framework**: Native iOS Keychain APIs for key creation, protection, and retrieval.
* **Optional Koin Support**: Pre-built Koin module artifact (`:secure-preferences-koin`) for Android Dependency Injection.

---

## 📦 Installation

Add the dependencies to your `libs.versions.toml` or `build.gradle.kts`:

### Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
securePreferences = "0.1.0"

[libraries]
secure-preferences-core = { group = "io.github.kratos1996", name = "secure-preferences-kmp", version.ref = "securePreferences" }
secure-preferences-koin = { group = "io.github.kratos1996", name = "secure-preferences-koin", version.ref = "securePreferences" }
```

### `build.gradle.kts` (Multiplatform / App module)

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.secure.preferences.core)
    }
}
```

For Android apps using Koin Dependency Injection:

```kotlin
dependencies {
    implementation(libs.secure.preferences.core)
    implementation(libs.secure.preferences.koin)
}
```

---

## 🚀 How To Use

### 1. Android

Initialize using `SecurePreferencesAndroid`:

```kotlin
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.android.SecurePreferencesAndroid

val preferences = SecurePreferencesAndroid.create(
    context = context,
    config = SecurePreferencesConfig(name = "user_session"),
)

// Suspending write calls
preferences.putString("access_token", "secret_jwt_token")
preferences.putBoolean("is_logged_in", true)
preferences.putInt("user_id", 1024)

// Suspending read calls
val token: String? = preferences.getString("access_token")
val isLoggedIn: Boolean = preferences.getBoolean("is_logged_in", defaultValue = false)
val userId: Int = preferences.getInt("user_id", defaultValue = -1)
```

### 2. iOS

Initialize using `SecurePreferencesIos`:

```kotlin
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.ios.SecurePreferencesIos

val preferences = SecurePreferencesIos.create(
    config = SecurePreferencesConfig(name = "user_session"),
)

preferences.putString("access_token", "secret_jwt_token")
val token = preferences.getString("access_token")
```

### 3. Desktop (JVM)

Initialize using `SecurePreferencesDesktop` (supports macOS, Windows, and Linux):

```kotlin
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.desktop.SecurePreferencesDesktop

val preferences = SecurePreferencesDesktop.create(
    config = SecurePreferencesConfig(name = "user_session"),
)

preferences.putString("access_token", "secret_jwt_token")
val token = preferences.getString("access_token")
```

### 4. Android with Koin Integration

Register `securePreferencesModule` inside your Koin startup block:

```kotlin
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.koin.securePreferencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

startKoin {
    androidContext(applicationContext)
    modules(
        securePreferencesModule(
            config = SecurePreferencesConfig(name = "user_session"),
        ),
    )
}
```

Inject `SecurePreferences` into your Repositories or ViewModels:

```kotlin
import com.ishant.securepreferences.SecurePreferences

class AuthRepository(
    private val preferences: SecurePreferences,
) {
    suspend fun saveAuthToken(token: String) {
        preferences.putString("auth_token", token)
    }

    suspend fun getAuthToken(): String? {
        return preferences.getString("auth_token")
    }

    suspend fun logout() {
        preferences.clear()
    }
}
```

#### Multiple Store Instances with Koin

```kotlin
val sessionStore = named("session")
val settingsStore = named("settings")

startKoin {
    androidContext(applicationContext)
    modules(
        securePreferencesModule(
            config = SecurePreferencesConfig(name = "session"),
            qualifier = sessionStore,
        ),
        securePreferencesModule(
            config = SecurePreferencesConfig(name = "settings"),
            qualifier = settingsStore,
        ),
    )
}
```

### 5. Custom Engine / Factory

For custom platform extensions or testing, use `SecurePreferencesFactory`:

```kotlin
val preferences = SecurePreferencesFactory.create(
    config = SecurePreferencesConfig(name = "custom"),
    cipher = myCustomCipher,
    storage = myCustomStorage,
)
```

---

## 📖 API Reference

All read and write methods are `suspend` functions:

| Method | Description |
| :--- | :--- |
| `putString(key: String, value: String)` | Saves a string preference |
| `getString(key: String, default: String? = null): String?` | Reads a string preference |
| `putInt(key: String, value: Int)` | Saves an integer preference |
| `getInt(key: String, default: Int = 0): Int` | Reads an integer preference |
| `putLong(key: String, value: Long)` | Saves a long preference |
| `getLong(key: String, default: Long = 0L): Long` | Reads a long preference |
| `putFloat(key: String, value: Float)` | Saves a float preference |
| `getFloat(key: String, default: Float = 0f): Float` | Reads a float preference |
| `putDouble(key: String, value: Double)` | Saves a double preference |
| `getDouble(key: String, default: Double = 0.0): Double` | Reads a double preference |
| `putBoolean(key: String, value: Boolean)` | Saves a boolean preference |
| `getBoolean(key: String, default: Boolean = false): Boolean` | Reads a boolean preference |
| `remove(key: String)` | Deletes a specific key |
| `clear()` | Deletes all keys and clears the store |
| `contains(key: String): Boolean` | Checks if a key exists |
| `getAllKeys(): Set<String>` | Returns all stored key names |

---

## 🔐 Security Architecture

- **Authenticated Encryption**: Uses AES-256-GCM. A fresh 96-bit nonce/IV is generated for every write operation.
- **Envelope Privacy**: Both preference keys and values are encrypted inside the snapshot payload. The disk contains only ciphertext.
- **Tamper Protection**: Authenticated Additional Data (AAD) binds each snapshot to its configured namespace (`SecurePreferencesConfig.name`), preventing cross-store snapshot swapping.
- **Fail-Closed Design**: Any tampered or corrupted snapshot throws `SecurePreferencesException.CorruptValue` rather than returning fallback values.

---

## 📤 Publishing To Maven

To publish the library locally or to remote Maven repositories:

### Publish to Local Maven Repository (`~/.m2/repository`)
```bash
./gradlew publishToMavenLocal
```

### Publish to Remote Maven Repository (Maven Central)
```bash
./gradlew publish
```
*(Requires Sonatype GPG keys and credentials configured in `gradle.properties` or environment variables)*

---

## 📜 License

```text
Copyright 2025 Ishant Sharma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License.
You may obtain a copy of the License.
```
