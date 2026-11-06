import { ChevronDown, ChevronUp, ChevronsUpDown } from "lucide-react";
import { Skeleton } from "@/components/lab";
import { messageErreurDemande, type DemandeResume } from "@/api/demandes";
import { StatutDemandeBadge } from "./StatutDemandeBadge";
import { formatDate } from "@/components/employes/dateUtils";

type ColTriDemande = "reference" | "dateSoumission";
type DirectionTri = "asc" | "desc";

interface DemandesTableProps {
  demandes: DemandeResume[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  filtresActifs: boolean;
  demandeSelectionneeId?: number | undefined;
  onSelect: (id: number) => void;
  onReinitialiser: () => void;
  onReessayer: () => void;
  tri: ColTriDemande;
  direction: DirectionTri;
  onTriChange: (colonne: ColTriDemande) => void;
}

function EnteteTri({
  label,
  colonne,
  tri,
  direction,
  onTriChange,
}: {
  label: string;
  colonne: ColTriDemande;
  tri: ColTriDemande;
  direction: DirectionTri;
  onTriChange: (colonne: ColTriDemande) => void;
}) {
  const active = tri === colonne;
  const Icon = !active ? ChevronsUpDown : direction === "asc" ? ChevronUp : ChevronDown;

  return (
    <th
      scope="col"
      aria-sort={!active ? "none" : direction === "asc" ? "ascending" : "descending"}
      className="px-4 py-3 font-semibold"
    >
      <button
        type="button"
        onClick={() => onTriChange(colonne)}
        className="inline-flex items-center gap-1 text-left hover:text-ink-900 focus-visible:outline-2 focus-visible:outline-brand-900"
      >
        {label}
        <Icon className={`size-3.5 ${active ? "" : "opacity-40"}`} aria-hidden="true" />
      </button>
    </th>
  );
}

export function DemandesTable({
  demandes,
  isLoading,
  isError,
  error,
  filtresActifs,
  demandeSelectionneeId,
  onSelect,
  onReinitialiser,
  onReessayer,
  tri,
  direction,
  onTriChange,
}: DemandesTableProps) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-180 text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <EnteteTri label="Référence" colonne="reference" tri={tri} direction={direction} onTriChange={onTriChange} />
            <th scope="col" className="px-4 py-3 font-semibold">Intitulé</th>
            <th scope="col" className="px-4 py-3 font-semibold">Client</th>
            <th scope="col" className="px-4 py-3 font-semibold">Statut</th>
            <EnteteTri label="Soumise le" colonne="dateSoumission" tri={tri} direction={direction} onTriChange={onTriChange} />
          </tr>
        </thead>
        <tbody>
          {isLoading && demandes === undefined &&
            Array.from({ length: 6 }).map((_, i) => (
              <tr key={i} className="border-t border-brand-900/10">
                {Array.from({ length: 5 }).map((__, j) => (
                  <td key={j} className="px-4 py-3">
                    <Skeleton className="h-4 w-full" />
                  </td>
                ))}
              </tr>
            ))}

          {isError && (
            <tr>
              <td colSpan={5} className="px-4 py-8 text-center">
                <p className="mb-3 text-sm text-red-600">{messageErreurDemande(error)}</p>
                <button
                  type="button"
                  onClick={onReessayer}
                  className="rounded-full border border-brand-900/20 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                >
                  Réessayer
                </button>
              </td>
            </tr>
          )}

          {!isLoading && !isError && demandes !== undefined && demandes.length > 0 &&
            demandes.map((demande) => {
              const selectionnee = demande.id === demandeSelectionneeId;
              return (
                <tr
                  key={demande.id}
                  onClick={() => onSelect(demande.id)}
                  className={`cursor-pointer border-t border-brand-900/10 transition-colors ${
                    selectionnee ? "bg-brand-900/5 hover:bg-brand-900/10" : "hover:bg-surface-card/60"
                  }`}
                >
                  <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                    {demande.reference}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelect(demande.id);
                      }}
                      className="rounded text-left font-semibold text-ink-900 no-underline hover:text-brand-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900"
                    >
                      {demande.objet}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {demande.demandeur || "—"}
                  </td>
                  <td className="px-4 py-3">
                    <StatutDemandeBadge statut={demande.statut} />
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {formatDate(demande.dateSoumission) || "—"}
                  </td>
                </tr>
              );
            })}

          {!isLoading && !isError && demandes !== undefined && demandes.length === 0 && (
            <tr>
              <td colSpan={5} className="px-4 py-10 text-center text-sm text-muted-foreground">
                {filtresActifs ? (
                  <>
                    <p>Aucune demande ne correspond à vos filtres.</p>
                    <button
                      type="button"
                      onClick={onReinitialiser}
                      className="mt-3 rounded-full border border-brand-900/20 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                    >
                      Réinitialiser les filtres
                    </button>
                  </>
                ) : (
                  <p>Aucune demande reçue pour ce laboratoire.</p>
                )}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
