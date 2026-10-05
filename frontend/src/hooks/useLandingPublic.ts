import { useQuery } from "@tanstack/react-query";
import { fetchLandingPublic } from "@/api/landing";

export const LANDING_PUBLIC_QUERY_KEY = ["landing", "public"] as const;

export function useLandingPublic() {
  return useQuery({
    queryKey: LANDING_PUBLIC_QUERY_KEY,
    queryFn: fetchLandingPublic,
    staleTime: 60_000,
    retry: 1,
  });
}
