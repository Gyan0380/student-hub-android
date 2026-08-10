# STUDENT HUB — Native Android Chat (No WebView)

Pure native Java + XML chat app. Real-time cross-platform sync via **Firebase
Realtime Database** (NOT Firestore, NOT WebView).

## ⚠️ Important: Realtime Database vs Firestore

Your existing "STUDENT HUB" web app (based on prior work) uses **Firestore**.
This native app is wired to **Firebase Realtime Database**, because that's
what was requested. These are two completely separate databases inside a
Firebase project — data in one is invisible to the other.

For messages to actually sync between this Android app and your Web App, **one
of these must be true**:
1. Your Web App also reads/writes the same `/messages` node in **Realtime
   Database** (I can give you the JS code for this — just ask), OR
2. You tell me to rebuild this Android app to use **Firestore** instead, to
   match your existing web app exactly.

If you're not sure which one your web app uses, check the Firebase Console
sidebar: "Realtime Database" and "Firestore Database" are separate products.

## Required setup before this builds

1. Go to the [Firebase Console](https://console.firebase.google.com/) → your
   project (or create one).
2. Enable **Realtime Database** (Build → Realtime Database → Create Database).
   Start in test mode while developing, then lock down rules before launch.
3. Add an Android app to the Firebase project with package name:
   `com.studenthub.app`
4. Download the generated **`google-services.json`** file.
5. Place it at: `app/google-services.json` (same folder as `app/build.gradle`).
   This repo does NOT include it — it contains project-specific keys and
   should not be committed publicly if your rules aren't locked down.
6. (Optional but used by this app) Enable **Anonymous** sign-in under
   Authentication → Sign-in method, so `FirebaseAuth.getInstance().signInAnonymously()`
   succeeds. The chat still works even if this fails.

## Realtime Database structure this app uses

```
messages/
  -Nabc123.../
    senderId: "uuid-generated-per-device"
    senderName: "Gyan"
    text: "Hello!"
    timestamp: 1699999999999
```

## Suggested basic security rules (tighten before production)

```json
{
  "rules": {
    "messages": {
      ".read": true,
      ".write": true,
      "$messageId": {
        ".validate": "newData.hasChildren(['senderId','senderName','text','timestamp'])"
      }
    }
  }
}
```

## What changed vs the old WebView version

- `MainActivity.java` — fully rewritten, zero WebView usage. Handles identity
  (SharedPreferences + UUID), Firebase init, listening, and sending.
- `activity_main.xml` — real RecyclerView chat UI (Toolbar, message list,
  input bar) instead of a bare WebView.
- New: `model/ChatMessage.java`, `adapter/MessageAdapter.java`
- New: `item_message_sent.xml`, `item_message_received.xml`, bubble/button
  drawables.
- `app/build.gradle` — added Firebase BoM, Realtime Database, Auth,
  RecyclerView, ConstraintLayout, Material dependencies + the
  `google-services` plugin.
- Root `build.gradle` — added the `com.google.gms.google-services` classpath.
- `.github/workflows/build-apk.yml` — now writes `google-services.json` from
  a `GOOGLE_SERVICES_JSON` GitHub secret before building.

## ⚠️ About `gradle/wrapper/gradle-wrapper.jar` (missing on purpose)

This zip includes `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`
— but **not** `gradle/wrapper/gradle-wrapper.jar`.

That file is a compiled binary blob, and Gradle's own security team advises
**never grabbing a `gradle-wrapper.jar` from an untrusted/third-party source**
(a malicious jar there can run arbitrary code the moment you build). Since I
can't cryptographically verify a binary before handing it to you, generating
it yourself from the official source is the safe move — and it only takes
one command.

**Pick whichever is easiest for you:**

1. **Android Studio (easiest):** Just open this project folder in Android
   Studio. It detects the missing wrapper jar and offers to regenerate it
   automatically (or builds using its own bundled Gradle either way).

2. **Termux / Linux / Mac terminal**, if `gradle` is already installed
   (`pkg install gradle` on Termux):
   ```
   gradle wrapper --gradle-version 8.9
   ```
   This downloads the official jar straight from `services.gradle.org` and
   writes it to `gradle/wrapper/gradle-wrapper.jar`. After that, `./gradlew`
   works normally from then on, including for teammates who `git clone` the
   repo (commit the generated jar once it exists).

3. **GitHub Actions:** already handled — `build-apk.yml` regenerates the
   wrapper jar automatically before every build, so you don't need to do
   anything for CI builds.

## Build locally

```
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

(On first run on Mac/Linux/Termux, if you get a "permission denied" on
`gradlew`, run `chmod +x gradlew` once.)
