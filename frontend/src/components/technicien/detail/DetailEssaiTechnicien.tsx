import { Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowLeft, Ban, Calendar, Clock, FileText, Play, Tag, TriangleAlert } from "lucide-react";
import { Button, Card, Label, Skeleton, Textarea } from "@/components/lab";
import { ApiError } from "@/api/axios";
import { peut } from "@/api/session";
import { useDeclareNonRealisable, useDemarrerEssai, useTechnicienEssai } from "@/hooks/useTechnicienEssais";
import { messageErreurTechnicien } from "@/api/technicien";
import { classeStatutEssai, formatDateHeure, libelleStatutEssai } from "@/components/technicien/libelles";
import { FriseEtapesEssai } from "@/components/technicien/detail/FriseEtapesEssai";
import { BlocEchantillon } from "@/components/technicien/detail/BlocEchantillon";
import { FormResultat } from "@/components/technicien/detail/FormResultat";
import { BlocTemps } from "@/components/technicien/detail/BlocTemps";
import { BlocValidationTechnique } from "@/components/technicien/detail/BlocValidationTechnique";
import { HistoriqueEssai } from "@/components/technicien/detail/HistoriqueValidations";
import { cn } from "@/lib/utils";

type DetailDto = NonNullable<ReturnType<typeof useTechnicienEssai>["data"]>;

function MetaItem({ icone: Icone, texte }: { icone: React.ElementType; texte: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-sm text-muted-foreground">
      <Icone className="size-3.5 shrink-0" aria-hidden="true" />
      {texte}
    </span>
  );
}

function Entete({ ligne }: { ligne: DetailDto }) {
  return (
    <div className="space-y-3">
      <Link
        to="/app/essais"
        className="inline-flex items-center gap-1 text-sm font-medium text-brand-900 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-900 rounded-lg"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Retour aux essais
      </Link>

      <div className="flex flex-wrap items-center gap-2">
        <span className={cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold", classeStatutEssai(ligne.statut))}>
          {libelleStatutEssai(ligne.statut)}
        </span>
        {ligne.enRetard && (
          <span className="inline-flex items-center gap-1 rounded-full border border-red-200 bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-red-700">
            <TriangleAlert className="size-3" aria-hidden="true" /> En retard
          </span>
        )}
      </div>

      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-1">Essai</p>
        <h1 className="text-2xl font-bold text-ink-900">
          {ligne.essai.code} — {ligne.essai.designation}
        </h1>
      </div>

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 rounded-2xl border border-brand-900/8 bg-surface-alt px-4 py-3">
        <MetaItem icone={FileText} texte={`Demande ${ligne.demandeNumero}`} />
        {ligne.demandeTitre && <MetaItem icone={FileText} texte={ligne.demandeTitre} />}
        <MetaItem icone={Tag} texte={`Ligne ${ligne.code}`} />
        <MetaItem icone={Calendar} texte={`Attribuée le ${formatDateHeure(ligne.dateAttribution)}`} />
        {ligne.echeanceEstimee && (
          <MetaItem icone={Clock} texte={`Échéance ${formatDateHeure(ligne.echeanceEstimee)}`} />
        )}
      </div>
    </div>
  );
}

/** Encart "Marquer non réalisable" inline — même style que BlocValidationTechnique. */
function BlocNonRealisable({ id }: { id: number }) {
  const [ouvert, setOuvert] = useState(false);
  const [motif, setMotif] = useState("");
  const [erreur, setErreur] = useState("");
  const mutation = useDeclareNonRealisable();
  const enCours = mutation.isPending;

  const confirmer = () => {
    if (motif.trim().length < 5) {
      setErreur("Le motif doit contenir au moins 5 caractères.");
      return;
    }
    setErreur("");
    mutation.mutate({ id, payload: { motif: motif.trim() } }, {
      onSuccess: () => { setOuvert(false); setMotif(""); },
    });
  };

  return (
    <Card variant="flat" padding="md" className="space-y-4 border-red-200 bg-red-50/40">
      <div className="flex items-center gap-2">
        <Ban className="size-5 text-red-600" aria-hidden="true" />
        <h2 className="text-base font-bold text-red-900">Marquer non réalisable</h2>
      </div>

      {!ouvert ? (
        <>
          <p className="text-sm text-red-800">
            L'essai ne peut pas être réalisé (échantillon insuffisant, problème technique, etc.).
            Cette action est irréversible.
          </p>
          <Button
            variant="secondary"
            icon={<Ban className="size-4" aria-hidden="true" />}
            iconPosition="left"
            onClick={() => setOuvert(true)}
            className="border-red-200 text-red-700 hover:bg-red-100"
          >
            Déclarer non réalisable
          </Button>
        </>
      ) : (
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="motif-non-realisable">
              Motif <span className="text-red-500 text-xs">*</span>
            </Label>
            <Textarea
              id="motif-non-realisable"
              rows={3}
              maxLength={1000}
              placeholder="Ex. : échantillon insuffisant pour réaliser l'analyse."
              disabled={enCours}
              value={motif}
              onChange={(e) => { setMotif(e.target.value); setErreur(""); }}
            />
            {erreur && <p role="alert" className="text-sm text-red-600">{erreur}</p>}
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              loading={enCours}
              disabled={enCours}
              onClick={confirmer}
              className="bg-red-600 text-white hover:bg-red-700 border-red-600"
            >
              Confirmer
            </Button>
            <Button
              type="button"
              variant="ghost"
              disabled={enCours}
              onClick={() => { setOuvert(false); setMotif(""); setErreur(""); }}
            >
              Annuler
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
}

export function DetailEssaiTechnicien({ id }: { id: number }) {
  const navigate = useNavigate();
  const query = useTechnicienEssai(id);
  const demarrerMutation = useDemarrerEssai();

  const peutSaisir = peut("ESSAIS_GERER");
  const peutValiderTech = peut("VALIDATION_TECHNIQUE_GERER");
  const peutDemarrer = peut("ESSAIS_GERER");

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

      {/* En-tête */}
      <Entete ligne={ligne} />

      {/* Bouton démarrer (hors grille, pleine largeur sur mobile) */}
      {peutDemarrer && statut === "ATTENTE_ECHANTILLON" && (
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
      )}

      {/* Frise de progression */}
      <Card variant="surface" padding="md">
        <FriseEtapesEssai demarre={demarre} resultatSaisi={resultatSaisi} valideTechniquement={valideTech} />
      </Card>

      {/* Corps principal — layout 7/5 */}
      <div className="grid items-start gap-6 lg:grid-cols-12">

        {/* Colonne gauche : Échantillon + Saisie résultat */}
        <div className="space-y-6 lg:col-span-7">

          {/* 1. Échantillon — toujours affiché en premier */}
          <BlocEchantillon echantillon={ligne.echantillon} exigences={ligne.exigencesEchantillon} />

          {/* 2. Saisie du résultat */}
          {statut === "EN_COURS" && peutSaisir && (
            <Card variant="surface" padding="md" className="space-y-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-1">
                  Action requise
                </p>
                <h2 className="text-base font-bold text-ink-900">Saisie du résultat</h2>
              </div>
              <FormResultat detail={ligne} />
            </Card>
          )}
        </div>

          {/* Colonne droite : Temps + Historique + Validation technique + Non réalisable */}
        <aside aria-label="Informations complémentaires" className="space-y-5 lg:col-span-5">

          <BlocTemps id={id} dureeMinutes={ligne.dureeMinutes} />
          <HistoriqueEssai ligne={ligne} />

          {/* 5. Validation technique */}
          {statut === "EN_COURS" && resultatSaisi && peutValiderTech && (
            <BlocValidationTechnique detail={ligne} />
          )}

          {/* 6. Non réalisable */}
          {peutDemarrer && (statut === "ATTENTE_ECHANTILLON" || statut === "EN_COURS") && (
            <BlocNonRealisable id={id} />
          )}
        </aside>
      </div>
    </div>
  );
}
