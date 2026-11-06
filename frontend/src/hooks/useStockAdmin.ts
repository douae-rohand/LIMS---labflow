import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as stockAdmin from "@/api/stockAdmin";

export function useStockAdmin() {
  const queryClient = useQueryClient();
  const enabled = getSession()?.user.role === "ADMINISTRATEUR";

  const produits = useQuery({
    queryKey: ["stock-admin-produits"],
    queryFn: () => stockAdmin.listerProduits(0, 50),
    enabled: true,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    placeholderData: (prev) => prev,
  });

  const alertes = useQuery({
    queryKey: ["stock-admin-alertes"],
    queryFn: stockAdmin.getAlertes,
    enabled: true,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });

  const creerProduit = useMutation({
    mutationFn: stockAdmin.creerProduit,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
    },
  });

  const modifierProduit = useMutation({
    mutationFn: ({ id, request }: { id: number; request: stockAdmin.ModifierProduitRequest }) =>
      stockAdmin.modifierProduit(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
    },
  });

  const ajusterStock = useMutation({
    mutationFn: ({ lotId, request }: { lotId: number; request: stockAdmin.AjusterStockRequest }) =>
      stockAdmin.ajusterStock(lotId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["stock-admin-produits"] });
      queryClient.invalidateQueries({ queryKey: ["stock-admin-alertes"] });
    },
  });

  return { produits, alertes, creerProduit, modifierProduit, ajusterStock };
}
