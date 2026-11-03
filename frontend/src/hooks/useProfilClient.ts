import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { getSession } from "@/api/session";
import {
  chargerProfilClient,
  messageErreurProfil,
  modifierProfilClient,
  type ModifierProfilClientBody,
} from "@/api/profilClient";

/** Clé racine du profil client : `["profil-client"]`. */
export const PROFIL_CLIENT_QUERY_KEY = ["profil-client"] as const;

/** Profil du client connecté (partie modifiable : compte client). */
export function useProfilClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: PROFIL_CLIENT_QUERY_KEY,
    queryFn: chargerProfilClient,
    enabled: role === "CLIENT",
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}

/**
 * Modification du profil : seuls les champs modifiés sont transmis.
 * Le profil renvoyé par le serveur remplace le cache.
 */
export function useModifierProfil() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: ModifierProfilClientBody) => modifierProfilClient(body),
    onSuccess: (profil) => {
      queryClient.setQueryData(PROFIL_CLIENT_QUERY_KEY, profil);
      toast.success("Profil mis à jour.");
    },
    onError: (error) => {
      toast.error(messageErreurProfil(error));
    },
  });
}
