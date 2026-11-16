import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { UserCheck, UserX, Send, AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "@/components/lab";
import { toast } from "sonner";
import {
  lireImpactDesactivationEmploye,
  messageErreurApi,
  prevenirResponsableAvantDesactivation,
} from "@/api/employes";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from "@/components/ui/alert-dialog";
import type { EmployeDetailDto } from "@/api/employes";

interface EmployeActionsProps {
  employe: EmployeDetailDto;
  isDirty: boolean;
  isMutatingGlobale: boolean;
  cooldownRenvoi: number;
  onDesactiver: () => void;
  onReactiver: () => void;
  onRenvoyerInvitation: () => void;
}

export function EmployeActions({
  employe,
  isDirty,
  isMutatingGlobale,
  cooldownRenvoi,
  onDesactiver,
  onReactiver,
  onRenvoyerInvitation,
}: EmployeActionsProps) {
  const [modalDesactivationOuverte, setModalDesactivationOuverte] = useState(false);
  const [responsablePrevenu, setResponsablePrevenu] = useState(false);

  const estActif = employe.statut === "ACTIF";
  const estDesactive = employe.statut === "DESACTIVE";
  const estEnAttente = employe.statut === "EN_ATTENTE";
  const impact = useQuery({
    queryKey: ["employe-impact-desactivation", employe.id],
    queryFn: () => lireImpactDesactivationEmploye(employe.id),
    enabled: modalDesactivationOuverte,
    retry: false,
  });
  const notification = useMutation({
    mutationFn: () => prevenirResponsableAvantDesactivation(employe.id),
    onSuccess: () => {
      setResponsablePrevenu(true);
      toast.success("Les Responsables actifs ont été prévenus.");
    },
    onError: (erreur) => toast.error(messageErreurApi(erreur)),
  });

  const handleConfirmerDesactivation = () => {
    setModalDesactivationOuverte(false);
    onDesactiver();
  };

  return (
    <div className="space-y-3 border-t border-brand-900/10 pt-4">
      <div className="flex items-center justify-between">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          Actions sur le statut
        </h3>
        {isDirty && (
          <p className="text-[11px] text-amber-700">
            Enregistrez ou annulez vos modifications avant de changer le statut.
          </p>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        {/* Compte ACTIF -> Désactiver */}
        {estActif && (
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={isDirty || isMutatingGlobale}
            onClick={() => setModalDesactivationOuverte(true)}
            className="text-red-700 border-red-200 hover:bg-red-50 hover:text-red-800"
            icon={<UserX className="size-4 text-red-600" aria-hidden="true" />}
            iconPosition="left"
          >
            Désactiver le compte
          </Button>
        )}

        {/* Compte DESACTIVE -> Réactiver */}
        {estDesactive && (
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={isDirty || isMutatingGlobale}
            onClick={onReactiver}
            icon={<UserCheck className="size-4 text-brand-900" aria-hidden="true" />}
            iconPosition="left"
          >
            Réactiver le compte
          </Button>
        )}

        {/* Compte EN_ATTENTE -> Renvoyer l'invitation */}
        {estEnAttente && (
          <div className="flex flex-col gap-1">
            <Button
              type="button"
              variant={employe.invitationExpiree ? "primary" : "secondary"}
              size="sm"
              disabled={isDirty || isMutatingGlobale || cooldownRenvoi > 0}
              onClick={onRenvoyerInvitation}
              icon={<Send className="size-4" aria-hidden="true" />}
              iconPosition="left"
            >
              {cooldownRenvoi > 0
                ? `Renvoyer (${cooldownRenvoi} s)`
                : "Renvoyer le lien d'activation"}
            </Button>
            {!employe.invitationExpiree && (
              <span className="text-[11px] text-muted-foreground">
                L'ancien lien sera invalidé.
              </span>
            )}
          </div>
        )}
      </div>

      {/* Confirmation de désactivation */}
      <AlertDialog
        open={modalDesactivationOuverte}
        onOpenChange={(open) => {
          setModalDesactivationOuverte(open);
          if (open) setResponsablePrevenu(false);
        }}
      >
        <AlertDialogContent className="w-[90vw] max-w-md rounded-2xl">
          <AlertDialogHeader>
            <div className="mb-2 flex size-10 items-center justify-center rounded-full bg-red-100 text-red-600">
              <AlertTriangle className="size-5" aria-hidden="true" />
            </div>
            <AlertDialogTitle className="text-lg font-bold text-ink-900">
              Désactiver le compte employé ?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-sm text-muted-foreground">
              Le compte de {employe.nomComplet} sera désactivé et ses sessions coupées.
            </AlertDialogDescription>
            {impact.isLoading && (
              <p className="text-sm text-muted-foreground">Calcul de l’impact…</p>
            )}
            {impact.isError && (
              <div className="flex items-center justify-between gap-3 rounded-xl bg-rose-50 p-3 text-sm text-rose-800">
                <span>Impossible de calculer les conséquences de la désactivation.</span>
                <Button
                  type="button"
                  size="sm"
                  variant="secondary"
                  onClick={() => void impact.refetch()}
                >
                  <RefreshCw className="size-4" aria-hidden="true" /> Réessayer
                </Button>
              </div>
            )}
            {impact.data && employe.role === "TECHNICIEN" && (
              <div className="space-y-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
                <p>
                  {impact.data.essaisOuverts} essai(s) ouvert(s) resteront visibles pour le
                  Responsable et devront être réaffectés.
                </p>
                {impact.data.essaisOuverts > 0 && impact.data.responsablesActifs > 0 && (
                  <Button
                    type="button"
                    size="sm"
                    variant="secondary"
                    disabled={responsablePrevenu || notification.isPending}
                    loading={notification.isPending}
                    onClick={() => notification.mutate()}
                  >
                    {responsablePrevenu
                      ? "Responsables prévenus"
                      : "Prévenir les Responsables maintenant"}
                  </Button>
                )}
                {impact.data.essaisOuverts > 0 && impact.data.responsablesActifs === 0 && (
                  <p className="font-semibold">
                    Aucun Responsable actif ne peut recevoir de notification.
                  </p>
                )}
              </div>
            )}
            {impact.data?.dernierCompteActifDuRole && employe.role === "RESPONSABLE" && (
              <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
                C’est le dernier Responsable actif du laboratoire. Après désactivation, personne ne
                pourra accepter ou refuser les demandes ni valider les résultats jusqu’à la
                réactivation ou la création d’un Responsable.
              </p>
            )}
            {impact.data?.dernierCompteActifDuRole && employe.role === "ACCUEIL" && (
              <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
                C’est le dernier compte Accueil actif. La réception des échantillons ne pourra plus
                être assurée jusqu’à la réactivation ou la création d’un compte Accueil.
              </p>
            )}
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isMutatingGlobale} className="rounded-full">
              Annuler
            </AlertDialogCancel>
            <AlertDialogAction
              disabled={
                isMutatingGlobale ||
                notification.isPending ||
                impact.isLoading ||
                impact.isError ||
                !impact.data
              }
              onClick={handleConfirmerDesactivation}
              className="rounded-full bg-red-600 text-white hover:bg-red-700"
            >
              Confirmer la désactivation
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
