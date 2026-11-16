import { Button, Card } from "@/components/lab";
import { Download, FileText, Eye } from "lucide-react";
import { StatutDemandeBadge } from "@/components/demandes/StatutDemandeBadge";
import { formatMontant } from "@/components/panier/format";
import type { DemandeDetailClient } from "@/api/demandesClient";
import { formatDate } from "./libelles";
import apiClient from "@/api/axios";

interface DetailDemandeEnteteProps {
  demande: DemandeDetailClient;
  onDemanderAnnulation: () => void;
}

/** En-tête du détail : référence, objet, statut, laboratoire et total. */
export function DetailDemandeEntete({ demande, onDemanderAnnulation }: DetailDemandeEnteteProps) {
  const annulable = demande.statut === "NOUVEAU";

  const ouvrirPieceJointe = async (pieceJointeId: number) => {
    try {
      const blob = await apiClient.getBlob(`/client/demandes/${demande.laboratoireId}/${demande.id}/pieces-jointes/${pieceJointeId}`);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (error) {
      console.error("Erreur lors de l'ouverture de la pièce jointe:", error);
    }
  };

  return (
    <Card variant="glossy" padding="lg">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="font-mono text-xs text-muted-foreground">{demande.reference}</p>
          <h1 className="mt-1 text-2xl font-extrabold text-ink-900">{demande.titre}</h1>
          <div className="mt-3">
            <StatutDemandeBadge statut={demande.statut} />
          </div>
        </div>
        {annulable ? (
          <Button variant="secondary" size="sm" onClick={onDemanderAnnulation}>
            Annuler la demande
          </Button>
        ) : null}
      </div>

      <dl className="mt-6 grid gap-4 sm:grid-cols-3">
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Laboratoire
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">{demande.laboratoireNom}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Soumise le
          </dt>
          <dd className="mt-1 text-sm font-semibold text-ink-900">
            {formatDate(demande.dateSoumission)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Total
          </dt>
          <dd className="mt-1 text-sm font-bold text-ink-900">{formatMontant(demande.total)}</dd>
        </div>
      </dl>

      {demande.objectif !== null && demande.objectif !== "" ? (
        <div className="mt-6 border-t border-brand-900/10 pt-4">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Objectif
          </p>
          <p className="mt-1 text-sm text-ink-900">{demande.objectif}</p>
        </div>
      ) : null}

      {demande.motif !== null && demande.motif !== "" ? (
        <div className="mt-6 rounded-2xl border border-brand-900/10 bg-surface-card px-4 py-3">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Motif communiqué par le laboratoire
          </p>
          <p className="mt-1 text-sm text-ink-900">{demande.motif}</p>
        </div>
      ) : null}

      {demande.documents && demande.documents.length > 0 ? (
        <div className="mt-6 border-t border-brand-900/10 pt-4">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase mb-3">
            Pièces jointes
          </p>
          <ul className="space-y-2">
            {demande.documents.map((document) => (
              <li key={document.id} className="flex items-center justify-between rounded-lg border border-brand-900/10 bg-surface-alt px-3 py-2">
                <div className="flex items-center gap-3">
                  <FileText className="size-4 shrink-0 text-brand-900" aria-hidden="true" />
                  <span className="text-sm text-ink-900">{document.nomFichier || document.code}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="secondary"
                    icon={<Eye className="size-4" aria-hidden="true" />}
                    iconPosition="left"
                    onClick={() => void ouvrirPieceJointe(document.id)}
                  >
                    Consulter
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<Download className="size-4" aria-hidden="true" />}
                    iconPosition="left"
                    onClick={() => void telechargerPieceJointeDemande(
                      demande.id,
                      document.id,
                      document.nomFichier || document.code,
                    )}
                  >
                    Télécharger
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </Card>
  );
}
