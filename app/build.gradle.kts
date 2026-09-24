plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---------------------------------------------------------------------------
// The web app lives at the PROJECT ROOT as `app.html` - that file is the single
// source of truth. This task copies it into the APK assets before every build,
// so you only ever edit the root file and never keep two copies in sync.
// The copy at app/src/main/assets/app.html is generated and git-ignored.
// ---------------------------------------------------------------------------
val rootWebApp = rootProject.file("app.html")

val syncWebApp by tasks.registering(Copy::class) {
    group = "build"
    description = "Copies the root app.html into app/src/main/assets/ for the WebView."
    from(rootWebApp)
    into(layout.projectDirectory.dir("src/main/assets"))
    doFirst {
        if (!rootWebApp.exists()) {
            throw GradleException(
                "app.html is missing from the project root: ${rootWebApp.absolutePath}\n" +
                "The WebView loads this file, so the build cannot continue without it."
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(syncWebApp) }

android {
    namespace = "club.matix.mathclub"
    compileSdk = 34

    defaultConfig {
        applicationId = "club.matix.mathclub"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "4.0"
        resourceConfigurations += listOf("en")
    }

    // Release signing is optional: CI (or a local dev) can point
    // KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD env vars at a
    // real upload keystore. Without those set, release falls back to the
    // debug keystore so `assembleRelease`/`bundleRelease` still produce an
    // installable APK/AAB out of the box.
    val ksPath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (ksPath != null && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (ksPath != null && file(ksPath).exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    // app.html is already ~850 KB of minified-ish HTML; don't let aapt
    // re-compress it badly or strip it.
    androidResources {
        noCompress += listOf("html")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.webkit:webkit:1.11.0")
}
