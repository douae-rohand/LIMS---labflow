import { Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowLeft, Ban, Play, TriangleAlert } from "lucide-react";
import { Button, Card, Skeleton } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { peut } from "@/api/session";
import { useDemarrerEssai, useTechnicienEssai } from "@/hooks/useTechnicienEssais";
import { messageErreurTechnicien } from "@/api/technicien";
import { classeStatutEssai, formatDateHeure, libelleStatutEssai } from "@/components/technicien/libelles";
import { FriseEtapesEssai } from "@/components/technicien/detail/FriseEtapesEssai";
import { BlocEchantillon } from "@/components/technicien/detail/BlocEchantillon";
import { FormResultat } from "@/components/technicien/detail/FormResultat";
import { BlocTemps } from "@/components/technicien/detail/BlocTemps";
import { BlocValidationTechnique } from "@/components/technicien/detail/BlocValidationTechnique";
import { DialogueNonRealisable } from "@/components/technicien/detail/DialogueNonRealisable";
import { HistoriqueEssai } from "@/components/technicien/detail/HistoriqueValidations";
import { cn } from "@/lib/utils";

type DetailDto = NonNullable<ReturnType<typeof useTechnicienEssai>["data"]>;

/** En-tête : retour, statut, retard, désignation, demande, dates. */
function Entete({ ligne }: { ligne: DetailDto }) {
  return (
    <div className="space-y-2">
      <Link
        to="/app/essais"
        className="inline-flex items-center gap-1 text-sm font-medium text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 rounded-lg"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Retour aux essais
      </Link>
      <div className="flex flex-wrap items-center gap-2">
        <span
          className={cn(
            "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold",
            classeStatutEssai(ligne.statut),
          )}
        >
          {libelleStatutEssai(ligne.statut)}
        </span>
        {ligne.enRetard && (
          <span className="inline-flex items-center gap-1 rounded-full border border-red-200 bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-red-700">
            <TriangleAlert className="size-3" aria-hidden="true" /> En retard
          </span>
        )}
      </div>
      <h1 className="text-2xl font-bold text-ink-900">
        {ligne.essai.code} — {ligne.essai.designation}
      </h1>
      <p className="text-sm text-muted-foreground">
        Demande {ligne.demandeNumero} · Ligne {ligne.code}
      </p>
      <p className="text-xs text-muted-foreground">
        Attribuée le {formatDateHeure(ligne.dateAttribution)}
        {ligne.echeanceEstimee && <> · Échéance estimée {formatDateHeure(ligne.echeanceEstimee)}</>}
      </p>
    </div>
  );
}

/** Carte actions secondaires : Démarrer, Non réalisable. */
function CarteActions({ ligne, onNonRealisable }: { ligne: DetailDto; onNonRealisable: () => void }) {
  const demarrerMutation = useDemarrerEssai();
  const peutDemarrer = peut("ESSAIS_GERER");
  const statut = ligne.statut;

  return (
    <Card variant="flat" padding="md" className="space-y-3">
      <h2 className="text-lg font-bold text-ink-900">Actions</h2>

      {statut === "ATTENTE_ECHANTILLON" &&
        (peutDemarrer ? (
          <Button
            variant="primary"
            icon={<Play className="size-4" aria-hidden="true" />}
            iconPosition="left"
            loading={demarrerMutation.isPending}
            disabled={demarrerMutation.isPending}
            onClick={() => demarrerMutation.mutate(ligne.ligneEssaiId)}
            className="w-full sm:w-auto"
          >
            Démarrer l'essai
          </Button>
        ) : (
          <p className="text-sm text-muted-foreground">
            Vous n'avez pas la permission de démarrer un essai.
          </p>
        ))}

      {(statut === "ATTENTE_ECHANTILLON" || statut === "EN_COURS") &&
        (peutDemarrer ? (
          <Button
            variant="secondary"
            icon={<Ban className="size-4" aria-hidden="true" />}
            iconPosition="left"
            onClick={onNonRealisable}
            disabled={demarrerMutation.isPending}
          >
            Marquer non réalisable
          </Button>
        ) : (
          <p className="text-sm text-muted-foreground">
            Vous n'avez pas la permission de marquer un essai non réalisable.
          </p>
        ))}

      {statut === "TERMINE" && (
        <p className="text-sm text-muted-foreground">Essai terminé. Aucune action disponible.</p>
      )}
      {statut === "NON_REALISABLE" && (
        <p className="text-sm text-muted-foreground">Essai déclaré non réalisable.</p>
      )}
    </Card>
  );
}

/** Page de travail : détail d'un essai + saisie + temps + validation. */
export function DetailEssaiTechnicien({ id }: { id: number }) {
  const navigate = useNavigate();
  const query = useTechnicienEssai(id);
  const [dialogueNonRealisable, setDialogueNonRealisable] = useState(false);

  const peutSaisir = peut("ESSAIS_GERER");
  const peutValiderTech = peut("VALIDATION_TECHNIQUE_GERER");

  if (query.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48 rounded-2xl" />
        <Skeleton className="h-32 w-full rounded-3xl" />
        <Skeleton className="h-48 w-full rounded-3xl" />
      </div>
    );
  }

  if (query.isError) {
    const error = query.error;
    if (error instanceof ApiError && error.status === 403) {
      return (
        <Card variant="flat" padding="lg" className="text-center">
          <p className="text-sm font-medium text-amber-700" role="alert">
            Vous n'avez pas la permission de consulter cet essai.
          </p>
          <Button variant="secondary" size="sm" className="mt-3" onClick={() => void navigate({ to: "/app/essais" })}>
            Retour aux essais
          </Button>
        </Card>
      );
    }
    return (
      <Card variant="flat" padding="lg" className="text-center">
        <p className="text-sm font-medium text-red-700" role="alert">
          {messageErreurTechnicien(error)}
        </p>
        <Button variant="secondary" size="sm" className="mt-3" onClick={() => void query.refetch()}>
          Réessayer
        </Button>
      </Card>
    );
  }

  const ligne = query.data;
  if (!ligne) return null;

  const demarre = ligne.statut === "EN_COURS" || ligne.statut === "TERMINE";
  const resultatSaisi = ligne.valeur !== null && ligne.valeur.trim() !== "";
  const valideTech = ligne.statut === "TERMINE";
  const statut = ligne.statut;

  return (
    <div className="space-y-6">
      <Entete ligne={ligne} />

      <Card variant="surface" padding="md">
        <FriseEtapesEssai demarre={demarre} resultatSaisi={resultatSaisi} valideTechniquement={valideTech} />
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <BlocEchantillon echantillon={ligne.echantillon} exigences={ligne.exigencesEchantillon} />
          <CarteActions ligne={ligne} onNonRealisable={() => setDialogueNonRealisable(true)} />
          <BlocTemps id={id} dureeMinutes={ligne.dureeMinutes} />
        </div>

        <div className="space-y-6">
          <Card variant="flat" padding="md" className="space-y-4">
            <h2 className="text-lg font-bold text-ink-900">Saisie du résultat</h2>
            {statut === "EN_COURS" ? (
              peutSaisir ? (
                <FormResultat detail={ligne} />
              ) : (
                <p className="text-sm text-muted-foreground">
                  Vous n'avez pas la permission de saisir un résultat.
                </p>
              )
            ) : (
              <p className="text-sm text-muted-foreground">
                {statut === "ATTENTE_ECHANTILLON"
                  ? "Démarrez l'essai pour saisir un résultat."
                  : statut === "TERMINE"
                    ? "Le résultat a été saisi et l'essai validé."
                    : "Cet essai est non réalisable."}
              </p>
            )}
          </Card>

          {statut === "EN_COURS" && resultatSaisi &&
            (peutValiderTech ? (
              <BlocValidationTechnique detail={ligne} />
            ) : (
              <Card variant="flat" padding="md">
                <p className="text-sm text-muted-foreground">
                  Vous n'avez pas la permission de valider techniquement cet essai.
                </p>
              </Card>
            ))}

          <HistoriqueEssai ligne={ligne} />
        </div>
      </div>

      <DialogueNonRealisable id={id} open={dialogueNonRealisable} onOpenChange={setDialogueNonRealisable} />
    </div>
  );
}
