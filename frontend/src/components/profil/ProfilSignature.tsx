import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { PenLine, Trash2, Upload } from "lucide-react";
import { toast } from "sonner";
import { Button, Card } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { importerSignature, lireImageSignature, lireStatutSignature, supprimerSignature } from "@/api/responsable";

const TAILLE_MAX = 2 * 1024 * 1024;

function messageErreur(erreur: unknown, defaut: string): string {
  if (erreur instanceof ApiError) return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "") || defaut;
  return defaut;
}

/** Image de signature du responsable, imprimée sur les rapports qu'il signe. */
export function ProfilSignature() {
  const queryClient = useQueryClient();
  const champ = useRef<HTMLInputElement>(null);
  const [apercu, setApercu] = useState<string | null>(null);

  const statut = useQuery({ queryKey: ["signature-responsable"], queryFn: lireStatutSignature });
  const presente = statut.data?.presente ?? false;
  const image = useQuery({
    queryKey: ["signature-responsable-image", statut.data?.miseAJour],
    queryFn: lireImageSignature,
    enabled: presente,
  });

  useEffect(() => {
    if (!image.data) {
      setApercu(null);
      return;
    }
    const url = URL.createObjectURL(image.data);
    setApercu(url);
    return () => URL.revokeObjectURL(url);
  }, [image.data]);

  const invalider = () => queryClient.invalidateQueries({ queryKey: ["signature-responsable"] });

  const envoi = useMutation({
    mutationFn: importerSignature,
    onSuccess: async () => {
      toast.success("Signature enregistrée. Elle figurera sur vos prochains rapports signés.");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La signature n'a pas pu être enregistrée.")),
  });
  const suppression = useMutation({
    mutationFn: supprimerSignature,
    onSuccess: async () => {
      toast.success("Signature supprimée.");
      await invalider();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La suppression a échoué.")),
  });

  const choisir = (fichier: File | undefined) => {
    if (!fichier) return;
    if (!["image/png", "image/jpeg"].includes(fichier.type)) {
      toast.error("Formats acceptés : PNG ou JPG.");
      return;
    }
    if (fichier.size > TAILLE_MAX) {
      toast.error("L'image ne doit pas dépasser 2 Mo.");
      return;
    }
    envoi.mutate(fichier);
  };

  return (
    <Card className="space-y-5 p-6">
      <div className="flex items-start gap-3">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-900/10 text-brand-900">
          <PenLine className="size-5" aria-hidden="true" />
        </span>
        <div>
          <h2 className="font-semibold text-ink-900">Ma signature</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Importez une image de votre signature manuscrite (PNG ou JPG, 2 Mo maximum). Elle est imprimée dans le cadre
            de signature de chaque rapport que vous signez. Signez sur une feuille blanche puis photographiez-la :
            le fond blanc est rendu transparent automatiquement.
          </p>
        </div>
      </div>

      <div className="flex min-h-28 items-center justify-center rounded-2xl border border-dashed border-brand-900/20 bg-white p-4">
        {statut.isLoading || (presente && image.isLoading) ? (
          <p className="text-sm text-muted-foreground">Chargement…</p>
        ) : apercu ? (
          <img src={apercu} alt="Votre signature" className="max-h-24 max-w-full object-contain" />
        ) : (
          <p className="text-sm text-muted-foreground">Aucune signature importée : vos rapports porteront seulement votre nom.</p>
        )}
      </div>

      <input
        ref={champ}
        type="file"
        accept="image/png,image/jpeg"
        className="sr-only"
        aria-label="Choisir l'image de signature"
        onChange={(event) => {
          choisir(event.target.files?.[0]);
          event.target.value = "";
        }}
      />
      <div className="flex flex-wrap justify-end gap-2">
        {presente && (
          <Button
            type="button"
            variant="ghost"
            icon={<Trash2 className="size-4" aria-hidden="true" />}
            iconPosition="left"
            loading={suppression.isPending}
            onClick={() => suppression.mutate()}
          >
            Supprimer
          </Button>
        )}
        <Button
          type="button"
          variant="secondary"
          icon={<Upload className="size-4" aria-hidden="true" />}
          iconPosition="left"
          loading={envoi.isPending}
          onClick={() => champ.current?.click()}
        >
          {presente ? "Remplacer la signature" : "Importer ma signature"}
        </Button>
      </div>
    </Card>
  );
}
