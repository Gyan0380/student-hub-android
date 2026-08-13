import { useMemo, useRef, useState } from "react";
import {
  Bug,
  Hash,
  Image as ImageIcon,
  Lightbulb,
  Send,
  ShieldCheck,
  Tag as TagIcon,
  Trash2,
  Trash,
  Users,
  X,
} from "lucide-react";
import { Card, Field, PrimaryButton, ScreenShell, SectionTitle, Sheet, SheetAction } from "./ui";
import {
  CLASS_ROOM_OPTIONS,
  MAX_NOTIF_PHOTOS,
  classRoomId,
  formatTimeout,
  timeoutPresets,
  type AppUser,
  type Role,
} from "@/lib/studentchat-data";
import {
  deleteBugReport,
  deleteNotification,
  deleteSuggestion,
  isAdminRole,
  saveAntiAbuseWords,
  saveCommunityRules,
  saveTags,
  sendGlobalNotification,
  setBanned,
  setClassAccess,
  setUserRole,
  timeoutUser,
  uploadNotificationPhoto,
  useAntiAbuseWords,
  useBugReports,
  useCommunityRules,
  useMembers,
  useNotifications,
  useSuggestions,
  useTags,
  type Tag,
} from "@/lib/studentchat-api";

const ROLES: Role[] = ["Student", "Admin", "Owner"];

export function AdminPanel({
  me,
  onBack,
  openRoom,
  openProfile,
  notify,
}: {
  me: AppUser;
  onBack: () => void;
  openRoom: (roomId: string) => void;
  openProfile: (username: string) => void;
  notify: (msg: string) => void;
}) {
  const isOwner = me.role === "Owner";
  const canModerate = isAdminRole(me.role);

  const members = useMembers(canModerate);
  const notifications = useNotifications(me.uid);
  const rules = useCommunityRules();
  const badWords = useAntiAbuseWords();
  const suggestions = useSuggestions(me.uid);
  const bugReports = useBugReports(me.uid);
  const tags = useTags();

  const [rulesText, setRulesText] = useState<string | null>(null);
  const [addWord, setAddWord] = useState("");
  const [removeWord, setRemoveWord] = useState("");

  const [notifTitle, setNotifTitle] = useState("");
  const [notifText, setNotifText] = useState("");
  const [photos, setPhotos] = useState<string[]>([]);
  const [sending, setSending] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const [search, setSearch] = useState("");
  const [editUser, setEditUser] = useState<AppUser | null>(null);
  const [newTag, setNewTag] = useState("");
  const [newTagColor, setNewTagColor] = useState("#9B8CFF");

  const effectiveRulesText = rulesText ?? rules.join("\n");
  const filteredMembers = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return members;
    return members.filter(
      (m) =>
        m.username.toLowerCase().includes(q) || (m.fullName ?? "").toLowerCase().includes(q),
    );
  }, [members, search]);

  if (!canModerate) {
    return (
      <ScreenShell title="Admin Panel" onBack={onBack}>
        <Card>
          <p className="text-sm text-muted-foreground">
            Aapke paas admin access nahi hai. Firestore rules bhi is action ko block karte hain.
          </p>
        </Card>
      </ScreenShell>
    );
  }

  const pickPhotos = async (files: FileList | null) => {
    if (!files) return;
    const chosen = Array.from(files).slice(0, MAX_NOTIF_PHOTOS - photos.length);
    try {
      const urls = await Promise.all(chosen.map((f) => uploadNotificationPhoto(f)));
      setPhotos((p) => [...p, ...urls]);
    } catch (e) {
      notify(e instanceof Error ? e.message : "Photo upload failed");
    }
  };

  const sendNotification = async () => {
    if (!notifText.trim()) {
      notify("Message likhna zaroori hai");
      return;
    }
    setSending(true);
    try {
      await sendGlobalNotification({
        title: notifTitle.trim() || "Announcement",
        body: notifText.trim(),
        sentBy: me.username,
        photos,
      });
      setNotifTitle("");
      setNotifText("");
      setPhotos([]);
      notify("Notification sent — push Cloud Function se jaayega");
    } catch (e) {
      notify(e instanceof Error ? e.message : "Send failed");
    } finally {
      setSending(false);
    }
  };

  const guard = (p: Promise<unknown>, ok: string) =>
    void p.then(() => notify(ok)).catch((e: Error) => notify(e.message));

  return (
    <ScreenShell title="Admin Panel" subtitle={`${me.role} · full control`} onBack={onBack}>
      <div className="glass rounded-2xl p-5 text-center">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          Total registered members
        </p>
        <p className="mt-1 text-4xl font-black text-primary">{members.length.toLocaleString()}</p>
      </div>

      <SectionTitle>Global notification (admin only)</SectionTitle>
      <Card>
        <Field
          label="Title (optional)"
          value={notifTitle}
          onChange={setNotifTitle}
          placeholder="e.g. Test Postponed"
        />
        <Field
          label="Message"
          value={notifText}
          onChange={setNotifText}
          multiline
          rows={2}
          placeholder="Tomorrow's test postponed to Friday"
        />
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Photos (optional, up to {MAX_NOTIF_PHOTOS})
          </p>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            multiple
            className="hidden"
            onChange={(e) => void pickPhotos(e.target.files)}
          />
          <button
            onClick={() => fileRef.current?.click()}
            className="flex items-center gap-2 rounded-2xl border border-border px-4 py-2 text-xs font-bold"
          >
            <ImageIcon className="size-4 text-primary" /> Add photos
          </button>
          {photos.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {photos.map((p) => (
                <span key={p} className="relative">
                  <img
                    src={p}
                    alt="Attachment preview"
                    className="size-16 rounded-xl object-cover"
                  />
                  <button
                    aria-label="Remove photo"
                    onClick={() => setPhotos((prev) => prev.filter((x) => x !== p))}
                    className="absolute -right-1.5 -top-1.5 grid size-5 place-items-center rounded-full bg-destructive text-destructive-foreground"
                  >
                    <X className="size-3" />
                  </button>
                </span>
              ))}
            </div>
          )}
        </div>
        <PrimaryButton onClick={() => void sendNotification()}>
          <span className="inline-flex items-center gap-2">
            <Send className="size-4" /> {sending ? "Sending…" : "Send to everyone"}
          </span>
        </PrimaryButton>
      </Card>

      <SectionTitle>Notification history</SectionTitle>
      <Card>
        {notifications.length === 0 && (
          <p className="text-xs text-muted-foreground">Koi notification nahi hai.</p>
        )}
        {notifications.map((n) => (
          <div
            key={n.id}
            className="flex items-start gap-3 border-b border-border/50 pb-3 last:border-0 last:pb-0"
          >
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold">{n.title}</p>
              <p className="text-xs text-muted-foreground">{n.body}</p>
              <p className="mt-1 text-[10px] uppercase tracking-wide text-muted-foreground">
                by {n.by} · {n.at}
                {n.photos?.length ? ` · ${n.photos.length} photo(s)` : ""}
              </p>
            </div>
            <button
              onClick={() => guard(deleteNotification(n.id), "Notification deleted for everyone")}
              aria-label="Delete notification"
              className="text-destructive"
            >
              <Trash2 className="size-4" />
            </button>
          </div>
        ))}
      </Card>

      <SectionTitle>All registered members</SectionTitle>
      <Card>
        <Field label="Search" value={search} onChange={setSearch} placeholder="@username" />
        <p className="text-xs text-muted-foreground">
          Naam pe tap karke profile dekhein, Edit se sab kuch manage karein.
        </p>
        <div className="max-h-64 space-y-2 overflow-y-auto rounded-2xl border border-border/60 p-2">
          {filteredMembers.length === 0 && (
            <p className="p-2 text-xs text-muted-foreground">Koi member nahi mila.</p>
          )}
          {filteredMembers.map((m) => (
            <div
              key={m.uid}
              className="flex items-center gap-2 border-b border-border/40 pb-2 last:border-0 last:pb-0"
            >
              <button onClick={() => openProfile(m.username)} className="min-w-0 flex-1 text-left">
                <span className="block truncate text-sm font-semibold text-secondary underline-offset-2 hover:underline">
                  {m.fullName || m.username}
                </span>
                <span className="block text-[11px] text-muted-foreground">
                  @{m.username} · {m.role}
                  {m.classLevel ? ` · ${m.classLevel}` : ""}
                  {m.isBanned ? " · BANNED" : ""}
                </span>
              </button>
              <button
                onClick={() => setEditUser(m)}
                className="rounded-full border border-border px-3 py-1 text-[11px] font-bold"
              >
                Edit
              </button>
            </div>
          ))}
        </div>
      </Card>

      <SectionTitle>Jump into any class chat</SectionTitle>
      <Card>
        <p className="text-xs text-muted-foreground">
          Admin/Owner ko sab rooms mein access hai — andar kisi ka bhi message delete kar sakte ho.
        </p>
        <div className="flex flex-wrap gap-2">
          {["global", "anonymous", "admin-room", ...CLASS_ROOM_OPTIONS.map(classRoomId)].map((r) => (
            <button
              key={r}
              onClick={() => openRoom(r)}
              className="flex items-center gap-1 rounded-full border border-border px-3 py-1.5 text-[11px] font-bold text-muted-foreground hover:border-primary/60 hover:text-foreground"
            >
              <Hash className="size-3" />
              {r}
            </button>
          ))}
        </div>
      </Card>

      <SectionTitle>Edit community rules</SectionTitle>
      <Card>
        <Field
          label="One rule per line"
          value={effectiveRulesText}
          onChange={setRulesText}
          multiline
          rows={6}
        />
        <PrimaryButton
          onClick={() =>
            guard(
              saveCommunityRules(
                effectiveRulesText
                  .split("\n")
                  .map((r) => r.trim())
                  .filter(Boolean),
              ),
              "Rules saved — Android live update ho jaayega",
            )
          }
        >
          Save rules
        </PrimaryButton>
      </Card>

      <SectionTitle>Anti-abuse (bad words)</SectionTitle>
      <Card>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <Field
              label="Add word(s)"
              value={addWord}
              onChange={setAddWord}
              placeholder="mc, bc, bsdk"
            />
          </div>
          <button
            onClick={() => {
              const words = addWord
                .split(",")
                .map((w) => w.trim().toLowerCase())
                .filter(Boolean);
              if (!words.length) return;
              guard(
                saveAntiAbuseWords(Array.from(new Set([...badWords, ...words]))),
                `${words.length} word(s) added`,
              );
              setAddWord("");
            }}
            className="rounded-2xl bg-secondary px-4 py-3 text-sm font-bold text-secondary-foreground"
          >
            Add
          </button>
        </div>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <Field
              label="Remove word"
              value={removeWord}
              onChange={setRemoveWord}
              placeholder="word to remove"
            />
          </div>
          <button
            onClick={() => {
              const w = removeWord.trim().toLowerCase();
              if (!w) return;
              guard(
                saveAntiAbuseWords(badWords.filter((x) => x !== w)),
                `"${w}" removed`,
              );
              setRemoveWord("");
            }}
            className="rounded-2xl bg-destructive px-4 py-3 text-sm font-bold text-destructive-foreground"
          >
            Remove
          </button>
        </div>
        <div className="flex flex-wrap gap-2">
          {badWords.length === 0 && (
            <p className="text-xs text-muted-foreground">List empty hai.</p>
          )}
          {badWords.map((w) => (
            <span
              key={w}
              className="rounded-full border border-border px-3 py-1 text-[11px] font-bold text-muted-foreground"
            >
              {w}
            </span>
          ))}
        </div>
        <p className="text-[11px] text-muted-foreground">
          Ye list Firestore (Settings/AntiAbuse) se live load hoti hai — web aur Android dono use
          karte hain.
        </p>
      </Card>

      <SectionTitle>Member tags</SectionTitle>
      <Card>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <Field label="New tag" value={newTag} onChange={setNewTag} placeholder="Sports Captain" />
          </div>
          <input
            type="color"
            aria-label="Tag colour"
            value={newTagColor}
            onChange={(e) => setNewTagColor(e.target.value)}
            className="size-11 rounded-2xl border border-border bg-input/60"
          />
          <button
            onClick={() => {
              const label = newTag.trim();
              if (!label) return;
              guard(
                saveTags([
                  ...tags,
                  { id: `t-${Date.now()}`, label, color: newTagColor, type: "custom" } as Tag,
                ]),
                "Tag added",
              );
              setNewTag("");
            }}
            className="rounded-2xl bg-secondary px-4 py-3 text-sm font-bold text-secondary-foreground"
          >
            <TagIcon className="size-4" />
          </button>
        </div>
        <div className="flex flex-wrap gap-2">
          {tags.map((t) => (
            <span
              key={t.id}
              className="flex items-center gap-1.5 rounded-full px-3 py-1 text-[11px] font-bold"
              style={{ backgroundColor: `${t.color}33`, color: t.color }}
            >
              {t.label}
              <button
                aria-label={`Remove ${t.label}`}
                onClick={() => guard(saveTags(tags.filter((x) => x.id !== t.id)), "Tag removed")}
              >
                <X className="size-3" />
              </button>
            </span>
          ))}
        </div>
      </Card>

      <SectionTitle>
        <span className="inline-flex items-center gap-2">
          <Lightbulb className="size-4" /> Suggestions
        </span>
      </SectionTitle>
      <Card>
        {suggestions.length === 0 && (
          <p className="text-xs text-muted-foreground">Koi suggestion nahi hai.</p>
        )}
        {suggestions.map((s) => (
          <div
            key={s.id}
            className="flex items-start gap-3 border-b border-border/50 pb-3 last:border-0 last:pb-0"
          >
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold">{s.text}</p>
              <p className="text-[11px] text-muted-foreground">
                by @{s.by} · {s.at}
              </p>
            </div>
            <button
              aria-label="Delete suggestion"
              onClick={() => guard(deleteSuggestion(s.id), "Suggestion deleted")}
              className="text-destructive"
            >
              <Trash className="size-4" />
            </button>
          </div>
        ))}
      </Card>

      <SectionTitle>
        <span className="inline-flex items-center gap-2">
          <Bug className="size-4" /> Bug reports
        </span>
      </SectionTitle>
      <Card>
        {bugReports.length === 0 && (
          <p className="text-xs text-muted-foreground">Koi bug report nahi hai.</p>
        )}
        {bugReports.map((b) => (
          <div
            key={b.id}
            className="flex items-start gap-3 border-b border-border/50 pb-3 last:border-0 last:pb-0"
          >
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold">{b.text}</p>
              <p className="text-[11px] text-muted-foreground">
                by @{b.by} · {b.at}
              </p>
            </div>
            <button
              aria-label="Delete bug report"
              onClick={() => guard(deleteBugReport(b.id), "Bug report deleted")}
              className="text-destructive"
            >
              <Trash className="size-4" />
            </button>
          </div>
        ))}
      </Card>

      <div className="flex items-center gap-2 rounded-2xl border border-border/60 p-3 text-[11px] text-muted-foreground">
        <Users className="size-4 text-primary" />
        Role changes Owner-only hain aur Cloud Function + Firestore rules se enforce hoti hain.
      </div>

      {editUser && (
        <Sheet onClose={() => setEditUser(null)} title={`Manage @${editUser.username}`}>
          <div className="flex items-center gap-2">
            <ShieldCheck className="size-4 text-primary" />
            <p className="text-xs text-muted-foreground">
              Current role: <span className="font-bold text-foreground">{editUser.role}</span>
            </p>
          </div>

          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Change role {isOwner ? "" : "(Owner only)"}
          </p>
          <div className="flex flex-wrap gap-2">
            {ROLES.map((r) => (
              <button
                key={r}
                disabled={!isOwner}
                onClick={() => {
                  guard(setUserRole(editUser.uid, r), `Role set to ${r}`);
                  setEditUser(null);
                }}
                className={`rounded-full border px-3 py-1.5 text-xs font-bold ${
                  editUser.role === r
                    ? "border-primary bg-primary/20 text-primary"
                    : "border-border text-muted-foreground"
                } ${isOwner ? "" : "opacity-40"}`}
              >
                {r}
              </button>
            ))}
          </div>

          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Timeout
          </p>
          <div className="flex flex-wrap gap-2">
            {timeoutPresets.map((p) => (
              <button
                key={p}
                onClick={() => {
                  guard(timeoutUser(editUser.uid, p), `Timed out for ${formatTimeout(p)}`);
                  setEditUser(null);
                }}
                className="rounded-full border border-border px-3 py-1.5 text-xs font-bold text-muted-foreground"
              >
                {formatTimeout(p)}
              </button>
            ))}
          </div>

          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Class access
          </p>
          <div className="flex flex-wrap gap-2">
            {CLASS_ROOM_OPTIONS.map((c) => {
              const on = editUser.classAccess.includes(c);
              return (
                <button
                  key={c}
                  onClick={() => {
                    const next = on
                      ? editUser.classAccess.filter((x) => x !== c)
                      : [...editUser.classAccess, c];
                    setEditUser({ ...editUser, classAccess: next });
                    guard(
                      setClassAccess(editUser.uid, next, editUser.classLevel),
                      "Class access updated",
                    );
                  }}
                  className={`rounded-full border px-3 py-1.5 text-[11px] font-bold ${
                    on
                      ? "border-secondary bg-secondary/20 text-secondary"
                      : "border-border text-muted-foreground"
                  }`}
                >
                  {c}
                </button>
              );
            })}
          </div>

          <SheetAction
            icon={<Trash2 className="size-4" />}
            label={editUser.isBanned ? "Unban user" : "Ban user"}
            danger
            onClick={() => {
              guard(
                setBanned(editUser.uid, !editUser.isBanned),
                editUser.isBanned ? "User unbanned" : "User banned",
              );
              setEditUser(null);
            }}
          />
        </Sheet>
      )}
    </ScreenShell>
  );
}
