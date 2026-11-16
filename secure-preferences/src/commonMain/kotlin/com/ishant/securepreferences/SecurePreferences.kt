package com.ishant.securepreferences

/**
 * SharedPreferences-style encrypted key-value store.
 *
 * All operations are suspending so platform cryptography and persistence APIs
 * can be used without blocking UI threads.
 */
interface SecurePreferences {
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String, defaultValue: String? = null): String?

    suspend fun putInt(key: String, value: Int)
    suspend fun getInt(key: String, defaultValue: Int = 0): Int

    suspend fun putLong(key: String, value: Long)
    suspend fun getLong(key: String, defaultValue: Long = 0L): Long

    suspend fun putFloat(key: String, value: Float)
    suspend fun getFloat(key: String, defaultValue: Float = 0f): Float

    suspend fun putDouble(key: String, value: Double)
    suspend fun getDouble(key: String, defaultValue: Double = 0.0): Double

    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    suspend fun contains(key: String): Boolean
    suspend fun remove(key: String)
    suspend fun clear()
    suspend fun getAllKeys(): Set<String>
}
