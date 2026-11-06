import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/api/axios";
import * as facturesAdmin from "@/api/facturesAdmin";

export function useFacturesAdmin() {
  const queryClient = useQueryClient();

  const emettre = useMutation({
    mutationFn: facturesAdmin.emettreFacture,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["factures-admin"] });
      void queryClient.invalidateQueries({ queryKey: ["factures-admin-stats"] });
      void queryClient.invalidateQueries({
        queryKey: facturesAdmin.DEMANDES_FACTURABLES_QUERY_KEY,
      });
    },
    onError: (error) => {
      if (error instanceof ApiError
        && ["FACTURE_DEJA_EMISE", "DEMANDE_NON_FACTURABLE", "DEMANDE_SANS_MONTANT"].includes(error.code ?? "")) {
        void queryClient.invalidateQueries({
          queryKey: facturesAdmin.DEMANDES_FACTURABLES_QUERY_KEY,
        });
      }
    },
  });

  return { emettre };
}
