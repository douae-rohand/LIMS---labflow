import { Link, useRouter } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { useCallback, useEffect, useMemo } from "react";
import { toast } from "sonner";
import { messageErreurPanier } from "@/api/paniers";
import { getSession } from "@/api/session";
import { AnalyseCard } from "@/components/laboratoires/AnalyseCard";
import { InfosLaboratoire } from "@/components/laboratoires/InfosLaboratoire";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { LabMap } from "@/components/map/LabMap";
import { Badge, Button, Card, SectionHeader } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { useAjouterAuPanier, usePanier } from "@/hooks/usePanier";
import { useAnalysesLaboratoire, useLaboratoirePublic } from "@/hooks/useLaboratoiresPublic";
import type { BaseCatalogue } from "@/lib/redirect";

/**
 * Garde anti double ajout. React StrictMode exécute les effets deux fois :
 * la clé est posée avant l'appel réseau puis retirée dès que la requête est
 * terminée (succès comme échec).
 */
const _ajoutsEnCours = new Set<string>();

/**
 * Fiche d'un laboratoire et son catalogue, rendue à l'identique dans l'espace
 * public et dans l'espace client. `base` détermine les liens internes
 * (`/laboratoires` ou `/app/laboratoires`) ; la logique du paramètre `ajouter`
 * (ajout unique, toast, nettoyage de l'URL) est identique dans les deux cas.
 */
export function PageLaboratoire({
  id,
  ajouter,
  base,
}: {
  id: string;
  ajouter?: number | undefined;
  base: BaseCatalogue;
}) {
  const router = useRouter();
  const laboratoireId = Number(id);

  const session = getSession();
  const estAnonyme = session === null;
  const estClient = session?.user.role === "CLIENT";
  const boutonVisible = estClient || estAnonyme;

  const labQuery = useLaboratoirePublic(laboratoireId);
  const analysesQuery = useAnalysesLaboratoire(laboratoireId);
  const panierQuery = usePanier(estClient ? laboratoireId : undefined);
  const ajoutMutation = useAjouterAuPanier(laboratoireId);
  const ajouterEssai = ajoutMutation.mutate;

  const lab = labQuery.data;
  const analyses = analysesQuery.data ?? [];
  const hasCoords = lab?.latitude != null && lab.longitude != null;

  const essaisDansPanier = useMemo(
    () => new Set((panierQuery.data?.lignes ?? []).map((ligne) => ligne.essaiId)),
    [panierQuery.data],
  );

  const retirerParamAjouter = useCallback(() => {
    router.history.replace(`${base}/${id}`);
  }, [router, base, id]);

  const ajouterAuPanier = useCallback(
    (analyseId: number) => {
      const cle = `${laboratoireId}:${analyseId}`;
      if (_ajoutsEnCours.has(cle)) return;
      _ajoutsEnCours.add(cle);
      ajouterEssai(analyseId, {
        onSuccess: () => {
          toast.success("Essai ajouté au panier.");
        },
        onError: (error) => {
          toast.error(messageErreurPanier(error));
        },
        onSettled: () => {
          _ajoutsEnCours.delete(cle);
        },
      });
    },
    [ajouterEssai, laboratoireId],
  );

  const demanderConnexion = useCallback(
    (analyseId: number) => {
      const cible = `${base}/${id}?ajouter=${analyseId}`;
      router.history.push(`/login?redirect=${encodeURIComponent(cible)}`);
    },
    [router, base, id],
  );

  // Retour d'une connexion : base/{id}?ajouter={essaiId}
  useEffect(() => {
    if (ajouter === undefined) return;
    if (estClient) {
      ajouterAuPanier(ajouter);
      retirerParamAjouter();
      return;
    }
    if (estAnonyme) {
      demanderConnexion(ajouter);
      return;
    }
    // Rôle sans panier : le paramètre est simplement retiré de l'URL.
    retirerParamAjouter();
  }, [
    ajouter,
    estClient,
    estAnonyme,
    ajouterAuPanier,
    demanderConnexion,
    retirerParamAjouter,
  ]);

  return (
    <Container className="py-12 sm:py-16">
      <Link to={base} className="inline-flex">
        <Button variant="ghost" icon={<ArrowLeft className="size-4" aria-hidden="true" />} iconPosition="left">
          Tous les laboratoires
        </Button>
      </Link>

      <div className="mt-6">
        <LandingState
          loading={labQuery.isPending}
          error={labQuery.error}
          onRetry={() => {
            void labQuery.refetch();
          }}
          empty={!lab}
          emptyMessage="Ce laboratoire est introuvable ou n'est plus actif."
          skeleton={<CardsSkeleton count={2} />}
        >
          {lab ? (
            <div className="flex flex-col gap-10">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <SectionHeader
                  align="left"
                  eyebrow={lab.code}
                  title={lab.raisonSociale}
                  description={
                    lab.ville?.trim()
                      ? `${lab.ville}${lab.adresse?.trim() ? ` — ${lab.adresse}` : ""}`
                      : "Localisation non renseignée"
                  }
                />
                <Badge variant="mint">{lab.statut}</Badge>
              </div>

              <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
                <InfosLaboratoire laboratoire={lab} />

                {hasCoords ? (
                  <LabMap
                    laboratoires={[lab]}
                    variant="embed"
                    className="max-w-none"
                    base={base}
                  />
                ) : (
                  <Card variant="surface" className="flex items-center justify-center">
                    <p className="text-sm text-muted-foreground">
                      Aucune localisation GPS n'est enregistrée pour ce laboratoire.
                    </p>
                  </Card>
                )}
              </div>

              <section>
                <SectionHeader
                  align="left"
                  eyebrow="Catalogue"
                  title="Analyses proposées"
                  description="Essais actifs lus dans le schéma de ce laboratoire."
                />
                <div className="mt-8">
                  <LandingState
                    loading={analysesQuery.isPending}
                    error={analysesQuery.error}
                    onRetry={() => {
                      void analysesQuery.refetch();
                    }}
                    empty={analyses.length === 0}
                    emptyMessage="Aucune analyse active n'est encore proposée par ce laboratoire."
                    skeleton={<CardsSkeleton count={3} />}
                  >
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                      {analyses.map((analyse) => (
                        <AnalyseCard
                          key={analyse.id}
                          analyse={analyse}
                          dejaDansPanier={essaisDansPanier.has(analyse.id)}
                          enAjout={
                            ajoutMutation.isPending && ajoutMutation.variables === analyse.id
                          }
                          onAjouter={
                            boutonVisible
                              ? () => {
                                  if (estClient) {
                                    ajouterAuPanier(analyse.id);
                                  } else {
                                    demanderConnexion(analyse.id);
                                  }
                                }
                              : undefined
                          }
                        />
                      ))}
                    </div>
                  </LandingState>
                </div>
              </section>
            </div>
          ) : null}
        </LandingState>
      </div>
    </Container>
  );
}
