
import java.net.URL
import java.io.FileOutputStream

val downloadRssOriginalLogo by tasks.registering {
    val output = file("src/main/res/drawable/rss_original_logo.png")
    outputs.file(output)
    doLast {
        output.parentFile.mkdirs()
        URL("https://raw.githubusercontent.com/riyazamra1/RSS-Data-Recovery/main/app/src/main/res/drawable/rss_original_logo.png").openStream().use { input ->
            FileOutputStream(output).use { outputStream -> input.copyTo(outputStream) }
        }
        check(output.length() > 100_000) { "RSS original logo download failed or is incomplete" }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(downloadRssOriginalLogo) }

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
