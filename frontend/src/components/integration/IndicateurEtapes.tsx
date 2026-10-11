import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export function IndicateurEtapes({
  etapes,
  etape,
  onAller,
  autoriserSuivantes = false,
}: {
  etapes: readonly string[];
  etape: number;
  onAller?: (index: number) => void;
  autoriserSuivantes?: boolean;
}) {
  return (
    <div>
      <p className="mb-2 text-xs font-semibold text-muted-foreground">
        Étape {etape + 1} sur {etapes.length}
      </p>
      <ol
        className={cn(
          "grid grid-cols-2 gap-2",
          etapes.length >= 5 ? "sm:grid-cols-5" : "sm:grid-cols-4",
        )}
      >
        {etapes.map((label, index) => {
          const actif = index === etape;
          const fait = index < etape;
          return (
            <li key={label}>
              <button
                type="button"
                disabled={!onAller || (!autoriserSuivantes && index > etape)}
                className={cn(
                  "flex w-full items-center gap-2 rounded-full px-3 py-2 text-left text-xs font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 disabled:cursor-default",
                  actif
                    ? "bg-brand-900 text-primary-foreground"
                    : fait
                      ? "bg-mint-200 text-brand-900"
                      : "bg-surface-card text-muted-foreground",
                )}
                onClick={() => onAller?.(index)}
              >
                <span
                  className={cn(
                    "flex size-5 shrink-0 items-center justify-center rounded-full",
                    actif ? "bg-white/20" : "bg-brand-900/10",
                  )}
                >
                  {fait ? <Check className="size-3" aria-hidden="true" /> : index + 1}
                </span>
                <span className="truncate">{label}</span>
              </button>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
