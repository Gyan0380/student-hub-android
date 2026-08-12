# StudentHub Native Live

Native Android app using the SAME Firebase project as the StudentHub web app.

## Included
- Native Android UI (no WebView, no React)
- Firebase Email/Password authentication using the web app's username@studentchat.com convention
- Firestore real-time Global, Anonymous and Class chat using the existing `Chats/{room}/Messages` schema
- Live Notifications from `Notifications` collection
- Live Community Rules from `Settings/CommunityRules`
- Admin global notification creation
- Owner role editing and anti-abuse/community-rule controls
- FCM token registration under `Users/{uid}/FcmTokens/{token}`

## Build
Open this folder in Android Studio, let Gradle sync, then Run.
The project uses package `com.studenthub.app` and includes the supplied `google-services.json`.

## Push notifications
The app registers FCM tokens, but server-side sending must be done from a trusted environment. A sample Firebase Cloud Function is included in `functions/index.js`. Deploy it with Firebase CLI after configuring the project. NEVER put a Firebase service-account private key in the APK or web app.

## Important compatibility note
The web project stores chat messages in:
Chats/{chatRoomId}/Messages
and announcements in:
Notifications
The native app intentionally uses the same paths so both clients see the same live data.
