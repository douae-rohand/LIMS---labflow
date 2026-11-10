import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { CheckCircle2, ShieldCheck, Upload, XCircle } from "lucide-react";
import { Button, Card } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";
import { ApiError } from "@/api/axios";
import { statutRapportPublic, verifierFichierRapport, type VerificationRapport } from "@/api/responsable";

/** Page ouverte par le QR code imprimé sur un rapport signé. */
export const Route = createFileRoute("/verifier-rapport")({
  validateSearch: (search: Record<string, unknown>): { laboratoire?: string; numero?: string } => {
    const texte = (valeur: unknown) => (typeof valeur === "string" && valeur.length <= 120 ? valeur : undefined);
    const laboratoire = texte(search["laboratoire"]);
    const numero = texte(search["numero"]);
    return { ...(laboratoire ? { laboratoire } : {}), ...(numero ? { numero } : {}) };
  },
  head: () => ({ meta: [{ title: "Vérifier un rapport — LabFlow LIMS" }, { name: "robots", content: "noindex" }] }),
  component: PageVerification,
});

function formaterDate(valeur?: string | null): string {
  if (!valeur) return "—";
  const date = new Date(valeur);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("fr-FR", { dateStyle: "long", timeStyle: "medium" }).format(date);
}

function messageErreur(erreur: unknown): string {
  if (erreur instanceof ApiError) return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "");
  return "Le service de vérification est indisponible.";
}

function PageVerification() {
  const { laboratoire, numero } = Route.useSearch();
  const [fichier, setFichier] = useState<File | null>(null);
  const statut = useQuery({
    queryKey: ["verification-rapport", laboratoire, numero],
    queryFn: () => statutRapportPublic(laboratoire as string, numero as string),
    enabled: Boolean(laboratoire && numero),
    retry: false,
  });
  const verification = useMutation({
    mutationFn: () => verifierFichierRapport(laboratoire as string, numero as string, fichier as File),
  });

  let contenu;
  if (!laboratoire || !numero) {
    contenu = <p className="text-muted-foreground">Scannez le QR code imprimé sur le rapport pour ouvrir cette page.</p>;
  } else if (statut.isLoading) {
    contenu = <p className="text-muted-foreground">Recherche du rapport…</p>;
  } else if (statut.isError) {
    contenu = <Resultat ok={false} titre="Rapport introuvable" texte={messageErreur(statut.error)} />;
  } else if (statut.data) {
    contenu = (
      <div className="flex flex-col gap-6">
        <dl className="grid gap-3 sm:grid-cols-2">
          <Info libelle="Laboratoire" valeur={statut.data.laboratoire} />
          <Info libelle="Rapport" valeur={statut.data.numero} />
          <Info libelle="Signé par" valeur={statut.data.signataire ?? "—"} />
          <Info libelle="Date de signature" valeur={formaterDate(statut.data.dateSignature)} />
        </dl>
        <form
          className="flex flex-col gap-3 rounded-2xl border border-brand-900/10 p-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (fichier) verification.mutate();
          }}
        >
          <p className="font-semibold text-ink-900">Comparer votre PDF au rapport signé</p>
          <p className="text-sm text-muted-foreground">
            Le fichier est seulement comparé à l'original enregistré : toute modification, même d'un caractère, est détectée.
          </p>
          <label className="flex cursor-pointer items-center gap-3 rounded-2xl border border-dashed border-brand-900/20 bg-surface-card px-4 py-4 text-sm">
            <Upload className="size-5 text-brand-900" aria-hidden="true" />
            <span className="min-w-0 flex-1 truncate">{fichier ? fichier.name : "Choisir le PDF du rapport"}</span>
            <input
              type="file"
              accept="application/pdf"
              className="sr-only"
              onChange={(event) => {
                setFichier(event.target.files?.[0] ?? null);
                verification.reset();
              }}
            />
          </label>
          <div className="flex justify-end">
            <Button type="submit" disabled={!fichier} loading={verification.isPending}>Vérifier</Button>
          </div>
        </form>
        {verification.isError && <Resultat ok={false} titre="Vérification impossible" texte={messageErreur(verification.error)} />}
        {verification.data && <ResultatFichier resultat={verification.data} />}
      </div>
    );
  }

  return (
    <PublicLayout>
      <Container className="max-w-3xl py-16">
        <Card variant="glossy" padding="lg">
          <div className="mb-6 flex items-center gap-3">
            <ShieldCheck className="size-7 text-brand-900" aria-hidden="true" />
            <h1 className="text-2xl font-extrabold text-ink-900">Vérifier un rapport d'analyse</h1>
          </div>
          {contenu}
        </Card>
      </Container>
    </PublicLayout>
  );
}

function ResultatFichier({ resultat }: { resultat: VerificationRapport }) {
  const authentique = Boolean(resultat.fichierAuthentique && resultat.signatureValide && resultat.documentComplet);
  return (
    <div className="flex flex-col gap-3">
      <Resultat
        ok={authentique}
        titre={authentique ? "Rapport authentique" : "Rapport non conforme à l'original"}
        texte={authentique
          ? "Ce PDF est identique au rapport signé par le laboratoire et sa signature électronique est valide."
          : "Ce PDF diffère du rapport signé ou sa signature est invalide. Ne vous fiez pas à son contenu."}
      />
      <ul className="grid gap-2 text-sm sm:grid-cols-2">
        <Point ok={Boolean(resultat.fichierAuthentique)} texte="Identique à l'original enregistré" />
        <Point ok={Boolean(resultat.signatureValide)} texte="Signature cryptographique valide" />
        <Point ok={Boolean(resultat.documentComplet)} texte="Aucun ajout après la signature" />
        <Point ok={Boolean(resultat.horodatage)} texte={resultat.horodatage ? `Horodaté le ${formaterDate(resultat.horodatage)}` : "Non horodaté"} />
      </ul>
      {resultat.certificat && <p className="text-xs text-muted-foreground">Certificat : {resultat.certificat}</p>}
    </div>
  );
}

function Resultat({ ok, titre, texte }: { ok: boolean; titre: string; texte: string }) {
  return (
    <div role="status" className={`flex items-start gap-3 rounded-2xl px-4 py-3 ${ok ? "bg-green-50 text-green-900" : "bg-red-50 text-red-900"}`}>
      {ok ? <CheckCircle2 className="mt-0.5 size-5 shrink-0" aria-hidden="true" /> : <XCircle className="mt-0.5 size-5 shrink-0" aria-hidden="true" />}
      <div>
        <p className="font-bold">{titre}</p>
        <p className="text-sm">{texte}</p>
      </div>
    </div>
  );
}

function Point({ ok, texte }: { ok: boolean; texte: string }) {
  return (
    <li className="flex items-center gap-2">
      {ok ? <CheckCircle2 className="size-4 text-green-700" aria-hidden="true" /> : <XCircle className="size-4 text-red-700" aria-hidden="true" />}
      {texte}
    </li>
  );
}

function Info({ libelle, valeur }: { libelle: string; valeur: string }) {
  return (
    <div className="rounded-2xl bg-surface-alt px-3 py-2">
      <dt className="text-xs font-semibold text-muted-foreground">{libelle}</dt>
      <dd className="text-sm text-ink-900">{valeur}</dd>
    </div>
  );
}
