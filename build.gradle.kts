plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.maven.publish) apply false
}

val preparePackageLock = tasks.register("preparePackageLock") {
    val buildDir = layout.buildDirectory
    doLast {
        val packageLockFile = buildDir.file("js/package-lock.json").get().asFile
        if (!packageLockFile.exists()) {
            packageLockFile.parentFile.mkdirs()
            packageLockFile.writeText("{}")
        }
    }
}

tasks.matching { it.name.endsWith("PackageLock") && it.name != "preparePackageLock" }.configureEach {
    dependsOn(preparePackageLock)
}

