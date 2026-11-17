plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("com.vanniktech.maven.publish")
}

group = "io.github.kratos1996"
version = "0.1.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":secure-preferences"))
            api(libs.koin.android)
        }
        androidInstrumentedTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

android {
    namespace = "com.ishant.securepreferences.koin"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    coordinates("io.github.kratos1996", "secure-preferences-koin", "0.1.0")
    pom {
        name.set("SecurePreferencesKMP Koin")
        description.set("Optional Koin integration for SecurePreferencesKMP on Android.")
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
