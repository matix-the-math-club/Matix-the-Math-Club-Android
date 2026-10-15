# Matix — Android WebView source

The Android app is restored to a **WebView host of the original supplied HTML**, not the native Compose redesign. The bundled `app/src/main/assets/app.html` contains the original UI, CSS, JavaScript features, and Math Learn implementation. JavaScript navigation timing errors found in testing were fixed without changing the design; see `HTML_FIXES.md`.

## Open in Android Studio

1. Extract the ZIP and open `Matix-the-Math-Club-Android`.
2. Use JDK 17. Install Android SDK Platform 34 and let Gradle sync finish.
3. Run on an emulator or device. Keep Android System WebView / Chrome updated.
4. When ready, use Android Studio's **Generate Signed Bundle / APK → Android App Bundle** with your own upload key. This handoff contains source only; no APK or AAB was generated.

## Requested Android settings

| Setting | Value |
| --- | --- |
| Namespace | `com.matixmathclubappandroid.app` |
| Application ID | `com.matixmathclubappandroid.app` |
| minSdk | 26 |
| targetSdk | 26 |
| compileSdk | 34 |
| versionName | `1.0.0` |
| versionCode | 7 |

**Play Console warning:** targetSdk 26 is retained because it was explicitly requested, but it is too old for a new Google Play submission. Raise the target SDK to the level currently required by Play before uploading. The lint rule `ExpiredTargetSdkVersion` is explicitly suppressed for this requested legacy configuration; it is not a Play-policy exemption. An update must also use a versionCode higher than the one already on Play.

## Android shell support

- HTTPS asset loading at the stable `https://appassets.androidplatform.net/assets/app.html` origin.
- JavaScript, localStorage, cookies, and normal page state persistence across launches. No clearing of stored app data on startup.
- System document picker for HTML, images, and multiple files, including cancellation/concurrent-request handling.
- Camera/microphone runtime permissions for the trusted app origin; other origins/resources are denied.
- JavaScript alerts/confirmations/prompts; user-initiated popup windows; external links opened in installed apps.
- Video fullscreen, Android Back handling, renderer-crash recovery, and WebView cleanup.
- No unrestricted JavaScript-to-Android bridge, no file-origin access, and no invalid-certificate bypass.

## Validation / remaining limits

See `QA_REPORT.md`. Compilation is not proof of complete on-device correctness: upload pickers, WebRTC calls, speech recognition, keyboard behavior, popup handling, and device-specific WebView behavior need a real Android run.

The optional Learn Anything server at `http://127.0.0.1:8787` is not included or started by this app. On a phone that address refers to the phone, not your development computer. Remote Firebase, AI services, and any configured server must be available and allow the app's origin. Prefer HTTPS for remote endpoints; arbitrary cleartext servers and mixed content are blocked. Loopback/emulator cleartext exceptions are declared only for local development.

Android WebView does not guarantee desktop folder-upload (`webkitdirectory`) or browser speech-recognition support. Use ordinary/multiple file selection where directory selection is unavailable. Browser-managed remote downloads open externally; blob/data downloads are not bridged.

Preserving the supplied page also preserves its client-side account/password logic and service configuration. Review authentication, embedded credentials, Firebase security rules, and data privacy before public distribution. Camera and microphone are requested only when the page asks for them; WebView session/API-key storage is excluded from cloud/device-transfer backup.
