import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertCircle, FileText, RefreshCw } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button, Skeleton } from "@/components/lab";
import { ApiError } from "@/api/axios";
import {
  accepterDemande,
  detailDemandeLaboratoire,
  messageErreurDemande,
  refuserDemande,
  type DemandeLaboratoire,
  type StatutDemande,
} from "@/api/demandes";
import { formatDate, formatDateTime } from "@/components/employes/dateUtils";
import { StatutDemandeBadge } from "./StatutDemandeBadge";
import { RefusDemandeModal } from "./RefusDemandeModal";

const LIBELLE_STATUT_LIGNE: Record<string, string> = {
  EN_ATTENTE: "En attente",
  PLANIFIEE: "Planifiée",
  PLANIFIE: "Planifiée",
  EN_COURS: "En cours",
  TERMINE: "Terminé",
  TERMINEE: "Terminée",
  ANNULE: "Annulé",
  ANNULEE: "Annulée",
  A_REFAIRE: "À refaire",
  REPORTEE: "Reportée",
  ATTENTE_ECHANTILLON: "En attente d'échantillon",
  NON_REALISABLE: "Non réalisable",
  RECEPTIONNE: "Réceptionné",
  EN_ANALYSE: "En analyse",
  ANALYSE_TERMINEE: "Analyse terminée",
  ARCHIVE: "Archivé",
  ELIMINE: "Éliminé",
  NON_CONFORME: "Non conforme",
};

function libelleStatut(valeur: string | null): string {
  if (!valeur) return "—";
  return LIBELLE_STATUT_LIGNE[valeur] ?? valeur;
}

function formatMontant(valeur: number | null): string {
  if (valeur == null) return "—";
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(valeur);
}

function formatTaille(octets: number | null): string {
  if (octets == null) return "";
  if (octets < 1024) return `${octets} o`;
  if (octets < 1024 * 1024) return `${Math.round(octets / 1024)} Ko`;
  return `${(octets / (1024 * 1024)).toFixed(1)} Mo`;
}

function messageStatut(statut: StatutDemande): string | null {
  switch (statut) {
    case "NOUVEAU":
      return null;
    case "EN_VALIDATION":
      return "Cette demande est en validation.";
    case "REFUSEE":
      return "Cette demande a été refusée.";
    case "ACCEPTEE":
      return "Cette demande a été acceptée.";
    case "EN_COURS":
      return "Cette demande est déjà en cours de traitement.";
    case "TERMINEE":
      return "Cette demande est terminée.";
    case "ANNULEE":
      return "Cette demande a été annulée.";
    default:
      return "Cette demande ne peut plus être acceptée ou refusée.";
  }
}

function Ligne({ label, valeur }: { label: string; valeur: string | null | undefined }) {
  return (
    <div>
      <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm text-ink-900">{valeur && valeur.trim().length > 0 ? valeur : "—"}</dd>
    </div>
  );
}

interface DemandePanneauProps {
  demandeId: number | undefined;
  onClose: () => void;
}

export function DemandePanneau({ demandeId, onClose }: DemandePanneauProps) {
  const estOuvert = demandeId !== undefined;
  const queryClient = useQueryClient();
  const [confirmationOuverte, setConfirmationOuverte] = useState(false);
  const [refusOuvert, setRefusOuvert] = useState(false);
  const [erreurAction, setErreurAction] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["demande-laboratoire", demandeId],
    queryFn: () => {
      if (demandeId === undefined) throw new Error("Identifiant manquant");
      return detailDemandeLaboratoire(demandeId);
    },
    enabled: estOuvert,
    staleTime: 15_000,
    retry: (count, error) => !(error instanceof ApiError && (error.status === 404 || error.status === 403)) && count < 2,
  });

  function rafraichir(detail: DemandeLaboratoire) {
    queryClient.setQueryData(["demande-laboratoire", detail.id], detail);
    void queryClient.invalidateQueries({ queryKey: ["demandes-laboratoire"] });
    void queryClient.invalidateQueries({ queryKey: ["demandes-indicateurs"] });
  }

  const acceptation = useMutation({
    mutationFn: (id: number) => accepterDemande(id),
    onSuccess: (detail) => {
      rafraichir(detail);
      setConfirmationOuverte(false);
      setErreurAction(null);
      toast.success("La demande a été acceptée.");
    },
    onError: (error) => {
      setErreurAction(messageErreurDemande(error));
    },
  });

  const refus = useMutation({
    mutationFn: ({ id, motif }: { id: number; motif: string }) => refuserDemande(id, motif),
    onSuccess: (detail) => {
      rafraichir(detail);
      setRefusOuvert(false);
      setErreurAction(null);
      toast.success("La demande a été refusée.");
    },
    onError: (error) => {
      setErreurAction(messageErreurDemande(error));
    },
  });

  const demande = query.data;
  const message = demande ? messageStatut(demande.statut) : null;

  return (
    <>
      <Sheet open={estOuvert} onOpenChange={(open) => { if (!open) onClose(); }}>
        <SheetContent className="flex w-full flex-col overflow-y-auto sm:max-w-xl">
          <SheetHeader className="pr-8 text-left">
            <SheetTitle className="text-ink-900">
              {demande?.reference ?? "Demande client"}
            </SheetTitle>
            <SheetDescription>
              {demande?.objet ?? "Détail de la demande adressée au laboratoire."}
            </SheetDescription>
          </SheetHeader>

          <div className="mt-6 flex-1 space-y-6">
            {query.isLoading && (
              <div className="space-y-3" aria-busy="true" aria-live="polite">
                <Skeleton className="h-6 w-40" />
                <Skeleton className="h-24 w-full" />
                <Skeleton className="h-24 w-full" />
              </div>
            )}

            {query.isError && (
              <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                <p className="flex items-center gap-2 font-semibold">
                  <AlertCircle className="size-4" aria-hidden="true" />
                  {messageErreurDemande(query.error)}
                </p>
                <Button
                  type="button"
                  className="mt-3"
                  size="sm"
                  variant="secondary"
                  icon={<RefreshCw className="size-4" aria-hidden="true" />}
                  iconPosition="left"
                  onClick={() => void query.refetch()}
                >
                  Réessayer
                </Button>
              </div>
            )}

            {demande && (
              <>
                <div className="flex flex-wrap items-center gap-2">
                  <StatutDemandeBadge statut={demande.statut} />
                </div>

                <section className="space-y-3">
                  <h3 className="text-sm font-bold text-ink-900">Demande</h3>
                  <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <Ligne label="Objet" valeur={demande.objet} />
                    <Ligne label="Soumise le" valeur={formatDateTime(demande.dateSoumission)} />
                    <div className="sm:col-span-2">
                      <Ligne label="Objectif" valeur={demande.objectif} />
                    </div>
                    {demande.dateDecision && (
                      <Ligne label="Date de décision" valeur={formatDateTime(demande.dateDecision)} />
                    )}
                    {demande.decideurNom && <Ligne label="Décision prise par" valeur={demande.decideurNom} />}
                    {demande.motif && (
                      <div className="sm:col-span-2">
                        <Ligne label="Motif du refus" valeur={demande.motif} />
                      </div>
                    )}
                  </dl>
                </section>

                <section className="space-y-3">
                  <h3 className="text-sm font-bold text-ink-900">Client</h3>
                  {demande.client ? (
                    <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                      <Ligne label="Raison sociale" valeur={demande.client.raisonSociale} />
                      <Ligne label="Code" valeur={demande.client.code} />
                      <Ligne label="Contact" valeur={demande.client.nomComplet} />
                      <Ligne label="E-mail" valeur={demande.client.email} />
                      <Ligne label="Téléphone" valeur={demande.client.telephone} />
                      <Ligne label="ICE" valeur={demande.client.ice} />
                      <div className="sm:col-span-2">
                        <Ligne label="Adresse" valeur={demande.client.adresse} />
                      </div>
                    </dl>
                  ) : (
                    <p className="text-sm text-muted-foreground">Aucun client n'est rattaché à cette demande.</p>
                  )}
                </section>

                {demande.patient && (
                  <section className="space-y-3">
                    <h3 className="text-sm font-bold text-ink-900">Patient</h3>
                    <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                      <Ligne label="Nom" valeur={demande.patient.nomComplet} />
                      <Ligne label="Code" valeur={demande.patient.code} />
                      <Ligne
                        label="Date de naissance"
                        valeur={demande.patient.dateNaissance ? formatDate(demande.patient.dateNaissance) : null}
                      />
                      <Ligne label="Téléphone" valeur={demande.patient.telephone} />
                      <Ligne label="E-mail" valeur={demande.patient.email} />
                    </dl>
                  </section>
                )}

                <section className="space-y-3">
                  <h3 className="text-sm font-bold text-ink-900">Analyses demandées</h3>
                  {demande.analyses.length === 0 ? (
                    <p className="text-sm text-muted-foreground">Aucune analyse n'est associée à cette demande.</p>
                  ) : (
                    <ul className="space-y-2">
                      {demande.analyses.map((analyse) => (
                        <li key={analyse.id} className="rounded-2xl border border-brand-900/10 px-4 py-3">
                          <p className="font-semibold text-ink-900">
                            {analyse.designation || analyse.code}
                          </p>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {[analyse.code, analyse.methode, analyse.unite].filter(Boolean).join(" · ")}
                          </p>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {libelleStatut(analyse.statut)} · {formatMontant(analyse.montant ?? analyse.tarif)}
                          </p>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>

                {demande.echantillons.length > 0 && (
                  <section className="space-y-3">
                    <h3 className="text-sm font-bold text-ink-900">Échantillons</h3>
                    <ul className="space-y-2">
                      {demande.echantillons.map((echantillon) => (
                        <li key={echantillon.id} className="rounded-2xl border border-brand-900/10 px-4 py-3 text-sm">
                          <p className="font-semibold text-ink-900">{echantillon.reference}</p>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {echantillon.nature ?? "—"}
                          </p>
                        </li>
                      ))}
                    </ul>
                  </section>
                )}

                <section className="space-y-3">
                  <h3 className="text-sm font-bold text-ink-900">Documents</h3>
                  {demande.documents.length === 0 ? (
                    <p className="text-sm text-muted-foreground">Aucun document n'est joint à cette demande.</p>
                  ) : (
                    <ul className="space-y-2">
                      {demande.documents.map((document) => (
                        <li key={document.id} className="flex items-start gap-3 rounded-2xl border border-brand-900/10 px-4 py-3">
                          <FileText className="mt-0.5 size-4 shrink-0 text-brand-900" aria-hidden="true" />
                          <div>
                            <p className="text-sm font-semibold text-ink-900">
                              {document.nomFichier || document.code}
                            </p>
                            <p className="text-xs text-muted-foreground">
                              {[document.typeMime, formatTaille(document.taille), document.dateAjout ? formatDate(document.dateAjout) : null]
                                .filter(Boolean)
                                .join(" · ")}
                            </p>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>

                {message && (
                  <p className="rounded-2xl bg-surface-card px-4 py-3 text-sm text-ink-900">{message}</p>
                )}
                {erreurAction && !refusOuvert && !confirmationOuverte && (
                  <p className="text-sm text-red-600">{erreurAction}</p>
                )}
              </>
            )}
          </div>

          {demande?.decisionPossible && (
            <div className="sticky bottom-0 mt-6 flex flex-col gap-2 border-t border-brand-900/10 bg-card py-4 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setErreurAction(null);
                  setRefusOuvert(true);
                }}
                disabled={acceptation.isPending || refus.isPending}
              >
                Refuser
              </Button>
              <Button
                type="button"
                onClick={() => {
                  setErreurAction(null);
                  setConfirmationOuverte(true);
                }}
                disabled={acceptation.isPending || refus.isPending}
              >
                Accepter
              </Button>
            </div>
          )}
        </SheetContent>
      </Sheet>

      <AlertDialog open={confirmationOuverte} onOpenChange={setConfirmationOuverte}>
        <AlertDialogContent className="rounded-3xl">
          <AlertDialogHeader>
            <AlertDialogTitle>Accepter cette demande ?</AlertDialogTitle>
            <AlertDialogDescription>
              La demande passera au statut « Acceptée ». Cette décision est enregistrée avec votre nom et la date du jour.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {erreurAction && confirmationOuverte && (
            <p className="text-sm text-red-600">{erreurAction}</p>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={acceptation.isPending}>Annuler</AlertDialogCancel>
            <AlertDialogAction
              disabled={acceptation.isPending || demandeId === undefined}
              onClick={(event) => {
                event.preventDefault();
                if (demandeId !== undefined) {
                  acceptation.mutate(demandeId);
                }
              }}
            >
              {acceptation.isPending ? "Acceptation…" : "Confirmer l'acceptation"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <RefusDemandeModal
        open={refusOuvert}
        chargement={refus.isPending}
        erreur={erreurAction}
        onOpenChange={(open) => {
          setRefusOuvert(open);
          if (!open) setErreurAction(null);
        }}
        onConfirmer={(motif) => {
          if (demandeId !== undefined) {
            setErreurAction(null);
            refus.mutate({ id: demandeId, motif });
          }
        }}
      />
    </>
  );
}
