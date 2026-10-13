import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { getSession } from "@/api/session";

export const Route = createFileRoute("/app/demandes-clients")({
  beforeLoad: () => {
    const role = getSession()?.user.role;
    if (role !== "RESPONSABLE" && role !== "ADMINISTRATEUR") {
      throw redirect({ to: "/app" });
    }
  },
  component: DemandesClientsLayout,
});

function DemandesClientsLayout() {
  return <Outlet />;
}
