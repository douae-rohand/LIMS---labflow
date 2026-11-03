import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/lab";

interface BandeauLaboratoiresIndisponiblesProps {
  nombre: number;
  onReessayer: () => void;
}

/**
 * Bandeau partagé signalant qu'une ou plusieurs sources (laboratoires du
 * client) n'ont pas pu être lues : la liste affichée peut être incomplète.
 */
export function BandeauLaboratoiresIndisponibles({
  nombre,
  onReessayer,
}: BandeauLaboratoiresIndisponiblesProps) {
  if (nombre <= 0) return null;

  const pluriel = nombre > 1 ? "s" : "";

  return (
    <div
      role="status"
      className="flex flex-col gap-3 rounded-2xl border border-amber-300 bg-amber-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="flex items-start gap-2 text-sm text-amber-900">
        <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <span>
          Certaines données sont momentanément indisponibles ({nombre} laboratoire
          {pluriel}). La liste peut être incomplète.
        </span>
      </p>
      <Button
        type="button"
        variant="secondary"
        size="sm"
        onClick={onReessayer}
        className="shrink-0"
      >
        Réessayer
      </Button>
    </div>
  );
}
