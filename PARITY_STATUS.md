# HTML-to-Android parity and QA

## Math Learn: adaptive MLV5 flow now active

- Replaced the Android fixed units/chapters course path with a self-assessment, topic selection, adaptive warm-up, starting skill map, and personalized lesson/challenge course.
- Course generator uses the HTML's nine Math Learn topics and node sequence: two lesson parts per selected topic, Blitz and Bubble Pop checkpoints, then an eight-question Final Quest.
- Lesson-card content is mapped from the supplied HTML's topic knowledge base; lesson quizzes adapt difficulty from placement and use the same 5-question lesson structure, with speed bonuses on Part 2 and the Final Quest.
- Native course includes topic progress, XP/course level, stars, streaks, revision tracking, best game scores, badges, course reset, warm-up retake, and free practice games.
- Personal adaptive-course data is local to the device and partitioned by username. It does not sync across devices.
- The old fixed course/editor/quest/shop/hearts/gems Math Learn screens and route were removed. “Ask Matix to teach me anything” remains a separate feature, as it is in the supplied HTML.

## Remaining Math Learn differences

- Compose/Material screens use Android-native layout, not the HTML/CSS pixel-for-pixel appearance.
- The question generator is a Kotlin reimplementation and is not byte-for-byte identical to every HTML randomized prompt/distractor. Arcade interaction is simplified; Bubble Pop uses native tappable bubbles and life/time rules, not the HTML's canvas-style rising-bubble animation.
- Some generated Learn-anything exercise types and source/server behavior require the separate Python service or cached Firebase lessons, and some practice interactions are simplified.
- No emulator/device runtime pass was possible; gameplay, orientation, keyboard, and edge-case behavior still need hands-on QA.

## Other known differences

- Live/video calling, AI chat file attachments, every original assistant tool/workflow, and full string translation/localization are not implemented.
- Reduce-motion and quiet-mode settings are stored but do not suppress every effect.
- The legacy Firebase username/password format is retained for compatibility. Secure Firebase rules and server-authenticated credential migration are required before public distribution.

## Checks

- `:app:compileDebugKotlin` — passed.
- `:app:lintDebug` — passed (warnings only).
- No APK or AAB was built.
