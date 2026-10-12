import { useState } from "react";
import { UserCheck, UserX, Send, AlertTriangle } from "lucide-react";
import { Button } from "@/components/lab";
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

  const estActif = employe.statut === "ACTIF";
  const estDesactive = employe.statut === "DESACTIVE";
  const estEnAttente = employe.statut === "EN_ATTENTE";

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
        onOpenChange={setModalDesactivationOuverte}
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
              L'employé {employe.nomComplet} perdra immédiatement l'accès, ses sessions seront coupées et ses tâches ne sont pas réaffectées automatiquement.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              disabled={isMutatingGlobale}
              className="rounded-full"
            >
              Annuler
            </AlertDialogCancel>
            <AlertDialogAction
              disabled={isMutatingGlobale}
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
