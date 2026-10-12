import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  modifierEmploye,
  desactiverEmploye,
  reactiverEmploye,
  renvoyerInvitationEmploye,
  messageErreurApi,
} from "@/api/employes";
import { ApiError } from "@/api/axios";
import type { EmployeDetailDto, ModifierEmployePayload } from "@/api/employes";

interface UseEmployeMutationsProps {
  employeId: number | undefined;
  onSuccessMutation?: (data: EmployeDetailDto) => void;
  on429Renvoi?: () => void;
}

export function useEmployeMutations({
  employeId,
  onSuccessMutation,
  on429Renvoi,
}: UseEmployeMutationsProps) {
  const queryClient = useQueryClient();

  const handleSuccesCommun = (data: EmployeDetailDto, messageSucces: string) => {
    if (employeId !== undefined) {
      queryClient.setQueryData(["employe", employeId], data);
    }
    void queryClient.invalidateQueries({ queryKey: ["employes"] });
    toast.success(messageSucces);
    onSuccessMutation?.(data);
  };

  const handleErreurCommune = (err: unknown) => {
    if (err instanceof ApiError && err.status === 404 && employeId !== undefined) {
      // Sur 404 pendant une mutation, forcer l'invalidation pour basculer sur l'état introuvable
      void queryClient.invalidateQueries({ queryKey: ["employe", employeId] });
      void queryClient.invalidateQueries({ queryKey: ["employes"] });
      return;
    }
    toast.error(messageErreurApi(err));
  };

  const mutationModifier = useMutation({
    mutationFn: (payload: ModifierEmployePayload) => {
      if (employeId === undefined) throw new Error("Identifiant manquant");
      return modifierEmploye(employeId, payload);
    },
    onSuccess: (data) => {
      handleSuccesCommun(data, "Employé modifié avec succès.");
    },
    onError: (err) => {
      handleErreurCommune(err);
    },
  });

  const mutationDesactiver = useMutation({
    mutationFn: () => {
      if (employeId === undefined) throw new Error("Identifiant manquant");
      return desactiverEmploye(employeId);
    },
    onSuccess: (data) => {
      handleSuccesCommun(data, "Compte désactivé.");
    },
    onError: (err) => {
      handleErreurCommune(err);
    },
  });

  const mutationReactiver = useMutation({
    mutationFn: () => {
      if (employeId === undefined) throw new Error("Identifiant manquant");
      return reactiverEmploye(employeId);
    },
    onSuccess: (data) => {
      handleSuccesCommun(data, "Compte réactivé.");
    },
    onError: (err) => {
      handleErreurCommune(err);
    },
  });

  const mutationRenvoyerInvitation = useMutation({
    mutationFn: () => {
      if (employeId === undefined) throw new Error("Identifiant manquant");
      return renvoyerInvitationEmploye(employeId);
    },
    onSuccess: (data) => {
      handleSuccesCommun(data, "Invitation renvoyée avec succès.");
    },
    onError: (err) => {
      if (err instanceof ApiError && err.status === 429) {
        on429Renvoi?.();
        toast.error("Trop de tentatives. Veuillez patienter 60 secondes.");
        return;
      }
      handleErreurCommune(err);
    },
  });

  const isMutating =
    mutationModifier.isPending ||
    mutationDesactiver.isPending ||
    mutationReactiver.isPending ||
    mutationRenvoyerInvitation.isPending;

  return {
    mutationModifier,
    mutationDesactiver,
    mutationReactiver,
    mutationRenvoyerInvitation,
    isMutating,
  };
}
