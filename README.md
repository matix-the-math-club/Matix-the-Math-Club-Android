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
| targetSdk | 36 |
| compileSdk | 34 |
| versionName | `1.1.0` |
| versionCode | 11 |

**Play Console note:** an update must use a versionCode higher than the one already on Play (this release uses 11). The lint rule `ExpiredTargetSdkVersion` remains suppressed from the earlier legacy configuration; it is not a Play-policy exemption.

## Version 1.1.0 — media pack (about 190 MB)

The app now ships a large offline media pack, so the download is roughly **190 MB** instead of about 2.7 MB. That stays under Google Play's 200 MB limit for the base download, so a normal App Bundle upload works without asset packs.

All of it is original: every film and loop is rendered from math by the scripts in `tools/media` (no stock footage, no downloaded images or icon packs), and the SVGs are Matix's own artwork taken out of `app.html`.

| Folder (`app/src/main/assets/…`) | What it is | Where it shows |
| --- | --- | --- |
| `svg/brand`, `svg/social`, `svg/learn` | 65 SVG files that used to be inline strings in `app.html` (logo, badge, social icons, Learn icons) | everywhere, loaded by `js/matix-svg.js` |
| `media/brand` | intro film + ambient welcome loop, portrait and landscape | boot screen (with **Skip intro**), sign-in / welcome screen |
| `media/hero` | 16 looping math animations | behind the coloured header card of each section |
| `media/fx` | 4 celebration effects × 2 orientations | layered over the confetti when you finish something |
| `media/gallery`, `media/posters` | **Math Motion**: 19 math films (Mandelbrot deep zoom, Julia morph, Newton fractal, Fourier epicycles, Lorenz attractor, Galton board, Ulam spiral, Game of Life, …) + highlight reel and posters | new **Math Motion** section (menu, Home, search) with player |

How it works:

- `js/matix-media.js` + `css/matix-media.css` add the videos without changing the existing pages. Videos are muted, play inline, pause when off-screen or when the app is in the background, and at most 3 background loops play at once.
- **Reduce motion / No motion** (app setting or Android "Remove animations") turns off the intro film, background loops and FX. **Math Motion → Animated backgrounds** turns off just the background loops. The full intro plays on first launch only; later launches cap it at 3 seconds.
- `MainActivity` serves `assets/media/*` with HTTP Range (206 Partial Content) so films seek and loop smoothly, and `build.gradle.kts` keeps `.mp4` files uncompressed in the APK (`androidResources.noCompress`).
- Android Back closes the Math Motion player first.
- `media/manifest.json` lists every file. All films were generated from real equations by the scripts in `tools/media` (Python 3 + pycairo + numpy + ffmpeg/x264, and a small C fractal renderer). To regenerate or resize the pack, see `tools/media/README.md`.

## Android shell support

- HTTPS asset loading at the stable `https://appassets.androidplatform.net/assets/app.html` origin.
- JavaScript, localStorage, cookies, and normal page state persistence across launches. No clearing of stored app data on startup.
- System document picker for HTML, images, and multiple files, including cancellation/concurrent-request handling.
- Camera/microphone runtime permissions for the trusted app origin; other origins/resources are denied.
- JavaScript alerts/confirmations/prompts; user-initiated popup windows; external links opened in installed apps.
- Video fullscreen, Android Back handling, renderer-crash recovery, and WebView cleanup.
- Byte-range streaming for bundled media in `assets/media` (seeking/looping large videos).
- No unrestricted JavaScript-to-Android bridge, no file-origin access, and no invalid-certificate bypass.

## Validation / remaining limits

See `QA_REPORT.md`. Compilation is not proof of complete on-device correctness: upload pickers, WebRTC calls, speech recognition, keyboard behavior, popup handling, and device-specific WebView behavior need a real Android run.

The optional Learn Anything server at `http://127.0.0.1:8787` is not included or started by this app. On a phone that address refers to the phone, not your development computer. Remote Firebase, AI services, and any configured server must be available and allow the app's origin. Prefer HTTPS for remote endpoints; arbitrary cleartext servers and mixed content are blocked. Loopback/emulator cleartext exceptions are declared only for local development.

Android WebView does not guarantee desktop folder-upload (`webkitdirectory`) or browser speech-recognition support. Use ordinary/multiple file selection where directory selection is unavailable. Browser-managed remote downloads open externally; blob/data downloads are not bridged.

Preserving the supplied page also preserves its client-side account/password logic and service configuration. Review authentication, embedded credentials, Firebase security rules, and data privacy before public distribution. Camera and microphone are requested only when the page asks for them; WebView session/API-key storage is excluded from cloud/device-transfer backup.
