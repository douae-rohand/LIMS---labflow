import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

/** Espace client « Mes demandes » : réservé au rôle CLIENT. */
export const Route = createFileRoute("/app/demandes")({
  head: () => ({
    meta: [{ title: "Mes demandes — LabFlow LIMS" }],
  }),
  beforeLoad: () => {
    if (getSession()?.user.role !== "CLIENT") {
      throw redirect({ to: "/app" });
    }
  },
  component: MesDemandesLayout,
});

function MesDemandesLayout() {
  return <Outlet />;
}
