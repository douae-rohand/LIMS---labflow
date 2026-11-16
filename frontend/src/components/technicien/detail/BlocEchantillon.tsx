import { AlertTriangle, CheckCircle2, FlaskConical, XCircle } from "lucide-react";
import { Card } from "@/components/lab";
import type {
  EchantillonRefDto,
  ExigenceEchantillonRefDto,
} from "@/api/technicien/types";

function LigneExigence({ exigence }: { exigence: ExigenceEchantillonRefDto }) {
  const quantite =
    exigence.quantiteMinimale !== null && exigence.quantiteMinimale !== undefined
      ? `${exigence.quantiteMinimale}${exigence.unite ? " " + exigence.unite : ""}`
      : "—";
  return (
    <li className="flex flex-col gap-0.5 rounded-2xl border border-brand-900/8 bg-surface-alt px-3 py-2.5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-semibold text-ink-900">{exigence.nature}</p>
        <p className="text-xs text-muted-foreground">
          Conservation : {exigence.etatConservation ?? "—"}
        </p>
      </div>
      <span className="text-xs text-muted-foreground">
        Qté min. <span className="font-semibold text-ink-900">{quantite}</span>
      </span>
    </li>
  );
}

function BadgeConformite({ conformite }: { conformite: boolean | null }) {
  if (conformite === true) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-green-50 border border-green-200 px-2 py-0.5 text-xs font-semibold text-green-700">
        <CheckCircle2 className="size-3.5" aria-hidden="true" /> Conforme
      </span>
    );
  }
  if (conformite === false) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-red-50 border border-red-200 px-2 py-0.5 text-xs font-semibold text-red-700">
        <XCircle className="size-3.5" aria-hidden="true" /> Non conforme
      </span>
    );
  }
  return <span className="text-sm text-muted-foreground">Non évaluée</span>;
}

export function BlocEchantillon({
  echantillon,
  exigences,
}: {
  echantillon: EchantillonRefDto | null;
  exigences: ExigenceEchantillonRefDto[];
}) {
  const exigencesAffichees = exigences ?? [];
  const manquant = echantillon === null;
  const nonConforme = echantillon !== null && echantillon.conformite === false;

  return (
    <Card variant="flat" padding="md" className="space-y-4">
      <div className="flex items-center gap-2">
        <FlaskConical className="size-5 text-brand-900" aria-hidden="true" />
        <h2 className="text-base font-bold text-ink-900">Échantillon</h2>
      </div>

      {manquant && (
        <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4" role="alert">
          <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-700" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-amber-900">Aucun échantillon rattaché</p>
            <p className="mt-0.5 text-xs text-amber-800">
              L'essai ne peut pas démarrer tant qu'aucun échantillon n'est associé.
            </p>
          </div>
        </div>
      )}

      {nonConforme && (
        <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4" role="alert">
          <AlertTriangle className="mt-0.5 size-5 shrink-0 text-red-700" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-red-900">Échantillon non conforme</p>
            <p className="mt-0.5 text-xs text-red-800">
              L'essai ne peut pas démarrer car l'échantillon a été déclaré non conforme à la réception.
            </p>
          </div>
        </div>
      )}

      {echantillon !== null && (
        <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
          <div>
            <dt className="text-xs text-muted-foreground mb-0.5">Référence</dt>
            <dd className="font-mono font-semibold text-ink-900">{echantillon.reference}</dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground mb-0.5">Nature</dt>
            <dd className="font-semibold text-ink-900">{echantillon.nature}</dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground mb-0.5">Conformité</dt>
            <dd><BadgeConformite conformite={echantillon.conformite} /></dd>
          </div>
          <div>
            <dt className="text-xs text-muted-foreground mb-0.5">État constaté</dt>
            <dd className="text-ink-900">{echantillon.etatConservationConstate ?? "—"}</dd>
          </div>
        </dl>
      )}

      {exigencesAffichees.length > 0 && (
        <div className="space-y-2 border-t border-brand-900/8 pt-3">
          <p className="text-xs font-semibold text-muted-foreground">
            Exigences attendues
          </p>
          <ul className="space-y-2">
            {exigencesAffichees.map((ex, i) => (
              <LigneExigence key={`${ex.nature}-${i}`} exigence={ex} />
            ))}
          </ul>
        </div>
      )}
    </Card>
  );
}
