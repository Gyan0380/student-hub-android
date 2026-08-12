import { useEffect, useMemo, useRef, useState } from "react";
import {
  Bell,
  Bug,
  Clock,
  CornerUpLeft,
  Eye,
  EyeOff,
  Image as ImageIcon,
  Lightbulb,
  Loader2,
  Lock,
  Pencil,
  Send,
  Shield,
  ShieldCheck,
  Trash2,
  User,
  X,
} from "lucide-react";
import { SplashScreen } from "./SplashScreen";
import { AdminPanel } from "./AdminPanel";
import {
  Card,
  Field,
  OutlineRow,
  PrimaryButton,
  ScreenShell,
  Sheet,
  SheetAction,
  TopBar,
} from "./ui";
import logo from "@/assets/studentchat-logo.jpg.asset.json";
import {
  ANONYMOUS_DISPLAY_NAME,
  CLASS_ROOM_OPTIONS,
  classRoomId,
  classRoomsFor,
  displayNameFor,
  formatTimeout,
  roomIdsFor,
  themeOptions,
  timeoutPresets,
  type AppUser,
  type ChatMessage,
} from "@/lib/studentchat-data";
import {
  addBugReport,
  addSuggestion,
  containsAbuse,
  deleteMessage,
  editMessage,
  isAdminRole,
  login,
  logout,
  register,
  resetPassword,
  saveOwnProfile,
  sendMessage,
  timeoutUser,
  uploadChatPhoto,
  uploadProfilePhoto,
  useAntiAbuseWords,
  useBugReports,
  useCommunityRules,
  useMessages,
  useNotifications,
  useProfileByUsername,
  useSession,
  useSuggestions,
} from "@/lib/studentchat-api";

type Screen =
  | { name: "login" }
  | { name: "register" }
  | { name: "home" }
  | { name: "chat"; roomId: string }
  | { name: "notifications" }
  | { name: "profile" }
  | { name: "userprofile"; username: string; from: Screen }
  | { name: "rules" }
  | { name: "suggestions" }
  | { name: "bugreport" }
  | { name: "admin" };

/* ------------------------------------------------------------------ auth --- */

function AuthScreen({
  mode,
  go,
  notify,
}: {
  mode: "login" | "register";
  go: (s: Screen) => void;
  notify: (msg: string) => void;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [school, setSchool] = useState("");
  const [dob, setDob] = useState("");
  const [klass, setKlass] = useState("Class 10");
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (!username.trim() || !password) {
      notify("Username aur password zaroori hai");
      return;
    }
    setBusy(true);
    try {
      if (mode === "login") {
        await login(username, password);
      } else {
        await register({
          username,
          password,
          fullName,
          classLevel: klass,
          schoolName: school,
          dob,
        });
      }
      // Auth state listener switches the screen automatically.
    } catch (e) {
      notify(e instanceof Error ? e.message.replace("Firebase: ", "") : "Something went wrong");
    } finally {
      setBusy(false);
    }
  };

  const forgot = async () => {
    if (!username.trim()) {
      notify("Pehle username likhein");
      return;
    }
    try {
      await resetPassword(username);
      notify("Password reset email bhej diya gaya");
    } catch (e) {
      notify(e instanceof Error ? e.message.replace("Firebase: ", "") : "Reset failed");
    }
  };

  return (
    <div className="aurora-backdrop flex min-h-full flex-1 flex-col justify-center px-6 py-10">
      <div className="glass rounded-[28px] p-6">
        <img src={logo.url} alt="StudentHub" className="mx-auto w-32 rounded-2xl" />
        <h2 className="mt-5 text-center text-2xl font-extrabold tracking-tight">
          {mode === "login" ? "Welcome back" : "Create account"}
        </h2>
        <p className="mt-1 text-center text-sm text-muted-foreground">
          Username se login karein — same account as the Android app.
        </p>

        <div className="mt-6 space-y-4">
          <Field label="Username" value={username} onChange={setUsername} placeholder="yourname" />
          <div className="relative">
            <Field
              label="Password"
              value={password}
              onChange={setPassword}
              type={show ? "text" : "password"}
              placeholder="••••••••"
            />
            <button
              onClick={() => setShow((s) => !s)}
              aria-label="Toggle password"
              className="absolute right-3 top-8 text-muted-foreground"
            >
              {show ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
          {mode === "register" && (
            <>
              <Field
                label="Full name"
                value={fullName}
                onChange={setFullName}
                placeholder="Aarav Sharma"
              />
              <Field
                label="School"
                value={school}
                onChange={setSchool}
                placeholder="Delhi Public School"
              />
              <Field label="Date of birth" value={dob} onChange={setDob} type="date" />
              <label className="block space-y-1.5">
                <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Your class (sirf ek chun sakte ho)
                </span>
                <select
                  value={klass}
                  onChange={(e) => setKlass(e.target.value)}
                  className="w-full rounded-2xl border border-border bg-input/60 px-4 py-3 text-sm text-foreground outline-none focus:border-primary"
                >
                  {CLASS_ROOM_OPTIONS.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
                <span className="block text-[11px] text-muted-foreground">
                  Aap sirf {klass} room, Global aur Anonymous mein chat kar paayenge.
                </span>
              </label>
            </>
          )}
          <PrimaryButton onClick={submit}>
            {busy ? (
              <span className="inline-flex items-center gap-2">
                <Loader2 className="size-4 animate-spin" /> Please wait…
              </span>
            ) : mode === "login" ? (
              "Log in"
            ) : (
              "Register"
            )}
          </PrimaryButton>
          <div className="flex items-center justify-between text-xs font-semibold">
            <button
              className="text-primary"
              onClick={() => go({ name: mode === "login" ? "register" : "login" })}
            >
              {mode === "login" ? "Create an account" : "I already have an account"}
            </button>
            <button className="text-muted-foreground" onClick={forgot}>
              Forgot password?
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ home --- */

function HomeScreen({
  me,
  go,
  unreadCount,
}: {
  me: AppUser;
  go: (s: Screen) => void;
  unreadCount: number;
}) {
  const rooms = useMemo(() => roomIdsFor(me), [me]);
  const myRooms = useMemo(() => classRoomsFor(me), [me]);
  const lockedClassRooms = useMemo(
    () => CLASS_ROOM_OPTIONS.map(classRoomId).filter((r) => !myRooms.includes(r)),
    [myRooms],
  );

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <TopBar
        title="StudentHub"
        subtitle={`@${me.username}${me.classLevel ? ` · ${me.classLevel}` : ""}`}
        right={
          <div className="flex items-center gap-1">
            <button
              onClick={() => go({ name: "notifications" })}
              aria-label="Notifications"
              className="relative rounded-full p-2 text-muted-foreground hover:text-foreground"
            >
              <Bell className="size-5" />
              {unreadCount > 0 && (
                <span className="absolute right-1 top-1 grid min-w-4 place-items-center rounded-full bg-accent px-1 text-[9px] font-black text-background">
                  {unreadCount}
                </span>
              )}
            </button>
            <button
              onClick={() => go({ name: "profile" })}
              aria-label="Profile"
              className="rounded-full p-2 text-muted-foreground hover:text-foreground"
            >
              <User className="size-5" />
            </button>
          </div>
        }
      />
      <div className="divide-y divide-border/50">
        {rooms.map((roomId) => (
          <button
            key={roomId}
            onClick={() => go({ name: "chat", roomId })}
            className="flex w-full items-center gap-3 px-4 py-3.5 text-left transition-colors hover:bg-muted/40"
          >
            <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-primary-container text-sm font-bold text-primary">
              {displayNameFor(roomId).slice(0, 2).toUpperCase()}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[15px] font-semibold">
                {displayNameFor(roomId)}
              </span>
              <span className="block truncate text-xs text-muted-foreground">
                {roomId === "global"
                  ? "Sab students ke saath baat karein"
                  : roomId === "anonymous"
                    ? "Bina naam ke apni baat kahein"
                    : roomId === "admin-room"
                      ? "Admins & Owners only"
                      : "Aapki class ka private room"}
              </span>
            </span>
          </button>
        ))}
      </div>
      <div className="p-4">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Other classes (read-only)
        </p>
        <div className="flex flex-wrap gap-2">
          {lockedClassRooms.map((r) => (
            <button
              key={r}
              onClick={() => go({ name: "chat", roomId: r })}
              className="flex items-center gap-1.5 rounded-full border border-border px-3 py-1.5 text-xs font-semibold text-muted-foreground"
            >
              <Lock className="size-3" /> {displayNameFor(r)}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ chat --- */

function Bubble({
  m,
  me,
  onLongPress,
  onReply,
  onOpenProfile,
}: {
  m: ChatMessage;
  me: AppUser;
  onLongPress: () => void;
  onReply: () => void;
  onOpenProfile: (username: string) => void;
}) {
  const mine = m.senderId === me.uid;
  const anon = m.senderName === ANONYMOUS_DISPLAY_NAME;
  const mentioned = !mine && m.text.toLowerCase().includes(`@${me.username}`);
  const [dx, setDx] = useState(0);
  const start = useRef<{ x: number; y: number } | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clear = () => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  };

  return (
    <div
      className={`select-none touch-pan-y transition-transform ${mine ? "ml-8" : "mr-8"}`}
      style={{ transform: `translateX(${dx}px)` }}
      onContextMenu={(e) => {
        e.preventDefault();
        onLongPress();
      }}
      onPointerDown={(e) => {
        start.current = { x: e.clientX, y: e.clientY };
        clear();
        timer.current = setTimeout(onLongPress, 450);
      }}
      onPointerMove={(e) => {
        if (!start.current) return;
        const delta = e.clientX - start.current.x;
        if (Math.abs(delta) > 6) clear();
        setDx(Math.max(-16, Math.min(72, delta)));
      }}
      onPointerUp={() => {
        clear();
        if (dx > 48) onReply();
        setDx(0);
        start.current = null;
      }}
      onPointerCancel={() => {
        clear();
        setDx(0);
        start.current = null;
      }}
    >
      <div className={`glass-soft rounded-3xl px-4 py-3 ${mentioned ? "border-primary/60" : ""}`}>
        <button
          onClick={() => !anon && onOpenProfile(m.senderName)}
          className={`text-[11px] font-bold uppercase tracking-wide ${
            anon ? "text-muted-foreground" : "text-secondary underline-offset-2 hover:underline"
          }`}
        >
          {m.senderName}
        </button>
        {m.replyTo && (
          <p className="mt-1 flex items-center gap-1 rounded-xl bg-muted/60 px-2 py-1 text-[11px] text-muted-foreground">
            <CornerUpLeft className="size-3" />
            {m.replyTo.senderName}: {m.replyTo.text}
          </p>
        )}
        {m.photos && m.photos.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-2">
            {m.photos.map((p) => (
              <img key={p} src={p} alt="Shared attachment" className="size-24 rounded-xl object-cover" />
            ))}
          </div>
        )}
        {m.text && (
          <p className="mt-1 text-sm leading-relaxed">
            {m.text.split(/(@\w+)/g).map((part, i) =>
              part.startsWith("@") ? (
                <span key={i} className="font-bold text-primary">
                  {part}
                </span>
              ) : (
                part
              ),
            )}
            {m.edited && <span className="text-xs text-muted-foreground"> (edited)</span>}
          </p>
        )}
        <p className="mt-1 text-right text-[10px] text-muted-foreground">{m.at}</p>
      </div>
    </div>
  );
}

function ChatScreen({
  roomId,
  me,
  go,
  notify,
}: {
  roomId: string;
  me: AppUser;
  go: (s: Screen) => void;
  notify: (msg: string) => void;
}) {
  const admin = isAdminRole(me.role);
  const allowedRooms = useMemo(() => roomIdsFor(me), [me]);
  const timedOut = Boolean(me.timeoutExpiry && me.timeoutExpiry > Date.now());
  const readOnly = (!allowedRooms.includes(roomId) && !admin) || Boolean(me.isBanned) || timedOut;

  const { messages, error } = useMessages(roomId);
  const badWords = useAntiAbuseWords();
  const [input, setInput] = useState("");
  const [replyTo, setReplyTo] = useState<ChatMessage | null>(null);
  const [editing, setEditing] = useState<ChatMessage | null>(null);
  const [sheetFor, setSheetFor] = useState<ChatMessage | null>(null);
  const [timeoutFor, setTimeoutFor] = useState<ChatMessage | null>(null);
  const [customMins, setCustomMins] = useState("2");
  const [sending, setSending] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages.length]);

  const send = async () => {
    const text = input.trim();
    if (!text || sending) return;
    if (containsAbuse(text, badWords)) {
      notify("Ye message community rules ke against hai");
      return;
    }
    setSending(true);
    try {
      if (editing) {
        await editMessage(roomId, editing.id, text);
        setEditing(null);
      } else {
        await sendMessage(roomId, me, text, replyTo ? { senderName: replyTo.senderName, text: replyTo.text.slice(0, 60) } : null);
        setReplyTo(null);
      }
      setInput("");
    } catch (e) {
      notify(e instanceof Error ? e.message : "Message bhejne mein problem hui");
    } finally {
      setSending(false);
    }
  };

  const attach = async (file: File | undefined) => {
    if (!file) return;
    setSending(true);
    try {
      const url = await uploadChatPhoto(roomId, file);
      await sendMessage(roomId, me, input.trim(), null, [url]);
      setInput("");
    } catch (e) {
      notify(e instanceof Error ? e.message : "Photo upload failed");
    } finally {
      setSending(false);
    }
  };

  const startReply = (m: ChatMessage) => {
    setEditing(null);
    setReplyTo(m);
    setSheetFor(null);
  };

  const applyTimeout = async (mins: number, target: ChatMessage) => {
    try {
      await timeoutUser(target.senderId, mins);
      notify(`@${target.senderName} timed out for ${formatTimeout(mins)}`);
    } catch {
      notify("Timeout apply nahi ho paaya (permission)");
    }
    setTimeoutFor(null);
    setSheetFor(null);
  };

  return (
    <div className="relative flex min-h-full flex-1 flex-col">
      <TopBar
        title={displayNameFor(roomId)}
        subtitle={
          readOnly
            ? timedOut
              ? "Aap timeout mein hain — abhi message nahi bhej sakte"
              : "Read-only — sirf apni class mein chat kar sakte ho"
            : "Hold a message for options · swipe right to reply"
        }
        onBack={() => go({ name: "home" })}
      />
      <div className="flex-1 space-y-3 p-3">
        {error && (
          <p className="pt-10 text-center text-sm text-destructive">
            Is room ka access nahi hai ({error})
          </p>
        )}
        {!error && messages.length === 0 && (
          <p className="pt-10 text-center text-sm text-muted-foreground">Koi message nahi abhi tak</p>
        )}
        {messages.map((m) => (
          <Bubble
            key={m.id}
            m={m}
            me={me}
            onLongPress={() => setSheetFor(m)}
            onReply={() => !readOnly && startReply(m)}
            onOpenProfile={(username) =>
              go({ name: "userprofile", username, from: { name: "chat", roomId } })
            }
          />
        ))}
        <div ref={bottomRef} />
      </div>

      {readOnly ? (
        <div className="sticky bottom-0 flex items-center gap-2 border-t border-border/60 bg-card/80 p-4 text-xs font-semibold text-muted-foreground backdrop-blur">
          <Lock className="size-4" />
          {me.isBanned
            ? "Aapka account banned hai."
            : timedOut
              ? "Timeout khatam hone ka intezaar karein."
              : `Aap ${me.classLevel ?? "apni class"} ke student hain — is room mein sirf padh sakte hain.`}
        </div>
      ) : (
        <div className="sticky bottom-0 border-t border-border/60 bg-card/80 p-3 backdrop-blur">
          {(replyTo || editing) && (
            <div className="mb-2 flex items-center gap-2 rounded-2xl bg-muted/60 px-3 py-2 text-[11px]">
              {editing ? <Pencil className="size-3" /> : <CornerUpLeft className="size-3" />}
              <span className="min-w-0 flex-1 truncate text-muted-foreground">
                {editing
                  ? `Editing: ${editing.text}`
                  : `Replying to ${replyTo?.senderName}: ${replyTo?.text}`}
              </span>
              <button
                aria-label="Cancel"
                onClick={() => {
                  setReplyTo(null);
                  setEditing(null);
                  setInput("");
                }}
                className="text-muted-foreground"
              >
                <X className="size-3.5" />
              </button>
            </div>
          )}
          <div className="flex items-center gap-2">
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(e) => void attach(e.target.files?.[0])}
            />
            <button
              aria-label="Attach photo"
              onClick={() => fileRef.current?.click()}
              className="rounded-full p-2 text-muted-foreground"
            >
              <ImageIcon className="size-5" />
            </button>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && void send()}
              placeholder="Type a message… (@username to mention)"
              className="min-w-0 flex-1 rounded-2xl border border-border bg-input/60 px-4 py-2.5 text-sm outline-none placeholder:text-muted-foreground/70 focus:border-primary"
            />
            <button
              onClick={() => void send()}
              aria-label="Send"
              className="grid size-10 shrink-0 place-items-center rounded-full bg-primary text-primary-foreground"
            >
              {sending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
            </button>
          </div>
        </div>
      )}

      {sheetFor && !timeoutFor && (
        <Sheet onClose={() => setSheetFor(null)} title={`Message · ${sheetFor.senderName}`}>
          {!readOnly && (
            <SheetAction
              icon={<CornerUpLeft className="size-4" />}
              label="Reply"
              onClick={() => startReply(sheetFor)}
            />
          )}
          {sheetFor.senderId === me.uid && (
            <SheetAction
              icon={<Pencil className="size-4" />}
              label="Edit message"
              onClick={() => {
                setEditing(sheetFor);
                setReplyTo(null);
                setInput(sheetFor.text);
                setSheetFor(null);
              }}
            />
          )}
          {(sheetFor.senderId === me.uid || admin) && (
            <SheetAction
              icon={<Trash2 className="size-4" />}
              label="Delete message"
              danger
              onClick={() => {
                void deleteMessage(roomId, sheetFor.id)
                  .then(() => notify("Message deleted"))
                  .catch(() => notify("Delete nahi ho paaya (permission)"));
                setSheetFor(null);
              }}
            />
          )}
          {admin && sheetFor.senderId !== me.uid && (
            <SheetAction
              icon={<Clock className="size-4" />}
              label={`Timeout ${sheetFor.senderName}`}
              danger
              onClick={() => setTimeoutFor(sheetFor)}
            />
          )}
          {sheetFor.senderName !== ANONYMOUS_DISPLAY_NAME && (
            <SheetAction
              icon={<User className="size-4" />}
              label="View profile"
              onClick={() =>
                go({
                  name: "userprofile",
                  username: sheetFor.senderName,
                  from: { name: "chat", roomId },
                })
              }
            />
          )}
        </Sheet>
      )}

      {timeoutFor && (
        <Sheet
          onClose={() => {
            setTimeoutFor(null);
            setSheetFor(null);
          }}
          title={`Timeout @${timeoutFor.senderName}`}
        >
          <p className="text-xs text-muted-foreground">
            Duration chunein ya apna custom time type karein — is time tak user message nahi bhej
            paayega.
          </p>
          <div className="flex flex-wrap gap-2">
            {timeoutPresets.map((p) => (
              <button
                key={p}
                onClick={() => void applyTimeout(p, timeoutFor)}
                className="rounded-full border border-border px-3 py-1.5 text-xs font-bold text-muted-foreground hover:border-primary/60 hover:text-foreground"
              >
                {formatTimeout(p)}
              </button>
            ))}
          </div>
          <div className="flex items-end gap-2">
            <div className="flex-1">
              <Field
                label="Custom minutes"
                value={customMins}
                onChange={setCustomMins}
                type="number"
                placeholder="e.g. 7"
              />
            </div>
            <button
              onClick={() => void applyTimeout(Math.max(1, Number(customMins) || 1), timeoutFor)}
              className="rounded-2xl bg-destructive px-4 py-3 text-sm font-bold text-destructive-foreground"
            >
              Apply
            </button>
          </div>
        </Sheet>
      )}
    </div>
  );
}

/* --------------------------------------------------------------- profiles --- */

function UserProfileScreen({
  username,
  me,
  onBack,
  go,
}: {
  username: string;
  me: AppUser;
  onBack: () => void;
  go: (s: Screen) => void;
}) {
  const { profile: p, loading } = useProfileByUsername(username);
  const admin = isAdminRole(me.role);

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <TopBar title={`@${username}`} onBack={onBack} />
      {loading ? (
        <p className="p-6 text-sm text-muted-foreground">Loading…</p>
      ) : !p ? (
        <p className="p-6 text-sm text-muted-foreground">Profile not available.</p>
      ) : (
        <div className="space-y-5 p-5">
          <div className="flex items-center gap-4">
            {p.profilePhoto ? (
              <img
                src={p.profilePhoto}
                alt={`${p.username} profile`}
                className="size-20 rounded-full object-cover"
              />
            ) : (
              <span className="grid size-20 place-items-center rounded-full bg-surface-variant text-2xl font-black text-primary">
                {p.username.slice(0, 1).toUpperCase()}
              </span>
            )}
            <div>
              <p className="text-lg font-bold">{p.fullName || p.username}</p>
              <p className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2 py-0.5 text-xs font-semibold text-primary">
                <ShieldCheck className="size-3" /> {p.role}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {p.classLevel ?? "—"}
                {p.createdAt
                  ? ` · joined ${new Date(p.createdAt).toLocaleDateString(undefined, { month: "short", year: "numeric" })}`
                  : ""}
              </p>
              {p.schoolName && <p className="text-xs text-muted-foreground">{p.schoolName}</p>}
            </div>
          </div>
          {p.bio && <p className="glass-soft rounded-2xl p-4 text-sm">{p.bio}</p>}
          {admin && p.uid !== me.uid && (
            <div className="space-y-2">
              <OutlineRow
                icon={<Clock className="size-4" />}
                label="Timeout user (10 min)"
                onClick={() => void timeoutUser(p.uid, 10)}
              />
              <OutlineRow
                icon={<Shield className="size-4" />}
                label="Manage class access"
                onClick={() => go({ name: "admin" })}
              />
              <OutlineRow
                icon={<ShieldCheck className="size-4" />}
                label="Open admin panel"
                onClick={() => go({ name: "admin" })}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function ProfileScreen({
  me,
  go,
  notify,
}: {
  me: AppUser;
  go: (s: Screen) => void;
  notify: (msg: string) => void;
}) {
  const [fullName, setFullName] = useState(me.fullName ?? "");
  const [school, setSchool] = useState(me.schoolName ?? "");
  const [bio, setBio] = useState(me.bio ?? "");
  const [theme, setTheme] = useState("Aurora");
  const photoRef = useRef<HTMLInputElement>(null);
  const admin = isAdminRole(me.role);

  const save = async () => {
    try {
      await saveOwnProfile(me.uid, { fullName, schoolName: school, bio });
      notify("Profile saved");
    } catch (e) {
      notify(e instanceof Error ? e.message : "Save failed");
    }
  };

  const pickPhoto = async (file: File | undefined) => {
    if (!file) return;
    try {
      await uploadProfilePhoto(me.uid, file);
      notify("Profile photo updated");
    } catch (e) {
      notify(e instanceof Error ? e.message : "Upload failed");
    }
  };

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <TopBar title="Profile" onBack={() => go({ name: "home" })} />
      <div className="space-y-5 p-5">
        <div className="flex items-center gap-4">
          <input
            ref={photoRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => void pickPhoto(e.target.files?.[0])}
          />
          <button onClick={() => photoRef.current?.click()} aria-label="Change photo">
            {me.profilePhoto ? (
              <img
                src={me.profilePhoto}
                alt="Your profile"
                className="size-20 rounded-full object-cover"
              />
            ) : (
              <span className="grid size-20 place-items-center rounded-full bg-surface-variant text-2xl font-black text-primary">
                {me.username.slice(0, 1).toUpperCase()}
              </span>
            )}
          </button>
          <div>
            <p className="text-lg font-bold">{me.username}</p>
            <p className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2 py-0.5 text-xs font-semibold text-primary">
              <ShieldCheck className="size-3" /> {me.role}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              {me.classLevel ?? "—"} · tap to change photo
            </p>
          </div>
        </div>

        <Field label="Full name" value={fullName} onChange={setFullName} />
        <Field label="School" value={school} onChange={setSchool} />
        <Field label="Bio" value={bio} onChange={setBio} multiline />

        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Theme
          </p>
          <div className="flex flex-wrap gap-2">
            {themeOptions.map((t) => (
              <button
                key={t}
                onClick={() => setTheme(t)}
                className={`rounded-full border px-3 py-1.5 text-xs font-semibold transition-colors ${
                  theme === t
                    ? "border-primary bg-primary/20 text-primary"
                    : "border-border text-muted-foreground"
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <PrimaryButton onClick={() => void save()}>Save changes</PrimaryButton>

        <div className="space-y-2">
          <OutlineRow
            icon={<Bell className="size-4" />}
            label="Notifications"
            onClick={() => go({ name: "notifications" })}
          />
          <OutlineRow
            icon={<Shield className="size-4" />}
            label="Community rules"
            onClick={() => go({ name: "rules" })}
          />
          <OutlineRow
            icon={<Lightbulb className="size-4" />}
            label="Suggestions"
            onClick={() => go({ name: "suggestions" })}
          />
          <OutlineRow
            icon={<Bug className="size-4" />}
            label="Report a bug"
            onClick={() => go({ name: "bugreport" })}
          />
          {admin && (
            <OutlineRow
              icon={<ShieldCheck className="size-4" />}
              label="Admin panel"
              onClick={() => go({ name: "admin" })}
            />
          )}
        </div>

        <button
          onClick={() => void logout()}
          className="w-full rounded-2xl border border-destructive/50 px-4 py-3 text-sm font-bold text-destructive"
        >
          Sign out
        </button>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------- app --- */

export function StudentChatApp() {
  const [booted, setBooted] = useState(false);
  const { loading, authUser, profile } = useSession();
  const [screen, setScreen] = useState<Screen>({ name: "login" });
  const [toast, setToast] = useState<string | null>(null);
  const [seen, setSeen] = useState(0);
  const [bugText, setBugText] = useState("");
  const [suggestText, setSuggestText] = useState("");

  const uid = profile?.uid ?? null;
  const notifications = useNotifications(uid);
  const rules = useCommunityRules();
  const suggestions = useSuggestions(uid);
  const bugReports = useBugReports(uid);

  const notify = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2400);
  };

  const go = (s: Screen) => {
    if (s.name === "notifications") setSeen(notifications.length);
    setScreen(s);
  };
  const back = () => go({ name: "profile" });

  // Keep the screen in sync with the real auth state.
  useEffect(() => {
    if (loading) return;
    if (!authUser) setScreen({ name: "login" });
    else
      setScreen((s) => (s.name === "login" || s.name === "register" ? { name: "home" } : s));
  }, [loading, authUser]);

  if (!booted) {
    return (
      <div className="flex min-h-full flex-1 flex-col bg-background text-foreground">
        <SplashScreen onDone={() => setBooted(true)} />
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex min-h-full flex-1 items-center justify-center bg-background text-foreground">
        <Loader2 className="size-6 animate-spin text-primary" />
      </div>
    );
  }

  const me = profile;

  return (
    <div className="relative flex min-h-full flex-1 flex-col bg-background text-foreground">
      {(!me || screen.name === "login") && screen.name !== "register" && (
        <AuthScreen mode="login" go={go} notify={notify} />
      )}
      {!me && screen.name === "register" && <AuthScreen mode="register" go={go} notify={notify} />}

      {me && screen.name === "home" && (
        <HomeScreen me={me} go={go} unreadCount={Math.max(0, notifications.length - seen)} />
      )}
      {me && screen.name === "chat" && (
        <ChatScreen roomId={screen.roomId} me={me} go={go} notify={notify} />
      )}
      {me && screen.name === "profile" && <ProfileScreen me={me} go={go} notify={notify} />}
      {me && screen.name === "userprofile" && (
        <UserProfileScreen
          username={screen.username}
          me={me}
          onBack={() => go(screen.from)}
          go={go}
        />
      )}

      {me && screen.name === "notifications" && (
        <ScreenShell
          title="Notifications"
          subtitle="Global announcements from admins"
          onBack={() => go({ name: "home" })}
        >
          {notifications.length === 0 && (
            <p className="text-sm text-muted-foreground">Koi notification nahi hai.</p>
          )}
          {notifications.map((n) => (
            <div key={n.id} className="glass-soft space-y-2 rounded-2xl p-4">
              <p className="text-sm font-bold">{n.title}</p>
              <p className="text-xs text-muted-foreground">{n.body}</p>
              {n.photos && n.photos.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {n.photos.map((p) => (
                    <img
                      key={p}
                      src={p}
                      alt="Announcement attachment"
                      className="size-20 rounded-xl object-cover"
                    />
                  ))}
                </div>
              )}
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground">
                by {n.by} · {n.at}
              </p>
            </div>
          ))}
        </ScreenShell>
      )}

      {me && screen.name === "rules" && (
        <ScreenShell title="Community rules" onBack={back}>
          {rules.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Abhi rules set nahi hain — admin panel se add karein.
            </p>
          )}
          {rules.map((r, i) => (
            <div key={r} className="glass-soft flex gap-3 rounded-2xl p-4">
              <span className="text-sm font-black text-primary">{i + 1}</span>
              <p className="text-sm">{r}</p>
            </div>
          ))}
        </ScreenShell>
      )}

      {me && screen.name === "suggestions" && (
        <ScreenShell title="Suggestion box" onBack={back}>
          <Card>
            <Field
              label="Your suggestion"
              value={suggestText}
              onChange={setSuggestText}
              multiline
              placeholder="Kya improve karna chahiye?"
            />
            <PrimaryButton
              onClick={() => {
                if (!suggestText.trim()) return;
                void addSuggestion(me, suggestText.trim())
                  .then(() => {
                    setSuggestText("");
                    notify("Suggestion sent to admins");
                  })
                  .catch((e: Error) => notify(e.message));
              }}
            >
              Submit suggestion
            </PrimaryButton>
          </Card>
          {suggestions.map((s) => (
            <div key={s.id} className="glass-soft flex items-center gap-3 rounded-2xl p-4">
              <div className="flex-1">
                <p className="text-sm font-semibold">{s.text}</p>
                <p className="text-xs text-muted-foreground">
                  by {s.by} · {s.at}
                </p>
              </div>
            </div>
          ))}
        </ScreenShell>
      )}

      {me && screen.name === "bugreport" && (
        <ScreenShell title="Report a bug" onBack={back}>
          <Card>
            <Field
              label="What happened?"
              value={bugText}
              onChange={setBugText}
              multiline
              placeholder="Describe the bug…"
            />
            <PrimaryButton
              onClick={() => {
                if (!bugText.trim()) return;
                void addBugReport(me, bugText.trim())
                  .then(() => {
                    setBugText("");
                    notify("Bug report submitted");
                  })
                  .catch((e: Error) => notify(e.message));
              }}
            >
              Submit report
            </PrimaryButton>
          </Card>
          {bugReports.map((b) => (
            <div key={b.id} className="glass-soft rounded-2xl p-4">
              <p className="text-sm font-semibold">{b.text}</p>
              <p className="text-xs text-muted-foreground">
                by {b.by} · {b.at}
              </p>
            </div>
          ))}
        </ScreenShell>
      )}

      {me && screen.name === "admin" && (
        <AdminPanel
          me={me}
          onBack={back}
          openRoom={(roomId) => go({ name: "chat", roomId })}
          openProfile={(username) =>
            go({ name: "userprofile", username, from: { name: "admin" } })
          }
          notify={notify}
        />
      )}

      {toast && (
        <div className="pointer-events-none fixed bottom-24 left-1/2 z-30 -translate-x-1/2 rounded-full bg-foreground px-4 py-2 text-xs font-bold text-background">
          {toast}
        </div>
      )}
    </div>
  );
}
