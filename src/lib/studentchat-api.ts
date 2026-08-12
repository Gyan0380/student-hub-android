import { useEffect, useMemo, useState } from "react";
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendPasswordResetEmail,
  setPersistence,
  browserLocalPersistence,
  signInWithEmailAndPassword,
  signOut,
  type User as FirebaseUser,
} from "firebase/auth";
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  Timestamp,
  type DocumentData,
  type QueryDocumentSnapshot,
} from "firebase/firestore";
import { getDownloadURL, ref, uploadBytes } from "firebase/storage";
import { httpsCallable } from "firebase/functions";
import {
  firebaseAuth,
  firebaseDb,
  firebaseFunctions,
  firebaseStorage,
  emailForUsername,
} from "./firebase";
import {
  ANONYMOUS_DISPLAY_NAME,
  classRoomsFor,
  clockLabel,
  relativeLabel,
  type AppNotification,
  type AppUser,
  type BugReport,
  type ChatMessage,
  type Role,
  type Suggestion,
} from "./studentchat-data";

const millis = (v: unknown): number | undefined =>
  v instanceof Timestamp ? v.toMillis() : typeof v === "number" ? v : undefined;

/* ------------------------------------------------------------------ auth --- */

export type Session = {
  loading: boolean;
  authUser: FirebaseUser | null;
  profile: AppUser | null;
};

export function useSession(): Session {
  const [state, setState] = useState<Session>({ loading: true, authUser: null, profile: null });

  useEffect(() => {
    const auth = firebaseAuth();
    let stopProfile: (() => void) | undefined;
    void setPersistence(auth, browserLocalPersistence);

    const stopAuth = onAuthStateChanged(auth, (authUser) => {
      stopProfile?.();
      stopProfile = undefined;
      if (!authUser) {
        setState({ loading: false, authUser: null, profile: null });
        return;
      }
      stopProfile = onSnapshot(
        doc(firebaseDb(), "Users", authUser.uid),
        (snap) => {
          const d = snap.data() ?? {};
          setState({
            loading: false,
            authUser,
            profile: {
              uid: authUser.uid,
              username: (d["username"] as string) ?? authUser.email?.split("@")[0] ?? "user",
              fullName: d["fullName"] as string | undefined,
              dob: d["dob"] as string | undefined,
              classLevel: d["classLevel"] as string | undefined,
              schoolName: d["schoolName"] as string | undefined,
              profilePhoto: d["profilePhoto"] as string | undefined,
              bio: d["bio"] as string | undefined,
              role: ((d["role"] as Role) ?? "Student") as Role,
              classAccess: (d["classAccess"] as string[]) ?? [],
              classRooms: d["classRooms"] as string[] | undefined,
              createdAt: millis(d["createdAt"]),
              isBanned: Boolean(d["isBanned"]),
              timeoutExpiry: millis(d["timeoutExpiry"]) ?? null,
            },
          });
        },
        () => setState({ loading: false, authUser, profile: null }),
      );
    });

    return () => {
      stopProfile?.();
      stopAuth();
    };
  }, []);

  return state;
}

export async function login(username: string, password: string) {
  const auth = firebaseAuth();
  await setPersistence(auth, browserLocalPersistence);
  await signInWithEmailAndPassword(auth, emailForUsername(username), password);
}

export async function register(input: {
  username: string;
  password: string;
  fullName: string;
  classLevel: string;
  schoolName?: string;
  dob?: string;
}) {
  const username = input.username.trim().toLowerCase();
  if (!/^[a-z0-9_.]{3,20}$/.test(username))
    throw new Error("Username 3-20 characters: a-z, 0-9, _ or . only");

  const db = firebaseDb();
  const reserved = await getDoc(doc(db, "Usernames", username));
  if (reserved.exists()) throw new Error("Ye username already taken hai");

  const auth = firebaseAuth();
  await setPersistence(auth, browserLocalPersistence);
  const cred = await createUserWithEmailAndPassword(
    auth,
    emailForUsername(username),
    input.password,
  );
  const uid = cred.user.uid;
  const classAccess = [input.classLevel];

  await setDoc(doc(db, "Users", uid), {
    uid,
    username,
    fullName: input.fullName.trim() || username,
    dob: input.dob ?? "",
    classLevel: input.classLevel,
    schoolName: input.schoolName ?? "",
    profilePhoto: "",
    bio: "",
    role: "Student" as Role,
    classAccess,
    classRooms: classRoomsFor({ classLevel: input.classLevel, classAccess }),
    createdAt: serverTimestamp(),
    isBanned: false,
    timeoutExpiry: null,
  });
  await setDoc(doc(db, "Usernames", username), { uid, createdAt: serverTimestamp() });
}

export const logout = () => signOut(firebaseAuth());

export const resetPassword = (username: string) =>
  sendPasswordResetEmail(firebaseAuth(), emailForUsername(username));

/* ------------------------------------------------------------- chat rooms --- */

function toMessage(d: QueryDocumentSnapshot<DocumentData>): ChatMessage {
  const v = d.data();
  const createdAt = millis(v["createdAt"]);
  const reply = v["replyTo"] as { senderName?: string; text?: string } | undefined;
  return {
    id: d.id,
    senderId: (v["senderId"] as string) ?? "",
    senderName: (v["senderName"] as string) ?? "user",
    senderPhoto: v["senderPhoto"] as string | undefined,
    text: (v["text"] as string) ?? "",
    photos: (v["photos"] as string[]) ?? [],
    photoUrl: v["photoUrl"] as string | undefined,
    createdAt,
    at: clockLabel(createdAt),
    edited: Boolean(v["edited"]),
    ...(reply?.senderName
      ? { replyTo: { senderName: reply.senderName, text: reply.text ?? "" } }
      : {}),
  };
}

/** Real-time listener for a chat room. Detaches on unmount / room change. */
export function useMessages(roomId: string | null) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!roomId) return;
    setMessages([]);
    setError(null);
    const q = query(
      collection(firebaseDb(), "Chats", roomId, "Messages"),
      orderBy("createdAt", "asc"),
      limit(300),
    );
    return onSnapshot(
      q,
      (snap) => setMessages(snap.docs.map(toMessage)),
      (e) => setError(e.message),
    );
  }, [roomId]);

  return { messages, error };
}

export async function sendMessage(
  roomId: string,
  user: AppUser,
  text: string,
  replyTo?: { senderName: string; text: string } | null,
  photos?: string[],
) {
  const anonymous = roomId === "anonymous";
  await addDoc(collection(firebaseDb(), "Chats", roomId, "Messages"), {
    text,
    photos: photos ?? [],
    photoUrl: photos?.[0] ?? "",
    senderId: user.uid,
    senderName: anonymous ? ANONYMOUS_DISPLAY_NAME : user.username,
    senderPhoto: anonymous ? "" : (user.profilePhoto ?? ""),
    createdAt: serverTimestamp(),
    ...(replyTo ? { replyTo } : {}),
  });
}

export const editMessage = (roomId: string, id: string, text: string) =>
  updateDoc(doc(firebaseDb(), "Chats", roomId, "Messages", id), { text, edited: true });

export const deleteMessage = (roomId: string, id: string) =>
  deleteDoc(doc(firebaseDb(), "Chats", roomId, "Messages", id));

export async function uploadChatPhoto(roomId: string, file: File) {
  const path = `chatPhotos/${roomId}/${Date.now()}-${file.name}`;
  const r = ref(firebaseStorage(), path);
  await uploadBytes(r, file);
  return getDownloadURL(r);
}

/* --------------------------------------------------------------- settings --- */

/** Live Settings/CommunityRules → rules array. */
export function useCommunityRules() {
  const [rules, setRules] = useState<string[]>([]);
  useEffect(
    () =>
      onSnapshot(
        doc(firebaseDb(), "Settings", "CommunityRules"),
        (snap) => setRules(((snap.data()?.["rules"] as string[]) ?? []).filter(Boolean)),
        () => setRules([]),
      ),
    [],
  );
  return rules;
}

/** Live Settings/AntiAbuse → words array. */
export function useAntiAbuseWords() {
  const [words, setWords] = useState<string[]>([]);
  useEffect(
    () =>
      onSnapshot(
        doc(firebaseDb(), "Settings", "AntiAbuse"),
        (snap) =>
          setWords(
            ((snap.data()?.["words"] as string[]) ?? []).map((w) => w.toLowerCase()).filter(Boolean),
          ),
        () => setWords([]),
      ),
    [],
  );
  return words;
}

export const saveCommunityRules = (rules: string[]) =>
  setDoc(
    doc(firebaseDb(), "Settings", "CommunityRules"),
    { rules, updatedAt: serverTimestamp() },
    { merge: true },
  );

export const saveAntiAbuseWords = (words: string[]) =>
  setDoc(
    doc(firebaseDb(), "Settings", "AntiAbuse"),
    { words, updatedAt: serverTimestamp() },
    { merge: true },
  );

export const containsAbuse = (text: string, words: string[]) => {
  const t = text.toLowerCase();
  return words.some((w) => w && new RegExp(`(^|[^a-z])${w}([^a-z]|$)`, "i").test(t));
};

/* ---------------------------------------------------------- notifications --- */

export function useNotifications(uid: string | null) {
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  useEffect(() => {
    if (!uid) return;
    const q = query(
      collection(firebaseDb(), "Notifications"),
      where("toUid", "in", ["all", uid]),
      orderBy("createdAt", "desc"),
      limit(100),
    );
    return onSnapshot(
      q,
      (snap) =>
        setNotifications(
          snap.docs.map((d) => {
            const v = d.data();
            const createdAt = millis(v["createdAt"]);
            return {
              id: d.id,
              title: (v["title"] as string) ?? "Announcement",
              body: (v["body"] as string) ?? "",
              toUid: (v["toUid"] as string) ?? "all",
              createdAt,
              at: relativeLabel(createdAt),
              by: (v["sentBy"] as string) ?? "admin",
              photos: (v["photos"] as string[]) ?? [],
            };
          }),
        ),
      () => setNotifications([]),
    );
  }, [uid]);
  return notifications;
}

export const sendGlobalNotification = (input: {
  title: string;
  body: string;
  sentBy: string;
  photos?: string[];
}) =>
  addDoc(collection(firebaseDb(), "Notifications"), {
    toUid: "all",
    title: input.title,
    body: input.body,
    sentBy: input.sentBy,
    photos: input.photos ?? [],
    createdAt: serverTimestamp(),
  });

export const deleteNotification = (id: string) =>
  deleteDoc(doc(firebaseDb(), "Notifications", id));

export async function uploadNotificationPhoto(file: File) {
  const r = ref(firebaseStorage(), `notificationPhotos/${Date.now()}-${file.name}`);
  await uploadBytes(r, file);
  return getDownloadURL(r);
}

/* ------------------------------------------------ suggestions / bugreports --- */

function useFeed<T>(path: "Suggestions" | "BugReports", uid: string | null) {
  const [items, setItems] = useState<T[]>([]);
  useEffect(() => {
    if (!uid) return;
    const q = query(collection(firebaseDb(), path), orderBy("createdAt", "desc"), limit(100));
    return onSnapshot(
      q,
      (snap) =>
        setItems(
          snap.docs.map((d) => {
            const v = d.data();
            const createdAt = millis(v["createdAt"]);
            return {
              id: d.id,
              uid: (v["uid"] as string) ?? "",
              by: (v["username"] as string) ?? "student",
              text: (v["text"] as string) ?? "",
              at: relativeLabel(createdAt),
            } as unknown as T;
          }),
        ),
      () => setItems([]),
    );
  }, [path, uid]);
  return items;
}

export const useSuggestions = (uid: string | null) => useFeed<Suggestion>("Suggestions", uid);
export const useBugReports = (uid: string | null) => useFeed<BugReport>("BugReports", uid);

export const addSuggestion = (user: AppUser, text: string) =>
  addDoc(collection(firebaseDb(), "Suggestions"), {
    uid: user.uid,
    username: user.username,
    text,
    createdAt: serverTimestamp(),
  });

export const addBugReport = (user: AppUser, text: string) =>
  addDoc(collection(firebaseDb(), "BugReports"), {
    uid: user.uid,
    username: user.username,
    text,
    createdAt: serverTimestamp(),
  });

export const deleteSuggestion = (id: string) => deleteDoc(doc(firebaseDb(), "Suggestions", id));
export const deleteBugReport = (id: string) => deleteDoc(doc(firebaseDb(), "BugReports", id));

/* --------------------------------------------------------------- profiles --- */

function toUser(d: QueryDocumentSnapshot<DocumentData>): AppUser {
  const v = d.data();
  return {
    uid: d.id,
    username: (v["username"] as string) ?? d.id,
    fullName: v["fullName"] as string | undefined,
    dob: v["dob"] as string | undefined,
    classLevel: v["classLevel"] as string | undefined,
    schoolName: v["schoolName"] as string | undefined,
    profilePhoto: v["profilePhoto"] as string | undefined,
    bio: v["bio"] as string | undefined,
    role: ((v["role"] as Role) ?? "Student") as Role,
    classAccess: (v["classAccess"] as string[]) ?? [],
    classRooms: v["classRooms"] as string[] | undefined,
    createdAt: millis(v["createdAt"]),
    isBanned: Boolean(v["isBanned"]),
    timeoutExpiry: millis(v["timeoutExpiry"]) ?? null,
  };
}

/** Live list of all registered members (admin panel + member lookup). */
export function useMembers(enabled: boolean) {
  const [members, setMembers] = useState<AppUser[]>([]);
  useEffect(() => {
    if (!enabled) return;
    const q = query(collection(firebaseDb(), "Users"), orderBy("username", "asc"), limit(500));
    return onSnapshot(
      q,
      (snap) => setMembers(snap.docs.map(toUser)),
      () => setMembers([]),
    );
  }, [enabled]);
  return members;
}

/** Look up a single profile by username (live). */
export function useProfileByUsername(username: string | null) {
  const [profile, setProfile] = useState<AppUser | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!username) return;
    setLoading(true);
    const q = query(
      collection(firebaseDb(), "Users"),
      where("username", "==", username.toLowerCase()),
      limit(1),
    );
    return onSnapshot(
      q,
      (snap) => {
        const first = snap.docs[0];
        setProfile(first ? toUser(first) : null);
        setLoading(false);
      },
      () => {
        setProfile(null);
        setLoading(false);
      },
    );
  }, [username]);
  return { profile, loading };
}

/** Fields a student is allowed to change on their own document. */
export const saveOwnProfile = (
  uid: string,
  patch: Partial<Pick<AppUser, "fullName" | "bio" | "schoolName" | "dob" | "profilePhoto">>,
) => updateDoc(doc(firebaseDb(), "Users", uid), patch);

export async function uploadProfilePhoto(uid: string, file: File) {
  const r = ref(firebaseStorage(), `profilePhotos/${uid}.jpg`);
  await uploadBytes(r, file);
  const url = await getDownloadURL(r);
  await updateDoc(doc(firebaseDb(), "Users", uid), { profilePhoto: url });
  return url;
}

/** Owner-only. Enforced by the setUserRole Cloud Function + Firestore rules. */
export async function setUserRole(uid: string, role: Role) {
  const call = httpsCallable<{ uid: string; role: Role }, { ok: boolean }>(
    firebaseFunctions(),
    "setUserRole",
  );
  await call({ uid, role });
}

/** Admin/Owner moderation actions (allowed for admins by Firestore rules). */
export const timeoutUser = (uid: string, minutes: number) =>
  updateDoc(doc(firebaseDb(), "Users", uid), {
    timeoutExpiry: Timestamp.fromMillis(Date.now() + minutes * 60_000),
  });

export const setBanned = (uid: string, isBanned: boolean) =>
  updateDoc(doc(firebaseDb(), "Users", uid), { isBanned });

export const setClassAccess = (uid: string, classAccess: string[], classLevel?: string) =>
  updateDoc(doc(firebaseDb(), "Users", uid), {
    classAccess,
    classRooms: classRoomsFor({ classAccess, ...(classLevel ? { classLevel } : {}) }),
  });

export async function countMembers() {
  const snap = await getDocs(query(collection(firebaseDb(), "Users"), limit(1000)));
  return snap.size;
}

/* ------------------------------------------------------------ web push ---- */

/**
 * Web push is optional: it needs a VAPID key from the Firebase console
 * (Cloud Messaging → Web configuration). Android push works via the native app.
 */
export async function enableWebPush(uid: string, vapidKey: string) {
  const { getMessaging, getToken, isSupported } = await import("firebase/messaging");
  if (!(await isSupported())) throw new Error("Push not supported in this browser");
  const permission = await Notification.requestPermission();
  if (permission !== "granted") throw new Error("Notification permission denied");
  const messaging = getMessaging(firebaseApp0());
  const token = await getToken(messaging, { vapidKey });
  if (!token) throw new Error("Could not get FCM token");
  await setDoc(doc(firebaseDb(), "Users", uid, "FcmTokens", token), {
    enabled: true,
    platform: "web",
    updatedAt: serverTimestamp(),
  });
  return token;
}

// Local alias to avoid an extra import cycle in the messaging path.
function firebaseApp0() {
  return firebaseDb().app;
}

export const isAdminRole = (role?: Role) => role === "Admin" || role === "Owner";

export function useIsAdmin(profile: AppUser | null) {
  return useMemo(() => isAdminRole(profile?.role), [profile?.role]);
}

/* ------------------------------------------------------------------ tags --- */

export type Tag = { id: string; label: string; color: string; type: "class" | "admin" | "custom" };

/** Live Settings/Tags → tags array (admin-managed member tags). */
export function useTags() {
  const [tags, setTags] = useState<Tag[]>([]);
  useEffect(
    () =>
      onSnapshot(
        doc(firebaseDb(), "Settings", "Tags"),
        (snap) => setTags(((snap.data()?.["tags"] as Tag[]) ?? []).filter((t) => t && t.label)),
        () => setTags([]),
      ),
    [],
  );
  return tags;
}

export const saveTags = (tags: Tag[]) =>
  setDoc(doc(firebaseDb(), "Settings", "Tags"), { tags, updatedAt: serverTimestamp() }, { merge: true });
