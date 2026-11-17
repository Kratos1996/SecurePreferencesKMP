package com.ishant.securepreferences.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.ishant.securepreferences.SecurePreferencesConfig
import com.ishant.securepreferences.android.SecurePreferencesAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember {
                SecurePreferencesAndroid.create(
                    context = applicationContext,
                    config = SecurePreferencesConfig(name = "sample_secure_prefs"),
                )
            }
            App(
                preferences = preferences,
                platformName = "Android (${android.os.Build.MODEL}, API ${android.os.Build.VERSION.SDK_INT})",
            )
        }
    }
}
