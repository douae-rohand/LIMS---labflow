import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/lab";
import type { PageResponse } from "@/api/utilisateurs";
import type { EmployeDto } from "@/api/employes";

interface EmployesPaginationProps {
  data: PageResponse<EmployeDto>;
  onPrecedent: () => void;
  onSuivant: () => void;
}

export function EmployesPagination({
  data,
  onPrecedent,
  onSuivant,
}: EmployesPaginationProps) {
  const { page, size, totalElements, totalPages } = data;

  if (totalElements === 0) return null;

  // page est 0-indexed côté backend
  const debut = page * size + 1;
  const fin = Math.min((page + 1) * size, totalElements);

  const estPremierePage = page === 0;
  const estDernierePage = page >= totalPages - 1;

  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <p
        aria-live="polite"
        aria-atomic="true"
        className="text-sm text-muted-foreground"
      >
        {debut}–{fin} sur {totalElements} employé{totalElements > 1 ? "s" : ""}
      </p>

      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          disabled={estPremierePage}
          onClick={onPrecedent}
          aria-label="Page précédente"
          icon={<ChevronLeft className="size-4" aria-hidden="true" />}
          iconPosition="left"
        >
          Précédent
        </Button>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          disabled={estDernierePage}
          onClick={onSuivant}
          aria-label="Page suivante"
          icon={<ChevronRight className="size-4" aria-hidden="true" />}
          iconPosition="right"
        >
          Suivant
        </Button>
      </div>
    </div>
  );
}
