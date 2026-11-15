import { Button, Card } from "@/components/lab";
import { Download, FileText, Eye, X } from "lucide-react";
import { StatutDemandeBadge } from "@/components/demandes/StatutDemandeBadge";
import { formatMontant } from "@/components/panier/format";
import type { DemandeDetailClient } from "@/api/demandesClient";
import { formatDate } from "./libelles";
import apiClient from "@/api/axios";
import { useState } from "react";

interface DetailDemandeEnteteProps {
  demande: DemandeDetailClient;
  onDemanderAnnulation: () => void;
}

/** En-tête du détail : référence, objet, statut, laboratoire et total. */
export function DetailDemandeEntete({ demande, onDemanderAnnulation }: DetailDemandeEnteteProps) {
  const annulable = demande.statut === "NOUVEAU";
  const [previewDoc, setPreviewDoc] = useState<{ id: number; nomFichier: string; url: string } | null>(null);

  const handlePreview = async (documentId: number, nomFichier: string) => {
    try {
      const blob = await apiClient.getBlob(
        `/client/demandes/${demande.laboratoireId}/${demande.id}/pieces-jointes/${documentId}`
      );
      const url = URL.createObjectURL(blob);
      setPreviewDoc({ id: documentId, nomFichier, url });
    } catch (error) {
      console.error("Erreur preview:", error);
    }
  };

  const closePreview = () => {
    if (previewDoc) {
      URL.revokeObjectURL(previewDoc.url);
      setPreviewDoc(null);
    }
  };

  const isImage = (typeMime: string | null) => {
    return typeMime?.startsWith("image/") || false;
  };

  const isPdf = (typeMime: string | null) => {
    return typeMime === "application/pdf";
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
                  {(isImage(document.typeMime) || isPdf(document.typeMime)) && (
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Eye className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => void handlePreview(document.id, document.nomFichier || document.code)}
                    >
                      Aperçu
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<Download className="size-4" aria-hidden="true" />}
                    iconPosition="left"
                    onClick={() => void apiClient.download(
                      `/client/demandes/${demande.laboratoireId}/${demande.id}/pieces-jointes/${document.id}`,
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

      {previewDoc && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={closePreview}>
          <div className="relative max-h-[90vh] max-w-[90vw] overflow-auto rounded-lg bg-white p-4" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={closePreview}
              className="absolute right-2 top-2 rounded-full bg-surface-card p-2 hover:bg-surface-alt"
            >
              <X className="size-5" aria-hidden="true" />
            </button>
            <h3 className="mb-4 text-lg font-semibold">{previewDoc.nomFichier}</h3>
            {isImage(demande.documents.find(d => d.id === previewDoc.id)?.typeMime || null) ? (
              <img src={previewDoc.url} alt={previewDoc.nomFichier} className="max-h-[80vh] max-w-full" />
            ) : (
              <iframe src={previewDoc.url} className="h-[80vh] w-full" title={previewDoc.nomFichier} />
            )}
          </div>
        </div>
      )}
    </Card>
  );
}
