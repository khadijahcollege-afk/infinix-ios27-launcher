plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tedo.ios27launcher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tedo.ios27launcher"
        minSdk = 29            // jalan juga di bawah Android 13, dioptimalkan utk Note 12 (API 33)
        targetSdk = 35
        versionCode = 14
        versionName = "0.14.0"
    }
    buildFeatures { compose = true }
    signingConfigs {
        // Audit v0.12: APK yang dibagikan kini build RELEASE. Ditandatangani keystore debug
        // yang sama dengan build sebelumnya (disimpan di folder proyek ini) supaya bisa
        // langsung dipasang menimpa versi lama tanpa uninstall.
        create("tedo") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("tedo")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.core:core-ktx:1.13.1")
    // v0.2: TANPA Shizuku (permintaan Tedo). Freezer memakai mekanisme bawaan Android
    // yang sama dengan XOS Freezer: menonaktifkan app (disable) lewat sistem +
    // pintasan ke Freezer bawaan Infinix yang tetap ada di HP.
}
