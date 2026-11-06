import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Sheet, SheetContent } from "@/components/ui/sheet";
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
import { schemaEmployeForm, type EmployeFormValues } from "./employeSchemas";
import { useEmployeMutations } from "./useEmployeMutations";
import { trouverEmploye } from "@/api/employes";
import { ApiError } from "@/api/axios";
import { getSession } from "@/api/session";
import { AlertTriangle } from "lucide-react";
import type { ModifierEmployePayload, EmployeDetailDto } from "@/api/employes";
import { EmployePanneauContenu } from "./EmployePanneauContenu";
import { useEmployeCooldown } from "./useEmployeCooldown";

interface EmployePanneauProps {
  employeId: number | undefined;
  onClose: () => void;
}

export function EmployePanneau({ employeId, onClose }: EmployePanneauProps) {
  const estOuvert = employeId !== undefined;
  const administrateurConnecte = getSession()?.user.role === "ADMINISTRATEUR";

  // Modale d'abandon des modifications non sauvegardées
  const [modalAbandonOuverte, setModalAbandonOuverte] = useState(false);
  const [permissionsModifiees, setPermissionsModifiees] = useState(false);

  const { cooldownRenvoi, lancerCooldown } = useEmployeCooldown(employeId);

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
      if (form.formState.isDirty || permissionsModifiees) {
        // Formulaire modifié -> afficher confirmation d'abandon au lieu de fermer
        setModalAbandonOuverte(true);
      } else {
        onClose();
      }
    }
  };

  const handleConfirmerAbandon = () => {
    setModalAbandonOuverte(false);
    setPermissionsModifiees(false);
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
          <EmployePanneauContenu
            chargement={query.isLoading}
            erreur={query.error}
            estEnErreur={query.isError}
            est404={is404}
            employe={emp}
            form={form}
            mutations={mutations}
            cooldownRenvoi={cooldownRenvoi}
            administrateurConnecte={administrateurConnecte}
            permissionsModifiees={permissionsModifiees}
            onFermer={onClose}
            onReessayer={() => void query.refetch()}
            onModifier={handleModifier}
            onDesactiver={handleDesactiver}
            onReactiver={handleReactiver}
            onRenvoyerInvitation={handleRenvoyerInvitation}
            onPermissionsModifiees={setPermissionsModifiees}
          />
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
              Des informations ou permissions ont été modifiées. Si vous fermez le panneau maintenant, vos modifications non enregistrées seront perdues.
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
