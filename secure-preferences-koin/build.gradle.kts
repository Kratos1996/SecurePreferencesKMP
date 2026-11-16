plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

group = "io.github.kratos1996"
version = "0.1.0"

android {
    namespace = "com.ishant.securepreferences.koin"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":secure-preferences"))
    api(libs.koin.android)
}
