import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import { toast } from "sonner";
import { ApiError } from "@/api/axios";
import { getSession } from "@/api/session";
import {
  detailEnqueteClient,
  listerEnquetesClient,
  messageErreurReponse,
  repondreEnqueteClient,
  type RepondreEnqueteBody,
} from "@/api/enquetesClient";
import { SYNTHESE_CLIENT_QUERY_KEY } from "./useSyntheseClient";

/** Clé racine des enquêtes client : `["enquetes-client"]`. */
export const ENQUETES_CLIENT_QUERY_KEY = ["enquetes-client"] as const;

/** Clé de la liste « Mes enquêtes ». */
export const ENQUETES_CLIENT_LISTE_KEY = ["enquetes-client", "liste"] as const;

/** Clé du détail d'une enquête. */
export function enqueteClientKey(laboratoireId: number, enqueteId: number) {
  return ["enquetes-client", laboratoireId, enqueteId] as const;
}

/** Liste agrégée des enquêtes du client (non répondues d'abord, tri serveur). */
export function useEnquetesClient() {
  const role = getSession()?.user.role;
  return useQuery({
    queryKey: ENQUETES_CLIENT_LISTE_KEY,
    queryFn: listerEnquetesClient,
    enabled: role === "CLIENT",
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}

/** Détail d'une enquête ; aucune requête si les identifiants sont invalides. */
export function useEnqueteClient(
  laboratoireId: number | undefined,
  enqueteId: number | undefined,
) {
  const role = getSession()?.user.role;
  const valides =
    laboratoireId != null &&
    Number.isInteger(laboratoireId) &&
    laboratoireId > 0 &&
    enqueteId != null &&
    Number.isInteger(enqueteId) &&
    enqueteId > 0;
  return useQuery({
    queryKey: enqueteClientKey(laboratoireId ?? 0, enqueteId ?? 0),
    queryFn: () => detailEnqueteClient(laboratoireId!, enqueteId!),
    enabled: role === "CLIENT" && valides,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}

interface RepondreVariables {
  laboratoireId: number;
  enqueteId: number;
  body: RepondreEnqueteBody;
}

/**
 * Réponse à une enquête :
 *  - succès : le détail renvoyé remplace le cache, la liste et la synthèse sont
 *    invalidées ;
 *  - ENQUETE_DEJA_REPONDUE : le détail est invalidé (les réponses s'affichent) ;
 *  - 404 : retour à la liste.
 */
export function useRepondreEnquete() {
  const queryClient = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: (variables: RepondreVariables) =>
      repondreEnqueteClient(variables.laboratoireId, variables.enqueteId, variables.body),
    onSuccess: (detail, variables) => {
      queryClient.setQueryData(
        enqueteClientKey(variables.laboratoireId, variables.enqueteId),
        detail,
      );
      void queryClient.invalidateQueries({ queryKey: ENQUETES_CLIENT_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: SYNTHESE_CLIENT_QUERY_KEY });
      toast.success("Enquête enregistrée, merci pour votre retour.");
    },
    onError: (error, variables) => {
      if (error instanceof ApiError && error.status === 404) {
        void queryClient.invalidateQueries({ queryKey: ENQUETES_CLIENT_QUERY_KEY });
        void queryClient.invalidateQueries({ queryKey: SYNTHESE_CLIENT_QUERY_KEY });
        toast.error(messageErreurReponse(error));
        router.history.push("/app/enquetes");
        return;
      }
      if (error instanceof ApiError && error.code === "ENQUETE_DEJA_REPONDUE") {
        void queryClient.invalidateQueries({
          queryKey: enqueteClientKey(variables.laboratoireId, variables.enqueteId),
        });
      }
      toast.error(messageErreurReponse(error));
    },
  });
}
