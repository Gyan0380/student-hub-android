import { useEffect, useState } from "react";
import logo from "@/assets/studentchat-logo.jpg.asset.json";

export function SplashScreen({ onDone }: { onDone: () => void }) {
  const [stage, setStage] = useState(0);

  useEffect(() => {
    const t1 = setTimeout(() => setStage(1), 60);
    const t2 = setTimeout(() => setStage(2), 1500);
    const t3 = setTimeout(onDone, 2600);
    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
      clearTimeout(t3);
    };
  }, [onDone]);

  return (
    <button
      onClick={onDone}
      aria-label="Skip intro"
      className="aurora-backdrop relative flex min-h-full flex-1 flex-col items-center justify-center overflow-hidden px-8"
    >
      <span
        className="pointer-events-none absolute size-[520px] rounded-full blur-3xl transition-all duration-1000"
        style={{
          background:
            "radial-gradient(circle, color-mix(in oklab, var(--primary) 45%, transparent), transparent 70%)",
          opacity: stage >= 1 ? 0.9 : 0,
          transform: `scale(${stage >= 1 ? 1 : 0.6})`,
        }}
      />
      <img
        src={logo.url}
        alt="StudentChat logo"
        className="relative w-56 rounded-3xl shadow-[0_30px_80px_-30px_oklch(0_0_0/0.9)] transition-all duration-1000 ease-out"
        style={{
          opacity: stage >= 1 ? 1 : 0,
          transform: `scale(${stage >= 1 ? 1 : 0.82}) translateY(${stage >= 1 ? 0 : 18}px)`,
          filter: stage >= 2 ? "brightness(1.05)" : "none",
        }}
      />
      <p
        className="relative mt-8 text-sm font-semibold tracking-[0.3em] text-foreground/80 transition-all duration-700"
        style={{ opacity: stage >= 2 ? 1 : 0, transform: `translateY(${stage >= 2 ? 0 : 10}px)` }}
      >
        CLASS · GLOBAL · ANONYMOUS
      </p>
      <span className="relative mt-10 h-1 w-40 overflow-hidden rounded-full bg-foreground/15">
        <span
          className="block h-full rounded-full bg-primary transition-all duration-[2400ms] ease-out"
          style={{ width: stage >= 1 ? "100%" : "0%" }}
        />
      </span>
      <span className="absolute bottom-8 text-[11px] uppercase tracking-widest text-muted-foreground">
        Tap to skip
      </span>
    </button>
  );
}
