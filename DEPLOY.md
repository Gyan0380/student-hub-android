# One-time setup for live FCM push

The Android app already registers FCM tokens. To actually send Android push notifications when the existing web Admin Panel creates a Firestore `Notifications` document, deploy the included Cloud Functions from a machine with Node.js 20 and Firebase CLI.

1. Install Firebase CLI.
2. Run `firebase login`.
3. From this project folder run `firebase use student-a866d` if needed.
4. Run `cd functions && npm install`.
5. Run `firebase deploy --only functions`.

The functions send push notifications for:
- Admin `Notifications` documents (`toUid: all` or a user UID).
- New messages in `Chats/{roomId}/Messages` for global, anonymous, class, and admin rooms.

Do not put a Firebase service-account private key into the Android project or website.
