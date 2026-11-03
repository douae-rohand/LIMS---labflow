import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import {
  detailFactureClient,
  listerFacturesClient,
} from "@/api/facturesClient";

/** Clé racine des factures client : `["factures-client"]`. */
export const FACTURES_CLIENT_QUERY_KEY = ["factures-client"] as const;

/** Clé de la liste « Mes factures ». */
export const FACTURES_CLIENT_LISTE_KEY = ["factures-client", "liste"] as const;

/** Clé du détail d'une facture. */
export function factureClientKey(laboratoireId: number, factureId: number) {
  return ["factures-client", laboratoireId, factureId] as const;
}

/** Liste agrégée des factures du client, tous laboratoires. */
export function useFacturesClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: FACTURES_CLIENT_LISTE_KEY,
    queryFn: listerFacturesClient,
    enabled: role === "CLIENT",
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}

/** Détail d'une facture ; aucune requête si les identifiants sont invalides. */
export function useFactureClient(
  laboratoireId: number | undefined,
  factureId: number | undefined,
) {
  const role = getSession()?.user.role;
  const valides =
    laboratoireId != null &&
    Number.isInteger(laboratoireId) &&
    laboratoireId > 0 &&
    factureId != null &&
    Number.isInteger(factureId) &&
    factureId > 0;
  return useQuery({
    queryKey: factureClientKey(laboratoireId ?? 0, factureId ?? 0),
    queryFn: () => detailFactureClient(laboratoireId!, factureId!),
    enabled: role === "CLIENT" && valides,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}
