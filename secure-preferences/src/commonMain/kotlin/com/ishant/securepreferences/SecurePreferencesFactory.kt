package com.ishant.securepreferences

import com.ishant.securepreferences.internal.AeadCipher
import com.ishant.securepreferences.internal.EncryptedSnapshotStorage
import com.ishant.securepreferences.internal.SecurePreferencesImpl

/**
 * Advanced factory for applications supplying their own audited platform
 * crypto and encrypted persistence adapters.
 *
 * Most consumers should use a platform factory (for example,
 * SecurePreferencesAndroid.create(context, config)).
 */
object SecurePreferencesFactory {
    fun create(
        config: SecurePreferencesConfig,
        cipher: AeadCipher,
        storage: EncryptedSnapshotStorage,
    ): SecurePreferences = SecurePreferencesImpl(config, cipher, storage)
}
