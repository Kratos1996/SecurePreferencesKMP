package com.ishant.securepreferences.sample

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.ios.SecurePreferencesIos
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = { enforceStrictPlistSanityCheck = false }
) {
    val preferences = remember {
        SecurePreferencesIos.create(
            config = SecurePreferencesConfig(name = "sample_secure_prefs"),
        )
    }
    App(
        preferences = preferences,
        platformName = "iOS",
    )
}
