import { Link } from "@tanstack/react-router";
import { ChevronRight, Clock, FlaskConical, Play } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import {
  classeStatutEssai,
  classeConformite,
  formatDateHeure,
  libelleConformite,
  libelleStatutEssai,
} from "@/components/technicien/libelles";
import { ApiError } from "@/api/axios";
import type { LigneEssaiTechnicienDto } from "@/api/technicien/types";
import { useTechnicienEssais } from "@/hooks/useTechnicienEssais";
import { cn } from "@/lib/utils";

/** Détermine le libellé du bouton d'action contextuel selon le statut. */
function actionContextuelle(statut: string | null): { label: string; icon: typeof Play } {
  switch (statut) {
    case "ATTENTE_ECHANTILLON":
      return { label: "Démarrer", icon: Play };
    case "EN_COURS":
      return { label: "Saisir le résultat", icon: FlaskConical };
    default:
      return { label: "Voir le détail", icon: ChevronRight };
  }
}

/** Une carte d'essai dans la liste des prochains. */
function CarteEssai({ essai }: { essai: LigneEssaiTechnicienDto }) {
  const action = actionContextuelle(essai.statut);
  const ActionIcon = action.icon;
  const enRetard = essai.enRetard;
  return (
    <Card variant="surface" padding="none" className="overflow-hidden">
      <div className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={cn(
                "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
                classeStatutEssai(essai.statut),
              )}
            >
              {libelleStatutEssai(essai.statut)}
            </span>
            {enRetard && (
              <span className="inline-flex items-center rounded-full border border-red-200 bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-red-700">
                En retard
              </span>
            )}
            {essai.valeur && (
              <span
                className={cn(
                  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
                  classeConformite(essai.niveauConformite),
                )}
              >
                {libelleConformite(essai.niveauConformite)}
              </span>
            )}
          </div>
          <p className="mt-2 truncate text-sm font-bold text-ink-900">
            {essai.essai.code} — {essai.essai.designation}
          </p>
          <p className="mt-0.5 truncate text-xs text-muted-foreground">
            {essai.demandeNumero} · {essai.code}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            Attribué le {formatDateHeure(essai.dateAttribution)}
          </p>
        </div>
        <Link
          to="/app/essais/$ligneEssaiId"
          params={{ ligneEssaiId: String(essai.ligneEssaiId) }}
          className="shrink-0"
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
      </div>
    </Card>
  );
}

/** État vide chaleureux. */
function EtatVide() {
  return (
    <Card variant="flat" padding="lg" className="text-center">
      <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-brand-900/10">
        <Clock className="size-6 text-brand-900" aria-hidden="true" />
      </div>
      <p className="mt-3 text-sm font-medium text-ink-900">Aucun essai attribué pour le moment</p>
      <p className="mt-1 text-xs text-muted-foreground">
        Les essais attribués par le responsable apparaîtront ici.
      </p>
    </Card>
  );
}

/** État erreur : bouton Réessayer via invalidation (la requête re-tentera au focus). */
function EtatErreur() {
  return (
    <Card variant="flat" padding="lg" className="text-center">
      <p className="text-sm font-medium text-red-700" role="alert">
        Impossible de charger vos essais.
      </p>
      <p className="mt-1 text-xs text-muted-foreground">Réessayez en rechargeant la page.</p>
    </Card>
  );
}

/**
 * Liste des 5 prochains essais du technicien (triés par date d'attribution).
 * Bouton d'action contextuel selon le statut.
 */
export function ProchainsEssais() {
  const query = useTechnicienEssais({ page: 1, size: 5, tri: "dateAttribution", direction: "asc" });
  const { data, isLoading, isError, error } = query;

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-3xl" />
        ))}
      </div>
    );
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 403) {
      return (
        <Card variant="flat" padding="lg" className="text-center">
          <p className="text-sm font-medium text-amber-700" role="alert">
            Vous n'avez pas la permission de consulter vos essais.
          </p>
        </Card>
      );
    }
    return <EtatErreur />;
  }

  const essais = data?.content ?? [];
  if (essais.length === 0) {
    return <EtatVide />;
  }

  return (
    <div className="space-y-3">
      {essais.map((essai) => (
        <CarteEssai key={essai.ligneEssaiId} essai={essai} />
      ))}
    </div>
  );
}
