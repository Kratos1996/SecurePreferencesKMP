package com.ishant.securepreferences.android

import android.content.Context
import com.ishant.securepreferences.internal.EncryptedSnapshotStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores a single encrypted snapshot in a private SharedPreferences file.
 * Preference names and keys are inside the encrypted snapshot, not file keys.
 */
internal class AndroidEncryptedSnapshotStorage(
    context: Context,
    name: String,
) : EncryptedSnapshotStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        name,
        Context.MODE_PRIVATE,
    )

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        preferences.getString(SNAPSHOT_KEY, null)
    }

    override suspend fun write(value: String) = withContext(Dispatchers.IO) {
        check(preferences.edit().putString(SNAPSHOT_KEY, value).commit()) {
            "Unable to persist encrypted preferences snapshot"
        }
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        check(preferences.edit().clear().commit()) {
            "Unable to clear encrypted preferences snapshot"
        }
    }

    private companion object {
        const val SNAPSHOT_KEY = "__encrypted_snapshot_v1"
    }
}
