import { createFileRoute } from "@tanstack/react-router";
import { StudentChatApp } from "@/components/studentchat/StudentChatApp";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "StudentHub — Class, Global & Anonymous Student Chat" },
      {
        name: "description",
        content:
          "StudentHub: class rooms, global and anonymous chat, profiles, admin panel with global notifications, timeouts and moderation tools.",
      },
      { property: "og:title", content: "StudentHub — Student Community App" },
      {
        property: "og:description",
        content:
          "Chat in your class, global and anonymous rooms. Admins send global notifications with photos and moderate every message.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

function Index() {
  return (
    <main className="min-h-screen bg-background">
      <div className="flex min-h-dvh flex-col md:hidden">
        <StudentChatApp />
      </div>

      <div className="hidden min-h-screen items-center justify-center px-10 py-14 md:flex">
        <div className="relative shrink-0 rounded-[3rem] border border-border bg-card p-3 shadow-[0_40px_100px_-30px_oklch(0_0_0/0.9)]">
          <div className="absolute left-1/2 top-5 z-10 h-6 w-28 -translate-x-1/2 rounded-full bg-background" />
          <div className="flex h-[780px] w-[380px] flex-col overflow-y-auto rounded-[2.4rem] bg-background">
            <StudentChatApp />
          </div>
        </div>
      </div>
    </main>
  );
}
