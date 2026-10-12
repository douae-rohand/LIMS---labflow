import { ChevronUp, ChevronDown, ChevronsUpDown } from "lucide-react";
import { Skeleton } from "@/components/lab";
import { StatutEmployeBadge } from "./StatutEmployeBadge";
import { messageErreurApi } from "@/api/employes";
import type { EmployeDto, TriEmploye, DirectionTri } from "@/api/employes";

import { LIBELLE_ROLE, formatDate, formatDerniereConnexion } from "./dateUtils";

export type ColTri = Extract<TriEmploye, "nom" | "matricule" | "dateCreation" | "derniereConnexion">;

interface ColDef {
  label: string;
  triKey?: ColTri;
  scope: "col";
}

const COLONNES: ColDef[] = [
  { label: "Matricule", triKey: "matricule", scope: "col" },
  { label: "Nom complet", triKey: "nom", scope: "col" },
  { label: "Fonction", scope: "col" },
  { label: "Statut", scope: "col" },
  { label: "Dernière connexion", triKey: "derniereConnexion", scope: "col" },
  { label: "Créé le", triKey: "dateCreation", scope: "col" },
];

function TriIndicateur({
  col,
  tri,
  direction,
}: {
  col: ColTri;
  tri: TriEmploye;
  direction: DirectionTri;
}) {
  if (tri !== col) {
    return <ChevronsUpDown className="ml-1 size-3.5 opacity-40" aria-hidden="true" />;
  }
  return direction === "asc" ? (
    <ChevronUp className="ml-1 size-3.5" aria-hidden="true" />
  ) : (
    <ChevronDown className="ml-1 size-3.5" aria-hidden="true" />
  );
}

function ariaSort(
  col: ColTri | undefined,
  tri: TriEmploye,
  direction: DirectionTri,
): "ascending" | "descending" | "none" | undefined {
  if (!col || tri !== col) return "none";
  return direction === "asc" ? "ascending" : "descending";
}

interface EmployesTableProps {
  employes: EmployeDto[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  tri: TriEmploye;
  direction: DirectionTri;
  filtresActifs: boolean;
  employeSelectionneId?: number | undefined;
  onSelectEmploye?: ((id: number) => void) | undefined;
  onTriChange: (col: ColTri) => void;
  onReinitialiser: () => void;
  onReessayer: () => void;
}

export function EmployesTable({
  employes,
  isLoading,
  isError,
  error,
  tri,
  direction,
  filtresActifs,
  employeSelectionneId,
  onSelectEmploye,
  onTriChange,
  onReinitialiser,
  onReessayer,
}: EmployesTableProps) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-brand-900/10">
      <table className="w-full min-w-[700px] text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            {COLONNES.map((col) => (
              <th
                key={col.label}
                scope={col.scope}
                aria-sort={ariaSort(col.triKey, tri, direction)}
                className={
                  col.triKey
                    ? "cursor-pointer select-none px-4 py-3 font-semibold hover:bg-brand-900/5"
                    : "px-4 py-3 font-semibold"
                }
                onClick={col.triKey ? () => onTriChange(col.triKey!) : undefined}
              >
                <span className="inline-flex items-center">
                  {col.label}
                  {col.triKey && (
                    <TriIndicateur col={col.triKey} tri={tri} direction={direction} />
                  )}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {/* État de chargement initial */}
          {isLoading && employes === undefined && (
            <>
              {Array.from({ length: 10 }).map((_, i) => (
                <tr key={i} className="border-t border-brand-900/10">
                  {COLONNES.map((col) => (
                    <td key={col.label} className="px-4 py-3">
                      <Skeleton className="h-4 w-full" />
                    </td>
                  ))}
                </tr>
              ))}
            </>
          )}

          {/* État d'erreur */}
          {isError && (
            <tr>
              <td colSpan={COLONNES.length} className="px-4 py-8 text-center">
                <p className="mb-3 text-sm text-red-600">
                  {messageErreurApi(error)}
                </p>
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

          {/* Lignes de données */}
          {!isLoading && !isError && employes !== undefined && employes.length > 0 && (
            <>
              {employes.map((emp) => {
                const estSelectionne = emp.id === employeSelectionneId;
                return (
                  <tr
                    key={emp.id}
                    onClick={onSelectEmploye ? () => onSelectEmploye(emp.id) : undefined}
                    className={`border-t border-brand-900/10 transition-colors ${
                      estSelectionne
                        ? "bg-brand-900/5 hover:bg-brand-900/10"
                        : "hover:bg-surface-card/60"
                    } ${onSelectEmploye ? "cursor-pointer" : ""}`}
                  >
                    <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                      {emp.matricule}
                    </td>
                    <td className="px-4 py-3">
                      {onSelectEmploye ? (
                        <button
                          type="button"
                          id={`btn-employe-${emp.id}`}
                          aria-label={`Voir le détail de ${emp.nomComplet}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            onSelectEmploye(emp.id);
                          }}
                          className="text-left font-semibold text-ink-900 hover:text-brand-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900 focus-visible:ring-offset-2 rounded"
                        >
                          {emp.nomComplet}
                        </button>
                      ) : (
                        <p className="font-semibold text-ink-900">{emp.nomComplet}</p>
                      )}
                      <p className="text-xs text-muted-foreground">{emp.email}</p>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {LIBELLE_ROLE[emp.role] ?? emp.role}
                    </td>
                    <td className="px-4 py-3">
                      <StatutEmployeBadge statut={emp.statut} />
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {formatDerniereConnexion(emp.derniereConnexion)}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {formatDate(emp.dateCreation)}
                    </td>
                  </tr>
                );
              })}
            </>
          )}

          {/* État vide */}
          {!isLoading && !isError && employes !== undefined && employes.length === 0 && (
            <tr>
              <td colSpan={COLONNES.length} className="px-4 py-10 text-center text-sm text-muted-foreground">
                {filtresActifs ? (
                  <>
                    <p>Aucun employé ne correspond à vos filtres.</p>
                    <button
                      type="button"
                      onClick={onReinitialiser}
                      className="mt-3 rounded-full border border-brand-900/20 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                    >
                      Réinitialiser les filtres
                    </button>
                  </>
                ) : (
                  <p>Aucun employé dans ce laboratoire pour l'instant.</p>
                )}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
