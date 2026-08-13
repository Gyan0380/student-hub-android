import { ArrowLeft, ChevronRight, X } from "lucide-react";

export function TopBar({
  title,
  subtitle,
  onBack,
  right,
}: {
  title: string;
  subtitle?: string | undefined;
  onBack?: () => void;
  right?: React.ReactNode;
}) {
  return (
    <header className="flex items-center gap-2 border-b border-border/60 bg-card/70 px-3 py-3 backdrop-blur">
      {onBack ? (
        <button
          onClick={onBack}
          aria-label="Back"
          className="rounded-full p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <ArrowLeft className="size-5" />
        </button>
      ) : (
        <span className="grid size-9 place-items-center rounded-xl bg-primary/20 text-sm font-black text-primary">
          SC
        </span>
      )}
      <div className="min-w-0 flex-1">
        <h1 className="truncate text-lg font-bold tracking-tight">{title}</h1>
        {subtitle && <p className="truncate text-[11px] text-muted-foreground">{subtitle}</p>}
      </div>
      {right}
    </header>
  );
}

export function Field({
  label,
  value,
  onChange,
  type = "text",
  placeholder,
  multiline,
  rows = 3,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  placeholder?: string;
  multiline?: boolean;
  rows?: number;
}) {
  const shared =
    "w-full rounded-2xl border border-border bg-input/60 px-4 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground/70 focus:border-primary";
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
      </span>
      {multiline ? (
        <textarea
          rows={rows}
          value={value}
          placeholder={placeholder}
          onChange={(e) => onChange(e.target.value)}
          className={shared}
        />
      ) : (
        <input
          type={type}
          value={value}
          placeholder={placeholder}
          onChange={(e) => onChange(e.target.value)}
          className={shared}
        />
      )}
    </label>
  );
}

export function PrimaryButton({
  children,
  onClick,
  tone = "primary",
}: {
  children: React.ReactNode;
  onClick?: () => void;
  tone?: "primary" | "danger" | "secondary";
}) {
  const tones = {
    primary: "bg-primary text-primary-foreground",
    danger: "bg-destructive text-destructive-foreground",
    secondary: "bg-secondary text-secondary-foreground",
  } as const;
  return (
    <button
      onClick={onClick}
      className={`w-full rounded-2xl px-4 py-3 text-sm font-bold transition-transform active:scale-[0.98] ${tones[tone]}`}
    >
      {children}
    </button>
  );
}

export function OutlineRow({
  icon,
  label,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  onClick?: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-center gap-3 rounded-2xl border border-border bg-card/60 px-4 py-3 text-left text-sm font-semibold transition-colors hover:border-primary/60"
    >
      <span className="text-primary">{icon}</span>
      <span className="flex-1">{label}</span>
      <ChevronRight className="size-4 text-muted-foreground" />
    </button>
  );
}

export function Sheet({
  onClose,
  title,
  children,
}: {
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="absolute inset-0 z-20 flex flex-col justify-end">
      <button
        aria-label="Close"
        onClick={onClose}
        className="absolute inset-0 bg-background/70 backdrop-blur-sm"
      />
      <div className="glass relative max-h-[85%] overflow-y-auto rounded-t-[28px] p-4">
        <div className="mb-3 flex items-center gap-2">
          <p className="flex-1 text-sm font-bold">{title}</p>
          <button onClick={onClose} aria-label="Dismiss" className="text-muted-foreground">
            <X className="size-4" />
          </button>
        </div>
        <div className="space-y-2">{children}</div>
      </div>
    </div>
  );
}

export function SheetAction({
  icon,
  label,
  danger,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  danger?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`flex w-full items-center gap-3 rounded-2xl border px-4 py-3 text-left text-sm font-semibold transition-colors ${
        danger ? "border-destructive/50 text-destructive" : "border-border hover:border-primary/60"
      }`}
    >
      <span className={danger ? "" : "text-primary"}>{icon}</span>
      {label}
    </button>
  );
}

export function ScreenShell({
  title,
  subtitle,
  onBack,
  children,
}: {
  title: string;
  subtitle?: string;
  onBack: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <TopBar title={title} subtitle={subtitle} onBack={onBack} />
      <div className="space-y-3 p-4">{children}</div>
    </div>
  );
}

export function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="pt-2 text-xs font-bold uppercase tracking-[0.18em] text-secondary">
      {children}
    </h2>
  );
}

export function Card({ children }: { children: React.ReactNode }) {
  return <div className="glass-soft space-y-3 rounded-2xl p-4">{children}</div>;
}
