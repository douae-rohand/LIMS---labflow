import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import { mesDomaines } from "@/api/technicien";

export function useTechnicienDomaines(enabled: boolean) {
  return useQuery({
    queryKey: ["technicien", "domaines"] as const,
    queryFn: mesDomaines,
    enabled: enabled && getSession()?.user.role === "TECHNICIEN",
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });
}
