import { useQuery } from "@tanstack/react-query";
import {
  fetchAnalysesLaboratoire,
  fetchLaboratoirePublic,
  fetchLaboratoiresPublics,
} from "@/api/laboratoires";

export const LABORATOIRES_PUBLIC_QUERY_KEY = ["laboratoires", "public"] as const;

export function useLaboratoiresPublic() {
  return useQuery({
    queryKey: LABORATOIRES_PUBLIC_QUERY_KEY,
    queryFn: fetchLaboratoiresPublics,
    staleTime: 60_000,
    retry: 1,
  });
}

export function useLaboratoirePublic(id: number | undefined) {
  return useQuery({
    queryKey: [...LABORATOIRES_PUBLIC_QUERY_KEY, id] as const,
    queryFn: () => fetchLaboratoirePublic(id!),
    enabled: id != null && Number.isFinite(id),
    staleTime: 60_000,
    retry: 1,
  });
}

export function useAnalysesLaboratoire(id: number | undefined) {
  return useQuery({
    queryKey: [...LABORATOIRES_PUBLIC_QUERY_KEY, id, "analyses"] as const,
    queryFn: () => fetchAnalysesLaboratoire(id!),
    enabled: id != null && Number.isFinite(id),
    staleTime: 60_000,
    retry: 1,
  });
}
