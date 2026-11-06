import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as satisfactionAdmin from "@/api/satisfactionAdmin";

export function useSatisfactionAdmin(debut?: string, fin?: string, page = 0, size = 10) {
  const enabled = getSession()?.user.role === "ADMINISTRATEUR";

  const stats = useQuery({
    queryKey: ["satisfaction-admin-stats", debut, fin],
    queryFn: () => satisfactionAdmin.getStats(debut, fin),
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const enquetes = useQuery({
    queryKey: ["satisfaction-admin-enquetes", page, size],
    queryFn: () => satisfactionAdmin.listerEnquetes({ page, size }),
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    placeholderData: (prev) => prev,
  });

  return { stats, enquetes };
}
