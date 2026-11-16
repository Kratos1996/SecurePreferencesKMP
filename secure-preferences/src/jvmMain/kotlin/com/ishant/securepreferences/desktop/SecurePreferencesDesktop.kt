package com.ishant.securepreferences.desktop

import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesFactory
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** JVM/Desktop entry point backed by the native OS credential store. */
object SecurePreferencesDesktop {
    private val instances = ConcurrentHashMap<String, SecurePreferences>()

    fun create(
        config: SecurePreferencesConfig = SecurePreferencesConfig(),
        storageDirectory: Path = defaultStorageDirectory(),
    ): SecurePreferences {
        val directory = storageDirectory.toAbsolutePath().normalize()
        val storeId = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(config.name.encodeToByteArray()))
        val instanceKey = "$directory|$storeId|${config.keyAlias}"
        return instances.computeIfAbsent(instanceKey) {
            val snapshot = directory.resolve("$storeId.snapshot")
            val lockFile = directory.resolve("$storeId.key.lock")
            SecurePreferencesFactory.create(
                config = config,
                cipher = DesktopAesGcmCipher(config.name, config.keyAlias, lockFile),
                storage = DesktopEncryptedSnapshotStorage(snapshot),
            )
        }
    }

    private fun defaultStorageDirectory(): Path =
        Path.of(System.getProperty("user.home"), ".securepreferences")
}
