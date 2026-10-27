import { Link } from "@tanstack/react-router";
import { CheckCircle2 } from "lucide-react";
import { LIBELLE_STATUT_DEMANDE } from "@/api/demandes";
import type { DemandeDetailClient } from "@/api/demandesClient";
import { Badge, Button, Card } from "@/components/lab";
import { formatMontant } from "./format";

/** Confirmation affichée après la création d'une demande depuis le panier. */
export function CarteConfirmationDemande({
  demande,
  laboratoireId,
}: {
  demande: DemandeDetailClient;
  laboratoireId: number;
}) {
  return (
    <Card variant="glossy" padding="lg" className="mx-auto max-w-2xl">
      <div className="flex items-start gap-3">
        <CheckCircle2 className="mt-0.5 size-6 text-green-600" aria-hidden="true" />
        <div className="min-w-0">
          <h2 className="text-xl font-extrabold text-ink-900">Demande envoyée</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Référence&nbsp;:{" "}
            <span className="font-semibold text-ink-900">{demande.reference}</span>
          </p>
        </div>
      </div>

      <dl className="mt-6 grid gap-4 sm:grid-cols-2">
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Titre
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">{demande.titre}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Laboratoire
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">{demande.laboratoireNom}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Statut
          </dt>
          <dd className="mt-1">
            <Badge variant="mint">{LIBELLE_STATUT_DEMANDE[demande.statut]}</Badge>
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Total
          </dt>
          <dd className="mt-1 text-sm font-bold text-ink-900">{formatMontant(demande.total)}</dd>
        </div>
      </dl>

      {demande.lignes.length > 0 ? (
        <ul className="mt-6 flex flex-col gap-2 border-t border-brand-900/10 pt-4">
          {demande.lignes.map((ligne) => (
            <li key={ligne.essaiId} className="flex items-center justify-between gap-3 text-sm">
              <span className="text-ink-900">
                {ligne.designation ?? ligne.code ?? "Essai"}
              </span>
              <span className="font-semibold text-ink-900">{formatMontant(ligne.tarif)}</span>
            </li>
          ))}
        </ul>
      ) : null}

      <div className="mt-6 flex flex-wrap gap-3">
        <Link
          to="/app/demandes/$laboratoireId/$demandeId"
          params={{ laboratoireId: String(laboratoireId), demandeId: String(demande.id) }}
        >
          <Button>Voir ma demande</Button>
        </Link>
        <Link to="/app/panier">
          <Button variant="secondary">Retour à mes paniers</Button>
        </Link>
      </div>
    </Card>
  );
}
