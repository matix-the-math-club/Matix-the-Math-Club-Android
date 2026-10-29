plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.matixmathclubappandroid.app"
    compileSdk = 37
    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "com.matixmathclubappandroid.app"
        minSdk = 26
        targetSdk = 37 // Retained as explicitly requested; raise for a new Play submission.
        versionCode = 16
        versionName = "1.0.3"
    }

    // Release signing stays in Android Studio or your own CI environment.
    val ksPath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug { isDebuggable = true }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    // v1.1 bundles ~170 MB of local videos in assets/media. Keep them stored (not deflated) so the
    // app can stream them with byte ranges via AssetManager.openFd() and they install faster.
    androidResources {
        noCompress += listOf("mp4", "m4v", "webm")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // The only suppressed error is the intentionally requested legacy target SDK.
        // This does NOT waive Google Play's current target-API requirements.
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.webkit:webkit:1.11.0")
}
