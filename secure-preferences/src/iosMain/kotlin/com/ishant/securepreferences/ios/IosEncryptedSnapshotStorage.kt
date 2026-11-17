package com.ishant.securepreferences.ios

import com.ishant.securepreferences.internal.EncryptedSnapshotStorage
import platform.Foundation.NSUserDefaults

/** Stores the encrypted envelope in NSUserDefaults; the AES key remains in Keychain. */
internal class IosEncryptedSnapshotStorage(
    name: String,
) : EncryptedSnapshotStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val storageKey = "$STORAGE_PREFIX$name"

    override suspend fun read(): String? = defaults.stringForKey(storageKey)

    override suspend fun write(value: String) {
        defaults.setObject(value, forKey = storageKey)
        check(defaults.synchronize()) { "Unable to persist encrypted preferences snapshot" }
    }

    override suspend fun delete() {
        defaults.removeObjectForKey(storageKey)
        check(defaults.synchronize()) { "Unable to delete encrypted preferences snapshot" }
    }

    private companion object {
        const val STORAGE_PREFIX = "com.ishant.securepreferences.snapshot."
    }
}
