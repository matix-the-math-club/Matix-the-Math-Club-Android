# HTML-to-Android parity and QA

## Implemented in the native project

- Compose welcome/sign-in/home shell, native theme presets and basic preferences.
- Games hub and builder/editor/player, including HTML file import and thumbnail photos.
- Ideas, translator, profile editor/photos, points ledger/rewards, notifications, and private notes/to-dos.
- Discussions and one-to-one Math Chat: member list, direct messages, typing/read/presence, reactions/stickers/math input, and delete-own-message.
- Owner HTML pages and a native Matix AI chat with supported tool calls, owner checks, and confirmation prompts for shared/owner writes.
- Math Learn path, lessons, seven exercise types, hearts/XP/gems, streaks, daily and shared friend quests, mistakes/practice, placement and skip tests, shop, badges, leaderboard, and owner course editor.
- Learn-anything Android client, local recent topics and Firebase lesson cache.
- Cross-device sign-out revision polling. No fixed sample-member passwords were copied into source.

## Known differences and unfinished work

- A native app is not pixel-identical to a desktop web app; Compose layouts and interactions intentionally use Android patterns.
- Live/video calling and AI chat file attachments are not implemented.
- The Python Math Learn server is not in the supplied Android archive. New research lessons require that service; only cached topics work offline. Configure an emulator/phone-reachable URL in Learn settings.
- The assistant does not yet reproduce every web-app tool/workflow or persistent AI chat history.
- Inbox handles pending points/reward approvals and quiz-result point awards, but not every original restoration/moderation action.
- English/Arabic/French/Spanish preferences are saved; Arabic uses RTL layout. Full string translation throughout the app is unfinished.
- Reduce-motion and quiet-mode preferences are stored, but do not yet suppress every animation/notification.
- No emulator/device runtime pass was possible in this environment; Firebase, AI providers, Learn server, direct messaging, and WebView content still need end-to-end device testing.
- The legacy Firebase username/password format is retained for compatibility. Secure Firebase rules and a server-authenticated credential migration are required before public distribution.

## Checks performed

- `:app:compileDebugKotlin` — passed.
- `:app:lintDebug` — passed with warnings only (notably target/compile SDK age and existing resource/icon/KTX suggestions).
- No APK or AAB was built for this handoff. Pre-existing APK outputs from the supplied branch were removed. Release builds are no longer silently signed with the debug keystore.