package com.ishant.securepreferences.android

import android.content.Context
import com.ishant.securepreferences.SecurePreferences
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.SecurePreferencesFactory

/**
 * Android entry point backed by Android Keystore AES-256-GCM and private
 * SharedPreferences containing ciphertext only.
 */
object SecurePreferencesAndroid {
    fun create(
        context: Context,
        config: SecurePreferencesConfig = SecurePreferencesConfig(),
    ): SecurePreferences = SecurePreferencesFactory.create(
        config = config,
        cipher = AndroidAesGcmCipher(config.keyAlias),
        storage = AndroidEncryptedSnapshotStorage(context, config.name),
    )
}
