import { Outlet, createFileRoute } from "@tanstack/react-router";
import { exigerSessionClient } from "@/lib/gardeClient";

/** Espace client « Mes rapports » : réservé au rôle CLIENT. */
export const Route = createFileRoute("/app/rapports")({
  head: () => ({
    meta: [{ title: "Mes rapports — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    exigerSessionClient(location);
  },
  component: MesRapportsLayout,
});

function MesRapportsLayout() {
  return <Outlet />;
}
