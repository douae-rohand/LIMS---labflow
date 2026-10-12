import { useEffect, type ReactNode } from "react";
import { X } from "lucide-react";

export function Dialogue({
  ouvert,
  titre,
  description,
  onFermer,
  large,
  children,
}: {
  ouvert: boolean;
  titre: string;
  description?: string;
  onFermer: () => void;
  large?: boolean;
  children: ReactNode;
}) {
  useEffect(() => {
    if (!ouvert) return;
    const fermer = (event: KeyboardEvent) => {
      if (event.key === "Escape") onFermer();
    };
    window.addEventListener("keydown", fermer);
    return () => window.removeEventListener("keydown", fermer);
  }, [ouvert, onFermer]);

  if (!ouvert) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink-900/40 p-0 sm:items-center sm:p-4">
      <button
        type="button"
        aria-label="Fermer la fenêtre"
        className="absolute inset-0 cursor-default"
        onClick={onFermer}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="titre-dialogue"
        className={`relative z-10 max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-card p-5 shadow-xl sm:rounded-3xl sm:p-6 ${
          large ? "max-w-4xl" : "max-w-lg"
        }`}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <div>
            <h2 id="titre-dialogue" className="text-lg font-extrabold text-ink-900">
              {titre}
            </h2>
            {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onFermer}
            aria-label="Fermer"
            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-card text-brand-900"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Panneau({
  ouvert,
  titre,
  onFermer,
  large,
  children,
}: {
  ouvert: boolean;
  titre: string;
  onFermer: () => void;
  large?: boolean;
  children: ReactNode;
}) {
  useEffect(() => {
    if (!ouvert) return;
    const fermer = (event: KeyboardEvent) => {
      if (event.key === "Escape") onFermer();
    };
    window.addEventListener("keydown", fermer);
    return () => window.removeEventListener("keydown", fermer);
  }, [ouvert, onFermer]);

  if (!ouvert) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-ink-900/40">
      <button
        type="button"
        aria-label="Fermer le panneau"
        className="absolute inset-0 cursor-default"
        onClick={onFermer}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-labelledby="titre-panneau"
        className={`relative z-10 h-full w-full overflow-y-auto bg-card p-5 shadow-xl sm:p-6 ${
          large ? "max-w-5xl" : "max-w-2xl"
        }`}
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <h2 id="titre-panneau" className="text-xl font-extrabold text-ink-900">
            {titre}
          </h2>
          <button
            type="button"
            onClick={onFermer}
            aria-label="Fermer"
            className="flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-card text-brand-900"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        </div>
        {children}
      </aside>
    </div>
  );
}
