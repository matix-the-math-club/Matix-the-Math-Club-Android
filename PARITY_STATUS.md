# HTML-to-native Android parity and QA

## Current approach

The project uses Kotlin and Jetpack Compose for its app UI. The supplied `app.html` was used as the reference for a best-effort native port of its layout, content, color themes, and workflows. This is not a literal DOM/CSS/JavaScript copy and cannot be described as 100% or pixel-identical. There is no WebView serving as the app's primary interface.

The Math Learn feature is the adaptive Kotlin implementation, backed by the bundled `math_learn_kb.json`; it is intended to assess a learner and generate a tailored course rather than show the old fixed-course leftover.

## Implemented / carried over

The native app includes its boot/welcome and sign-in flow, main navigation and dashboard, themes/profile/settings, games and game-building/editing, Labs, and the adaptive Math Learn flow with learning activities and related rewards/progress screens. Firebase-backed data requires a valid project configuration and network access.

## Known parity gaps

The port remains incomplete. In particular, discussions/private chat, the full assistant tool-calling experience, some course-authoring and assessment workflows, certain upload and owner-approval tools, custom owner pages, and some browser-specific animations/settings are not at full HTML parity. Compose layout, responsive behavior, transitions, and input interactions may also differ from the web version.

## Android configuration

- applicationId `com.matixmathclubappandroid.app`
- minSdk 26; targetSdk 26
- versionName `1.0.0`; versionCode 7

## Checks

- `:app:compileDebugKotlin` — passed.
- `:app:lintDebug` — passed with lint warnings.
- No APK/AAB was generated. Device/emulator visual and end-to-end verification is still needed.
