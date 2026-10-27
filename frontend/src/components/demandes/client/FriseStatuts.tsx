import { Check } from "lucide-react";
import { LIBELLE_STATUT_DEMANDE, type StatutDemande } from "@/api/demandes";
import { cn } from "@/lib/utils";
import { formatDate } from "./libelles";

interface EtapeFrise {
  statut: StatutDemande;
  description: string;
}

/** Étapes linéaires d'une demande menée jusqu'à sa clôture. */
const ETAPES: EtapeFrise[] = [
  { statut: "NOUVEAU", description: "Demande transmise au laboratoire." },
  { statut: "ACCEPTEE", description: "Prise en charge par le laboratoire." },
  { statut: "EN_COURS", description: "Analyses en cours de réalisation." },
  { statut: "EN_VALIDATION", description: "Contrôle et validation des résultats." },
  { statut: "TERMINEE", description: "Demande clôturée, résultats disponibles." },
];

const STATUTS_ARRET: StatutDemande[] = ["ANNULEE", "REFUSEE"];

/**
 * Frise d'avancement d'une demande. Toute étape strictement antérieure à
 * l'étape courante est affichée comme complétée. Une demande annulée ou
 * refusée affiche un bandeau daté (date lue dans l'historique).
 */
export function FriseStatuts({
  statut,
  dateArret = null,
}: {
  statut: StatutDemande;
  dateArret?: string | null;
}) {
  const arretee = STATUTS_ARRET.includes(statut);
  const indexCourant = arretee ? 0 : ETAPES.findIndex((etape) => etape.statut === statut);

  return (
    <div className="flex flex-col gap-4">
      {arretee ? (
        <p
          role="status"
          className="rounded-2xl border border-brand-900/10 bg-surface-card px-4 py-3 text-sm text-ink-900"
        >
          Demande {statut === "ANNULEE" ? "annulée" : "refusée"} le{" "}
          {dateArret !== null ? formatDate(dateArret) : "date non disponible"}.
        </p>
      ) : null}

      <ol
        aria-label="Avancement de la demande"
        className="flex flex-col gap-4 sm:flex-row sm:items-start sm:gap-2"
      >
        {ETAPES.map((etape, index) => {
          const complete = index < indexCourant;
          const courante = index === indexCourant;
          return (
            <li
              key={etape.statut}
              aria-current={courante ? "step" : undefined}
              className="flex flex-1 items-start gap-3 sm:flex-col sm:gap-2"
            >
              <span
                aria-hidden="true"
                className={cn(
                  "flex size-7 shrink-0 items-center justify-center rounded-full border text-xs font-bold",
                  complete && "border-brand-900 bg-brand-900 text-primary-foreground",
                  courante && "border-brand-900 bg-accent-lime text-ink-900",
                  !complete && !courante && "border-brand-900/20 bg-card text-muted-foreground",
                )}
              >
                {complete ? <Check className="size-3.5" /> : index + 1}
              </span>
              <span className="flex flex-col gap-0.5 sm:gap-1">
                <span
                  className={cn(
                    "text-sm font-semibold",
                    courante ? "text-brand-900" : "text-ink-900",
                  )}
                >
                  {LIBELLE_STATUT_DEMANDE[etape.statut]}
                </span>
                <span className="text-xs text-muted-foreground">{etape.description}</span>
              </span>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
