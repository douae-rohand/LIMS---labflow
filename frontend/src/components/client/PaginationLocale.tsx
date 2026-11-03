import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/lab";

interface PaginationLocaleProps {
  page: number;
  totalPages: number;
  total: number;
  size: number;
  /** Nom singulier de l'élément compté (ex. « facture »). */
  nom: string;
  onPrecedent: () => void;
  onSuivant: () => void;
}

/** Pagination locale (page 1-indexée) partagée par les listes de l'espace client. */
export function PaginationLocale({
  page,
  totalPages,
  total,
  size,
  nom,
  onPrecedent,
  onSuivant,
}: PaginationLocaleProps) {
  if (total === 0) return null;

  const debut = (page - 1) * size + 1;
  const fin = Math.min(page * size, total);
  const estPremierePage = page <= 1;
  const estDernierePage = page >= totalPages;

  return (
    <div className="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
      <p aria-live="polite" aria-atomic="true" className="text-sm text-muted-foreground">
        {debut}–{fin} sur {total} {nom}
        {total > 1 ? "s" : ""}
      </p>
      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          icon={<ChevronLeft className="size-4" aria-hidden="true" />}
          iconPosition="left"
          onClick={onPrecedent}
          disabled={estPremierePage}
        >
          Précédent
        </Button>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          icon={<ChevronRight className="size-4" aria-hidden="true" />}
          iconPosition="right"
          onClick={onSuivant}
          disabled={estDernierePage}
        >
          Suivant
        </Button>
      </div>
    </div>
  );
}
