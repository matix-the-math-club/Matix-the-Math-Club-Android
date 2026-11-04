// Top-level build file for Matix the Math Club (Android).
plugins {
    id("com.android.application") version "9.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
}

// `./gradlew clean` from the root.
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
