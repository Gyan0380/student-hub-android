# StudentHub — WebView se 100% Native Android (Kotlin + Compose) — Build Prompt

Agar ye chat limit/token khatam ho jaye, is poori file ko copy karke naye chat mein paste
karo aur likho: **"Yahan se aage continue karo"** — Claude ko poora context mil jayega.

## Goal
Current app WebView wrapper hai jo `https://student-hub-five-ashy.vercel.app` load karta
hai. Isko 100% native Android app (Kotlin, Jetpack Compose, MVVM) banana hai, jisme:
- Har screen native UI ho (WebView bilkul use nahi).
- Data layer **same Firebase project** use kare jo website use karta hai, taaki chats/users/
  notifications website aur native app ke beech **cross-compatible** rahein — same Firestore
  collections/fields, koi naya schema nahi.
- Feature parity website ke saath (list neeche hai).

## Firebase project (same as website — DO NOT create a new project)
```
apiKey: AIzaSyCDLlqMtCGcKfbchKblBNNLec9Y4AkRXL0
authDomain: student-a866d.firebaseapp.com
projectId: student-a866d
storageBucket: student-a866d.firebasestorage.app
messagingSenderId: 742359477068
appId: 1:742359477068:web:0d8481cbd5032428a9fde9
```
(`google-services.json` project mein already hai — same project.)

## Firestore schema (extracted from live app.js — MUST match exactly for cross-chat)
- `Users/{uid}`: `username, role ("Student"|"Admin"|"Owner"), profilePhoto, bio, tags,
  isBanned, timedOutUntil, classAccess[], photoCooldowns{}, createdAt`
- `Chats/{roomId}/Messages/{msgId}`: `text, photos[]|null, senderId, senderName, senderPhoto,
  createdAt (serverTimestamp), replyTo{id,text,senderName}|null, edited(bool), editedAt`
  - `roomId` values: `"global"`, `"anonymous"`, `"admin-room"`, or a slugified class name.
- `Notifications/{id}`: `toUid ("all"|uid), title, body, photos[]|null, createdAt`
- `Suggestions/{id}`: `uid, username, text, photo|null, createdAt`
- `BugReports/{id}`: `uid, username, text, photos[], createdAt`
- `Settings/AntiAbuse`: `{ words: [] }`
- `Settings/CommunityRules`: `{ rules: "" }`
- `Tags/{id}`: `{ id, label, color, type }`

Roles: `isAdminOrOwner(u) = u.role === 'Admin' || u.role === 'Owner'`.

## Screens to build (native), in priority order
1. **Auth**: Login, Register, Forgot Password (Firebase Auth email/password).
2. **Chat list / Home**: rooms user has access to (global, anonymous, their classes,
   admin-room if admin), unread indicator per room (`studentchat-seen-{uid}-{roomId}`
   pattern — port to local Room DB or DataStore instead of localStorage).
3. **Chat screen** (core, do this first — this is the "cross chat" piece):
   - Realtime Firestore listener on `Chats/{roomId}/Messages` ordered by `createdAt`.
   - Send text + up to 5 photos (base64, matches web's `photos[]` field — keep same
     encoding so old/new messages both render).
   - Reply (tap → reply-to bar), Edit (own messages only), Delete (own message, or any
     message if admin/owner — "(mod)" label).
   - **Message action UX (already spec'd for the web version, mirror natively):** each
     message shows only a Reply affordance inline; tapping a message opens an action
     sheet with Reply/Edit/Delete — Edit+Delete only appear for your own messages, or
     for any message if you're Admin/Owner (Edit stays own-message-only even for admins).
     Tapping someone else's message as a non-admin does nothing.
   - Mentions (`@username`) highlighting + "mentioned" state.
4. **Profile / Edit Profile**: photo, bio, theme (Light/Dark/Sepia/Ocean).
5. **Notifications screen**: list from `Notifications` collection + native push via FCM
   (already working — `StudentHubMessagingService.java` — keep this, don't rewrite).
6. **Notification settings**: per-category toggles (Global/Anonymous/Class/Announcements)
   — persist via `NotificationSettings.java` (already exists, native SharedPreferences) —
   this replaces the old JS `StudentHubAndroid` bridge entirely since there's no WebView.
7. **Community Rules** (read-only view + admin edit).
8. **Suggestions** (submit + admin list/delete).
9. **Bug Report** (submit + admin list/delete).
10. **Admin panel**: manage users (role, ban, timeout, class access, tags), anti-abuse
    word list, community rules editor, send/delete notifications, view suggestions/bugs.

## Architecture decisions (defaults chosen — say if you want different)
- Kotlin + Jetpack Compose + Material 3.
- MVVM: `ViewModel` + `StateFlow` per screen, Repository layer wrapping Firestore/Auth.
- Navigation: `androidx.navigation.compose`.
- Min SDK 23 (unchanged from current `build.gradle`), target/compile SDK 35.
- Keep `StudentHubMessagingService.java` and `NotificationSettings.java` as-is (native,
  already correct) — just remove `WebAppActivity`/WebView and the JS bridge.
- Local persistence for "seen/unread": Jetpack `DataStore` (replaces `localStorage`).

## Build order for this rewrite (do in this sequence, one PR-sized chunk at a time)
1. Project scaffold: Gradle (Compose enabled), Firebase deps, `MainActivity`,
   navigation graph with placeholder screens.
2. Data layer: `Message`, `User`, `Notification`, `Suggestion`, `BugReport` models +
   `ChatRepository`, `UserRepository`, `AuthRepository` (Firestore/Auth calls, matching
   schema above exactly).
3. Auth screens + session (Firebase Auth persists natively by default — no WebView
   logout bug to worry about).
4. Chat list + Chat screen (the "same chats cross-chat" priority piece).
5. Profile/theme, Notifications screen + settings.
6. Community rules, Suggestions, Bug report.
7. Admin panel (biggest screen, do last).
8. Wire FCM token → `Users/{uid}.fcmToken` so admin "Send Notification" can eventually
   target devices directly (currently token is fetched but not stored — see
   `getFCMToken()` TODO in `WebAppActivity.java`).

## Status
- [x] Step 1 scaffold.
- [x] Step 2 data layer — Message, AppUser, AppNotification, Suggestion, BugReport models;
  AuthRepository, UserRepository, ChatRepository, NotificationRepository.
- [x] Step 3 auth — Login, Register, ForgotPassword screens + AuthViewModel; session
  persists natively (no WebView logout bug), `loading` route auto-skips to Home if a
  Firebase session already exists.
- [x] Step 4 (done) — Home/room-list screen with unread dot + last-message preview
  (DataStore-backed `UnreadStore`, replaces `localStorage` seen-tracking) and Chat screen
  (realtime listener, send/edit/delete, reply, action-sheet with own-message/admin rules).
  Photo attachments now wired: `ChatViewModel` stages up to 5 base64 photos
  (`pendingPhotos`), `ChatScreen` uses `ActivityResultContracts.PickMultipleVisualMedia`
  + `ImageUtils.uriToCompressedBase64` (downscale + JPEG compress, same idea as the web's
  canvas compression) with a thumbnail strip + remove button, and message bubbles render
  attached photos via Coil. @mention highlighting also wired: `ChatViewModel.isMentioned()`
  (word-boundary, case-insensitive `@username` match) tints the whole bubble, and
  `ChatScreen.highlightMentions()` bolds/colors `@username` tokens inline in message text.
- [x] Step 5 (done) — Notifications screen (merged "all" + per-uid stream). Profile /
  Edit Profile screen built (`ui/profile/ProfileScreen.kt` + `ProfileViewModel.kt`): photo
  picker (reuses `ImageUtils`), bio field, theme picker (Light/Dark/Sepia/Ocean via new
  `util/ThemeStore.kt` DataStore + `ui/theme/AppTheme.kt` color schemes, applied at the
  `MaterialTheme` root in `MainActivity`), save, sign out. Notification settings screen
  built (`ui/settings/NotificationSettingsScreen.kt`) — reads/writes the existing
  `NotificationSettings.java` directly (this *is* the native replacement for the old JS
  `StudentHubAndroid` bridge, as planned — no bridge needed since there's no WebView).
  `UserRepository.updateProfile()` added (bio + profilePhoto only; username intentionally
  left immutable here since it's used as a chat identity/search key elsewhere).
- [x] Step 6 (done) — Community Rules, Suggestions, Bug Report screens built.
  `ui/rules/CommunityRulesScreen.kt` (read-only for everyone, inline edit mode for
  Admin/Owner, backed by new `CommunityRulesRepository` on `Settings/CommunityRules`).
  `ui/suggestions/SuggestionsScreen.kt` (submit form with 1 optional photo for everyone;
  Admin/Owner also see the full list with delete, backed by new `SuggestionRepository`).
  `ui/bugreport/BugReportScreen.kt` (submit form with up to 5 photos, same
  picker/compress pattern as chat; Admin/Owner see full list with delete, backed by new
  `BugReportRepository`). All three reachable from the new buttons on `ProfileScreen`.
- [x] Step 7 (done) — Admin panel built (`ui/admin/AdminPanelScreen.kt` +
  `AdminViewModel.kt`), tabbed: **Users** (live-searchable list; expand a row to change
  role — Owner grant/revoke is blocked client-side unless the acting user is already
  Owner, mirroring the `roleUnchanged()` Firestore-rules guard — plus ban toggle, timeout
  presets (1h/6h/24h/72h/clear) via new `AppUser.timedOutUntil`, class-access chips off
  `CLASS_ROOM_OPTIONS`, and tag toggles), **Tags** (create/delete, new `Tag.kt` model +
  `TagRepository`), **Anti-Abuse** (add/remove blocked words, new `AntiAbuseRepository`
  on `Settings/AntiAbuse`), **Notify** (broadcast push via existing
  `NotificationRepository`, `toUid: "all"`). Quick-link buttons at the top jump to
  Rules/Suggestions/Bug-reports for their delete/edit views. Reachable only for
  Admin/Owner via a button on `ProfileScreen`.
  Also fixed while here: `NotificationRepository.send()`/`.delete()` were missing
  `.await()` (fire-and-forget bug — calls returned before the write actually completed).
- [x] Step 8 (done) — FCM token now stored on `Users/{uid}.fcmToken`.
  `UserRepository.updateFcmToken(uid, token)` added (best-effort, swallows failures so a
  cold-start offline device doesn't block sign-in). Two call sites cover both cases:
  1. `MainActivity`'s `AppNavHost` has a `LaunchedEffect(currentUser?.uid)` that fires
     whenever a session becomes active (fresh login, register, or an already-persisted
     session via the `loading` route) and fetches `FirebaseMessaging.getInstance().token`
     to store it.
  2. `StudentHubMessagingService.onNewToken()` (new override) persists later token
     rotations — app reinstall, data cleared, FCM-initiated refresh — for an
     already-signed-in user directly via `FirebaseAuth.getInstance().getUid()`.
  This was the last remaining item from the original build order — all 8 steps are now
  done. Nothing structural left; only the "Known gaps" below (real Gradle build,
  wrapper jar, launcher icon) need attention on your machine.

### Bugs fixed this session (were present in the previous zip, would have broken the build)
1. `app/build.gradle` had `applicationId 'com.studenthub.native_app'` but
   `google-services.json` registers the Android app under package
   `com.studenthub.app` — Gradle would fail with "No matching client found for package
   name". Fixed: applicationId/namespace now `com.studenthub.app`, and all Kotlin
   sources were moved from the `com.studenthub.native_app` package into
   `com.studenthub.app` (unifying with the existing `NotificationSettings.java` /
   `StudentHubMessagingService.java`, which were already in that package).
2. `StudentHubMessagingService.java` built a tap-intent for `WebAppActivity.class`,
   which no longer exists in the native rewrite (compile error). Fixed: now targets
   `MainActivity.class`.

### Known gaps / things to sanity-check on your machine
- No network in this sandbox, so the project could not actually be Gradle-built here —
  review compiles logically (imports, package names, method signatures all cross-checked
  by hand) but run `./gradlew assembleDebug` yourself as the real check.
- `gradle/wrapper/gradle-wrapper.jar` (binary) is not included — regenerate with
  `gradle wrapper --gradle-version 8.9` if Android Studio doesn't do it for you on open.
- ~~No launcher icon (`mipmap/ic_launcher`) is set in the manifest~~ — **done this
  session.** User supplied the StudentChat logo (graduation cap + chat bubble mark,
  "StudentChat" wordmark, dark navy background). Generated: legacy `ic_launcher.png` /
  `ic_launcher_round.png` per density (mdpi–xxxhdpi, full logo incl. wordmark padded
  onto a navy square/circle) plus an API 26+ adaptive icon (`mipmap-anydpi-v26/
  ic_launcher.xml` + `ic_launcher_round.xml`) with a solid-navy `ic_launcher_background`
  layer and an alpha-keyed `ic_launcher_foreground` (mark only, no wordmark — text
  would get clipped by launcher masks, so it's legacy-icon-only) centered in the 66%
  safe zone. `AndroidManifest.xml` now sets `android:icon="@mipmap/ic_launcher"` and
  `android:roundIcon="@mipmap/ic_launcher_round"` on `<application>`.
- `google-services.json`'s `api_key` differs from the `apiKey` in the original prompt's
  Firebase config block — that's expected (the prompt's key was the **web** app's key;
  `google-services.json` correctly carries the **Android** app's own key from the same
  Firebase project). Left as-is.
