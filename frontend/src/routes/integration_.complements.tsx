import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Button, Card, Label, Textarea } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { DocumentUploader } from "@/components/integration/DocumentUploader";
import { TAILLE_MAX_DOCUMENT } from "@/components/integration/constants";
import { ApiError } from "@/api/axios";
import { consulterComplements, deposerComplements } from "@/api/plateforme";

export const Route = createFileRoute("/integration_/complements")({
  validateSearch: (search: Record<string, unknown>): { jeton?: string } => {
    const jeton = typeof search["jeton"] === "string" ? search["jeton"].trim() : "";
    return jeton && jeton.length <= 200 ? { jeton } : {};
  },
  head: () => ({
    meta: [
      { title: "Compléter ma demande d'intégration — LabFlow LIMS" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: PageComplements,
});

function formaterDate(valeur?: string | null): string {
  if (!valeur) return "—";
  const date = new Date(valeur);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("fr-FR", { dateStyle: "long", timeStyle: "short" }).format(date);
}

function messageErreur(erreur: unknown): string {
  if (erreur instanceof ApiError) {
    return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "") || "L'opération a échoué.";
  }
  return "Le serveur est indisponible. Réessayez plus tard.";
}

function PageComplements() {
  const { jeton } = Route.useSearch();
  const [reponse, setReponse] = useState("");
  const [fichiers, setFichiers] = useState<Partial<Record<string, File>>>({});
  const [erreurs, setErreurs] = useState<Partial<Record<string, string | undefined>>>({});
  const [progression, setProgression] = useState<number | null>(null);

  const demande = useQuery({
    queryKey: ["complements-integration", jeton],
    queryFn: () => consulterComplements(jeton as string),
    enabled: Boolean(jeton),
    retry: false,
  });

  const envoi = useMutation({
    mutationFn: () => {
      const retenus: Record<string, File> = {};
      Object.entries(fichiers).forEach(([code, fichier]) => {
        if (fichier) retenus[code] = fichier;
      });
      return deposerComplements(jeton as string, reponse, retenus, setProgression);
    },
    onSettled: () => setProgression(null),
  });

  const documents = demande.data?.documents ?? [];

  const envoyer = () => {
    const suivantes: Partial<Record<string, string>> = {};
    documents.forEach((document) => {
      const fichier = fichiers[document.code];
      if (!fichier) suivantes[document.code] = "Ce document est demandé.";
      else if (fichier.size > TAILLE_MAX_DOCUMENT) suivantes[document.code] = "Fichier trop volumineux (10 Mo max).";
    });
    setErreurs(suivantes);
    if (Object.keys(suivantes).length > 0) return;
    if (documents.length === 0 && !reponse.trim()) return;
    envoi.mutate();
  };

  let contenu;
  if (!jeton) {
    contenu = <Message titre="Lien incomplet" texte="Ouvrez le lien reçu par e-mail sans le modifier." />;
  } else if (demande.isLoading) {
    contenu = <p className="text-sm text-muted-foreground">Chargement de votre demande…</p>;
  } else if (demande.isError) {
    contenu = <Message titre="Lien indisponible" texte={messageErreur(demande.error)} />;
  } else if (envoi.isSuccess) {
    contenu = (
      <Message
        titre="Compléments transmis"
        texte="Merci. Votre dossier repasse en vérification. Vous serez informé(e) par e-mail de la décision."
      />
    );
  } else if (demande.data) {
    contenu = (
      <form
        className="flex flex-col gap-6"
        onSubmit={(event) => {
          event.preventDefault();
          envoyer();
        }}
      >
        <div>
          <p className="text-sm text-muted-foreground">
            Demande {demande.data.numero} · {demande.data.nomLaboratoire}
          </p>
          <h1 className="mt-1 text-2xl font-extrabold text-ink-900">Compléter votre demande d'intégration</h1>
          <p className="mt-1 text-xs text-muted-foreground">Lien valable jusqu'au {formaterDate(demande.data.expiration)}.</p>
        </div>
        <section className="rounded-2xl border border-brand-900/15 bg-surface-alt px-4 py-3">
          <p className="text-sm font-semibold text-ink-900">Message de l'équipe LabFlow</p>
          <p className="mt-1 whitespace-pre-line text-sm text-ink-900">{demande.data.message}</p>
        </section>
        {documents.length > 0 && (
          <DocumentUploader
            types={documents.map((document) => ({ ...document, obligatoire: true }))}
            files={fichiers}
            errors={erreurs}
            onChange={(code, fichier) => {
              setFichiers((courant) => {
                const suivant = { ...courant };
                if (fichier) suivant[code] = fichier;
                else delete suivant[code];
                return suivant;
              });
              setErreurs((courant) => ({ ...courant, [code]: undefined }));
            }}
          />
        )}
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="reponse">
            Votre réponse {documents.length === 0 ? "*" : "(facultatif)"}
          </Label>
          <Textarea id="reponse" maxLength={4000} value={reponse} onChange={(event) => setReponse(event.target.value)} />
        </div>
        {envoi.isError && <p role="alert" className="text-sm text-red-600">{messageErreur(envoi.error)}</p>}
        {progression != null && <p className="text-sm text-muted-foreground">Envoi en cours… {progression}%</p>}
        <div className="flex justify-end">
          <Button
            type="submit"
            loading={envoi.isPending}
            disabled={documents.length === 0 && !reponse.trim()}
          >
            Envoyer les compléments
          </Button>
        </div>
      </form>
    );
  }

  return (
    <PublicLayout>
      <Container className="max-w-3xl py-16">
        <Card variant="glossy" padding="lg">{contenu}</Card>
      </Container>
    </PublicLayout>
  );
}

function Message({ titre, texte }: { titre: string; texte: string }) {
  return (
    <div>
      <h1 className="text-2xl font-extrabold text-ink-900">{titre}</h1>
      <p className="mt-3 text-muted-foreground">{texte}</p>
    </div>
  );
}
