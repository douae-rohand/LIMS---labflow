import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Download, Eye } from "lucide-react";
import { Button } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { contenuDocumentIntegration, ouvrirDocumentIntegration, type DocumentIntegrationDto } from "@/api/plateforme";
import { LIBELLES_DOCUMENTS, TYPES_DOCUMENTS, type TypeDocumentIntegration } from "@/components/integration/constants";
import { messageErreur } from "../texte";

function modeApercu(nom: string, mime: string): "pdf" | "image" | "aucun" {
  const type = mime.toLowerCase();
  const fichier = nom.toLowerCase();
  if (type === "application/pdf" || fichier.endsWith(".pdf")) return "pdf";
  if (type.startsWith("image/") || /\.(jpe?g|png|webp|gif)$/.test(fichier)) return "image";
  return "aucun";
}

export function ListeDocuments({
  demandeId,
  documents,
  chargement,
  introuvable,
  peutRemplacer,
  remplacementEnCours,
  onRemplacer,
}: {
  demandeId: number | null;
  documents: DocumentIntegrationDto[] | undefined;
  chargement?: boolean;
  introuvable?: boolean;
  peutRemplacer?: boolean;
  remplacementEnCours?: boolean;
  onRemplacer?: (type: TypeDocumentIntegration, fichier: File) => void;
}) {
  const [selection, setSelection] = useState<TypeDocumentIntegration | null>(null);
  const presents = new Map((documents ?? []).map((document) => [document.typeDocument, document]));
  const choisi = selection ? presents.get(selection) : undefined;

  useEffect(() => {
    setSelection(null);
  }, [demandeId]);

  if (chargement) {
    return <p className="text-sm text-muted-foreground">Chargement des documents…</p>;
  }
  if (introuvable || demandeId == null) {
    return (
      <p className="rounded-2xl bg-surface-alt px-4 py-3 text-sm text-ink-900">
        Aucun dossier d'intégration n'est rattaché à ce laboratoire. Les justificatifs ne sont donc pas disponibles ici.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-2">
        {TYPES_DOCUMENTS.map((type) => {
          const document = presents.get(type);
          return (
            <li key={type} className="rounded-2xl border border-brand-900/10 p-3">
              <p className="font-semibold text-ink-900">{LIBELLES_DOCUMENTS[type].libelle}</p>
              <p className="text-xs text-muted-foreground">{LIBELLES_DOCUMENTS[type].raison}</p>
              {document ? (
                <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs text-muted-foreground">
                    {document.nomFichier} · {Math.max(1, Math.round(document.taille / 1024))} Ko
                  </p>
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      size="sm"
                      variant={selection === type ? "primary" : "secondary"}
                      icon={<Eye className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => setSelection(type)}
                    >
                      Consulter
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="ghost"
                      icon={<Download className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => void ouvrirDocumentIntegration(demandeId, type, true)}
                    >
                      Télécharger
                    </Button>
                  </div>
                </div>
              ) : (
                <p className="mt-2 text-xs text-red-600">Document manquant</p>
              )}
              {peutRemplacer && onRemplacer && (
                <label className="mt-2 block text-xs font-semibold text-brand-900">
                  {document ? "Remplacer le fichier" : "Ajouter le fichier"}
                  <input
                    type="file"
                    accept="application/pdf,image/jpeg,image/png,image/webp"
                    disabled={remplacementEnCours}
                    className="mt-1 block w-full text-sm font-normal text-ink-900"
                    onChange={(event) => {
                      const fichier = event.target.files?.[0];
                      if (fichier) onRemplacer(type, fichier);
                      event.target.value = "";
                    }}
                  />
                </label>
              )}
            </li>
          );
        })}
      </ul>
      {choisi && selection && (
        <ApercuDocument demandeId={demandeId} type={selection} document={choisi} />
      )}
    </div>
  );
}

function ApercuDocument({
  demandeId,
  type,
  document,
}: {
  demandeId: number;
  type: TypeDocumentIntegration;
  document: DocumentIntegrationDto;
}) {
  const mode = modeApercu(document.nomFichier, document.typeMime ?? "");
  const apercu = useQuery({
    queryKey: ["document-apercu", demandeId, type, document.dateAjout, document.taille],
    queryFn: async () => {
      const brut = await contenuDocumentIntegration(demandeId, type);
      const mime = document.typeMime || brut.type || "application/octet-stream";
      return brut.type === mime ? brut : new Blob([brut], { type: mime });
    },
    enabled: mode !== "aucun",
  });
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!apercu.data) {
      setUrl(null);
      return;
    }
    const suivant = URL.createObjectURL(apercu.data);
    setUrl(suivant);
    return () => URL.revokeObjectURL(suivant);
  }, [apercu.data]);

  return (
    <section className="overflow-hidden rounded-2xl border border-brand-900/10">
      <div className="border-b border-brand-900/10 bg-surface-alt px-4 py-2">
        <p className="text-sm font-semibold text-ink-900">{document.nomFichier}</p>
        <p className="text-xs text-muted-foreground">{LIBELLES_DOCUMENTS[type].libelle}</p>
      </div>
      {mode === "aucun" && (
        <p className="px-4 py-6 text-sm text-ink-900">
          Ce format ne peut pas être prévisualisé. Utilisez le téléchargement pour ouvrir le fichier.
        </p>
      )}
      {mode !== "aucun" && apercu.isLoading && (
        <p className="px-4 py-6 text-sm text-muted-foreground">Préparation de l'aperçu…</p>
      )}
      {mode !== "aucun" && apercu.isError && (
        <p className="px-4 py-6 text-sm text-red-600">
          {apercu.error instanceof ApiError
            ? messageErreur(apercu.error, "Ce document est indisponible.")
            : "Ce document est indisponible."}
        </p>
      )}
      {url && mode === "pdf" && (
        <iframe title={document.nomFichier} src={url} className="h-[28rem] w-full bg-surface-alt" />
      )}
      {url && mode === "image" && (
        <img src={url} alt={document.nomFichier} className="max-h-[28rem] w-full object-contain bg-surface-alt" />
      )}
    </section>
  );
}
