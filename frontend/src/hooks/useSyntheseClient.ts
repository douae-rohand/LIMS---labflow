import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import { chargerSyntheseClient } from "@/api/syntheseClient";

/** Clé racine de la synthèse client : `["synthese-client"]`. */
export const SYNTHESE_CLIENT_QUERY_KEY = ["synthese-client"] as const;

/**
 * Synthèse du tableau de bord (rapports, factures impayées, enquêtes en
 * attente, laboratoires indisponibles). Une erreur ne doit jamais casser la
 * navigation : les consommateurs lisent `data?.champ` sans toast.
 */
export function useSyntheseClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: SYNTHESE_CLIENT_QUERY_KEY,
    queryFn: chargerSyntheseClient,
    enabled: role === "CLIENT",
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}
