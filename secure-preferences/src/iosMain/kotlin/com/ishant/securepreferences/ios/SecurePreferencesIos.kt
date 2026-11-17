package com.ishant.securepreferences.ios

import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesFactory
import platform.Foundation.NSBundle
import platform.Foundation.NSLock

/** iOS entry point backed by AES-256-GCM and an AES key protected by Keychain. */
object SecurePreferencesIos {
    private val lock = NSLock()
    private val instances = mutableMapOf<String, SecurePreferences>()

    fun create(
        config: SecurePreferencesConfig = SecurePreferencesConfig(),
    ): SecurePreferences {
        val bundleId = NSBundle.mainBundle.bundleIdentifier ?: DEFAULT_SERVICE
        val instanceKey = "$bundleId|${config.name}|${config.keyAlias}"
        lock.lock()
        return try {
            instances.getOrPut(instanceKey) {
                SecurePreferencesFactory.create(
                    config = config,
                    cipher = IosAesGcmCipher(
                        service = "$bundleId.${config.name}",
                        account = config.keyAlias,
                    ),
                    storage = IosEncryptedSnapshotStorage(config.name),
                )
            }
        } finally {
            lock.unlock()
        }
    }

    private const val DEFAULT_SERVICE = "com.ishant.securepreferences"
}
