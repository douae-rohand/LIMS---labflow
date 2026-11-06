import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  lirePermissionsEmploye,
  remplacerPermissionsEmploye,
  retablirPermissionsEmploye,
  type EcartPermissionEmploye,
  type PermissionsEmploye,
} from "@/api/employePermissions";
import { messageErreurApi } from "@/api/employes";

const CLE_PERMISSIONS = "permissions-employe";

export function useEmployePermissions(employeId: number | undefined) {
  const queryClient = useQueryClient();
  const queryKey = [CLE_PERMISSIONS, employeId] as const;

  const permissions = useQuery({
    queryKey,
    queryFn: () => {
      if (employeId === undefined) throw new Error("Identifiant d'employé manquant.");
      return lirePermissionsEmploye(employeId);
    },
    enabled: employeId !== undefined,
  });

  const mettreAJourCache = async (data: PermissionsEmploye) => {
    if (employeId !== undefined) {
      queryClient.setQueryData(queryKey, data);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["employe", employeId] }),
        queryClient.invalidateQueries({ queryKey: ["employes"] }),
        queryClient.invalidateQueries({ queryKey }),
      ]);
    }
  };

  const enregistrer = useMutation({
    mutationFn: (ecarts: EcartPermissionEmploye[]) => {
      if (employeId === undefined) throw new Error("Identifiant d'employé manquant.");
      return remplacerPermissionsEmploye(employeId, ecarts);
    },
    onSuccess: async (data) => {
      await mettreAJourCache(data);
      toast.success("Permissions enregistrées.");
    },
    onError: (error) => toast.error(messageErreurApi(error)),
  });

  const retablir = useMutation({
    mutationFn: () => {
      if (employeId === undefined) throw new Error("Identifiant d'employé manquant.");
      return retablirPermissionsEmploye(employeId);
    },
    onSuccess: async (data) => {
      await mettreAJourCache(data);
      toast.success("Permissions réinitialisées aux défauts du rôle.");
    },
    onError: (error) => toast.error(messageErreurApi(error)),
  });

  return { permissions, enregistrer, retablir };
}
