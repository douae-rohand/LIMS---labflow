import { AlertTriangle, CheckCircle2, FlaskConical } from "lucide-react";
import { Card } from "@/components/lab";
import type {
  EchantillonRefDto,
  ExigenceEchantillonRefDto,
} from "@/api/technicien/types";

/** Affiche une exigence d'échantillon (nature + état + quantité). */
function LigneExigence({ exigence }: { exigence: ExigenceEchantillonRefDto }) {
  const quantite =
    exigence.quantiteMinimale !== null && exigence.quantiteMinimale !== undefined
      ? `${exigence.quantiteMinimale}${exigence.unite ? " " + exigence.unite : ""}`
      : "—";
  return (
    <li className="flex flex-col gap-0.5 rounded-2xl border border-brand-900/5 bg-surface-alt px-3 py-2 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-semibold text-ink-900">{exigence.nature}</p>
        <p className="text-xs text-muted-foreground">
          Conservation : {exigence.etatConservation}
        </p>
      </div>
      <p className="text-xs font-medium text-muted-foreground">
        Quantité min. : <span className="text-ink-900">{quantite}</span>
      </p>
    </li>
  );
}

/**
 * Bloc échantillon : référence, nature, conformité, état constaté.
 * Affiche les exigences attendues et une alerte si l'échantillon est
 * manquant ou non conforme.
 */
export function BlocEchantillon({
  echantillon,
  exigences,
}: {
  echantillon: EchantillonRefDto | null;
  exigences: ExigenceEchantillonRefDto[];
}) {
  const manquant = echantillon === null;
  const nonConforme = echantillon !== null && echantillon.conformite === false;

  return (
    <Card variant="flat" padding="md" className="space-y-4">
      <div className="flex items-center gap-2">
        <FlaskConical className="size-5 text-brand-900" aria-hidden="true" />
        <h2 className="text-lg font-bold text-ink-900">Échantillon</h2>
      </div>

      {/* Alerte manquant */}
      {manquant && (
        <div
          className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4"
          role="alert"
        >
          <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-amber-900">
              Aucun échantillon rattaché
            </p>
            <p className="mt-0.5 text-xs text-amber-800">
              L'essai ne peut pas démarrer tant qu'aucun échantillon n'est associé.
            </p>
          </div>
        </div>
      )}

      {/* Alerte non conforme */}
      {nonConforme && (
        <div
          className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4"
          role="alert"
        >
          <AlertTriangle className="mt-0.5 size-5 shrink-0 text-red-700" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-red-900">
              Échantillon non conforme
            </p>
            <p className="mt-0.5 text-xs text-red-800">
              L'essai ne peut pas démarrer car l'échantillon a été déclaré non conforme
              à la réception.
            </p>
          </div>
        </div>
      )}

      {/* Détails échantillon */}
      {echantillon !== null && (
        <dl className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <dt className="text-xs font-medium text-muted-foreground">Référence</dt>
            <dd className="font-mono font-semibold text-ink-900">
              {echantillon.reference}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-muted-foreground">Nature</dt>
            <dd className="font-semibold text-ink-900">{echantillon.nature}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-muted-foreground">Conformité</dt>
            <dd className="flex items-center gap-1.5 font-semibold">
              {echantillon.conformite === true ? (
                <>
                  <CheckCircle2 className="size-4 text-green-600" aria-hidden="true" />
                  <span className="text-green-700">Conforme</span>
                </>
              ) : echantillon.conformite === false ? (
                <span className="text-red-700">Non conforme</span>
              ) : (
                <span className="text-muted-foreground">Non évaluée</span>
              )}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-muted-foreground">État constaté</dt>
            <dd className="text-ink-900">
              {echantillon.etatConservationConstate ?? "—"}
            </dd>
          </div>
        </dl>
      )}

      {/* Exigences attendues */}
      {exigences.length > 0 && (
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Exigences d'échantillon attendues
          </p>
          <ul className="space-y-2">
            {exigences.map((ex, i) => (
              <LigneExigence key={`${ex.nature}-${i}`} exigence={ex} />
            ))}
          </ul>
        </div>
      )}
    </Card>
  );
}
