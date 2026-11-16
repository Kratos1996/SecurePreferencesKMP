package com.ishant.securepreferences.desktop

import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * JVM/Desktop entry point.
 *
 * The master AES key is stored in the native OS credential store. The
 * encrypted preference snapshot is stored under the current user's home
 * directory. No plaintext-key file fallback is provided.
 */
object SecurePreferencesDesktop {
    private val instances = ConcurrentHashMap<String, SecurePreferences>()

    fun create(
        config: SecurePreferencesConfig = SecurePreferencesConfig(),
        storageDirectory: Path = defaultStorageDirectory(),
    ): SecurePreferences {
        val safeName = config.name.replace(UNSAFE_NAME_CHARS, "_")
        require(safeName.isNotBlank() && safeName != "." && safeName != "..") {
            "Preferences name must contain at least one safe character"
        }
        val directory = storageDirectory.toAbsolutePath().normalize()
        val key = "${directory}|${config.name}|${config.keyAlias}"
        return instances.computeIfAbsent(key) {
            val snapshot = directory.resolve("$safeName.snapshot")
            val lockFile = directory.resolve("$safeName.key.lock")
            SecurePreferencesFactory.create(
                config = config,
                cipher = DesktopAesGcmCipher(
                    configName = config.name,
                    keyAlias = config.keyAlias,
                    lockFile = lockFile,
                ),
                storage = DesktopEncryptedSnapshotStorage(snapshot),
            )
        }
    }

    private fun defaultStorageDirectory(): Path =
        Path.of(System.getProperty("user.home"), ".securepreferences")

    private val UNSAFE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")
}
