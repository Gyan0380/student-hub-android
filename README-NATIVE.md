# STUDENT HUB — Native Android App (No WebView)

Pure native Java + XML app. Login, Register, Home (options menu), Chat, and
Settings — all built with real Android UI components, zero WebView. Real-time
chat sync with the Web App via **Firebase Firestore** (matching the Web
App's own database, so both platforms share the exact same data).

## What this app now has

- **Login / Register** — same `{username}@studentchat.com` email convention
  and same `Users/{uid}` Firestore document shape as the website
  (`renderRegister()` in `app.js`). An account created on the app logs into
  the website with the same username/password, and vice-versa.
- **Home screen (options menu)** — Global Chat, Anonymous Chat, your Class
  Room, Community Rules, Suggestion Box, Report a Bug, Settings — same list
  and same destinations as the website's home screen.
- **Chat (cross-platform, like Discord)** — reads/writes
  `Chats/{roomId}/Messages` in Firestore, the exact path and field names
  (`senderId`, `senderName`, `senderPhoto`, `text`, `createdAt`) the Web App
  uses. A message sent from the browser appears on the app instantly, and
  a message sent from the app appears on the website instantly.
- **Settings** — edit bio/school, dark mode toggle, logout.
- Theme colors (blue `#2563EB`, background, card, text colors) match the
  website's `style.css` palette.

## What's intentionally NOT included yet (scope of this pass)

To keep this buildable and correct, the following web-only features were
left out — say the word and I'll add any of them next:
- Photo attachments in chat, message reply/edit/delete, @mentions
- Admin Panel (user/role/ban management, tag editor, broadcast notifications)
- Push/browser notifications, unread-dot tracking
- Profile photo upload (registration uses a default avatar for now)

## Required setup before this builds

1. `app/google-services.json` is already included in this zip and points at
   the **same Firebase project as the website** (`student-a866d`) — no
   Firebase Console setup needed for that part.
2. In the [Firebase Console](https://console.firebase.google.com/) →
   your `student-a866d` project → **Firestore Database**, make sure
   Firestore is enabled (it already is, since the website uses it).
3. **Authentication → Sign-in method**: make sure **Email/Password** is
   enabled (the website's login already needs this, so it likely already is).
4. Check your **Firestore security rules** allow a logged-in user to read/write
   `Users/{uid}`, `Chats/{roomId}/Messages`, `Suggestions`, `BugReports`, and
   read `Settings/CommunityRules`. If your rules are still in the Firebase
   default "test mode", this all works out of the box.

## ⚠️ About `gradle/wrapper/gradle-wrapper.jar` (missing on purpose)

This zip includes `gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.properties` — but **not**
`gradle/wrapper/gradle-wrapper.jar`. That file is a compiled binary blob;
never grab one from an untrusted source. Generate it yourself — it's one
command, or Android Studio does it automatically:

1. **Android Studio (easiest):** open this project folder. It offers to
   regenerate the wrapper jar automatically.
2. **Termux / Linux / Mac**, if `gradle` is installed:
   ```
   gradle wrapper --gradle-version 8.9
   ```
3. **GitHub Actions:** `.github/workflows/build-apk.yml` already regenerates
   it before every CI build.

## Build locally

```
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

(First run on Mac/Linux/Termux: `chmod +x gradlew` if you get "permission denied".)

## Cross-platform notification sync added
- Native Notifications screen reads the same `Notifications` collection as the web app.
- Admin/Owner can create an announcement from Android; the web app's existing announcement listener sees the same Firestore document.
- Chat messages already use the shared `Chats/{roomId}/Messages` path.
- No WebView is used.

For true OS-level Android push while the app is closed, Firebase Cloud Messaging still needs a trusted sender (for example Cloud Functions/Admin SDK) to write/send FCM messages. The Firestore notification feed itself is already shared cross-platform.
