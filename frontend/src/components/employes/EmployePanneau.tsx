import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
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
import { Skeleton } from "@/components/lab";
import { StatutEmployeBadge } from "./StatutEmployeBadge";
import { LIBELLE_ROLE, formatDate, formatDerniereConnexion } from "./dateUtils";
import { EmployeInvitation } from "./EmployeInvitation";
import { EmployeForm } from "./EmployeForm";
import { schemaEmployeForm, type EmployeFormValues } from "./employeSchemas";
import { EmployeActions } from "./EmployeActions";
import { useEmployeMutations } from "./useEmployeMutations";
import { trouverEmploye, messageErreurApi } from "@/api/employes";
import { ApiError } from "@/api/axios";
import { AlertCircle, RefreshCw, X, AlertTriangle } from "lucide-react";
import type { ModifierEmployePayload, EmployeDetailDto } from "@/api/employes";

interface EmployePanneauProps {
  employeId: number | undefined;
  onClose: () => void;
}

export function EmployePanneau({ employeId, onClose }: EmployePanneauProps) {
  const estOuvert = employeId !== undefined;

  // Modale d'abandon des modifications non sauvegardées
  const [modalAbandonOuverte, setModalAbandonOuverte] = useState(false);

  // Compte à rebours 60s pour le renvoi d'invitation (local au panneau, lié à employeId)
  const [cooldownRenvoi, setCooldownRenvoi] = useState<number>(0);
  const cooldownTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Remise à zéro du compte à rebours au changement d'employé
  useEffect(() => {
    if (cooldownTimerRef.current !== null) {
      clearInterval(cooldownTimerRef.current);
      cooldownTimerRef.current = null;
    }
    setCooldownRenvoi(0);
  }, [employeId]);

  // Nettoyage de l'intervalle au démontage
  useEffect(() => {
    return () => {
      if (cooldownTimerRef.current !== null) {
        clearInterval(cooldownTimerRef.current);
        cooldownTimerRef.current = null;
      }
    };
  }, []);

  const lancerCooldown = (secondes: number) => {
    if (cooldownTimerRef.current !== null) {
      clearInterval(cooldownTimerRef.current);
    }
    setCooldownRenvoi(secondes);
    cooldownTimerRef.current = setInterval(() => {
      setCooldownRenvoi((prev) => {
        if (prev <= 1) {
          if (cooldownTimerRef.current !== null) {
            clearInterval(cooldownTimerRef.current);
            cooldownTimerRef.current = null;
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  // Requête du détail de l'employé
  const query = useQuery({
    queryKey: ["employe", employeId],
    queryFn: () => {
      if (employeId === undefined) {
        throw new Error("Identifiant d'employé manquant");
      }
      return trouverEmploye(employeId);
    },
    enabled: estOuvert,
    staleTime: 30_000,
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.status === 404) {
        return false;
      }
      return failureCount < 1;
    },
    refetchOnWindowFocus: false,
  });

  const emp = query.data;
  const is404 = query.error instanceof ApiError && query.error.status === 404;

  // Formulaire react-hook-form unique porté au niveau du panneau
  const form = useForm<EmployeFormValues>({
    resolver: zodResolver(schemaEmployeForm),
    defaultValues: {
      prenom: "",
      nom: "",
      email: "",
      telephone: "",
      role: "TECHNICIEN",
    },
  });

  // Réinitialiser le formulaire au changement d'id de l'employé
  useEffect(() => {
    if (emp) {
      form.reset({
        prenom: emp.prenom ?? "",
        nom: emp.nom ?? "",
        email: emp.email ?? "",
        telephone: emp.telephone ?? "",
        role: emp.role,
      });
    }
  }, [emp?.id, form]);

  // Mutations avec invalidation et toasts
  const mutations = useEmployeMutations({
    employeId,
    onSuccessMutation: (donneesMisesAJour: EmployeDetailDto) => {
      // Réinitialisation explicite du formulaire avec les valeurs renvoyées
      form.reset({
        prenom: donneesMisesAJour.prenom ?? "",
        nom: donneesMisesAJour.nom ?? "",
        email: donneesMisesAJour.email ?? "",
        telephone: donneesMisesAJour.telephone ?? "",
        role: donneesMisesAJour.role,
      });
    },
    on429Renvoi: () => {
      lancerCooldown(60);
    },
  });

  // Soumission de modification
  const handleModifier = (payload: ModifierEmployePayload) => {
    mutations.mutationModifier.mutate(payload);
  };

  // Actions de statut
  const handleDesactiver = () => {
    mutations.mutationDesactiver.mutate();
  };

  const handleReactiver = () => {
    mutations.mutationReactiver.mutate();
  };

  const handleRenvoyerInvitation = () => {
    mutations.mutationRenvoyerInvitation.mutate(undefined, {
      onSuccess: () => {
        lancerCooldown(60);
      },
    });
  };

  // Interception de la fermeture pour vérifier isDirty
  const handleOpenChange = (open: boolean) => {
    if (!open) {
      if (form.formState.isDirty) {
        // Formulaire modifié -> afficher confirmation d'abandon au lieu de fermer
        setModalAbandonOuverte(true);
      } else {
        onClose();
      }
    }
  };

  const handleConfirmerAbandon = () => {
    setModalAbandonOuverte(false);
    if (emp) {
      form.reset({
        prenom: emp.prenom ?? "",
        nom: emp.nom ?? "",
        email: emp.email ?? "",
        telephone: emp.telephone ?? "",
        role: emp.role,
      });
    }
    onClose();
  };

  return (
    <>
      <Sheet open={estOuvert} onOpenChange={handleOpenChange}>
        <SheetContent
          side="right"
          className="w-full sm:max-w-lg overflow-y-auto p-6 focus:outline-none"
          onCloseAutoFocus={(e) => {
            // Restauration propre du focus sans setTimeout
            if (employeId !== undefined) {
              const el = document.getElementById(`btn-employe-${employeId}`);
              if (el) {
                e.preventDefault();
                el.focus();
              }
            }
          }}
        >
          {/* État de chargement initial avec SheetTitle accessible sr-only */}
          {query.isLoading && (
            <div aria-live="polite" className="space-y-6">
              <SheetHeader className="text-left space-y-2">
                <SheetTitle className="sr-only">Chargement des données de l'employé</SheetTitle>
                <SheetDescription className="sr-only">Veuillez patienter...</SheetDescription>
                <Skeleton className="h-7 w-3/4" />
                <Skeleton className="h-4 w-1/3" />
              </SheetHeader>
              <div className="space-y-4 pt-4">
                <Skeleton className="h-6 w-24" />
                <div className="grid grid-cols-2 gap-4">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
                <Skeleton className="h-20 w-full rounded-xl" />
              </div>
            </div>
          )}

          {/* État d'erreur */}
          {query.isError && (
            <div aria-live="assertive" className="flex flex-col items-center py-12 text-center">
              <div className="mb-4 flex size-12 items-center justify-center rounded-full bg-red-100 text-red-600">
                <AlertCircle className="size-6" aria-hidden="true" />
              </div>
              <SheetHeader className="text-center">
                <SheetTitle className="text-lg font-bold text-ink-900">
                  {is404 ? "Employé introuvable" : "Erreur de chargement"}
                </SheetTitle>
                <SheetDescription className="text-sm text-muted-foreground mt-1">
                  {messageErreurApi(query.error)}
                </SheetDescription>
              </SheetHeader>
              <div className="mt-6 flex items-center gap-3">
                {is404 ? (
                  <button
                    type="button"
                    onClick={onClose}
                    className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-5 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                  >
                    <X className="size-4" aria-hidden="true" />
                    Fermer
                  </button>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={() => void query.refetch()}
                      className="inline-flex items-center gap-2 rounded-full bg-brand-900 px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-brand-900/90"
                    >
                      <RefreshCw className="size-4" aria-hidden="true" />
                      Réessayer
                    </button>
                    <button
                      type="button"
                      onClick={onClose}
                      className="inline-flex items-center gap-2 rounded-full border border-brand-900/20 px-5 py-2 text-sm font-semibold text-brand-900 hover:bg-surface-card"
                    >
                      Fermer
                    </button>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Détail, Modification et Actions */}
          {!query.isLoading && !query.isError && emp && (
            <div className="space-y-6">
              <SheetHeader className="text-left space-y-1 pr-6">
                <SheetTitle className="text-xl font-bold text-ink-900">
                  {emp.nomComplet}
                </SheetTitle>
                <SheetDescription className="font-mono text-xs text-muted-foreground">
                  Matricule : {emp.matricule}
                </SheetDescription>
              </SheetHeader>

              {/* Statut & Fonction */}
              <div className="flex flex-wrap items-center gap-3 border-b border-brand-900/10 pb-4">
                <StatutEmployeBadge statut={emp.statut} />
                <span className="text-xs font-medium text-muted-foreground bg-surface-card px-2.5 py-1 rounded-full border border-brand-900/10">
                  {LIBELLE_ROLE[emp.role] ?? emp.role}
                </span>
              </div>

              {/* Métadonnées de dates */}
              <div className="grid grid-cols-2 gap-3 rounded-2xl border border-brand-900/10 bg-surface-card/40 p-4 text-xs text-muted-foreground">
                <div>
                  <span className="block text-[11px]">Dernière connexion</span>
                  <span className="font-medium text-ink-900">
                    {formatDerniereConnexion(emp.derniereConnexion)}
                  </span>
                </div>
                <div>
                  <span className="block text-[11px]">Créé le</span>
                  <span className="font-medium text-ink-900">
                    {formatDate(emp.dateCreation)}
                  </span>
                </div>
              </div>

              {/* État de l'invitation (si EN_ATTENTE) */}
              <EmployeInvitation employe={emp} />

              {/* Formulaire de modification */}
              <div className="border-t border-brand-900/10 pt-4">
                <EmployeForm
                  key={emp.id}
                  employe={emp}
                  form={form}
                  onSubmitModif={handleModifier}
                  isSubmitting={mutations.mutationModifier.isPending}
                  erreurModification={mutations.mutationModifier.error}
                  isMutatingGlobale={mutations.isMutating}
                />
              </div>

              {/* Actions de statut */}
              <EmployeActions
                employe={emp}
                isDirty={form.formState.isDirty}
                isMutatingGlobale={mutations.isMutating}
                cooldownRenvoi={cooldownRenvoi}
                onDesactiver={handleDesactiver}
                onReactiver={handleReactiver}
                onRenvoyerInvitation={handleRenvoyerInvitation}
              />
            </div>
          )}
        </SheetContent>
      </Sheet>

      {/* Modale d'abandon des modifications non enregistrées */}
      <AlertDialog
        open={modalAbandonOuverte}
        onOpenChange={setModalAbandonOuverte}
      >
        <AlertDialogContent className="w-[90vw] max-w-md rounded-2xl">
          <AlertDialogHeader>
            <div className="mb-2 flex size-10 items-center justify-center rounded-full bg-amber-100 text-amber-600">
              <AlertTriangle className="size-5" aria-hidden="true" />
            </div>
            <AlertDialogTitle className="text-lg font-bold text-ink-900">
              Abandonner les modifications ?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-sm text-muted-foreground">
              Des informations ont été modifiées. Si vous fermez le panneau maintenant, vos modifications non enregistrées seront perdues.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="rounded-full">
              Continuer à modifier
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmerAbandon}
              className="rounded-full bg-brand-900 text-primary-foreground hover:bg-brand-900/90"
            >
              Abandonner
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
