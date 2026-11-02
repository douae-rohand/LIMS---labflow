import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import {
  detailRapportClient,
  listerRapportsClient,
} from "@/api/rapportsClient";

/** Clé racine des rapports client : `["rapports-client"]`. */
export const RAPPORTS_CLIENT_QUERY_KEY = ["rapports-client"] as const;

/** Clé de la liste « Mes rapports ». */
export const RAPPORTS_CLIENT_LISTE_KEY = ["rapports-client", "liste"] as const;

/** Clé du détail d'un rapport. */
export function rapportClientKey(laboratoireId: number, rapportId: number) {
  return ["rapports-client", laboratoireId, rapportId] as const;
}

/** Liste agrégée des rapports visibles du client, tous laboratoires. */
export function useRapportsClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: RAPPORTS_CLIENT_LISTE_KEY,
    queryFn: listerRapportsClient,
    enabled: role === "CLIENT",
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}

/** Détail d'un rapport ; aucune requête si les identifiants sont invalides. */
export function useRapportClient(
  laboratoireId: number | undefined,
  rapportId: number | undefined,
) {
  const role = getSession()?.user.role;
  const valides =
    laboratoireId != null &&
    Number.isInteger(laboratoireId) &&
    laboratoireId > 0 &&
    rapportId != null &&
    Number.isInteger(rapportId) &&
    rapportId > 0;
  return useQuery({
    queryKey: rapportClientKey(laboratoireId ?? 0, rapportId ?? 0),
    queryFn: () => detailRapportClient(laboratoireId!, rapportId!),
    enabled: role === "CLIENT" && valides,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}
