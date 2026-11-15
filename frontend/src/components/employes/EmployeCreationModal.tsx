import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
} from "@/components/ui/dialog";
import {
  creerEmploye,
  messageErreurApi,
  modifierDomainesEmploye,
  type CreerEmployePayload,
  type RoleEmploye,
} from "@/api/employes";
import { ApiError } from "@/api/axios";
import {
  schemaCreerEmploye,
  type CreerEmployeFormValues,
} from "./employeSchemas";
import { EmployeCreationCoordonnees } from "./EmployeCreationCoordonnees";
import { EmployeCreationRole } from "./EmployeCreationRole";
import { EmployeCreationStepper } from "./EmployeCreationStepper";

interface EmployeCreationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccessCreated: (employeId: number | undefined) => void;
}

interface CreationVariables {
  payload: CreerEmployePayload;
  domaineIds: number[];
}

interface CreationResult {
  id: number;
  domainesEnregistres: boolean;
}

export function EmployeCreationModal({
  open,
  onOpenChange,
  onSuccessCreated,
}: EmployeCreationModalProps) {
  const queryClient = useQueryClient();
  const isSubmittingRef = useRef(false);
  const [etape, setEtape] = useState<1 | 2>(1);
  const [domainesSelectionnes, setDomainesSelectionnes] = useState<number[]>([]);
  const form = useForm<CreerEmployeFormValues>({
    resolver: zodResolver(schemaCreerEmploye),
    defaultValues: {
      prenom: "",
      nom: "",
      email: "",
      telephone: "",
      role: "" as unknown as RoleEmploye,
    },
    mode: "onBlur",
  });

  const mutation = useMutation<CreationResult, unknown, CreationVariables>({
    mutationFn: async ({ payload, domaineIds }) => {
      const employe = await creerEmploye(payload);
      let domainesEnregistres = true;
      if (payload.role === "TECHNICIEN" && domaineIds.length > 0) {
        try {
          await modifierDomainesEmploye(employe.id, domaineIds);
        } catch {
          domainesEnregistres = false;
        }
      }
      return { id: employe.id, domainesEnregistres };
    },
    onSuccess: (result) => {
      void queryClient.invalidateQueries({ queryKey: ["employes"] });
      if (result.domainesEnregistres) {
        toast.success("Invitation envoyée avec succès.");
      } else {
        toast.error(
          "Employé créé, mais les domaines n'ont pas été enregistrés. Vous pouvez les ajouter depuis sa fiche.",
        );
      }
      form.reset();
      setDomainesSelectionnes([]);
      setEtape(1);
      onOpenChange(false);
      onSuccessCreated(result.id);
    },
    onError: (err: unknown) => {
      isSubmittingRef.current = false;
      if (err instanceof ApiError) {
        const status = err.status;
        const msg = String(err.message || "");
        if (
          status === 409 ||
          msg.includes("[EMAIL_DEJA_UTILISE]") ||
          msg.includes("email déjà utilisé")
        ) {
          setEtape(1);
          form.setError("email", {
            type: "server",
            message: "Cette adresse email est déjà associée à un compte utilisateur.",
          });
          setTimeout(() => form.setFocus("email"), 50);
          return;
        }
        const errData = err.data as { erreurs?: Record<string, string> } | undefined;
        if (status === 400 && errData?.erreurs) {
          let premierChamp: keyof CreerEmployeFormValues | null = null;
          for (const [champ, message] of Object.entries(errData.erreurs)) {
            if (champ in form.getValues()) {
              const champForm = champ as keyof CreerEmployeFormValues;
              form.setError(champForm, { type: "server", message });
              if (!premierChamp) {
                premierChamp = champForm;
              }
            }
          }
          if (premierChamp) {
            const champFocus = premierChamp;
            setEtape(["prenom", "nom", "email", "telephone"].includes(premierChamp) ? 1 : 2);
            setTimeout(() => form.setFocus(champFocus), 50);
            return;
          }
        }
      }
      toast.error(messageErreurApi(err));
    },
    onSettled: () => {
      isSubmittingRef.current = false;
    },
  });

  const handleSuivant = async () => {
    const valide = await form.trigger(["prenom", "nom", "email", "telephone"]);
    if (valide) {
      setEtape(2);
    } else if (form.formState.errors.prenom) {
      form.setFocus("prenom");
    } else if (form.formState.errors.nom) {
      form.setFocus("nom");
    } else if (form.formState.errors.email) {
      form.setFocus("email");
    } else if (form.formState.errors.telephone) {
      form.setFocus("telephone");
    }
  };

  const handlePrecedent = () => {
    if (!mutation.isPending) {
      setEtape(1);
    }
  };

  const onSubmit = (data: CreerEmployeFormValues) => {
    if (isSubmittingRef.current || mutation.isPending) {
      return;
    }
    isSubmittingRef.current = true;
    const payload: CreerEmployePayload = {
      prenom: data.prenom.trim(),
      nom: data.nom.trim(),
      email: data.email.trim().toLowerCase(),
      role: data.role as RoleEmploye,
      ...(data.telephone && data.telephone.trim().length > 0
        ? { telephone: data.telephone.trim() }
        : {}),
    };
    mutation.mutate({
      payload,
      domaineIds: data.role === "TECHNICIEN" ? domainesSelectionnes : [],
    });
  };

  const handleClose = () => {
    if (mutation.isPending) {
      return;
    }
    form.reset();
    setDomainesSelectionnes([]);
    setEtape(1);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={(value) => (!value ? handleClose() : onOpenChange(true))}>
      <DialogContent className="fixed top-1/2 left-1/2 z-50 w-[94vw] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-3xl border border-brand-900/10 bg-surface-card p-6 shadow-2xl backdrop-blur-md sm:p-7">
        <EmployeCreationStepper etape={etape} />
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate className="mt-4 flex flex-col gap-4">
          {etape === 1 ? (
            <EmployeCreationCoordonnees
              form={form}
              disabled={mutation.isPending}
              onCancel={handleClose}
              onContinue={() => void handleSuivant()}
            />
          ) : (
            <EmployeCreationRole
              form={form}
              domainesSelectionnes={domainesSelectionnes}
              disabled={mutation.isPending || isSubmittingRef.current}
              erreurCreation={mutation.error}
              onDomainesChange={setDomainesSelectionnes}
              onPrevious={handlePrecedent}
            />
          )}
        </form>
      </DialogContent>
    </Dialog>
  );
}
