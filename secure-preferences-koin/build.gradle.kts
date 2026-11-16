plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

group = "io.github.kratos1996"
version = "0.1.0"

kotlin {
    androidTarget()

    sourceSets {
        androidMain.dependencies {
            api(project(":secure-preferences"))
            api(libs.koin.android)
        }
    }
}

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
