import { useQuery } from "@tanstack/react-query";
import { listerDomainesLaboratoire } from "@/api/employes";

export function useDomainesLaboratoire() {
  return useQuery({
    queryKey: ["domaines-laboratoire"],
    queryFn: listerDomainesLaboratoire,
    staleTime: 10 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
}
