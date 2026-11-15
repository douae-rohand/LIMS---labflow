import { Badge, Button, Card } from "@/components/lab";
import { formatMontant } from "@/components/panier/format";
import { echeanceDepassee, formatDateLocaleFr } from "@/lib/dates";
import { telechargerFactureClientPdf, type FactureDetailClient } from "@/api/facturesClient";
import { libelleStatutFacture, variantStatutFacture } from "./libelles";

/** En-tête du détail d'une facture : numéro, statut, dates et montants. */
export function FactureEntete({ facture }: { facture: FactureDetailClient }) {
  const enRetard = facture.statut === "IMPAYEE" && echeanceDepassee(facture.dateEcheance);

  return (
    <Card variant="glossy" padding="lg">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="font-mono text-xs text-muted-foreground">{facture.numero}</p>
          <h1 className="mt-1 text-2xl font-extrabold text-ink-900">Facture</h1>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Badge variant={variantStatutFacture(facture.statut)} size="sm">
              {libelleStatutFacture(facture.statut)}
            </Badge>
            {enRetard ? (
              <Badge variant="outline" size="sm">
                En retard
              </Badge>
            ) : null}
          </div>
        </div>

        <div className="flex flex-col items-start gap-1">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => void telechargerFactureClientPdf(facture.laboratoireId, facture.factureId, facture.numero)}
          >
            Télécharger le PDF
          </Button>
        </div>
      </div>

      <dl className="mt-6 grid gap-4 sm:grid-cols-3">
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Laboratoire
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {facture.laboratoireNom ?? "—"}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Émise le
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatDateLocaleFr(facture.dateEmission)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Échéance
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatDateLocaleFr(facture.dateEcheance)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Montant
          </dt>
          <dd className="mt-1 text-sm font-bold text-ink-900">{formatMontant(facture.montantHt)}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Payé
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatMontant(facture.montantPaye)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Reste à payer
          </dt>
          <dd className="mt-1 text-sm font-bold text-ink-900">
            {formatMontant(facture.resteAPayer)}
          </dd>
        </div>
      </dl>

      {facture.ice !== null && facture.ice !== "" ? (
        <div className="mt-6 border-t border-brand-900/10 pt-4">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            ICE
          </p>
          <p className="mt-1 font-mono text-sm text-ink-900">{facture.ice}</p>
        </div>
      ) : null}

      {facture.demandeReference !== null && facture.demandeReference !== "" ? (
        <div className="mt-6 border-t border-brand-900/10 pt-4">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Demande associée
          </p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm text-ink-900">{facture.demandeReference}</span>
            {facture.demandeTitre !== null && facture.demandeTitre !== "" ? (
              <>
                <span className="text-muted-foreground">|</span>
                <span className="text-sm text-ink-900">{facture.demandeTitre}</span>
              </>
            ) : null}
          </div>
        </div>
      ) : null}
    </Card>
  );
}
