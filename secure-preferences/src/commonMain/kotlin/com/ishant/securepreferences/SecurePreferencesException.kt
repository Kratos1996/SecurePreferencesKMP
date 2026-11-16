package com.ishant.securepreferences

sealed class SecurePreferencesException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data object InvalidKey : SecurePreferencesException("Preference key cannot be blank.")
    data object TypeMismatch : SecurePreferencesException("Stored preference has a different value type.")
    data object NotInitialized : SecurePreferencesException("SecurePreferences has not been initialized.")
    class CorruptValue(cause: Throwable) :
        SecurePreferencesException("Stored preference is corrupted or failed authentication.", cause)
    class PlatformUnavailable(platform: String) :
        SecurePreferencesException("SecurePreferences is unavailable on $platform.")
}
