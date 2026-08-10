# StudentHub Android v2.0

This project wraps the supplied StudentHub web application into an Android app.

## Modes
- Online Mode: opens the supplied Firebase-backed StudentHub web app. It uses the same web code/data paths.
- Demo Mode: opens `app/src/main/assets/studenthub/demo.html`. It contains fake users/messages/profile/notifications/admin UI and does NOT import or initialize Firebase. Demo actions are kept in JavaScript memory and reset when the demo is left.

## Important
The Android APK itself does not include the Firebase Android SDK. Firebase is only part of the Online web mode because the supplied web app imports the Firebase Web SDK from Google's CDN.
Demo Mode never loads `firebase-config.js` or `app.js`.

## Build
Open this folder in Android Studio and run Gradle Sync, then Build > Build APK(s).
The app version is 2.0 / versionCode 20.

## Main files
- `app/src/main/assets/studenthub/index.html` — online web entry
- `app/src/main/assets/studenthub/app.js` — online StudentHub features
- `app/src/main/assets/studenthub/firebase-config.js` — online Firebase config
- `app/src/main/assets/studenthub/demo.html` — Firebase-free demo
- `LoginActivity.java` — mode selector
- `WebAppActivity.java` — secure local WebView using WebViewAssetLoader
