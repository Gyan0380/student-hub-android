# StudentHub — Native Android App

Real native Android client (Kotlin + Jetpack Compose + Material 3, MVVM) sharing the
**same Firebase project (`student-a866d`)** as the StudentHub web app: same Authentication,
same Firestore database, same Storage bucket, same Cloud Messaging. No WebView, no mock data.

## 1. Add your `google-services.json`

1. Firebase Console → project **student-a866d** → Project settings → **Your apps** → Add app → Android.
2. Package name: `com.studenthub.app`.
3. Download `google-services.json` and place it at:

   ```
   android/app/google-services.json
   ```

   A documented template lives at `app/google-services.json.EXAMPLE`. The build fails without the real file.
4. (Optional but recommended) add your debug SHA-1 in the console so password reset emails / future
   Google sign-in work.

## 2. Build & run

```bash
cd android
./gradlew assembleDebug        # APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected device/emulator
```

Requires JDK 17 and Android SDK 34+.

## 3. Architecture

```
data/model        Firestore-mapped models (User, ChatMessage, AppNotification, FeedbackItem)
data/repository   Auth, User, Chat, Notification, Settings, Feedback, FcmToken (snapshot listeners)
ui/screens        Splash, Login, Register, Home, Chat, Notifications, Rules, Profile,
                  Suggestions, BugReport, AdminPanel
ui/viewmodel      One ViewModel per screen, StateFlow + stateIn(viewModelScope)
ui/NavGraph.kt    Navigation-Compose graph + bottom navigation
ui/theme          Aurora Dark theme (bg #08080D, primary #9B8CFF, secondary #52E2D0)
fcm               StudentHubMessagingService — receives pushes sent by Cloud Functions
```

## 4. Shared backend

- Login uses `username` → `username@studentchat.com` internally, identical to the web app,
  so one account works on both clients.
- Chat rooms use the same ids as the web app: `global`, `anonymous`, `class-<slug>`.
- Firestore Security Rules, indexes, Storage rules and the FCM Cloud Functions live in the
  repository root (`firestore.rules`, `storage.rules`, `functions/`). Deploy them with
  `firebase deploy` before shipping the app.
- Messages sent on Android appear instantly on web and vice versa (Firestore snapshot listeners).
- Admin notifications written to the `Notifications` collection trigger a Cloud Function that
  pushes FCM messages to every registered Android device.
