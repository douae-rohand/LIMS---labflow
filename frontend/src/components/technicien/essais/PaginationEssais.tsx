import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/lab";
import type { PageResponse } from "@/api/utilisateurs";
import type { LigneEssaiTechnicienDto } from "@/api/technicien/types";

/** Pagination Précédent / Suivant avec compteur de page. */
export function PaginationEssais({
  data,
  page,
  onPrecedent,
  onSuivant,
}: {
  data: PageResponse<LigneEssaiTechnicienDto>;
  page: number;
  onPrecedent: () => void;
  onSuivant: () => void;
}) {
  const pageCourante1 = page;
  const totalPages = data.totalPages > 0 ? data.totalPages : 1;
  const premierDesactive = data.page === 0;
  const dernierDesactive = data.last;

  return (
    <nav
      className="flex items-center justify-between gap-4"
      aria-label="Pagination des essais"
    >
      <p className="text-sm text-muted-foreground">
        Page {pageCourante1} sur {totalPages}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          icon={<ChevronLeft className="size-4" aria-hidden="true" />}
          iconPosition="left"
          onClick={onPrecedent}
          disabled={premierDesactive}
        >
          Précédent
        </Button>
        <Button
          variant="secondary"
          size="sm"
          icon={<ChevronRight className="size-4" aria-hidden="true" />}
          iconPosition="right"
          onClick={onSuivant}
          disabled={dernierDesactive}
        >
          Suivant
        </Button>
      </div>
    </nav>
  );
}
