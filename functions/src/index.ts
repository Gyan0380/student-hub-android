import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging, type MulticastMessage } from "firebase-admin/messaging";
import {
  onDocumentCreated,
  onDocumentWritten,
} from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2";

initializeApp();

const db = getFirestore();
const messaging = getMessaging();

// ---------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------

/** Convert an arbitrary label into a URL/id-safe slug, e.g. "Grade 10 - A" -> "grade-10-a" */
function slugify(input: string): string {
  return input
    .toString()
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/** Compute the set of chat room ids a user is authorized to access from classLevel/classAccess. */
function computeClassRooms(data: FirebaseFirestore.DocumentData | undefined): string[] {
  if (!data) return [];
  const labels: string[] = [];
  if (typeof data.classLevel === "string" && data.classLevel.trim().length > 0) {
    labels.push(data.classLevel);
  }
  if (Array.isArray(data.classAccess)) {
    for (const entry of data.classAccess) {
      if (typeof entry === "string" && entry.trim().length > 0) {
        labels.push(entry);
      }
    }
  }
  const rooms = new Set<string>();
  for (const label of labels) {
    rooms.add(`class-${slugify(label)}`);
  }
  return Array.from(rooms).sort();
}

function arraysEqual(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) return false;
  }
  return true;
}

interface TokenDocRef {
  ref: FirebaseFirestore.DocumentReference;
  token: string;
}

/** Fetch all enabled FCM token docs for a specific user. */
async function getUserTokens(uid: string): Promise<TokenDocRef[]> {
  const snap = await db
    .collection("Users")
    .doc(uid)
    .collection("FcmTokens")
    .where("enabled", "==", true)
    .get();
  return snap.docs.map((d) => ({ ref: d.ref, token: d.id }));
}

/** Fetch all enabled FCM token docs across all users via a collection group query. */
async function getAllTokens(): Promise<TokenDocRef[]> {
  const snap = await db
    .collectionGroup("FcmTokens")
    .where("enabled", "==", true)
    .get();
  return snap.docs.map((d) => ({ ref: d.ref, token: d.id }));
}

/** Chunk an array into fixed-size batches (FCM multicast max is 500 tokens). */
function chunk<T>(items: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    out.push(items.slice(i, i + size));
  }
  return out;
}

/** Send a multicast push in batches of 500, cleaning up invalid tokens afterward. */
async function sendPushToTokens(
  tokenDocs: TokenDocRef[],
  buildMessage: (tokens: string[]) => MulticastMessage
): Promise<void> {
  const batches = chunk(tokenDocs, 500);
  const invalidRefs: FirebaseFirestore.DocumentReference[] = [];

  for (const batch of batches) {
    const tokens = batch.map((t) => t.token);
    if (tokens.length === 0) continue;
    const message = buildMessage(tokens);
    const response = await messaging.sendEachForMulticast(message);

    response.responses.forEach((res, idx) => {
      if (!res.success) {
        const code = res.error?.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-argument"
        ) {
          invalidRefs.push(batch[idx].ref);
        }
      }
    });
  }

  if (invalidRefs.length > 0) {
    const batchWrite = db.batch();
    invalidRefs.forEach((ref) => batchWrite.delete(ref));
    await batchWrite.commit();
    logger.info(`Cleaned up ${invalidRefs.length} invalid FCM token(s).`);
  }
}

// ---------------------------------------------------------------------
// onDocumentCreated("Notifications/{id}") -> send FCM push
// ---------------------------------------------------------------------

export const onNotificationCreated = onDocumentCreated(
  "Notifications/{id}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const notificationId = event.params.id as string;
    const eventId = event.id;

    // Idempotency guard #1: dedicated event marker doc (protects against
    // Cloud Functions' at-least-once delivery retries).
    const eventRef = db.collection("_pushEvents").doc(eventId);
    const eventDoc = await eventRef.get();
    if (eventDoc.exists) {
      logger.info(`Push event ${eventId} already processed. Skipping.`);
      return;
    }

    const data = snap.data();

    // Idempotency guard #2: field on the notification doc itself.
    if (data.pushSentAt) {
      await eventRef.set({ processedAt: FieldValue.serverTimestamp() });
      logger.info(`Notification ${notificationId} already has pushSentAt. Skipping.`);
      return;
    }

    const toUid: string | undefined = data.toUid;
    const title: string = data.title ?? "StudentHub";
    const body: string = data.body ?? "";

    if (!toUid) {
      logger.warn(`Notification ${notificationId} missing toUid.`);
      await eventRef.set({ processedAt: FieldValue.serverTimestamp() });
      return;
    }

    const tokenDocs =
      toUid === "all" ? await getAllTokens() : await getUserTokens(toUid);

    if (tokenDocs.length > 0) {
      await sendPushToTokens(tokenDocs, (tokens) => ({
        tokens,
        notification: { title, body },
        data: {
          route: "notifications",
          notificationId,
        },
      }));
    } else {
      logger.info(`No tokens found for notification ${notificationId} (toUid=${toUid}).`);
    }

    await snap.ref.update({ pushSentAt: FieldValue.serverTimestamp() });
    await eventRef.set({ processedAt: FieldValue.serverTimestamp() });
  }
);

// ---------------------------------------------------------------------
// onDocumentCreated("Chats/{roomId}/Messages/{messageId}")
// Push for the "global" room only, rate-limited per sender.
// ---------------------------------------------------------------------

export const onChatMessageCreated = onDocumentCreated(
  "Chats/{roomId}/Messages/{messageId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const roomId = event.params.roomId as string;
    if (roomId !== "global") return;

    const message = snap.data();
    const senderId: string | undefined = message.senderId;
    if (!senderId) return;

    // Rate limit: skip if the sender posted another message in this room
    // within the last 30 seconds.
    const recentSnap = await db
      .collection("Chats")
      .doc(roomId)
      .collection("Messages")
      .where("senderId", "==", senderId)
      .orderBy("createdAt", "desc")
      .limit(2)
      .get();

    if (recentSnap.size >= 2) {
      const docs = recentSnap.docs;
      const latest = docs[0].id === snap.id ? docs[1] : docs[0];
      const latestData = latest.data();
      const latestCreatedAt: Timestamp | undefined = latestData.createdAt;
      if (latestCreatedAt) {
        const diffMs = Date.now() - latestCreatedAt.toMillis();
        if (diffMs < 30_000) {
          logger.info(`Rate limiting push for sender ${senderId} in room ${roomId}.`);
          return;
        }
      }
    }

    const allTokens = await getAllTokens();
    const tokenDocs = allTokens.filter((t) => t.ref.path.split("/")[1] !== senderId);

    if (tokenDocs.length === 0) return;

    const preview: string =
      typeof message.text === "string" ? message.text.slice(0, 120) : "New message";

    await sendPushToTokens(tokenDocs, (tokens) => ({
      tokens,
      notification: {
        title: "New message in Global Chat",
        body: preview,
      },
      data: {
        route: "chat",
        roomId,
      },
    }));
  }
);

// ---------------------------------------------------------------------
// callable: setUserRole
// ---------------------------------------------------------------------

const VALID_ROLES = ["Student", "Teacher", "Admin", "Owner"] as const;
type ValidRole = (typeof VALID_ROLES)[number];

export const setUserRole = onCall(async (request) => {
  const auth = request.auth;
  if (!auth) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }

  const { uid, role } = request.data as { uid?: string; role?: string };

  if (!uid || typeof uid !== "string") {
    throw new HttpsError("invalid-argument", "uid is required.");
  }
  if (!role || !VALID_ROLES.includes(role as ValidRole)) {
    throw new HttpsError(
      "invalid-argument",
      `role must be one of: ${VALID_ROLES.join(", ")}`
    );
  }

  const callerSnap = await db.collection("Users").doc(auth.uid).get();
  const callerRole = callerSnap.data()?.role;

  if (callerRole !== "Owner") {
    throw new HttpsError(
      "permission-denied",
      "Only an Owner may change user roles."
    );
  }

  const targetRef = db.collection("Users").doc(uid);
  const targetSnap = await targetRef.get();
  if (!targetSnap.exists) {
    throw new HttpsError("not-found", `User ${uid} does not exist.`);
  }

  await targetRef.update({ role });

  return { success: true, uid, role };
});

// ---------------------------------------------------------------------
// callable: syncClassRooms
// ---------------------------------------------------------------------

export const syncClassRooms = onCall(async (request) => {
  const auth = request.auth;
  if (!auth) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }

  const userRef = db.collection("Users").doc(auth.uid);
  const userSnap = await userRef.get();
  if (!userSnap.exists) {
    throw new HttpsError("not-found", "User document does not exist.");
  }

  const data = userSnap.data();
  const newRooms = computeClassRooms(data);
  const currentRooms: string[] = Array.isArray(data?.classRooms)
    ? data!.classRooms
    : [];

  if (!arraysEqual(newRooms, currentRooms)) {
    await userRef.update({ classRooms: newRooms });
  }

  return { success: true, classRooms: newRooms };
});

// ---------------------------------------------------------------------
// trigger: keep classRooms in sync automatically on Users writes
// ---------------------------------------------------------------------

export const onUserWriteSyncClassRooms = onDocumentWritten(
  "Users/{uid}",
  async (event) => {
    const after = event.data?.after;
    if (!after || !after.exists) return; // deleted

    const afterData = after.data();
    const newRooms = computeClassRooms(afterData);
    const currentRooms: string[] = Array.isArray(afterData?.classRooms)
      ? afterData!.classRooms
      : [];

    // Guard against infinite loops: only write when the computed value differs.
    if (arraysEqual(newRooms, currentRooms)) {
      return;
    }

    await after.ref.update({ classRooms: newRooms });
  }
);
