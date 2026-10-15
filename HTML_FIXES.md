# HTML runtime fixes (no redesign)

The bundled `app/src/main/assets/app.html` comes directly from the supplied `app (1).html`. The original CSS and non-script markup are unchanged. Only JavaScript guards for stale asynchronous updates were added.

## Fixed / hardened

- Home tips: a response can arrive after a newer Home renderer or navigation removes `homeBody`. It now verifies that its target still exists and is the same element.
- Games list / My projects / project detail / editor: responses no longer update a removed or replaced screen.
- Messages: late notification responses no longer render into missing `msgBody`; message builders also guard missing output targets.
- Users list/detail and Changelog: guard asynchronous responses and failure handlers after navigation.
- AI key test: ignore output if the settings result element has been removed.

The initial local test reproduced `Cannot set properties of null (setting innerHTML)` on Home, Games, and Messages. The repaired normal/rapid-navigation tests completed without uncaught JavaScript exceptions.

No original CSS, markup, learning data, exercise logic, authentication policy, or visual layout was redesigned. Backend/service failures are not masked by these guards; device and live-service testing is still needed.
