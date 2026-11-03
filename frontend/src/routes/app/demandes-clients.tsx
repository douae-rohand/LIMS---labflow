import { createFileRoute, Outlet } from "@tanstack/react-router";
import { exigerPermission } from "@/features/shell/garde";

export const Route = createFileRoute("/app/demandes-clients")({
  beforeLoad: () => {
    exigerPermission("DEMANDES_CONSULTER");
  },
  component: DemandesClientsLayout,
});

function DemandesClientsLayout() {
  return <Outlet />;
}
