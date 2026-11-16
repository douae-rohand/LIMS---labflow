import { AlertCircle, RefreshCw, X } from "lucide-react";
import { Skeleton } from "@/components/lab";
import { SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { messageErreurApi } from "@/api/employes";

interface EmployePanneauEtatProps {
  chargement: boolean;
  erreur: unknown;
  estEnErreur: boolean;
  est404: boolean;
  onFermer: () => void;
  onReessayer: () => void;
}

export function EmployePanneauEtat({
  chargement,
  erreur,
  estEnErreur,
  est404,
  onFermer,
  onReessayer,
}: EmployePanneauEtatProps) {
  if (chargement) {
    return (
      <div aria-live="polite" className="space-y-6">
        <SheetHeader className="text-left space-y-2">
          <SheetTitle className="sr-only">Chargement des données de l'employé</SheetTitle>
          <SheetDescription className="sr-only">Veuillez patienter...</SheetDescription>
          <Skeleton className="h-7 w-3/4" />
          <Skeleton className="h-4 w-1/3" />
        </SheetHeader>
        <div className="space-y-4 pt-4">
          <Skeleton className="h-6 w-24" />
          <div className="grid grid-cols-2 gap-4">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
          <Skeleton className="h-20 w-full rounded-xl" />
        </div>
      </div>
    );
  }
  if (!estEnErreur) {
    return null;
  }

  return (
    <div aria-live="assertive" className="flex flex-col items-center py-12 text-center">
      <div className="mb-4 flex size-12 items-center justify-center rounded-full bg-red-100 text-red-600">
        <AlertCircle className="size-6" aria-hidden="true" />
      </div>
      <SheetHeader className="text-center">
        <SheetTitle className="text-lg font-bold text-ink-900">
          {est404 ? "Employé introuvable" : "Erreur de chargement"}
        </SheetTitle>
        <SheetDescription className="text-sm text-muted-foreground mt-1">
          {messageErreurApi(erreur)}
        </SheetDescription>
      </SheetHeader>
      <div className="mt-6 flex items-center gap-3">
        {est404 ? (
          <button
            type="button"
            onClick={onFermer}
            className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-5 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
          >
            <X className="size-4" aria-hidden="true" />
            Fermer
          </button>
        ) : (
          <>
            <button
              type="button"
              onClick={onReessayer}
              className="inline-flex items-center gap-2 rounded-full bg-brand-900 px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-brand-900/90"
            >
              <RefreshCw className="size-4" aria-hidden="true" />
              Réessayer
            </button>
            <button
              type="button"
              onClick={onFermer}
              className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-5 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
            >
              Fermer
            </button>
          </>
        )}
      </div>
    </div>
  );
}
