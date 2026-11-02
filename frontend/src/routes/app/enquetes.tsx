import { Outlet, createFileRoute } from "@tanstack/react-router";
import { exigerSessionClient } from "@/lib/gardeClient";

/** Espace client « Mes enquêtes » : réservé au rôle CLIENT. */
export const Route = createFileRoute("/app/enquetes")({
  head: () => ({
    meta: [{ title: "Mes enquêtes — LabFlow LIMS" }],
  }),
  beforeLoad: ({ location }) => {
    exigerSessionClient(location);
  },
  component: MesEnquetesLayout,
});

function MesEnquetesLayout() {
  return <Outlet />;
}
