import {
  AlertTriangle,
  Check,
  CornerUpLeft,
  FileSignature,
  MessageSquare,
  Sparkles,
  X,
} from "lucide-react";
import { Badge } from "@/components/lab";
import { cn } from "@/lib/utils";

function Frame({
  title,
  children,
  className,
}: {
  title: string;
  children: React.ReactNode;
  className?: string | undefined;
}) {
  return (
    <div
      className={cn(
        "glass-card glossy-top overflow-hidden rounded-3xl",
        className,
      )}
    >
      <div className="flex items-center gap-2 border-b border-brand-900/10 px-4 py-3">
        <span className="size-2.5 rounded-full bg-brand-900/20" />
        <span className="size-2.5 rounded-full bg-brand-900/20" />
        <span className="size-2.5 rounded-full bg-accent-lime" />
        <span className="ml-2 text-xs font-semibold text-brand-900">{title}</span>
      </div>
      <div className="p-4">{children}</div>
    </div>
  );
}

const bars = [42, 68, 55, 80, 62, 90, 74];

export function DashboardMockup({ className }: { className?: string }) {
  return (
    <Frame title="Tableau de bord — Responsable" className={className}>
      <div className="grid grid-cols-3 gap-3">
        {[
          ["128", "Demandes"],
          ["36", "En validation"],
          ["2,4 j", "Délai moyen"],
        ].map(([v, l]) => (
          <div key={l} className="rounded-2xl bg-surface-card p-3">
            <p className="text-lg font-extrabold text-brand-900">{v}</p>
            <p className="text-[11px] text-muted-foreground">{l}</p>
          </div>
        ))}
      </div>
      <div className="mt-3 rounded-2xl bg-surface-alt p-3">
        <p className="text-[11px] font-semibold text-brand-900">
          Essais réalisés par jour
        </p>
        <div className="mt-2 flex h-20 items-end gap-2">
          {bars.map((h, i) => (
            <span
              key={i}
              style={{ height: `${h}%` }}
              className={cn(
                "flex-1 rounded-t-lg",
                i === 5 ? "bg-accent-lime" : "bg-brand-600/70",
              )}
            />
          ))}
        </div>
      </div>
      <ul className="mt-3 flex flex-col gap-2">
        {[
          ["DEM-2026-0412", "EN_VALIDATION", "lime"],
          ["DEM-2026-0409", "EN_COURS", "mint"],
          ["DEM-2026-0401", "TERMINÉE", "soft"],
        ].map(([id, s, v]) => (
          <li
            key={id}
            className="flex items-center justify-between rounded-xl bg-card px-3 py-2 text-xs"
          >
            <span className="font-semibold text-ink-900">{id}</span>
            <Badge size="sm" variant={v as "lime" | "mint" | "soft"}>
              {s}
            </Badge>
          </li>
        ))}
      </ul>
    </Frame>
  );
}

export function ValidationMockup({ className }: { className?: string }) {
  return (
    <Frame title="Validation — DEM-2026-0412" className={className}>
      <div className="rounded-2xl bg-surface-card p-3 text-xs">
        {[
          ["Glycémie à jeun", "0,92 g/L", false],
          ["Cholestérol total", "2,41 g/L", true],
          ["Créatinine", "9,8 mg/L", false],
        ].map(([n, v, out]) => (
          <div
            key={n as string}
            className="flex items-center justify-between border-b border-brand-900/10 py-2 last:border-0"
          >
            <span className="text-ink-900">{n}</span>
            <span
              className={cn(
                "font-semibold",
                out ? "text-brand-600" : "text-ink-900",
              )}
            >
              {v}
              {out && (
                <AlertTriangle className="ml-1 inline size-3" aria-hidden="true" />
              )}
            </span>
          </div>
        ))}
      </div>
      <div className="mt-3 grid grid-cols-3 gap-2 text-[11px] font-semibold">
        <span className="flex items-center justify-center gap-1 rounded-full bg-brand-900 py-2 text-primary-foreground">
          <Check className="size-3" aria-hidden="true" /> Valider
        </span>
        <span className="flex items-center justify-center gap-1 rounded-full bg-mint-200 py-2 text-brand-900">
          <CornerUpLeft className="size-3" aria-hidden="true" /> Retourner
        </span>
        <span className="flex items-center justify-center gap-1 rounded-full border border-brand-900/20 py-2 text-brand-900">
          <X className="size-3" aria-hidden="true" /> Rejeter
        </span>
      </div>
      <div className="mt-3 flex items-center gap-2 rounded-2xl bg-accent-lime/60 p-3 text-xs text-ink-900">
        <FileSignature className="size-4" aria-hidden="true" />
        Signé par Dr S. Amrani — 28/09/2026 09:41
      </div>
    </Frame>
  );
}

export function AiMockup({ className }: { className?: string }) {
  return (
    <Frame title="Assistant LabFlow" className={className}>
      <div className="flex flex-col gap-2 text-xs">
        <div className="ml-auto max-w-[80%] rounded-2xl rounded-br-md bg-brand-900 px-3 py-2 text-primary-foreground">
          Quelle est la procédure de dosage de la créatinine ?
        </div>
        <div className="max-w-[85%] rounded-2xl rounded-bl-md bg-surface-card px-3 py-2 text-ink-900">
          <MessageSquare className="mr-1 inline size-3 text-brand-600" aria-hidden="true" />
          Méthode enzymatique, mode opératoire MO-BIO-014 §3. Calibration
          quotidienne requise.
          <span className="mt-1 block text-[10px] text-muted-foreground">
            Source : MO-BIO-014 v5
          </span>
        </div>
      </div>
      <div className="mt-3 rounded-2xl bg-surface-alt p-3">
        <div className="flex items-center justify-between text-[11px]">
          <span className="font-semibold text-brand-900">Score d'anomalie</span>
          <span className="font-bold text-ink-900">0,82</span>
        </div>
        <div className="mt-2 h-2 overflow-hidden rounded-full bg-mint-200">
          <span className="block h-full w-[82%] rounded-full bg-accent-lime" />
        </div>
        <p className="mt-2 flex items-center gap-1 text-[10px] text-muted-foreground">
          <Sparkles className="size-3" aria-hidden="true" /> Suggestion soumise à
          validation humaine
        </p>
      </div>
    </Frame>
  );
}
