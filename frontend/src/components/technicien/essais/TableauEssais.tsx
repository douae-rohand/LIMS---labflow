import { Link } from "@tanstack/react-router";
import { ChevronRight, FlaskConical, Play } from "lucide-react";
import { Button, Skeleton } from "@/components/lab";
import {
  classeConformite,
  classeStatutEssai,
  formatDateHeure,
  libelleConformite,
  libelleStatutEssai,
} from "@/components/technicien/libelles";
import type { LigneEssaiTechnicienDto } from "@/api/technicien/types";
import { ApiError } from "@/api/axios";
import { cn } from "@/lib/utils";

/** Libellé du bouton d'action rapide selon le statut. */
function actionRapide(statut: string | null): { label: string; icon: typeof Play } {
  switch (statut) {
    case "ATTENTE_ECHANTILLON":
      return { label: "Démarrer", icon: Play };
    case "EN_COURS":
      return { label: "Saisir", icon: FlaskConical };
    default:
      return { label: "Détail", icon: ChevronRight };
  }
}

function LigneTableau({ essai }: { essai: LigneEssaiTechnicienDto }) {
  const action = actionRapide(essai.statut);
  const ActionIcon = action.icon;
  return (
    <tr className="border-b border-brand-900/5 last:border-0 hover:bg-brand-900/[0.02]">
      <td className="px-4 py-3">
        <span className="font-mono text-xs font-semibold text-ink-900">{essai.code}</span>
      </td>
      <td className="px-4 py-3">
        <p className="text-sm font-semibold text-ink-900">{essai.essai.designation}</p>
        <p className="text-xs text-muted-foreground">{essai.essai.code}</p>
      </td>
      <td className="px-4 py-3 text-sm text-ink-900">{essai.demandeNumero}</td>
      <td className="px-4 py-3">
        <span
          className={cn(
            "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
            classeStatutEssai(essai.statut),
          )}
        >
          {libelleStatutEssai(essai.statut)}
        </span>
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {formatDateHeure(essai.dateAttribution)}
      </td>
      <td className="px-4 py-3">
        {essai.enRetard ? (
          <span className="text-xs font-semibold text-red-700">En retard</span>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        )}
      </td>
      <td className="px-4 py-3">
        {essai.valeur ? (
          <span
            className={cn(
              "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
              classeConformite(essai.niveauConformite),
            )}
          >
            {libelleConformite(essai.niveauConformite)}
          </span>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        )}
      </td>
      <td className="px-4 py-3 text-right">
        <Link
          to="/app/essais/$ligneEssaiId"
          params={{ ligneEssaiId: String(essai.ligneEssaiId) }}
        >
          <Button
            variant={essai.statut === "EN_COURS" ? "primary" : "secondary"}
            size="sm"
            icon={<ActionIcon className="size-4" aria-hidden="true" />}
            iconPosition="left"
          >
            {action.label}
          </Button>
        </Link>
      </td>
    </tr>
  );
}

/**
 * Tableau natif stylisé pour desktop (≥ md).
 * scope="col" sur les en-têtes, scope="row" implicite via les cellules.
 */
export function TableauEssais({
  essais,
  isLoading,
  isError,
  error,
  onReessayer,
}: {
  essais: LigneEssaiTechnicienDto[] | undefined;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  onReessayer: () => void;
}) {
  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full rounded-2xl" />
        ))}
      </div>
    );
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 403) {
      return (
        <div className="rounded-3xl border border-amber-200 bg-amber-50 p-6 text-center">
          <p className="text-sm font-medium text-amber-700" role="alert">
            Vous n'avez pas la permission de consulter vos essais.
          </p>
        </div>
      );
    }
    return (
      <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-center">
        <p className="text-sm font-medium text-red-700" role="alert">
          Impossible de charger la liste.
        </p>
        <Button variant="secondary" size="sm" className="mt-3" onClick={onReessayer}>
          Réessayer
        </Button>
      </div>
    );
  }

  if (!essais || essais.length === 0) {
    return null;
  }

  return (
    <div className="overflow-x-auto rounded-3xl border border-brand-900/10 bg-card">
      <table className="w-full text-left">
        <thead className="bg-surface-alt text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          <tr>
            <th scope="col" className="px-4 py-3">Ligne</th>
            <th scope="col" className="px-4 py-3">Essai</th>
            <th scope="col" className="px-4 py-3">Demande</th>
            <th scope="col" className="px-4 py-3">Statut</th>
            <th scope="col" className="px-4 py-3">Attribution</th>
            <th scope="col" className="px-4 py-3">Retard</th>
            <th scope="col" className="px-4 py-3">Conformité</th>
            <th scope="col" className="px-4 py-3 text-right">Action</th>
          </tr>
        </thead>
        <tbody>
          {essais.map((essai) => (
            <LigneTableau key={essai.ligneEssaiId} essai={essai} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
