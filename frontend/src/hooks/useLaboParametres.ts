import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as laboratoireParametres from "@/api/laboratoireParametres";

export function useLaboParametres() {
  const queryClient = useQueryClient();
  const enabled = getSession()?.user.role === "ADMINISTRATEUR";

  const parametres = useQuery({
    queryKey: ["labo-parametres"],
    queryFn: laboratoireParametres.getParametres,
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const modifier = useMutation({
    mutationFn: laboratoireParametres.modifierParametres,
    onSuccess: (data) => {
      queryClient.setQueryData(["labo-parametres"], data);
    },
  });

  return { parametres, modifier };
}
