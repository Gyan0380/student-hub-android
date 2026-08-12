# StudentHub Firebase Backend (project: `student-a866d`)

This directory documents the Firebase backend. The deployable config files
(`firebase.json`, `.firebaserc`, `firestore.rules`, `firestore.indexes.json`,
`storage.rules`) live at the **repo root** because the Firebase CLI expects
them there. Cloud Functions source lives in `/functions`.

## 1. Manual Firebase Console setup (one-time)

Before deploying, complete these steps in the [Firebase Console](https://console.firebase.google.com/project/student-a866d):

1. **Enable Email/Password authentication**: Authentication → Sign-in method → enable "Email/Password".
2. **Create Firestore in Native mode**: Firestore Database → Create database → choose a region → **Native mode** (not Datastore mode).
3. **Enable Cloud Storage**: Storage → Get started → choose a bucket/region.
4. **Upgrade to the Blaze (pay-as-you-go) plan**: required for Cloud Functions (v2) and outbound network calls (FCM). Project settings → Usage and billing → Modify plan.
5. **Register the Android app** (if using Android): Project settings → Your apps → Add app → Android, provide the applicationId, then download `google-services.json` and place it in the Android app module.
6. **Generate a Web Push (VAPID) key** (if using web push): Project settings → Cloud Messaging → Web configuration → "Web Push certificates" → Generate key pair. Use this key with `getToken()` from the Firebase JS SDK.
7. **(iOS, if applicable)**: upload an APNs auth key under Cloud Messaging settings.

## 2. Deployment

```bash
# One-time: authenticate the CLI
firebase login

# Point the CLI at this project
firebase use student-a866d

# Deploy rules + indexes + functions (storage rules too)
firebase deploy --only firestore:rules,firestore:indexes,storage,functions
```

To deploy pieces individually:

```bash
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
firebase deploy --only storage
firebase deploy --only functions
```

The `functions` predeploy hook runs `npm run build` (TypeScript compile) automatically.
To develop locally: `cd functions && npm install && npm run build:watch`, and use
`firebase emulators:start` for local testing.

## 3. Firestore Security Rules — explanation

Rules live in `firestore.rules`. Key design points:

- **Role model**: `Users/{uid}.role` is one of `Student`, `Teacher`, `Admin`, `Owner`.
  Helper functions `isAdmin()` (Admin or Owner) and `isOwner()` (Owner only) look up
  the caller's own `Users` doc via `get()`.
- **Users/{uid}**:
  - Any signed-in user can **read** any user profile.
  - A user can **create** only their own doc (`uid` must match, must start unbanned).
  - A user can **self-update** only `fullName, dob, schoolName, bio, profilePhoto, classLevel`
    — enforced via `diff(resource.data).affectedKeys().hasOnly([...])` plus explicit
    equality checks that `uid`, `role`, `isBanned`, `timeoutExpiry`, `classAccess` are unchanged.
  - **Admins/Owners** may update `isBanned`, `timeoutExpiry`, `classAccess`, `classRooms` on
    any user (but not their own role, and Admins cannot change `role` at all).
  - Only the **Owner** may change `role` (also exposed as the safer `setUserRole` callable,
    which should be preferred over direct writes).
  - `Users/{uid}/FcmTokens/{token}` sub-collection: only the owning uid may read/write —
    this is where device push tokens are stored, each doc keyed by the token string with
    an `enabled` boolean field.
- **Usernames/{username}**: a uniqueness-reservation collection. Any signed-in user can read
  (to check availability); create is allowed only if the new doc's `uid` matches the caller
  and no doc already exists at that path (prevents overwriting someone else's reservation).
  Only Admin/Owner can update or delete a reservation (e.g. to free up an abandoned username).
- **Chats/{roomId}/Messages/{msgId}** — see the **classRooms** note below for how class-scoped
  rooms are authorized. Rules:
  - **Read**: allowed for `global`/`anonymous` rooms, for Admin/Owner, or if `roomId` is present
    in the caller's `classRooms` array (see below).
  - **Create**: sender must be `request.auth.uid`, must not be banned, `timeoutExpiry` must be
    null/in the past, the target room must be authorized, and `text` must be a string ≤ 2000 chars.
  - **Update**: only the author may edit, and only the `text` field; Admin/Owner may update anything.
  - **Delete**: author or Admin/Owner.
- **Notifications/{id}**: readable by any signed-in user if `toUid == "all"` or `toUid == auth.uid`.
  Create/update/delete restricted to Admin/Owner, and on create `sentBy` must equal the caller's uid.
- **Settings/CommunityRules**, **Settings/AntiAbuse**: readable by any signed-in user, writable
  only by Admin/Owner.
- **Suggestions**, **BugReports**: any signed-in user may create their own (uid must match),
  read is restricted to the author or Admin/Owner, and only Admin/Owner may delete. No updates
  are permitted (immutable once submitted) to prevent tampering after moderation review.
- **Default deny**: a catch-all `match /{document=**}` denies any read/write not explicitly
  allowed above.

### The `classRooms` field (important!)

Firestore Security Rules cannot execute arbitrary string manipulation (slugification) at
evaluation time efficiently/portably, so class-scoped chat access is **precomputed** and
stored directly on each `Users/{uid}` document as an array field:

```
classRooms: ["class-grade-10-a", "class-grade-11-b", ...]
```

This is derived from `classLevel` (single string) and `classAccess` (array of strings) via
`"class-" + slugify(label)`, where `slugify` lowercases, replaces non-alphanumerics with `-`,
and trims leading/trailing dashes. It is kept in sync automatically by:

1. The `onUserWriteSyncClassRooms` Firestore trigger (recomputes on every write to a `Users` doc,
   comparing before writing to avoid infinite trigger loops).
2. The `syncClassRooms` callable function, which a client can invoke to force an immediate
   recompute (e.g. right after an admin changes `classAccess`).

Rules then simply check `roomId in classRooms` — no string logic needed inside `firestore.rules`.

## 4. Storage Security Rules — explanation & caveat

Rules live in `storage.rules`.

- **`profilePhotos/{uid}.*`**: only the matching uid may write (max 5MB, must be an image
  content type); readable by any signed-in user.
- **`chatPhotos/{roomId}/{file}`**: writable by any signed-in, non-banned user (checked via a
  cross-service `firestore.get()` lookup of the caller's `Users` doc); readable by any signed-in user.
- **`notificationPhotos/*`**: intended to be admin-only. **Caveat**: Storage Security Rules have
  no native concept of Firestore custom roles, so this is enforced via a `firestore.get()` call
  into `Users/{uid}` to check `role in ['Admin', 'Owner']`. This works but adds a Firestore read
  (billed, rate-limited) on every Storage rule evaluation for that path — acceptable for a
  low-frequency admin-only upload path, but do not reuse this pattern for high-traffic paths.

## 5. Required Firestore composite indexes

Defined in `firestore.indexes.json` (deployed via `firebase deploy --only firestore:indexes`):

| Collection      | Fields                                   | Purpose                                   |
|------------------|-------------------------------------------|--------------------------------------------|
| `Notifications`  | `toUid` ASC, `createdAt` DESC             | Per-user notification feed, newest first  |
| `Suggestions`    | `createdAt` DESC                          | Admin suggestions list, newest first      |
| `BugReports`     | `createdAt` DESC                          | Admin bug report list, newest first       |
| `Users`          | `username` ASC                            | Username lookup/search                    |
| `Users`          | `role` ASC, `createdAt` DESC              | Admin user management by role             |

`Chats/{roomId}/Messages` ordering by `createdAt` only requires a single-field index, which
Firestore creates automatically — no composite index needed.

## 6. Cloud Functions summary

All functions are defined in `functions/src/index.ts` using Cloud Functions v2 syntax
(`firebase-functions@6`, `firebase-admin@13`, Node 20 runtime):

- **`onNotificationCreated`** (`onDocumentCreated("Notifications/{id}")`): sends an FCM push
  when a `Notifications` doc is created. Resolves target tokens (`toUid == "all"` → collection-group
  query across all `Users/*/FcmTokens` with `enabled == true`; otherwise just that user's tokens),
  sends via `sendEachForMulticast` in batches of 500, deletes token docs that come back
  `messaging/registration-token-not-registered` or `messaging/invalid-argument`. Idempotent via
  both a `pushSentAt` field on the notification doc and a `_pushEvents/{eventId}` marker doc keyed
  by the Cloud Functions event id (protects against at-least-once retry duplication).
- **`onChatMessageCreated`** (`onDocumentCreated("Chats/{roomId}/Messages/{messageId}")`): sends a
  push only for the `global` room, to all users with enabled tokens except the sender, rate-limited
  to skip if the sender posted another message in the same room within the last 30 seconds.
- **`setUserRole`** (callable): only callers whose own `Users` doc has `role == "Owner"` may change
  another user's role; validates the role value against an allow-list; throws `HttpsError`
  (`unauthenticated`, `invalid-argument`, `permission-denied`, `not-found`) as appropriate.
- **`syncClassRooms`** (callable): recomputes and persists the caller's own `classRooms` array
  from `classLevel`/`classAccess`.
- **`onUserWriteSyncClassRooms`** (`onDocumentWritten("Users/{uid}")`): automatically keeps
  `classRooms` in sync on every write to a user doc, comparing the computed array against the
  current one before writing to avoid retrigger loops.

### Local verification

```bash
cd functions
npm install
npx tsc --noEmit   # already verified to pass with no errors
```
