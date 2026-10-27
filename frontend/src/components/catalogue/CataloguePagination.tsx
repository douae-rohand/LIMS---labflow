import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/lab";
import type { PageResponse } from "@/api/utilisateurs";
import type { AnalyseCatalogue } from "@/api/catalogue";

interface CataloguePaginationProps {
  data: PageResponse<AnalyseCatalogue>;
  onPrecedent: () => void;
  onSuivant: () => void;
}

export function CataloguePagination({ data, onPrecedent, onSuivant }: CataloguePaginationProps) {
  const { page, size, totalElements, totalPages } = data;
  if (totalElements === 0) return null;

  const debut = page * size + 1;
  const fin = Math.min((page + 1) * size, totalElements);
  const estPremierePage = page === 0;
  const estDernierePage = page >= totalPages - 1;

  return (
    <div className="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
      <p aria-live="polite" aria-atomic="true" className="text-sm text-muted-foreground">
        {debut}–{fin} sur {totalElements} analyse{totalElements > 1 ? "s" : ""}
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
