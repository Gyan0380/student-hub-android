export type Role = "Student" | "Admin" | "Owner";

/** Users/{uid} document. Field names match the existing Firestore structure. */
export type AppUser = {
  uid: string;
  fullName?: string | undefined;
  username: string;
  dob?: string | undefined;
  classLevel?: string | undefined;
  schoolName?: string | undefined;
  profilePhoto?: string | undefined;
  bio?: string | undefined;
  role: Role;
  classAccess: string[];
  /** Denormalised chat room ids the user may write to (kept in sync by Cloud Functions). */
  classRooms?: string[] | undefined;
  createdAt?: number | undefined;
  isBanned?: boolean | undefined;
  timeoutExpiry?: number | null | undefined;
};

/** Chats/{roomId}/Messages/{messageId} document. */
export type ChatMessage = {
  id: string;
  senderId: string;
  senderName: string;
  senderPhoto?: string | undefined;
  text: string;
  photos?: string[] | undefined;
  photoUrl?: string | undefined;
  createdAt?: number | undefined;
  /** Rendered clock label derived from createdAt. */
  at: string;
  edited?: boolean | undefined;
  replyTo?: { senderName: string; text: string } | undefined;
};

/** Notifications/{id} document. */
export type AppNotification = {
  id: string;
  title: string;
  body: string;
  toUid: string;
  createdAt?: number | undefined;
  /** Rendered relative label. */
  at: string;
  by: string;
  photos?: string[] | undefined;
};

export type Suggestion = { id: string; uid: string; by: string; text: string; at: string };
export type BugReport = { id: string; uid: string; by: string; text: string; at: string };

export const ANONYMOUS_DISPLAY_NAME = "Anonymous Ninja";

export const CLASS_ROOM_OPTIONS = [
  ...Array.from({ length: 12 }, (_, i) => `Class ${i + 1}`),
  "12th Pass / College",
];

export const slugifyClassName = (name: string) =>
  name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");

/** Deterministic class room id, e.g. "Class 9" -> "class-class-9". */
export const classRoomId = (className: string) => `class-${slugifyClassName(className)}`;

export const classRoomsFor = (user: Pick<AppUser, "classLevel" | "classAccess">) => {
  const names = [...(user.classAccess ?? [])];
  if (user.classLevel && !names.includes(user.classLevel)) names.unshift(user.classLevel);
  return Array.from(new Set(names.map(classRoomId)));
};

export const displayNameFor = (roomId: string) => {
  if (roomId === "global") return "Global Chat";
  if (roomId === "anonymous") return "Anonymous Chat";
  if (roomId === "admin-room") return "Admin Room";
  return roomId
    .replace(/^class-/, "")
    .replace(/-/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
};

export const roomIdsFor = (user: Pick<AppUser, "classLevel" | "classAccess" | "role">) => {
  const rooms = ["global", "anonymous", ...classRoomsFor(user)];
  if (user.role === "Admin" || user.role === "Owner") rooms.push("admin-room");
  return rooms;
};

export const themeOptions = ["Aurora", "System", "Light", "Dark", "Sepia", "Ocean"];

export const timeoutPresets = [1, 4, 10, 30, 60, 1440];

export const formatTimeout = (mins: number) =>
  mins >= 1440
    ? `${Math.round(mins / 1440)}d`
    : mins >= 60
      ? `${Math.round(mins / 60)}h`
      : `${mins}m`;

export const MAX_NOTIF_PHOTOS = 4;

export const clockLabel = (ms?: number) => {
  if (!ms) return "now";
  return new Date(ms).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
};

export const relativeLabel = (ms?: number) => {
  if (!ms) return "just now";
  const diff = Date.now() - ms;
  const m = Math.round(diff / 60000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.round(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.round(h / 24);
  return d === 1 ? "Yesterday" : new Date(ms).toLocaleDateString();
};

export const defaultRules = [
  "Kisi ke saath gaali-galoch ya bullying allowed nahi hai.",
  "Personal details (address, phone number) share na karein.",
  "Sirf apni class ke room mein message bhej sakte ho; doosri class ke rooms read-only hain.",
  "Spam ya baar-baar same message bhejna ban ka reason ban sakta hai.",
  "Admin/Owner ke decisions final hain.",
];
