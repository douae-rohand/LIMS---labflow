import { Badge, Card } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import type { RapportDetailClient, RapportLigneClient } from "@/api/rapportsClient";
import { libelleConformiteRapport, libelleTypeRapport } from "./libelles";

/** Assemblage du détail d'un rapport : en-tête, résultats, synthèse. */
export function RapportDetail({ rapport }: { rapport: RapportDetailClient }) {
  const synthese = rapport.syntheseIa;

  return (
    <div className="flex flex-col gap-8">
      <Card variant="glossy" padding="lg">
        <div className="min-w-0">
          <p className="font-mono text-xs text-muted-foreground">
            {rapport.demandeReference ?? "Rapport"}
          </p>
          <h1 className="mt-1 text-2xl font-extrabold text-ink-900">
            {rapport.demandeTitre ?? "Rapport d'analyses"}
          </h1>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Badge variant="soft" size="sm">
              {libelleTypeRapport(rapport.type)}
            </Badge>
            {rapport.version != null ? (
              <Badge variant="outline" size="sm">
                {`Version ${rapport.version}`}
              </Badge>
            ) : null}
          </div>
        </div>

        <dl className="mt-6 grid gap-4 sm:grid-cols-3">
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Laboratoire
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">
              {rapport.laboratoireNom ?? "—"}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Émis le
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">
              {formatDateFr(rapport.dateEmission)}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Signé le
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">
              {formatDateFr(rapport.dateSignature)}
            </dd>
          </div>
        </dl>
      </Card>

      <section>
        <h2 className="mb-4 text-lg font-extrabold text-ink-900">Résultats d&apos;analyses</h2>
        <TableauResultats lignes={rapport.lignes} />
      </section>

      {synthese !== null && synthese !== "" ? (
        <section>
          <h2 className="mb-4 text-lg font-extrabold text-ink-900">Synthèse</h2>
          <Card variant="surface" padding="lg">
            <p className="text-sm whitespace-pre-line text-ink-900">{synthese}</p>
            {rapport.mentionSyntheseIa !== null && rapport.mentionSyntheseIa !== "" ? (
              <p className="mt-4 text-xs text-muted-foreground">{rapport.mentionSyntheseIa}</p>
            ) : null}
          </Card>
        </section>
      ) : null}
    </div>
  );
}

function TableauResultats({ lignes }: { lignes: RapportLigneClient[] }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-brand-900/10">
      <table className="w-full text-left text-sm">
        <thead className="bg-surface-card text-brand-900">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">
              Analyse
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Valeur
            </th>
            <th scope="col" className="px-4 py-3 font-semibold">
              Conformité
            </th>
          </tr>
        </thead>
        <tbody>
          {lignes.length === 0 ? (
            <tr>
              <td colSpan={3} className="px-4 py-6 text-center text-sm text-muted-foreground">
                Aucun résultat détaillé pour ce rapport.
              </td>
            </tr>
          ) : (
            lignes.map((ligne, index) => (
              <tr key={index} className="border-t border-brand-900/10">
                <td className="px-4 py-3 text-ink-900">{ligne.designation ?? "—"}</td>
                <td className="px-4 py-3 text-ink-900">
                  {ligne.valeur ?? "—"}
                  {ligne.unite !== null && ligne.unite !== "" ? ` ${ligne.unite}` : ""}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {libelleConformiteRapport(ligne.conformite)}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
