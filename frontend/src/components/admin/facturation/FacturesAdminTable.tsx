import { ChevronDown, ChevronUp, ChevronsUpDown } from "lucide-react";
import { Button } from "@/components/lab";
import { messageErreurFacturesAdmin, type FactureAdminDto } from "@/api/facturesAdmin";

type ColTriFacture = "numero" | "date";
type DirectionTri = "asc" | "desc";

interface FacturesAdminTableProps {
  factures: FactureAdminDto[] | undefined;
  isLoading: boolean;
  error: unknown;
  page: number;
  totalPages: number;
  hasFilters: boolean;
  onPrevious: () => void;
  onNext: () => void;
  onDetails: (id: number) => void;
  tri: ColTriFacture;
  direction: DirectionTri;
  onSortChange: (colonne: ColTriFacture) => void;
}

function EnteteTri({
  label,
  colonne,
  tri,
  direction,
  onSortChange,
  className,
}: {
  label: string;
  colonne: ColTriFacture;
  tri: ColTriFacture;
  direction: DirectionTri;
  onSortChange: (colonne: ColTriFacture) => void;
  className: string;
}) {
  const active = tri === colonne;
  const Icon = !active ? ChevronsUpDown : direction === "asc" ? ChevronUp : ChevronDown;

  return (
    <th
      scope="col"
      aria-sort={!active ? "none" : direction === "asc" ? "ascending" : "descending"}
      className={className}
    >
      <button
        type="button"
        onClick={() => onSortChange(colonne)}
        className="inline-flex items-center gap-1 text-left hover:text-ink-900 focus-visible:outline-2 focus-visible:outline-brand-900"
      >
        {label}
        <Icon className={`size-3.5 shrink-0 ${active ? "" : "opacity-40"}`} aria-hidden="true" />
      </button>
    </th>
  );
}

export function FacturesAdminTable({
  factures,
  isLoading,
  error,
  page,
  totalPages,
  hasFilters,
  onPrevious,
  onNext,
  onDetails,
  tri,
  direction,
  onSortChange,
}: FacturesAdminTableProps) {
  if (isLoading && factures === undefined) {
    return <p className="text-muted-foreground">Chargement...</p>;
  }
  if (error) {
    return <p className="text-destructive" role="alert">{messageErreurFacturesAdmin(error)}</p>;
  }
  if (!factures?.length) {
    return (
      <p className="text-muted-foreground">
        {hasFilters ? "Aucune facture ne correspond à vos filtres." : "Aucune facture."}
      </p>
    );
  }

  return (
    <>
      <div className="overflow-hidden rounded-2xl border border-brand-900/10">
        <table className="w-full table-fixed border-collapse text-left">
          <thead className="bg-surface-card text-brand-900">
            <tr>
              <EnteteTri label="Numéro" colonne="numero" tri={tri} direction={direction} onSortChange={onSortChange} className="w-[17%] px-1.5 py-3 text-xs font-semibold sm:px-3 sm:text-sm" />
              <th scope="col" className="w-[22%] px-1.5 py-3 text-xs font-semibold sm:px-3 sm:text-sm">Réf. demande</th>
              <EnteteTri label="Date" colonne="date" tri={tri} direction={direction} onSortChange={onSortChange} className="w-[13%] px-1.5 py-3 text-xs font-semibold sm:px-3 sm:text-sm" />
              <th scope="col" className="w-[16%] px-1.5 py-3 text-xs font-semibold sm:px-3 sm:text-sm">Client</th>
              <th scope="col" className="w-[18%] px-1.5 py-3 text-right text-xs font-semibold sm:px-3 sm:text-sm">Montant</th>
              <th scope="col" className="w-[14%] px-1.5 py-3 text-xs font-semibold sm:px-3 sm:text-sm">Statut</th>
            </tr>
          </thead>
          <tbody>
            {factures.map((facture) => (
              <tr
                key={facture.id}
                onClick={() => onDetails(facture.id)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    onDetails(facture.id);
                  }
                }}
                tabIndex={0}
                aria-label={`Afficher les détails de la facture ${facture.numero}`}
                className="cursor-pointer border-t border-brand-900/10 transition-colors hover:bg-surface-card/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-900"
              >
                <td className="break-words px-1.5 py-3 text-xs sm:px-3 sm:text-sm">{facture.numero}</td>
                <td className="break-all px-1.5 py-3 text-xs sm:px-3 sm:text-sm">{facture.demandeReference ?? "—"}</td>
                <td className="break-words px-1.5 py-3 text-xs sm:px-3 sm:text-sm">{facture.dateFacture}</td>
                <td className="break-words px-1.5 py-3 text-xs sm:px-3 sm:text-sm" title={facture.clientNom ?? undefined}>{facture.clientNom ?? "—"}</td>
                <td className="break-words px-1.5 py-3 text-right text-xs sm:px-3 sm:text-sm">{facture.montantHt.toFixed(2)} MAD<span className="block text-[11px] text-muted-foreground">commission {facture.montantCommission.toFixed(2)} ({facture.tauxCommission} %)</span></td>
                <td className="px-1.5 py-3 text-xs sm:px-3 sm:text-sm">
                  <span className={`inline-block rounded-full px-1.5 py-1 text-[10px] font-medium sm:px-3 sm:text-xs ${
                    facture.statut === "PAYEE"
                      ? "bg-emerald-100 text-emerald-800"
                      : facture.statut === "ANNULEE"
                        ? "bg-slate-200 text-slate-700"
                      : facture.statut === "EN_RETARD"
                        ? "bg-rose-100 text-rose-800"
                        : "bg-amber-100 text-amber-800"
                  }`}>
                    {facture.statut === "PAYEE" ? "Payée" : facture.statut === "ANNULEE" ? "Annulée"
                      : facture.statut === "EN_RETARD" ? "En retard" : "Impayée"}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex items-center justify-between">
        <p className="text-sm text-muted-foreground">Page {page} sur {totalPages}</p>
        <div className="flex gap-2">
          <Button variant="outlineLight" size="sm" disabled={page <= 1} onClick={onPrevious}>
            Précédent
          </Button>
          <Button variant="outlineLight" size="sm" disabled={page >= totalPages} onClick={onNext}>
            Suivant
          </Button>
        </div>
      </div>
    </>
  );
}
