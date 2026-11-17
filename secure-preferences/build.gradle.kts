plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

group = "io.github.kratos1996"
version = "0.1.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    js(IR) {
        browser {
            testTask {
                failOnNoDiscoveredTests = false
            }
        }
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                failOnNoDiscoveredTests = false
            }
        }
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }
        jvmMain.dependencies {
            implementation(libs.java.keyring)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.ishant.securepreferences"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (project.hasProperty("signing.keyId") ||
        project.hasProperty("signing.gnupg.key") ||
        project.hasProperty("signing.secretKeyRingFile")
    ) {
        signAllPublications()
    }
    coordinates("io.github.kratos1996", "secure-preferences-kmp", "0.1.0")
    pom {
        name.set("SecurePreferencesKMP")
        description.set("Encrypted key-value preferences for Kotlin Multiplatform.")
        url.set("https://github.com/Kratos1996/SecurePreferencesKMP")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ishant")
                name.set("Ishant Sharma")
            }
        }
        scm {
            connection.set("scm:git:github.com/Kratos1996/SecurePreferencesKMP.git")
            developerConnection.set("scm:git:ssh://github.com/Kratos1996/SecurePreferencesKMP.git")
            url.set("https://github.com/Kratos1996/SecurePreferencesKMP")
        }
    }
}
