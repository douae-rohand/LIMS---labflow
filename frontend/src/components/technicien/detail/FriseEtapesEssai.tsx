import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

type EtatEtape = "fait" | "courante" | "aVenir";

interface Etape {
  key: string;
  label: string;
  etat: EtatEtape;
}

/**
 * Frise d'étapes de l'essai : Démarré → Résultat saisi → Validé techniquement.
 * 3 étapes en flex, libellé textuel, aria-current sur l'étape courante.
 * Jamais la couleur seule : chaque étape porte un texte d'état lisible.
 */
export function FriseEtapesEssai({
  demarre,
  resultatSaisi,
  valideTechniquement,
}: {
  demarre: boolean;
  resultatSaisi: boolean;
  valideTechniquement: boolean;
}) {
  const etapes: Etape[] = [
    {
      key: "demarre",
      label: "Démarré",
      etat: valideTechniquement || resultatSaisi || demarre ? "fait" : "courante",
    },
    {
      key: "resultat",
      label: "Résultat saisi",
      etat: valideTechniquement
        ? "fait"
        : resultatSaisi
          ? "courante"
          : demarre
            ? "courante"
            : "aVenir",
    },
    {
      key: "validation",
      label: "Validé techniquement",
      etat: valideTechniquement ? "courante" : "aVenir",
    },
  ];

  // L'étape « courante » à mettre en avant : la première non terminée.
  const indexCourant = etapes.findIndex((e) => e.etat === "courante");

  return (
    <ol
      className="flex items-center gap-2 sm:gap-4"
      aria-label="Progression de l'essai"
    >
      {etapes.map((etape, i) => {
        const estCourant = i === indexCourant;
        const libelleEtat =
          etape.etat === "fait"
            ? "Terminé"
            : etape.etat === "courante"
              ? "En cours"
              : "À venir";
        return (
          <li key={etape.key} className="flex flex-1 items-center gap-2 sm:gap-4">
            <div className="flex flex-1 flex-col items-center text-center">
              <div
                className={cn(
                  "flex size-10 items-center justify-center rounded-full border-2 text-sm font-bold transition-colors",
                  etape.etat === "fait" && "bg-brand-900 border-brand-900 text-primary-foreground",
                  etape.etat === "courante" && "bg-accent-lime border-accent-lime text-ink-900",
                  etape.etat === "aVenir" && "bg-card border-brand-900/15 text-muted-foreground",
                )}
                aria-current={estCourant ? "step" : undefined}
              >
                {etape.etat === "fait" ? (
                  <Check className="size-5" aria-hidden="true" />
                ) : (
                  i + 1
                )}
              </div>
              <p
                className={cn(
                  "mt-2 text-xs font-semibold",
                  etape.etat === "aVenir" ? "text-muted-foreground" : "text-ink-900",
                )}
              >
                {etape.label}
              </p>
              <p className="text-[11px] text-muted-foreground">{libelleEtat}</p>
            </div>
            {i < etapes.length - 1 && (
              <div
                className={cn(
                  "h-0.5 flex-1 rounded-full",
                  etapes[i + 1] !== undefined && etape.etat === "fait"
                    ? "bg-brand-900"
                    : "bg-brand-900/15",
                )}
                aria-hidden="true"
              />
            )}
          </li>
        );
      })}
    </ol>
  );
}
