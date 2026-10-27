import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import { toast } from "sonner";
import { ApiError } from "@/api/axios";
import {
  annulerDemandeClient,
  detailDemandeClient,
  listerDemandesClient,
  messageErreurAnnulation,
} from "@/api/demandesClient";
import { getSession } from "@/api/session";

/** Clé racine des demandes client : `["demande-client"]`. */
export const DEMANDES_CLIENT_QUERY_KEY = ["demande-client"] as const;

/** Clé de la liste « Mes demandes ». */
export const DEMANDES_CLIENT_LISTE_KEY = ["demande-client", "liste"] as const;

/** Clé du détail d'une demande. */
export function demandeClientKey(laboratoireId: number, demandeId: number) {
  return ["demande-client", laboratoireId, demandeId] as const;
}

/**
 * Liste de toutes les demandes du client, tous laboratoires confondus.
 * Aucune requête n'est déclenchée pour un rôle autre que CLIENT.
 */
export function useDemandesClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: DEMANDES_CLIENT_LISTE_KEY,
    queryFn: listerDemandesClient,
    enabled: role === "CLIENT",
    staleTime: 15_000,
  });
}

/** Détail enrichi d'une demande du client. */
export function useDemandeClient(
  laboratoireId: number | undefined,
  demandeId: number | undefined,
) {
  const role = getSession()?.user.role;
  const valides =
    laboratoireId != null &&
    Number.isInteger(laboratoireId) &&
    laboratoireId > 0 &&
    demandeId != null &&
    Number.isInteger(demandeId) &&
    demandeId > 0;
  return useQuery({
    queryKey: demandeClientKey(laboratoireId ?? 0, demandeId ?? 0),
    queryFn: () => detailDemandeClient(laboratoireId!, demandeId!),
    enabled: role === "CLIENT" && valides,
    staleTime: 15_000,
  });
}

interface AnnulerVariables {
  laboratoireId: number;
  demandeId: number;
  motif: string;
}

/**
 * Annulation d'une demande (statut NOUVEAU uniquement).
 *  - succès : le détail renvoyé remplace le cache, la liste est invalidée ;
 *  - 404 : la demande n'existe plus, retour à la liste ;
 *  - ANNULATION_IMPOSSIBLE : le détail est réactualisé (le statut réel s'affiche).
 */
export function useAnnulerDemande() {
  const queryClient = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: (variables: AnnulerVariables) =>
      annulerDemandeClient(variables.laboratoireId, variables.demandeId, variables.motif),
    onSuccess: (detail, variables) => {
      queryClient.setQueryData(
        demandeClientKey(variables.laboratoireId, variables.demandeId),
        detail,
      );
      void queryClient.invalidateQueries({ queryKey: DEMANDES_CLIENT_QUERY_KEY });
      toast.success("Demande annulée.");
    },
    onError: (error, variables) => {
      if (error instanceof ApiError && error.status === 404) {
        void queryClient.invalidateQueries({ queryKey: DEMANDES_CLIENT_QUERY_KEY });
        toast.error(messageErreurAnnulation(error));
        router.history.push("/app/demandes");
        return;
      }
      void queryClient.invalidateQueries({
        queryKey: demandeClientKey(variables.laboratoireId, variables.demandeId),
      });
      toast.error(messageErreurAnnulation(error));
    },
  });
}
