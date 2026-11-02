import { Badge, Card } from "@/components/lab";
import { formatDateFr } from "@/lib/dates";
import type { EnqueteDetailClient, RepondreEnqueteBody } from "@/api/enquetesClient";
import { FormulaireEnquete } from "./FormulaireEnquete";
import { libelleNote, libelleStatutEnquete, variantStatutEnquete } from "./libelles";

interface EnqueteDetailProps {
  enquete: EnqueteDetailClient;
  enregistrement: boolean;
  onRepondre: (body: RepondreEnqueteBody) => void;
}

/** Détail d'une enquête : en-tête, puis saisie ou relecture de l'évaluation. */
export function EnqueteDetail({
  enquete,
  enregistrement,
  onRepondre,
}: EnqueteDetailProps) {
  return (
    <div className="flex flex-col gap-8">
      <Card variant="glossy" padding="lg">
        <div className="min-w-0">
          <p className="font-mono text-xs text-muted-foreground">
            {enquete.demandeReference ?? "Enquête"}
          </p>
          <h1 className="mt-1 text-2xl font-extrabold text-ink-900">
            {enquete.demandeTitre ?? enquete.laboratoireNom ?? "Enquête de satisfaction"}
          </h1>
          <div className="mt-3">
            <Badge variant={variantStatutEnquete(enquete.repondue)} size="sm">
              {libelleStatutEnquete(enquete.repondue)}
            </Badge>
          </div>
        </div>

        <dl className="mt-6 grid gap-4 sm:grid-cols-3">
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Laboratoire
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">
              {enquete.laboratoireNom ?? "—"}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Envoyée le
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">
              {formatDateFr(enquete.dateEnvoi)}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Répondue le
            </dt>
            <dd className="mt-1 text-sm font-semibold text-ink-900">
              {formatDateFr(enquete.dateReponse)}
            </dd>
          </div>
        </dl>
      </Card>

      <section>
        <h2 className="mb-4 text-lg font-extrabold text-ink-900">Votre évaluation</h2>
        {enquete.repondue ? (
          <LectureEvaluation enquete={enquete} />
        ) : (
          <FormulaireEnquete
            enquete={enquete}
            enregistrement={enregistrement}
            onRepondre={onRepondre}
          />
        )}
      </section>
    </div>
  );
}

function LectureEvaluation({ enquete }: { enquete: EnqueteDetailClient }) {
  const criteres: { libelle: string; valeur: string }[] = [
    { libelle: "Satisfaction globale", valeur: libelleNote(enquete.noteGlobale) },
    { libelle: "Respect des délais", valeur: libelleNote(enquete.noteDelai) },
    { libelle: "Clarté du rapport", valeur: libelleNote(enquete.noteClarte) },
    { libelle: "Qualité de l'accueil", valeur: libelleNote(enquete.noteRelation) },
  ];

  return (
    <div className="flex flex-col gap-4">
      <Card variant="surface" padding="lg">
        <dl className="grid gap-4 sm:grid-cols-2">
          {criteres.map((critere) => (
            <div key={critere.libelle}>
              <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                {critere.libelle}
              </dt>
              <dd className="mt-1 text-sm font-semibold text-ink-900">{critere.valeur}</dd>
            </div>
          ))}
        </dl>
      </Card>

      {enquete.commentaire !== null && enquete.commentaire !== "" ? (
        <Card variant="surface" padding="lg">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Commentaire
          </p>
          <p className="mt-2 text-sm whitespace-pre-line text-ink-900">{enquete.commentaire}</p>
        </Card>
      ) : null}
    </div>
  );
}
