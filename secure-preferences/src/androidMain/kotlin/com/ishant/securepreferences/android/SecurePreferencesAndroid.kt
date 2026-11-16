package com.ishant.securepreferences.android

import android.content.Context
import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Android entry point backed by Android Keystore AES-256-GCM and private
 * SharedPreferences containing ciphertext only.
 *
 * Instances are cached by application + namespace so calls made through
 * multiple create() calls share the same in-process mutation lock.
 */
object SecurePreferencesAndroid {
    private val instances = ConcurrentHashMap<String, SecurePreferences>()

    fun create(
        context: Context,
        config: SecurePreferencesConfig = SecurePreferencesConfig(),
    ): SecurePreferences {
        val appContext = context.applicationContext
        val instanceKey = "${appContext.packageName}|${config.name}|${config.keyAlias}"
        return instances.getOrPut(instanceKey) {
            SecurePreferencesFactory.create(
                config = config,
                cipher = AndroidAesGcmCipher(config.keyAlias),
                storage = AndroidEncryptedSnapshotStorage(appContext, config.name),
            )
        }
    }
}
