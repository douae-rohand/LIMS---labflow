import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Download, Eye } from "lucide-react";
import { Badge, Button } from "@/components/lab";
import { ApiError } from "@/api/axios";
import {
  contenuDocumentIntegration,
  ouvrirDocumentIntegration,
  type DocumentIntegrationDto,
  type TypeDocumentIntegrationDto,
} from "@/api/plateforme";
import { messageErreur } from "../texte";

function modeApercu(nom: string, mime: string): "pdf" | "image" | "aucun" {
  const type = mime.toLowerCase();
  const fichier = nom.toLowerCase();
  if (type === "application/pdf" || fichier.endsWith(".pdf")) return "pdf";
  if (type.startsWith("image/") || /\.(jpe?g|png|webp|gif)$/.test(fichier)) return "image";
  return "aucun";
}

interface LigneDocument {
  code: string;
  libelle: string;
  raison?: string | null | undefined;
  obligatoire: boolean;
  actif: boolean;
}

/**
 * Liste les documents attendus pour un dossier : les types actifs du paramétrage
 * et ceux déjà déposés (même s'ils ont été retirés depuis de la liste exigée).
 */
function lignesAttendues(
  attendus: TypeDocumentIntegrationDto[] | undefined,
  documents: DocumentIntegrationDto[] | undefined,
): LigneDocument[] {
  const lignes = new Map<string, LigneDocument>();
  (attendus ?? []).forEach((type) =>
    lignes.set(type.code, {
      code: type.code,
      libelle: type.libelle,
      raison: type.raison,
      obligatoire: type.obligatoire,
      actif: type.actif,
    }),
  );
  (documents ?? []).forEach((document) => {
    if (!lignes.has(document.typeDocument)) {
      lignes.set(document.typeDocument, {
        code: document.typeDocument,
        libelle: document.libelle,
        raison: document.raison,
        obligatoire: false,
        actif: false,
      });
    }
  });
  return [...lignes.values()];
}

export function ListeDocuments({
  demandeId,
  documents,
  attendus,
  demandes,
  chargement,
  introuvable,
  peutRemplacer,
  remplacementEnCours,
  onRemplacer,
}: {
  demandeId: number | null;
  documents: DocumentIntegrationDto[] | undefined;
  attendus?: TypeDocumentIntegrationDto[] | undefined;
  /** Codes des documents réclamés au demandeur (demande de compléments en cours). */
  demandes?: string[] | undefined;
  chargement?: boolean;
  introuvable?: boolean;
  peutRemplacer?: boolean;
  remplacementEnCours?: boolean;
  onRemplacer?: (type: string, fichier: File) => void;
}) {
  const [selection, setSelection] = useState<string | null>(null);
  const presents = new Map((documents ?? []).map((document) => [document.typeDocument, document]));
  const choisi = selection ? presents.get(selection) : undefined;
  const lignes = lignesAttendues(attendus, documents);

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
        {lignes.map((ligne) => {
          const document = presents.get(ligne.code);
          const reclame = demandes?.includes(ligne.code) ?? false;
          return (
            <li key={ligne.code} className="rounded-2xl border border-brand-900/10 p-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="font-semibold text-ink-900">{ligne.libelle}</p>
                  {ligne.raison && <p className="text-xs text-muted-foreground">{ligne.raison}</p>}
                </div>
                <div className="flex flex-wrap gap-1">
                  {reclame && <Badge variant="lime" size="sm">Réclamé</Badge>}
                  {!ligne.actif && <Badge variant="outline" size="sm">Plus exigé</Badge>}
                  {ligne.actif && !ligne.obligatoire && <Badge variant="outline" size="sm">Facultatif</Badge>}
                </div>
              </div>
              {document ? (
                <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs text-muted-foreground">
                    {document.nomFichier} · {Math.max(1, Math.round(document.taille / 1024))} Ko
                  </p>
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      size="sm"
                      variant={selection === ligne.code ? "primary" : "secondary"}
                      icon={<Eye className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => setSelection(ligne.code)}
                    >
                      Consulter
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="ghost"
                      icon={<Download className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => void ouvrirDocumentIntegration(demandeId, ligne.code, true)}
                    >
                      Télécharger
                    </Button>
                  </div>
                </div>
              ) : (
                <p className={`mt-2 text-xs ${ligne.obligatoire ? "text-red-600" : "text-muted-foreground"}`}>
                  {ligne.obligatoire ? "Document manquant" : "Non fourni"}
                </p>
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
                      if (fichier) onRemplacer(ligne.code, fichier);
                      event.target.value = "";
                    }}
                  />
                </label>
              )}
            </li>
          );
        })}
      </ul>
      {choisi && selection && <ApercuDocument demandeId={demandeId} type={selection} document={choisi} />}
    </div>
  );
}

function ApercuDocument({
  demandeId,
  type,
  document,
}: {
  demandeId: number;
  type: string;
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
        <p className="text-xs text-muted-foreground">{document.libelle}</p>
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
