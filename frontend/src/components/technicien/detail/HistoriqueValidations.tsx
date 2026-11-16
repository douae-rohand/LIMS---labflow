import { CheckCircle2, Clock, RotateCcw, XCircle } from "lucide-react";
import { Card } from "@/components/lab";
import {
  formatDateHeure,
  formaterDuree,
  libelleNiveauValidation,
} from "@/components/technicien/libelles";
import type { LigneEssaiDetailTechnicienDto } from "@/api/technicien/types";

type Decision = "VALIDEE" | "REVISION_DEMANDEE" | "REJETEE" | string;

function iconeDecision(decision: Decision) {
  if (decision === "VALIDEE") return <CheckCircle2 className="size-4 text-green-600 shrink-0" aria-hidden="true" />;
  if (decision === "REJETEE") return <XCircle className="size-4 text-red-500 shrink-0" aria-hidden="true" />;
  if (decision === "REVISION_DEMANDEE") return <RotateCcw className="size-4 text-amber-500 shrink-0" aria-hidden="true" />;
  return null;
}

function libelleDecision(decision: Decision): string {
  if (decision === "VALIDEE") return "Validée";
  if (decision === "REJETEE") return "Rejetée";
  if (decision === "REVISION_DEMANDEE") return "Révision demandée";
  return decision;
}

function couleurDecision(decision: Decision): string {
  if (decision === "VALIDEE") return "text-green-700";
  if (decision === "REJETEE") return "text-red-600";
  if (decision === "REVISION_DEMANDEE") return "text-amber-700";
  return "text-ink-900";
}

function ListeValidations({ ligne }: { ligne: LigneEssaiDetailTechnicienDto }) {
  const validations = ligne.historiqueValidations;
  if (validations.length === 0) {
    return (
      <p className="text-sm text-muted-foreground italic">Aucune validation enregistrée.</p>
    );
  }
  return (
    <ul className="space-y-2">
      {validations.map((v) => (
        <li
          key={v.id}
          className="flex items-start gap-3 rounded-2xl border border-brand-900/8 bg-surface-alt px-3 py-2.5"
        >
          <div className="mt-0.5">{iconeDecision(v.decision)}</div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-xs font-semibold text-muted-foreground">
                {libelleNiveauValidation(v.niveau)}
              </span>
              <span className={`text-sm font-semibold ${couleurDecision(v.decision)}`}>
                {libelleDecision(v.decision)}
              </span>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              {formatDateHeure(v.dateValidation)}
            </p>
            {v.motif && (
              <p className="mt-1 text-xs text-ink-900 italic">« {v.motif} »</p>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}

export function HistoriqueEssai({ ligne }: { ligne: LigneEssaiDetailTechnicienDto }) {
  return (
    <Card variant="flat" padding="md" className="space-y-4">
      <h2 className="text-base font-bold text-ink-900">Historique</h2>

      {/* Temps déclaré */}
      {ligne.dureeMinutes !== null && (
        <div className="flex items-center gap-3 rounded-2xl border border-brand-900/8 bg-surface-alt px-3 py-2.5">
          <Clock className="size-4 text-brand-900 shrink-0" aria-hidden="true" />
          <div>
            <p className="text-xs text-muted-foreground">Temps déclaré</p>
            <p className="text-sm font-semibold text-ink-900">{formaterDuree(ligne.dureeMinutes)}</p>
          </div>
        </div>
      )}

      {/* Validations */}
      <div className="space-y-2">
        <p className="text-xs font-semibold text-muted-foreground">Validations</p>
        <ListeValidations ligne={ligne} />
      </div>
    </Card>
  );
}
