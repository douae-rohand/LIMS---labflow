import { useQuery } from "@tanstack/react-query";
import { fetchLaboratoiresClient } from "@/api/clientAgrege";

export const LABORATOIRES_CLIENT_QUERY_KEY = ["laboratoires", "client"] as const;

/**
 * Hook pour récupérer la liste complète des laboratoires rattachés au client.
 * Utilise l'endpoint /api/client/laboratoires.
 */
export function useLaboratoiresClient() {
  return useQuery({
    queryKey: LABORATOIRES_CLIENT_QUERY_KEY,
    queryFn: fetchLaboratoiresClient,
    staleTime: 5 * 60_000, // 5 minutes
    retry: 1,
  });
}
