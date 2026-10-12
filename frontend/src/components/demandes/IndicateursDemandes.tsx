import { CheckCircle2, Clock3, XCircle } from "lucide-react";
import type { IndicateursDemandes, StatutDemande } from "@/api/demandes";

interface IndicateursDemandesProps {
  indicateurs: IndicateursDemandes | undefined;
  statut: StatutDemande | undefined;
  onStatut: (statut: StatutDemande | undefined) => void;
}

const CARTES: {
  cle: keyof Pick<IndicateursDemandes, "enAttente" | "acceptees" | "refusees">;
  statut: StatutDemande;
  libelle: string;
  icon: typeof Clock3;
}[] = [
  { cle: "enAttente", statut: "SOUMISE", libelle: "En attente", icon: Clock3 },
  { cle: "acceptees", statut: "ACCEPTEE", libelle: "Acceptées", icon: CheckCircle2 },
  { cle: "refusees", statut: "REJETEE", libelle: "Refusées", icon: XCircle },
];

export function IndicateursDemandesCartes({
  indicateurs,
  statut,
  onStatut,
}: IndicateursDemandesProps) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      {CARTES.map((carte) => {
        const actif = statut === carte.statut;
        const Icon = carte.icon;
        const valeur = indicateurs?.[carte.cle];
        return (
          <button
            key={carte.cle}
            type="button"
            aria-pressed={actif}
            onClick={() => onStatut(actif ? undefined : carte.statut)}
            className={`rounded-3xl border px-4 py-4 text-left transition-colors ${
              actif
                ? "border-brand-900 bg-brand-900 text-primary-foreground"
                : "border-brand-900/10 bg-card hover:bg-brand-900/5"
            }`}
          >
            <span className="flex items-center justify-between gap-3">
              <span className={`text-sm font-semibold ${actif ? "text-primary-foreground" : "text-muted-foreground"}`}>
                {carte.libelle}
              </span>
              <Icon className="size-4 shrink-0" aria-hidden="true" />
            </span>
            <span className={`mt-2 block text-3xl font-extrabold ${actif ? "text-primary-foreground" : "text-brand-900"}`}>
              {valeur ?? "—"}
            </span>
          </button>
        );
      })}
    </div>
  );
}
