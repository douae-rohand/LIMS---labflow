import { Link } from "@tanstack/react-router";
import { ChevronRight, FlaskConical, Play } from "lucide-react";
import { Button, Card } from "@/components/lab";
import {
  classeConformite,
  classeStatutEssai,
  formatDateHeure,
  libelleConformite,
  libelleStatutEssai,
} from "@/components/technicien/libelles";
import type { LigneEssaiTechnicienDto } from "@/api/technicien/types";
import { cn } from "@/lib/utils";

/** Libellé du bouton d'action rapide selon le statut. */
function actionRapide(statut: string | null): { label: string; icon: typeof Play } {
  switch (statut) {
    case "ATTENTE_ECHANTILLON":
      return { label: "Démarrer", icon: Play };
    case "EN_COURS":
      return { label: "Saisir", icon: FlaskConical };
    case "TERMINE":
      return { label: "Détail", icon: ChevronRight };
    case "NON_REALISABLE":
      return { label: "Détail", icon: ChevronRight };
    default:
      return { label: "Détail", icon: ChevronRight };
  }
}

/** Carte d'essai pour la vue mobile (et secondairement desktop si besoin). */
export function LigneEssaiCard({ essai }: { essai: LigneEssaiTechnicienDto }) {
  const action = actionRapide(essai.statut);
  const ActionIcon = action.icon;
  return (
    <Card variant="surface" padding="none" className="overflow-hidden">
      <div className="flex flex-col gap-3 p-4">
        <div className="flex flex-wrap items-center gap-2">
          <span
            className={cn(
              "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
              classeStatutEssai(essai.statut),
            )}
          >
            {libelleStatutEssai(essai.statut)}
          </span>
          {essai.enRetard && (
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

        <div className="min-w-0">
          <p className="truncate text-sm font-bold text-ink-900">
            {essai.essai.code} — {essai.essai.designation}
          </p>
          <p className="mt-0.5 truncate text-xs text-muted-foreground">
            {essai.demandeNumero} · ligne {essai.code}
          </p>
        </div>

        <div className="flex items-center justify-between gap-2">
          <p className="text-xs text-muted-foreground">
            Attribué le {formatDateHeure(essai.dateAttribution)}
          </p>
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
        </div>
      </div>
    </Card>
  );
}
