import { Card } from "@/components/lab";
import {
  formatDateHeure,
  formaterDuree,
  libelleDecision,
  libelleNiveauValidation,
} from "@/components/technicien/libelles";
import type { LigneEssaiDetailTechnicienDto } from "@/api/technicien/types";

/** Liste immuable des validations (tous niveaux). */
function ListeValidations({ ligne }: { ligne: LigneEssaiDetailTechnicienDto }) {
  const validations = ligne.historiqueValidations;
  if (validations.length === 0) {
    return <p className="text-sm text-muted-foreground">Aucune validation enregistrée.</p>;
  }
  return (
    <ul className="space-y-2">
      {validations.map((v) => (
        <li
          key={v.id}
          className="rounded-2xl border border-brand-900/5 bg-surface-alt px-3 py-2"
        >
          <p className="text-sm font-semibold text-ink-900">
            {libelleNiveauValidation(v.niveau)} — {libelleDecision(v.decision)}
          </p>
          <p className="text-xs text-muted-foreground">
            {formatDateHeure(v.dateValidation)}
            {v.motif ? ` · ${v.motif}` : ""}
          </p>
        </li>
      ))}
    </ul>
  );
}

/** Carte regroupant l'historique des validations et le temps déclaré. */
export function HistoriqueEssai({ ligne }: { ligne: LigneEssaiDetailTechnicienDto }) {
  return (
    <Card variant="flat" padding="md" className="space-y-3">
      <h2 className="text-lg font-bold text-ink-900">Historique</h2>
      <div>
        <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Validations
        </p>
        <ListeValidations ligne={ligne} />
      </div>
      {ligne.dureeMinutes !== null && (
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Temps déclaré
          </p>
          <p className="text-sm font-semibold text-ink-900">
            {formaterDuree(ligne.dureeMinutes)}
          </p>
        </div>
      )}
    </Card>
  );
}
