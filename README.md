# Matix the Math Club — Android Studio source

This is the source-only native Android project. It uses Kotlin, Java 17, and Jetpack Compose; no APK or Android App Bundle is included.

## Open in Android Studio

1. Extract `Matix-Android-Source.zip` and open `Matix-the-Math-Club-Android`.
2. Use JDK 17 and Android SDK 34; allow Gradle sync to finish.
3. Choose an emulator/device and use **Run** from Android Studio.
4. For your own release bundle, use **Build → Generate Signed Bundle / APK → Android App Bundle** and select your upload key. No APK/AAB was produced for this handoff.

## Requested app settings

- `minSdk`: 26
- `versionName`: `1.0.0`
- `versionCode`: 7
- `targetSdk` / `compileSdk`: 34

## Math Learn

The old fixed unit/chapter Math Learn course UI has been removed from the Android navigation and replaced with the supplied HTML's MLV5-style adaptive journey: level/topic self-assessment, adaptive warm-up, skill map, generated personal course, two lesson parts per topic, Blitz/Bubble Pop checkpoints, Final Quest, XP/stars/streaks/badges, and local per-user progress. The course's topic lesson cards use the corresponding teaching content from the supplied HTML. Progress is stored on-device in SharedPreferences; it is not synced to Firebase.

“Ask Matix to teach me anything” remains available as a separate feature, like in the HTML. It uses the Python Learn service or cached Firebase lessons. That server is not bundled; configure a reachable URL in Learn settings (the Android emulator can use `http://10.0.2.2:8787` when the service runs on the development computer). Some AI-generated exercise formats and interactive effects are simplified in the native practice/game UI.

## Other app notes

- Native club screens are written in Kotlin/Compose. HTML games and owner-uploaded HTML pages still run in WebViews.
- Matix AI requires a key in Settings. The native assistant does not cover every web-app tool workflow.
- Legacy Firebase data layout is retained for compatibility. Review Firebase security rules and migrate client-side password handling before public distribution. No hard-coded member passwords were copied.

## Validation and parity

Kotlin compile and Android lint pass. This is a functional native port, not a guarantee of pixel-for-pixel or complete behavior parity with the desktop HTML. There has been no emulator/device end-to-end QA; see `PARITY_STATUS.md` for known limitations.
