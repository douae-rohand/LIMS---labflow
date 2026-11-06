import { createFileRoute, redirect } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { TableauDeBordAdmin } from "@/components/admin/TableauDeBordAdmin";
import { TableauDeBord } from "@/features/super-admin/TableauDeBord";
import { getSession } from "@/api/session";
import { getStats as getStatsDemandes } from "@/api/statsDemandes";
import { getStats as getStatsFactures } from "@/api/facturesAdmin";
import { getAlertes } from "@/api/stockAdmin";
import { listerAudit } from "@/api/audit";
import { listerDemandesLaboratoire } from "@/api/demandes";

export interface TableauDeBordSearch {
  mois?: number | undefined;
}

export const Route = createFileRoute("/app/tableau-de-bord")({
  head: () => ({ meta: [{ title: "Tableau de bord — LabFlow LIMS" }] }),
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    if (
      session.user.role !== "ADMINISTRATEUR" &&
      session.user.role !== "SUPER_ADMINISTRATEUR"
    ) {
      throw redirect({ to: "/app" });
    }
  },
  validateSearch: (search: Record<string, unknown>): TableauDeBordSearch => {
    const mois = typeof search["mois"] === "number" ? search["mois"] : undefined;
    return mois === undefined ? {} : { mois };
  },
  component: TableauDeBordPage,
});

function TableauDeBordPage() {
  if (getSession()?.user.role === "SUPER_ADMINISTRATEUR") {
    return <TableauDeBord />;
  }

  return <TableauDeBordAdminPage />;
}

function TableauDeBordAdminPage() {
  const search = Route.useSearch();
  const mois = search.mois ?? 12;
  const session = getSession();
  const debut = new Date();
  debut.setMonth(debut.getMonth() - mois + 1);
  debut.setDate(1);
  const fin = new Date();

  const demandesStats = useQuery({
    queryKey: ["stats-demandes", debut.toISOString().split("T")[0], fin.toISOString().split("T")[0]],
    queryFn: () => getStatsDemandes(debut.toISOString().split("T")[0], fin.toISOString().split("T")[0]),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const facturesStats = useQuery({
    queryKey: ["factures-admin-stats"],
    queryFn: () => getStatsFactures(),
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });

  const stockAlertes = useQuery({
    queryKey: ["stock-admin-alertes"],
    queryFn: getAlertes,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });

  const auditRecent = useQuery({
    queryKey: ["audit-recent"],
    queryFn: () => listerAudit({ page: 0, size: 5 }),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const demandesEnCours = useQuery({
    queryKey: ["demandes-en-cours"],
    queryFn: () => listerDemandesLaboratoire({ statut: "EN_COURS", size: 1 }),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const prenom = session?.user.nomComplet?.split(" ")[0] ?? "";
  const dateAujourdhui = new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "full",
  }).format(new Date());

  const demandesEnCoursCount = demandesEnCours.data?.totalElements ?? 0;
  const facturesEnRetard = facturesStats.data?.nombreEnRetard ?? 0;

  return (
    <TableauDeBordAdmin
      prenom={prenom}
      dateAujourdhui={dateAujourdhui}
      demandesStats={demandesStats.data}
      facturesStats={facturesStats.data}
      stockAlertes={stockAlertes.data}
      auditRecent={auditRecent.data?.content}
      demandesEnCours={demandesEnCoursCount}
      facturesEnRetard={facturesEnRetard}
    />
  );
}
