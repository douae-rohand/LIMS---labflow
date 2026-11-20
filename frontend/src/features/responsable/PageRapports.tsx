import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Download, Eye, FileSignature, PenLine, Send, ShieldCheck } from "lucide-react";
import { Link } from "@tanstack/react-router";
import { toast } from "sonner";
import { Badge, Button, Input, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  envoyerRapport,
  listerRapportsTravail,
  lireStatutSignature,
  ouvrirApercuRapport,
  ouvrirPdfRapport,
  renvoyerRapport,
  signerRapportDemande,
  telechargerRapport,
  type RapportTravail,
} from "@/api/responsable";
import { Dialogue } from "@/features/super-admin/Fenetres";
import { SyntheseIaBouton } from "@/components/ia/SyntheseIaBouton";
import {
  Actualisation,
  BarreFiltres,
  contient,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  messageErreur,
  Recherche,
} from "./commun";

type Filtre = "tous" | "a-signer" | "a-envoyer" | "envoye";

function dateRapport(rapport: RapportTravail): number {
  const valeur = rapport.dateSignature ?? rapport.dateEmission;
  return valeur ? Date.parse(valeur) : 0;
}

function estASigner(rapport: RapportTravail): boolean {
  return rapport.statut === "GENERE" || rapport.statut === "BROUILLON";
}

function estAEnvoyer(rapport: RapportTravail): boolean {
  return rapport.statut === "SIGNE";
}

function estEnvoye(rapport: RapportTravail): boolean {
  return rapport.statut === "ENVOYE" || rapport.statut === "ARCHIVE";
}

const FILTRES: { id: Filtre; libelle: string }[] = [
  { id: "tous", libelle: "Tous" },
  { id: "a-signer", libelle: "À signer" },
  { id: "a-envoyer", libelle: "À envoyer" },
  { id: "envoye", libelle: "Envoyés" },
];

export function PageRapports() {
  const queryClient = useQueryClient();
  const [aSigner, setASigner] = useState<RapportTravail | null>(null);
  const [motDePasse, setMotDePasse] = useState("");
  const [filtre, setFiltre] = useState<Filtre>("tous");
  const [recherche, setRecherche] = useState("");
  const rapports = useQuery({
    queryKey: ["responsable-rapports"],
    queryFn: listerRapportsTravail,
  });

  const signatureImage = useQuery({ queryKey: ["signature-responsable"], queryFn: lireStatutSignature });

  // Le serveur trie : la dernière signature en haut.
  const compteurs = useMemo(() => {
    const total: Record<Filtre, number> = { tous: 0, "a-signer": 0, "a-envoyer": 0, envoye: 0 };
    (rapports.data ?? []).forEach((r) => {
      total.tous += 1;
      if (estASigner(r)) total["a-signer"] += 1;
      else if (estAEnvoyer(r)) total["a-envoyer"] += 1;
      else if (estEnvoye(r)) total.envoye += 1;
    });
    return total;
  }, [rapports.data]);
  const visibles = (rapports.data ?? [])
    .filter((r) => {
      if (filtre === "tous") return true;
      if (filtre === "a-signer") return estASigner(r);
      if (filtre === "a-envoyer") return estAEnvoyer(r);
      if (filtre === "envoye") return estEnvoye(r);
      return true;
    })
    .filter((r) => !recherche.trim()
      || contient(r.demandeReference, recherche)
      || contient(r.numero, recherche)
      || contient(r.client, recherche)
      || contient(r.demandeTitre, recherche))
    // À signer et à envoyer d'abord (action attendue), puis du plus récent au plus ancien.
    .map((r, index) => ({ r, index }))
    .sort((x, y) => {
      const prioriteX = estASigner(x.r) ? 0 : estAEnvoyer(x.r) ? 1 : 2;
      const prioriteY = estASigner(y.r) ? 0 : estAEnvoyer(y.r) ? 1 : 2;
      return prioriteX - prioriteY || dateRapport(y.r) - dateRapport(x.r) || x.index - y.index;
    })
    .map(({ r }) => r);

  const signature = useMutation({
    mutationFn: (demandeId: number) => signerRapportDemande(demandeId, motDePasse),
    onSuccess: async () => {
      toast.success("Rapport signé. Vous pouvez maintenant l'envoyer au client.");
      setASigner(null);
      setMotDePasse("");
      await queryClient.invalidateQueries({ queryKey: ["responsable-rapports"] });
      await queryClient.invalidateQueries({ queryKey: ["responsable-suivi"] });
    },
    onError: (erreur) => {
      setMotDePasse("");
      toast.error(messageErreur(erreur, "La signature a échoué."));
    },
  });

  const signaler = (promesse: Promise<void>, defaut: string) =>
    void promesse.catch((erreur: unknown) => toast.error(messageErreur(erreur, defaut)));

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={FileSignature}
        titre="Rapports"
        description="Demandes dont tous les résultats sont validés. Signez le rapport, puis envoyez-le au client pour qu'il puisse le consulter."
        action={
          <Actualisation
            misAJour={rapports.dataUpdatedAt}
            enCours={rapports.isFetching}
            onActualiser={() => void rapports.refetch()}
          />
        }
      />

      {signatureImage.data && !signatureImage.data.presente && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-surface-alt px-4 py-3 text-sm">
          <p className="flex items-center gap-2 text-ink-900">
            <PenLine className="size-4 text-brand-900" aria-hidden="true" />
            Aucune image de signature : vos rapports porteront seulement votre nom.
          </p>
          <Link to="/app/profil" className="font-semibold text-brand-900 underline-offset-2 hover:underline">
            Importer ma signature
          </Link>
        </div>
      )}

      <BarreFiltres>
        <div className="flex shrink-0 flex-wrap gap-1 rounded-xl bg-surface-alt p-1" role="tablist" aria-label="Filtrer les rapports">
          {FILTRES.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={filtre === item.id}
              onClick={() => setFiltre(item.id)}
              className={`inline-flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-1.5 text-sm font-semibold whitespace-nowrap lg:flex-none ${
                filtre === item.id ? "bg-brand-900 text-primary-foreground shadow-sm" : "text-brand-900 hover:bg-card"
              }`}
            >
              {item.libelle}
              <span className={`rounded-full px-2 text-[11px] leading-4 ${filtre === item.id ? "bg-white/15" : "bg-card"}`}>
                {compteurs[item.id]}
              </span>
            </button>
          ))}
        </div>
        <Recherche className="w-full lg:flex-1" valeur={recherche} onChange={setRecherche} placeholder="Rechercher une demande, un client…" />
      </BarreFiltres>

      <EtatChargement
        chargement={rapports.isLoading}
        erreur={rapports.isError}
        vide={!rapports.isLoading && visibles.length === 0}
        messageVide={(rapports.data?.length ?? 0) === 0 ? "Aucune demande prête au rapport pour le moment." : "Aucun rapport ne correspond."}
        onReessayer={() => void rapports.refetch()}
      />

      <ul className="flex flex-col gap-3">
        {visibles.map((rapport) => {
          const aSigner = estASigner(rapport);
          const aEnvoyer = estAEnvoyer(rapport);
          const envoye = estEnvoye(rapport);
          return (
            <li key={rapport.demandeId} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-4">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink-900">{rapport.demandeReference}</p>
                  <Badge size="sm" variant={aSigner ? "lime" : aEnvoyer ? "dark" : "mint"}>
                    {aSigner ? "À signer" : aEnvoyer ? "À envoyer" : "Envoyé"}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                  {rapport.demandeTitre ?? "—"} · {rapport.client ?? "Client"} · {rapport.nombreAnalyses} analyse(s)
                </p>
                {(aEnvoyer || envoye) && (
                  <p className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
                    <ShieldCheck className="size-3.5 text-brand-900" aria-hidden="true" />
                    Signé par {rapport.signataire ?? "—"} le {formaterDate(rapport.dateSignature, true)}
                  </p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {aSigner && (
                  <>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Eye className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => signaler(ouvrirApercuRapport(rapport.demandeId), "L'aperçu n'a pas pu être ouvert.")}
                    >
                      Aperçu
                    </Button>
                    <Button
                      size="sm"
                      icon={<FileSignature className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => { setMotDePasse(""); setASigner(rapport); }}
                    >
                      Signer le rapport
                    </Button>
                  </>
                )}
                {aEnvoyer && rapport.rapportId && (
                  <>
                    <Button
                      size="sm"
                      variant="secondary"
                      icon={<Eye className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => signaler(ouvrirPdfRapport(rapport.rapportId as number), "Le PDF n'a pas pu être ouvert.")}
                    >
                      Consulter
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Download className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => signaler(telechargerRapport(rapport.rapportId as number, rapport.numero ?? "rapport"),
                        "Le téléchargement a échoué.")}
                    >
                      Télécharger
                    </Button>
                    <SyntheseIaBouton demandeId={rapport.demandeId} reference={rapport.demandeReference} />
                    <BoutonEnvoyer rapportId={rapport.rapportId} />
                  </>
                )}
                {envoye && rapport.rapportId && (
                  <>
                    <Button
                      size="sm"
                      variant="secondary"
                      icon={<Eye className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => signaler(ouvrirPdfRapport(rapport.rapportId as number), "Le PDF n'a pas pu être ouvert.")}
                    >
                      Consulter
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Download className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => signaler(telechargerRapport(rapport.rapportId as number, rapport.numero ?? "rapport"),
                        "Le téléchargement a échoué.")}
                    >
                      Télécharger
                    </Button>
                    <SyntheseIaBouton demandeId={rapport.demandeId} reference={rapport.demandeReference} />
                    <BoutonRenvoi rapportId={rapport.rapportId} />
                  </>
                )}
              </div>
            </li>
          );
        })}
      </ul>

      <Dialogue
        ouvert={aSigner != null}
        titre={`Signer le rapport ${aSigner?.demandeReference ?? ""}`}
        description="Votre nom, votre fonction et la date seront inscrits sur le rapport. Après signature, vous pourrez l'envoyer au client."
        onFermer={() => setASigner(null)}
      >
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (aSigner && motDePasse) signature.mutate(aSigner.demandeId);
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="mot-de-passe-signature">Votre mot de passe</Label>
            <Input
              id="mot-de-passe-signature"
              type="password"
              autoComplete="current-password"
              value={motDePasse}
              onChange={(event) => setMotDePasse(event.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={() => setASigner(null)}>Annuler</Button>
            <Button type="submit" loading={signature.isPending} disabled={!motDePasse}>
              Signer
            </Button>
          </div>
        </form>
      </Dialogue>
    </Container>
  );
}

/** Renvoie au client l'e-mail de mise à disposition, avec une confirmation. */
function BoutonRenvoi({ rapportId }: { rapportId: number }) {
  const [confirmer, setConfirmer] = useState(false);
  const renvoi = useMutation({
    mutationFn: () => renvoyerRapport(rapportId),
    onSuccess: () => {
      toast.success("E-mail renvoyé au client.");
      setConfirmer(false);
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "Le renvoi a échoué.")),
  });
  if (!confirmer) {
    return (
      <Button size="sm" variant="ghost" icon={<Send className="size-4" aria-hidden="true" />} iconPosition="left"
        onClick={() => setConfirmer(true)}>
        Renvoyer au client
      </Button>
    );
  }
  return (
    <span className="inline-flex items-center gap-2 rounded-xl bg-surface-alt px-2 py-1 text-xs text-ink-900">
      Renvoyer l'e-mail au client ?
      <Button size="sm" loading={renvoi.isPending} onClick={() => renvoi.mutate()}>Confirmer</Button>
      <Button size="sm" variant="ghost" disabled={renvoi.isPending} onClick={() => setConfirmer(false)}>Annuler</Button>
    </span>
  );
}

/** Envoie un rapport signé au client (statut SIGNE → ENVOYE). */
function BoutonEnvoyer({ rapportId }: { rapportId: number }) {
  const [confirmer, setConfirmer] = useState(false);
  const queryClient = useQueryClient();
  const envoi = useMutation({
    mutationFn: () => envoyerRapport(rapportId),
    onSuccess: async () => {
      toast.success("Rapport envoyé au client. La demande est clôturée.");
      setConfirmer(false);
      await queryClient.invalidateQueries({ queryKey: ["responsable-rapports"] });
      await queryClient.invalidateQueries({ queryKey: ["responsable-suivi"] });
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "L'envoi a échoué."))
  });
  if (!confirmer) {
    return (
      <Button size="sm" icon={<Send className="size-4" aria-hidden="true" />} iconPosition="left"
        onClick={() => setConfirmer(true)}>
        Envoyer au client
      </Button>
    );
  }
  return (
    <span className="inline-flex items-center gap-2 rounded-xl bg-surface-alt px-2 py-1 text-xs text-ink-900">
      Envoyer au client ?
      <Button size="sm" loading={envoi.isPending} onClick={() => envoi.mutate()}>Confirmer</Button>
      <Button size="sm" variant="ghost" disabled={envoi.isPending} onClick={() => setConfirmer(false)}>Annuler</Button>
    </span>
  );
}
