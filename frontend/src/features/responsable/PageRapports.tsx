import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Download, ExternalLink, Eye, FilePlus2, FileSignature, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { Badge, Button, Input, Label } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import {
  genererRapport,
  listerRapportsTravail,
  ouvrirPdfRapport,
  signerRapport,
  telechargerRapport,
  type RapportTravail,
} from "@/api/responsable";
import { Dialogue } from "@/features/super-admin/Fenetres";
import {
  Actualisation,
  contient,
  EnTeteResponsable,
  EtatChargement,
  formaterDate,
  INTERVALLE_ACTUALISATION,
  messageErreur,
  Recherche,
} from "./commun";

type Etape = "a-generer" | "a-signer" | "libere";
type Filtre = "tous" | Etape;

function etape(rapport: RapportTravail): Etape {
  if (!rapport.rapportId || !rapport.statut) return "a-generer";
  if (rapport.statut === "GENERE" || rapport.statut === "BROUILLON") return "a-signer";
  return "libere";
}

const FILTRES: { id: Filtre; libelle: string }[] = [
  { id: "tous", libelle: "Tous" },
  { id: "a-generer", libelle: "À générer" },
  { id: "a-signer", libelle: "À signer" },
  { id: "libere", libelle: "Libérés" },
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
    refetchInterval: INTERVALLE_ACTUALISATION,
  });

  // Le serveur trie par dernière activité (signature, génération), la plus récente en haut.
  const compteurs = useMemo(() => {
    const total: Record<Filtre, number> = { tous: 0, "a-generer": 0, "a-signer": 0, libere: 0 };
    (rapports.data ?? []).forEach((r) => {
      total.tous += 1;
      total[etape(r)] += 1;
    });
    return total;
  }, [rapports.data]);
  const visibles = (rapports.data ?? [])
    .filter((r) => filtre === "tous" || etape(r) === filtre)
    .filter((r) => !recherche.trim()
      || contient(r.demandeReference, recherche)
      || contient(r.numero, recherche)
      || contient(r.client, recherche)
      || contient(r.demandeTitre, recherche));

  const actualiser = async () => {
    await queryClient.invalidateQueries({ queryKey: ["responsable-rapports"] });
    await queryClient.invalidateQueries({ queryKey: ["responsable-suivi"] });
  };

  const generation = useMutation({
    mutationFn: (demandeId: number) => genererRapport(demandeId),
    onSuccess: async () => {
      toast.success("Projet de rapport généré. Vérifiez l'aperçu avant de signer.");
      await actualiser();
    },
    onError: (erreur) => toast.error(messageErreur(erreur, "La génération a échoué.")),
  });

  const signature = useMutation({
    mutationFn: (rapportId: number) => signerRapport(rapportId, motDePasse),
    onSuccess: async () => {
      toast.success("Rapport signé et libéré. Le client a été prévenu par e-mail.");
      setASigner(null);
      setMotDePasse("");
      await actualiser();
    },
    onError: (erreur) => {
      setMotDePasse("");
      toast.error(messageErreur(erreur, "La signature a échoué."));
    },
  });

  const ouvrir = (rapportId: number, signe: boolean) =>
    void ouvrirPdfRapport(rapportId, signe).catch((erreur: unknown) =>
      toast.error(messageErreur(erreur, "Le PDF n'a pas pu être ouvert.")));

  return (
    <Container className="py-8">
      <EnTeteResponsable
        icone={FileSignature}
        titre="Rapports"
        description="Demandes dont tous les résultats sont validés : générez le rapport, vérifiez l'aperçu, puis signez-le pour le libérer au client."
        action={
          <Actualisation
            misAJour={rapports.dataUpdatedAt}
            enCours={rapports.isFetching}
            onActualiser={() => void rapports.refetch()}
          />
        }
      />

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex w-full flex-wrap gap-1 rounded-2xl border border-brand-900/10 bg-card p-1 sm:w-auto" role="tablist" aria-label="Filtrer les rapports">
          {FILTRES.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={filtre === item.id}
              onClick={() => setFiltre(item.id)}
              className={`inline-flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold sm:flex-none ${
                filtre === item.id ? "bg-brand-900 text-primary-foreground" : "text-brand-900 hover:bg-surface-alt"
              }`}
            >
              {item.libelle}
              <span className={`rounded-full px-2 py-0.5 text-[11px] ${filtre === item.id ? "bg-white/15" : "bg-surface-alt"}`}>
                {compteurs[item.id]}
              </span>
            </button>
          ))}
        </div>
        <Recherche valeur={recherche} onChange={setRecherche} placeholder="Rechercher une demande, un client…" />
      </div>

      <EtatChargement
        chargement={rapports.isLoading}
        erreur={rapports.isError}
        vide={!rapports.isLoading && visibles.length === 0}
        messageVide={(rapports.data?.length ?? 0) === 0 ? "Aucune demande terminée pour le moment." : "Aucun rapport ne correspond."}
        onReessayer={() => void rapports.refetch()}
      />

      <ul className="flex flex-col gap-3">
        {visibles.map((rapport) => {
          const etat = etape(rapport);
          return (
            <li key={rapport.demandeId} className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-900/10 bg-card px-4 py-4">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink-900">{rapport.demandeReference}</p>
                  <Badge size="sm" variant={etat === "libere" ? "mint" : etat === "a-signer" ? "lime" : "outline"}>
                    {etat === "libere" ? "Signé et libéré" : etat === "a-signer" ? "À signer" : "Rapport à générer"}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                  {rapport.demandeTitre ?? "—"} · {rapport.client ?? "Client"} · {rapport.nombreAnalyses} analyse(s)
                  {etat === "a-signer" && rapport.dateEmission ? ` · généré le ${formaterDate(rapport.dateEmission, true)}` : ""}
                </p>
                {etat === "libere" && (
                  <p className="mt-1 flex flex-wrap items-center gap-1 text-xs text-muted-foreground">
                    <ShieldCheck className="size-3.5 text-brand-900" aria-hidden="true" />
                    {rapport.numero} · signé par {rapport.signataire ?? "—"} le {formaterDate(rapport.dateSignature, true)}
                    {rapport.empreinte && <span className="font-mono" title={rapport.empreinte}> · SHA-256 {rapport.empreinte.slice(0, 12)}…</span>}
                  </p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {etat === "a-generer" && (
                  <Button
                    size="sm"
                    icon={<FilePlus2 className="size-4" aria-hidden="true" />}
                    iconPosition="left"
                    loading={generation.isPending && generation.variables === rapport.demandeId}
                    disabled={generation.isPending}
                    onClick={() => generation.mutate(rapport.demandeId)}
                  >
                    Générer le rapport
                  </Button>
                )}
                {etat === "a-signer" && rapport.rapportId && (
                  <>
                    <Button size="sm" variant="secondary" icon={<Eye className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => ouvrir(rapport.rapportId as number, false)}>
                      Aperçu
                    </Button>
                    <Button size="sm" icon={<FileSignature className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => { setMotDePasse(""); setASigner(rapport); }}>
                      Signer et libérer
                    </Button>
                  </>
                )}
                {etat === "libere" && rapport.rapportId && (
                  <>
                    <Button size="sm" variant="secondary" icon={<Eye className="size-4" aria-hidden="true" />} iconPosition="left" onClick={() => ouvrir(rapport.rapportId as number, true)}>
                      Consulter
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Download className="size-4" aria-hidden="true" />}
                      iconPosition="left"
                      onClick={() => void telechargerRapport(rapport.rapportId as number, rapport.numero ?? "rapport")
                        .catch((erreur: unknown) => toast.error(messageErreur(erreur, "Le téléchargement a échoué.")))}
                    >
                      Télécharger
                    </Button>
                    {rapport.urlVerification && (
                      <a
                        href={rapport.urlVerification}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-semibold text-brand-900 hover:bg-surface-alt"
                      >
                        <ExternalLink className="size-4" aria-hidden="true" />
                        Vérifier
                      </a>
                    )}
                  </>
                )}
              </div>
            </li>
          );
        })}
      </ul>

      <Dialogue
        ouvert={aSigner != null}
        titre="Signer et libérer ce rapport ?"
        description="Le PDF est signé numériquement (PAdES) au nom du laboratoire, horodaté par une autorité indépendante et muni d'un QR code de vérification. Il devient visible au client, prévenu par e-mail. Un rapport signé ne peut plus être modifié."
        onFermer={() => setASigner(null)}
      >
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (aSigner?.rapportId && motDePasse) signature.mutate(aSigner.rapportId);
          }}
        >
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="mot-de-passe-signature">Confirmez avec votre mot de passe</Label>
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
              Signer et libérer
            </Button>
          </div>
        </form>
      </Dialogue>
    </Container>
  );
}
