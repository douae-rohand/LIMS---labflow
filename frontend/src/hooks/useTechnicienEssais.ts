/**
 * useTechnicienEssais.ts — Hooks TanStack Query pour l'espace technicien.
 *
 * Clés :
 *  - ["technicien", "essais", filtres]   liste paginée
 *  - ["technicien", "essai", id]        détail
 *  - ["technicien", "planning", plage]  planning par jour
 *  - ["technicien", "synthese"]         tableau de bord
 *
 * Options communes : staleTime 30 s, refetchOnWindowFocus false,
 * enabled = role TECHNICIEN, placeholderData (prev) pour les listes.
 */

import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
  type UseMutationOptions,
} from "@tanstack/react-query";
import { toast } from "sonner";
import { getSession } from "@/api/session";
import {
  declareNonRealisable,
  demarrerEssai,
  declarerTemps,
  detailEssai,
  importApercu,
  importConfirmer,
  listerMesEssais,
  messageErreurTechnicien,
  planning as planningApi,
  saisirResultat,
  synthese as syntheseApi,
  validerTechniquement,
} from "@/api/technicien";
import type {
  ImportApercuResponse,
  ImportConfirmerResponse,
  LigneEssaiTechnicienDto,
  ListerEssaisParams,
  NonRealisablePayload,
  ResultatSaisiePayload,
  TempsPayload,
  ValidationTechniquePayload,
} from "@/api/technicien/types";

const estTechnicien = (): boolean => getSession()?.user.role === "TECHNICIEN";

const OPTIONS_LECTURE = {
  staleTime: 30_000,
  refetchOnWindowFocus: false,
} as const;

// ---------------------------------------------------------------------------
// Lecture — liste des essais
// ---------------------------------------------------------------------------

export function useTechnicienEssais(params: ListerEssaisParams) {
  return useQuery({
    queryKey: ["technicien", "essais", params] as const,
    queryFn: () => listerMesEssais(params),
    enabled: estTechnicien(),
    placeholderData: keepPreviousData,
    ...OPTIONS_LECTURE,
  });
}

// ---------------------------------------------------------------------------
// Lecture — détail d'un essai
// ---------------------------------------------------------------------------

export function useTechnicienEssai(id: number | undefined) {
  return useQuery({
    queryKey: ["technicien", "essai", id] as const,
    queryFn: () => detailEssai(id!),
    enabled: estTechnicien() && id !== undefined && id > 0,
    ...OPTIONS_LECTURE,
  });
}

// ---------------------------------------------------------------------------
// Lecture — planning
// ---------------------------------------------------------------------------

export function useTechnicienPlanning(debut: string | undefined, fin: string | undefined) {
  return useQuery({
    queryKey: ["technicien", "planning", { debut, fin }] as const,
    queryFn: () => planningApi(debut!, fin!),
    enabled: estTechnicien() && debut !== undefined && fin !== undefined,
    ...OPTIONS_LECTURE,
  });
}

// ---------------------------------------------------------------------------
// Lecture — synthèse / tableau de bord
// ---------------------------------------------------------------------------

export function useTechnicienSynthese() {
  return useQuery({
    queryKey: ["technicien", "synthese"] as const,
    queryFn: syntheseApi,
    enabled: estTechnicien(),
    ...OPTIONS_LECTURE,
  });
}

// ---------------------------------------------------------------------------
// Helpers d'invalidation
// ---------------------------------------------------------------------------

/** Invalide tout le cache technicien après une mutation. */
function invaliderApresMutation(
  queryClient: ReturnType<typeof useQueryClient>,
  id?: number,
): void {
  void queryClient.invalidateQueries({ queryKey: ["technicien", "essais"] });
  void queryClient.invalidateQueries({ queryKey: ["technicien", "synthese"] });
  void queryClient.invalidateQueries({ queryKey: ["technicien", "planning"] });
  if (id !== undefined) {
    void queryClient.invalidateQueries({ queryKey: ["technicien", "essai", id] });
  }
}

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

type MutationOpts<TData, TVars> = Omit<
  UseMutationOptions<TData, unknown, TVars, unknown>,
  "mutationFn"
>;

/** Démarrer un essai. */
export function useDemarrerEssai(opts?: MutationOpts<LigneEssaiTechnicienDto, number>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => demarrerEssai(id),
    onSuccess: (data, id) => {
      queryClient.setQueryData(["technicien", "essai", id], data);
      invaliderApresMutation(queryClient, id);
      toast.success("Essai démarré.");
    },
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}

/** Saisir ou modifier le résultat. */
export function useSaisirResultat(
  opts?: MutationOpts<LigneEssaiTechnicienDto, { id: number; payload: ResultatSaisiePayload }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => saisirResultat(id, payload),
    onSuccess: (data, { id }) => {
      queryClient.setQueryData(["technicien", "essai", id], data);
      invaliderApresMutation(queryClient, id);
      toast.success("Résultat enregistré.");
    },
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}

/** Déclarer non réalisable. */
export function useDeclareNonRealisable(
  opts?: MutationOpts<LigneEssaiTechnicienDto, { id: number; payload: NonRealisablePayload }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => declareNonRealisable(id, payload),
    onSuccess: (data, { id }) => {
      queryClient.setQueryData(["technicien", "essai", id], data);
      invaliderApresMutation(queryClient, id);
      toast.success("Essai déclaré non réalisable.");
    },
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}

/** Déclarer le temps passé. */
export function useDeclarerTemps(
  opts?: MutationOpts<LigneEssaiTechnicienDto, { id: number; payload: TempsPayload }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => declarerTemps(id, payload),
    onSuccess: (data, { id }) => {
      queryClient.setQueryData(["technicien", "essai", id], data);
      invaliderApresMutation(queryClient, id);
      toast.success("Temps enregistré.");
    },
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}

/** Validation technique (niveau 1). */
export function useValiderTechniquement(
  opts?: MutationOpts<LigneEssaiTechnicienDto, { id: number; payload: ValidationTechniquePayload }>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => validerTechniquement(id, payload),
    onSuccess: (data, { id }) => {
      queryClient.setQueryData(["technicien", "essai", id], data);
      invaliderApresMutation(queryClient, id);
      toast.success("Validation technique enregistrée.");
    },
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}

// ---------------------------------------------------------------------------
// Import CSV
// ---------------------------------------------------------------------------

/** Aperçu du CSV (sans écriture). */
export function useImportApercu(
  opts?: MutationOpts<ImportApercuResponse, File>,
) {
  return useMutation({
    mutationFn: (fichier: File) => importApercu(fichier),
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}

/** Confirmation de l'import (écriture tout ou rien). Invalide tout le cache. */
export function useImportConfirmer(
  opts?: MutationOpts<ImportConfirmerResponse, File>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (fichier: File) => importConfirmer(fichier),
    onSuccess: (data) => {
      // Tout invalider : la liste, le détail, la synthèse, le planning
      void queryClient.invalidateQueries({ queryKey: ["technicien"] });
      toast.success(`${data.lignesImportees} résultat(s) importé(s).`);
    },
    onError: (error) => toast.error(messageErreurTechnicien(error)),
    ...opts,
  });
}
