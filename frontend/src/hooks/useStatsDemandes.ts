import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as statsDemandes from "@/api/statsDemandes";

export function useStatsDemandes(debut?: string, fin?: string) {
  const enabled = getSession()?.user.role === "ADMINISTRATEUR";

  return useQuery({
    queryKey: ["stats-demandes", debut, fin],
    queryFn: () => statsDemandes.getStats(debut, fin),
    enabled: true,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });
}
