package com.ishant.securepreferences

data class SecurePreferencesConfig(
    val name: String = "secure_preferences",
    val keyAlias: String = "com.ishant.securepreferences.master.v1",
) {
    init {
        require(name.isNotBlank()) { "Preferences name cannot be blank" }
        require(keyAlias.isNotBlank()) { "Key alias cannot be blank" }
    }
}
