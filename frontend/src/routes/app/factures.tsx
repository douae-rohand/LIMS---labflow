import { Outlet, createFileRoute } from "@tanstack/react-router";
import { exigerSessionClient } from "@/lib/gardeClient";

/** Espace client « Mes factures » : réservé au rôle CLIENT. */
export const Route = createFileRoute("/app/factures")({
  head: () => ({
    meta: [{ title: "Mes factures — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    exigerSessionClient(location);
  },
  component: MesFacturesLayout,
});

function MesFacturesLayout() {
  return <Outlet />;
}
