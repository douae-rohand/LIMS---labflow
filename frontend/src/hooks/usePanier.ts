import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import {
  ajouterEssaiPanier,
  listerPaniers,
  obtenirPanier,
  retirerEssaiPanier,
  type PanierDetail,
  type PanierResume,
} from "@/api/paniers";

/** Clé racine des paniers : `["paniers"]`. */
export const PANIERS_QUERY_KEY = ["paniers"] as const;

function panierKey(laboratoireId: number) {
  return [...PANIERS_QUERY_KEY, laboratoireId] as const;
}

/**
 * Liste des paniers du client. Aucune requête n'est déclenchée pour un autre
 * rôle que CLIENT (l'endpoint exige le rôle CLIENT).
 */
export function usePaniers() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: PANIERS_QUERY_KEY,
    queryFn: listerPaniers,
    enabled: role === "CLIENT",
    staleTime: 30_000,
  });
}

/** Détail du panier d'un laboratoire. */
export function usePanier(laboratoireId: number | undefined) {
  return useQuery({
    queryKey: panierKey(laboratoireId ?? 0),
    queryFn: () => obtenirPanier(laboratoireId!),
    enabled: laboratoireId != null && Number.isFinite(laboratoireId),
    staleTime: 30_000,
  });
}

/** Ajoute un essai au panier d'un laboratoire. */
export function useAjouterAuPanier(laboratoireId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (essaiId: number) => ajouterEssaiPanier(laboratoireId, essaiId),
    onSuccess: (detail: PanierDetail) => {
      queryClient.setQueryData(panierKey(laboratoireId), detail);
      void queryClient.invalidateQueries({ queryKey: PANIERS_QUERY_KEY });
    },
  });
}

/** Retire un essai du panier d'un laboratoire. */
export function useRetirerDuPanier(laboratoireId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (essaiId: number) => retirerEssaiPanier(laboratoireId, essaiId),
    onSuccess: (detail: PanierDetail) => {
      queryClient.setQueryData(panierKey(laboratoireId), detail);
      void queryClient.invalidateQueries({ queryKey: PANIERS_QUERY_KEY });
    },
  });
}

/** Somme des essais de tous les laboratoires (badge « Mon panier »). */
export function nombreTotalEssais(paniers: PanierResume[] | undefined): number {
  return (paniers ?? []).reduce((total, panier) => total + panier.nombreEssais, 0);
}
