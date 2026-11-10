import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { CheckCircle2, FileUp, Upload } from "lucide-react";
import { Button, Card } from "@/components/lab";
import { ApiError } from "@/api/axios";
import {
  useImportApercu,
  useImportConfirmer,
} from "@/hooks/useTechnicienEssais";
import { messageErreurTechnicien } from "@/api/technicien";
import type { ImportApercuResponse } from "@/api/technicien/types";
import { ZoneDepotFichier } from "@/components/technicien/import/ZoneDepotFichier";
import { ApercuImport } from "@/components/technicien/import/ApercuImport";
import { cn } from "@/lib/utils";

type Etape = 1 | 2 | 3;

export function ImportCsvTechnicien() {
  const [etape, setEtape] = useState<Etape>(1);
  const [fichier, setFichier] = useState<File | undefined>(undefined);
  const [resultatConfirmation, setResultatConfirmation] = useState<number | undefined>(undefined);

  const apercuMutation = useImportApercu();
  const confirmerMutation = useImportConfirmer();

  const apercuData: ImportApercuResponse | undefined = apercuMutation.data;

  const handleChoisirFichier = (f: File) => {
    setFichier(f);
    setResultatConfirmation(undefined);
    setEtape(1);
  };

  const handleApercu = () => {
    if (!fichier) return;
    apercuMutation.mutate(fichier, { onSuccess: () => setEtape(2) });
  };

  const handleConfirmer = () => {
    if (!fichier || !apercuData?.pret) return;
    confirmerMutation.mutate(fichier, {
      onSuccess: (res) => {
        setResultatConfirmation(res.lignesImportees);
        setEtape(3);
        // Le fichier n'est jamais conservé : on l'oublie
        setFichier(undefined);
      },
    });
  };

  const erreurApercu =
    apercuMutation.isError && apercuMutation.error instanceof ApiError
      ? messageErreurTechnicien(apercuMutation.error)
      : undefined;

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="flex items-center gap-3">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-brand-900/10 text-brand-900">
          <Upload className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Import CSV de résultats</h1>
          <p className="text-sm text-muted-foreground">
            Import groupé de résultats d&apos;essai en une opération
          </p>
        </div>
      </div>

      {/* Indicateur d'étapes */}
      <IndicateurEtapes etapeCourante={etape} />

      {/* Étape 1 : Choisir le fichier */}
      {etape === 1 && (
        <div className="space-y-4">
          <ZoneDepotFichier
            onFichier={handleChoisirFichier}
            fichierNom={fichier?.name}
            desactive={false}
          />
          {fichier && (
            <div className="flex items-center justify-between gap-4">
              <p className="text-sm text-muted-foreground">
                Fichier prêt à analyser.
              </p>
              <Button
                icon={<FileUp className="size-4" aria-hidden="true" />}
                iconPosition="left"
                loading={apercuMutation.isPending}
                disabled={apercuMutation.isPending}
                onClick={handleApercu}
              >
                Analyser le fichier
              </Button>
            </div>
          )}
          {erreurApercu && (
            <p className="text-sm text-red-700" role="alert">
              {erreurApercu}
            </p>
          )}
        </div>
      )}

      {/* Étape 2 : Aperçu */}
      {etape === 2 && apercuData && (
        <div className="space-y-4">
          <ApercuImport apercu={apercuData} />

          {/* Message « tout ou rien » */}
          <Card variant="surface" padding="md">
            <p className="text-sm text-muted-foreground">
              La confirmation importe toutes les lignes valides en une seule transaction :
              si une ligne est invalide, aucune écriture n&apos;est effectuée.
            </p>
          </Card>

          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button
              variant="secondary"
              onClick={() => {
                setFichier(undefined);
                apercuMutation.reset();
                setEtape(1);
              }}
              disabled={confirmerMutation.isPending}
            >
              Choisir un autre fichier
            </Button>
            <Button
              loading={confirmerMutation.isPending}
              disabled={!apercuData.pret || confirmerMutation.isPending}
              onClick={handleConfirmer}
              icon={<CheckCircle2 className="size-4" aria-hidden="true" />}
              iconPosition="left"
            >
              Confirmer l&apos;import
            </Button>
          </div>

          {!apercuData.pret && (
            <p className="text-sm text-amber-700" role="alert">
              Le bouton « Confirmer » est désactivé tant que le fichier contient des erreurs.
            </p>
          )}
        </div>
      )}

      {/* Étape 3 : Confirmation */}
      {etape === 3 && (
        <Card variant="flat" padding="lg" className="text-center">
          <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-green-100">
            <CheckCircle2 className="size-7 text-green-700" aria-hidden="true" />
          </div>
          <h2 className="mt-4 text-xl font-bold text-ink-900">Import réussi</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {resultatConfirmation ?? 0} résultat{resultatConfirmation !== undefined && resultatConfirmation > 1 ? "s" : ""} importé{resultatConfirmation !== undefined && resultatConfirmation > 1 ? "s" : ""}.
          </p>
          <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-center">
            <Link to="/app/essais">
              <Button variant="primary">Voir mes essais</Button>
            </Link>
            <Button
              variant="secondary"
              onClick={() => {
                setEtape(1);
                setResultatConfirmation(undefined);
              }}
            >
              Importer un autre fichier
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}

/** Indicateur visuel des 3 étapes. */
function IndicateurEtapes({ etapeCourante }: { etapeCourante: Etape }) {
  const etapes: { num: Etape; label: string }[] = [
    { num: 1, label: "Choisir le fichier" },
    { num: 2, label: "Aperçu" },
    { num: 3, label: "Confirmation" },
  ];
  return (
    <ol className="flex items-center gap-2" aria-label="Étapes de l'import">
      {etapes.map((e, i) => {
        const fait = etapeCourante > e.num;
        const courant = etapeCourante === e.num;
        return (
          <li key={e.num} className="flex flex-1 items-center gap-2">
            <div className="flex flex-1 flex-col items-center gap-1">
              <div
                className={cn(
                  "flex size-9 items-center justify-center rounded-full border-2 text-sm font-bold transition-colors",
                  fait && "bg-brand-900 border-brand-900 text-primary-foreground",
                  courant && "bg-accent-lime border-accent-lime text-ink-900",
                  !fait && !courant && "bg-card border-brand-900/15 text-muted-foreground",
                )}
                aria-current={courant ? "step" : undefined}
              >
                {fait ? <CheckCircle2 className="size-4" aria-hidden="true" /> : e.num}
              </div>
              <span
                className={cn(
                  "text-xs font-medium",
                  !fait && !courant && "text-muted-foreground",
                )}
              >
                {e.label}
              </span>
            </div>
            {i < etapes.length - 1 && (
              <div
                className={cn(
                  "h-0.5 flex-1 rounded-full",
                  fait ? "bg-brand-900" : "bg-brand-900/15",
                )}
                aria-hidden="true"
              />
            )}
          </li>
        );
      })}
    </ol>
  );
}
