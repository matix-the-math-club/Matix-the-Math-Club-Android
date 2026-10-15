# WebView QA report

## Android source checks

- `:app:compileDebugKotlin` — passed.
- `:app:compileReleaseKotlin` — passed.
- `:app:lintDebug` — passed: 17 warnings, 0 active errors/fatal issues.
- `:app:lintRelease` — passed: 17 warnings, 0 active errors/fatal issues.
- Lint aborts on errors. Only `ExpiredTargetSdkVersion` is explicitly disabled because targetSdk 26 was requested. This legacy target is not eligible for a new Play submission.
- Namespace and applicationId are both `com.matixmathclubappandroid.app`; versionName 1.0.0, versionCode 7, minSdk 26, targetSdk 26, compileSdk 34.
- No APK or AAB was built.

## HTML checks

- All eight inline JavaScript blocks parsed successfully (`node qa/check-html.cjs`).
- All 9 original style blocks are byte-identical.
- All non-script markup is unchanged. Only the JavaScript guards documented in `HTML_FIXES.md` differ.

## Local smoke tests

An isolated headless Chromium test used a mobile-sized viewport, a synthetic QA user, and mocked external requests with added delay. No production credentials were used and no real backend requests or writes were allowed.

- Welcome page and sign-in form displayed.
- All 13 main navigation routes were exercised, plus Changelog and Private with owner behavior mocked.
- Rapid navigation across those routes completed without uncaught JavaScript exceptions after fixes.
- Math Learn started its adaptive warm-up, accepted all eight answers, completed the assessment, and built a personalized course with the course path visible.
- localStorage survived page reload.
- Final uncaught JavaScript exceptions: 0.

## Not covered / release checks

This is not an Android emulator/device run. Camera/microphone permission dialogs, actual WebRTC calls, document picker return values, speech recognition, fullscreen, keyboard/insets, popup/back handling, renderer recovery, and process recreation still need on-device testing. Live Firebase rules/authentication, AI-provider responses, and the optional Learn server were not verified. No assertion is made that every interaction or upstream service is error-free.

The original HTML's client-side authentication/password storage and service configuration are preserved; audit them before public distribution. Desktop `webkitdirectory` and browser speech APIs may not work identically in Android WebView.

## Source traceability

- Supplied HTML SHA-256: `facdf33391df88f83ea623de0847e679ee430c53d75ad5396742c70fa66e677a`
- Bundled HTML SHA-256: `39609e080affe2178059a29eebfed8bec02b3388fffe1d7cbf07a31b693c8395`
