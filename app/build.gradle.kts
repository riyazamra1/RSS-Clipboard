import java.io.File

val syncRssBrandAssets by tasks.registering {
    val sourceDir = rootProject.file("rss-brand-kit/brand/logo")
    val outputDir = file("src/main/res/drawable")
    outputs.files(
        outputDir.resolve("rss_main_logo.png"),
        outputDir.resolve("rss_logo_only.png"),
        outputDir.resolve("rss_favicon.png")
    )
    doLast {
        check(sourceDir.isDirectory) { "RSS-Brand-Kit submodule is missing. Initialize/update submodules before building." }
        val assets = mapOf(
            "RSS Logo with Name Transparent.png" to "rss_main_logo.png",
            "RSS Logo Only.png" to "rss_logo_only.png",
            "RSS Logo Favicon.png" to "rss_favicon.png"
        )
        assets.forEach { (source, target) ->
            val input = sourceDir.resolve(source)
            check(input.isFile) { "Missing RSS Brand Kit asset: $source" }
            input.copyTo(outputDir.resolve(target), overwrite = true)
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(syncRssBrandAssets) }

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.riyaz.rssclipboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.riyaz.rssclipboard"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.riyaz.rss.common:rss-common:0.1.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
