import { useEffect, useState } from "react";
import type { UseFormReturn } from "react-hook-form";
import { ArrowLeft, SlidersHorizontal } from "lucide-react";
import { SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
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
import {
  type EmployeDetailDto,
  type ModifierEmployePayload,
} from "@/api/employes";
import { StatutEmployeBadge } from "./StatutEmployeBadge";
import { LIBELLE_ROLE, formatDate, formatDerniereConnexion } from "./dateUtils";
import { EmployeInvitation } from "./EmployeInvitation";
import { EmployeForm } from "./EmployeForm";
import type { EmployeFormValues } from "./employeSchemas";
import { EmployeActions } from "./EmployeActions";
import type { useEmployeMutations } from "./useEmployeMutations";
import { EmployePermissionsSection } from "./permissions/EmployePermissionsSection";
import { EmployePanneauEtat } from "./EmployePanneauEtat";
import { EmployeDomaines } from "./EmployeDomaines";

type MutationsEmploye = ReturnType<typeof useEmployeMutations>;

interface EmployePanneauContenuProps {
  chargement: boolean;
  erreur: unknown;
  estEnErreur: boolean;
  est404: boolean;
  employe: EmployeDetailDto | undefined;
  form: UseFormReturn<EmployeFormValues>;
  mutations: MutationsEmploye;
  cooldownRenvoi: number;
  administrateurConnecte: boolean;
  permissionsModifiees: boolean;
  onFermer: () => void;
  onReessayer: () => void;
  onModifier: (payload: ModifierEmployePayload) => void;
  onDesactiver: () => void;
  onReactiver: () => void;
  onRenvoyerInvitation: () => void;
  onPermissionsModifiees: (dirty: boolean) => void;
}

export function EmployePanneauContenu({
  chargement,
  erreur,
  estEnErreur,
  est404,
  employe,
  form,
  mutations,
  cooldownRenvoi,
  administrateurConnecte,
  permissionsModifiees,
  onFermer,
  onReessayer,
  onModifier,
  onDesactiver,
  onReactiver,
  onRenvoyerInvitation,
  onPermissionsModifiees,
}: EmployePanneauContenuProps) {
  const [vuePermissions, setVuePermissions] = useState(false);
  const [confirmationAbandonPermissions, setConfirmationAbandonPermissions] = useState(false);
  const peutModifierPermissions =
    administrateurConnecte &&
    employe !== undefined &&
    ["RESPONSABLE", "TECHNICIEN", "ACCUEIL"].includes(employe.role);

  useEffect(() => {
    setVuePermissions(false);
  }, [employe?.id]);

  const retournerFicheEmploye = () => {
    if (permissionsModifiees) {
      setConfirmationAbandonPermissions(true);
      return;
    }
    setVuePermissions(false);
  };

  return (
    <>
      <EmployePanneauEtat
        chargement={chargement}
        erreur={erreur}
        estEnErreur={estEnErreur}
        est404={est404}
        onFermer={onFermer}
        onReessayer={onReessayer}
      />

      {!chargement && !estEnErreur && employe && (
        <div className="space-y-6">
          {vuePermissions ? (
            <>
              <div className="space-y-3 border-b border-brand-900/10 pb-4">
                <button
                  type="button"
                  onClick={retournerFicheEmploye}
                  className="inline-flex items-center gap-2 text-sm font-medium text-brand-900 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900 focus-visible:ring-offset-2"
                >
                  <ArrowLeft className="size-4" aria-hidden="true" />
                  Retour à la fiche
                </button>
                <SheetHeader className="text-left space-y-1">
                  <SheetTitle className="text-xl font-bold text-ink-900">Permissions</SheetTitle>
                  <SheetDescription>
                    Ajustez les permissions délégables de {employe.nomComplet}.
                  </SheetDescription>
                </SheetHeader>
              </div>
              {peutModifierPermissions && (
                <EmployePermissionsSection
                  key={employe.id}
                  employeId={employe.id}
                  onDirtyChange={onPermissionsModifiees}
                />
              )}
            </>
          ) : (
            <>
              <SheetHeader className="text-left space-y-1 pr-6">
                <SheetTitle className="text-xl font-bold text-ink-900">
                  {employe.nomComplet}
                </SheetTitle>
                <SheetDescription className="font-mono text-xs text-muted-foreground">
                  Matricule : {employe.matricule}
                </SheetDescription>
              </SheetHeader>

              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-brand-900/10 pb-4">
                <div className="flex flex-wrap items-center gap-3">
                  <StatutEmployeBadge statut={employe.statut} />
                  <span className="text-xs font-medium text-muted-foreground bg-surface-card px-2.5 py-1 rounded-full border border-brand-900/10">
                    {LIBELLE_ROLE[employe.role] ?? employe.role}
                  </span>
                </div>
                {peutModifierPermissions && (
                  <button
                    type="button"
                    onClick={() => setVuePermissions(true)}
                    className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-3 py-1.5 text-xs font-semibold text-brand-900 transition-colors hover:bg-brand-900/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-900 focus-visible:ring-offset-2"
                  >
                    <SlidersHorizontal className="size-3.5" aria-hidden="true" />
                    Modifier les permissions
                  </button>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3 rounded-2xl border border-brand-900/10 bg-surface-card/40 p-4 text-xs text-muted-foreground">
                <div>
                  <span className="block text-[11px]">Dernière connexion</span>
                  <span className="font-medium text-ink-900">
                    {formatDerniereConnexion(employe.derniereConnexion)}
                  </span>
                </div>
                <div>
                  <span className="block text-[11px]">Créé le</span>
                  <span className="font-medium text-ink-900">
                    {formatDate(employe.dateCreation)}
                  </span>
                </div>
              </div>

              <EmployeInvitation employe={employe} />

              <div className="border-t border-brand-900/10 pt-4">
                <EmployeForm
                  key={employe.id}
                  employe={employe}
                  form={form}
                  onSubmitModif={onModifier}
                  isSubmitting={mutations.mutationModifier.isPending}
                  erreurModification={mutations.mutationModifier.error}
                  isMutatingGlobale={mutations.isMutating}
                />
              </div>

              {employe.role === "TECHNICIEN" && (
                <EmployeDomaines
                  key={employe.id}
                  employe={employe}
                  isSaving={mutations.mutationDomaines.isPending}
                  onSave={(domaines) => mutations.mutationDomaines.mutate(domaines)}
                />
              )}

              <EmployeActions
                employe={employe}
                isDirty={form.formState.isDirty}
                isMutatingGlobale={mutations.isMutating}
                cooldownRenvoi={cooldownRenvoi}
                onDesactiver={onDesactiver}
                onReactiver={onReactiver}
                onRenvoyerInvitation={onRenvoyerInvitation}
              />
            </>
          )}
          <AlertDialog
            open={confirmationAbandonPermissions}
            onOpenChange={setConfirmationAbandonPermissions}
          >
            <AlertDialogContent className="rounded-2xl">
              <AlertDialogHeader>
                <AlertDialogTitle>Abandonner les modifications ?</AlertDialogTitle>
                <AlertDialogDescription>
                  Les permissions modifiées ne seront pas enregistrées.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Continuer à modifier</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => {
                    setVuePermissions(false);
                    onPermissionsModifiees(false);
                  }}
                  className="bg-brand-900 text-primary-foreground hover:bg-brand-900/90"
                >
                  Abandonner
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      )}
    </>
  );
}
