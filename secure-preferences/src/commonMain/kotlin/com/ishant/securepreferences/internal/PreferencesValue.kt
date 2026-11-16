package com.ishant.securepreferences.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class PreferencesValue(
    val type: Type,
    val value: String,
) {
    @Serializable
    enum class Type { STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN }
}
