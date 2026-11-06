import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as catalogueAdmin from "@/api/catalogueAdmin";

export function useCatalogueAdmin() {
  const queryClient = useQueryClient();
  const enabled = getSession()?.user.role === "ADMINISTRATEUR";

  const liste = useQuery({
    queryKey: ["catalogue-admin"],
    queryFn: () => catalogueAdmin.listerEssais({ page: 0, size: 10 }),
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    placeholderData: (prev) => prev,
  });

  const domaines = useQuery({
    queryKey: ["catalogue-admin-domaines"],
    queryFn: catalogueAdmin.listerDomaines,
    enabled,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });

  const stats = useQuery({
    queryKey: ["catalogue-admin-stats"],
    queryFn: catalogueAdmin.getStats,
    enabled,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });

  const creer = useMutation({
    mutationFn: catalogueAdmin.creerEssai,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-stats"] });
    },
  });

  const modifier = useMutation({
    mutationFn: ({ id, request }: { id: number; request: catalogueAdmin.ModifierEssaiRequest }) =>
      catalogueAdmin.modifierEssai(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin"] });
    },
  });

  const activer = useMutation({
    mutationFn: catalogueAdmin.activerEssai,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin"] });
    },
  });

  const desactiver = useMutation({
    mutationFn: catalogueAdmin.desactiverEssai,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin"] });
    },
  });

  const supprimer = useMutation({
    mutationFn: catalogueAdmin.supprimerEssai,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin"] });
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-stats"] });
    },
  });

  const creerDomaine = useMutation({
    mutationFn: catalogueAdmin.creerDomaine,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-domaines"] });
    },
  });

  const modifierDomaine = useMutation({
    mutationFn: ({ id, request }: { id: number; request: catalogueAdmin.ModifierDomaineRequest }) =>
      catalogueAdmin.modifierDomaine(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-domaines"] });
    },
  });

  const supprimerDomaine = useMutation({
    mutationFn: catalogueAdmin.supprimerDomaine,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalogue-admin-domaines"] });
    },
  });

  return {
    liste,
    domaines,
    stats,
    creer,
    modifier,
    activer,
    desactiver,
    supprimer,
    creerDomaine,
    modifierDomaine,
    supprimerDomaine,
  };
}
