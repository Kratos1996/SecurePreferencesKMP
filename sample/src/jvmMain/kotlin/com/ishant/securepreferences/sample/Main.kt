package com.ishant.securepreferences.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.desktop.SecurePreferencesDesktop

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SecurePreferences KMP Sample - Desktop",
    ) {
        val preferences = remember {
            SecurePreferencesDesktop.create(
                config = SecurePreferencesConfig(name = "sample_secure_prefs"),
            )
        }
        App(
            preferences = preferences,
            platformName = "Desktop JVM (${System.getProperty("os.name")})",
        )
    }
}
